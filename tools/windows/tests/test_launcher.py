"""Focused launch checks. No application suite or habitual MySQL database."""
import contextlib
import io
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
import time
import unittest
from unittest import mock

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import ping_launcher as launch
import ping_panel
import ping_ops as ops


@unittest.skipUnless(os.name == 'nt', 'Windows-native launcher checks')
class LaunchChecks(unittest.TestCase):
    def config(self):
        return {'DB_URL': 'jdbc:mysql://127.0.0.1:18306/ping_launcher_exclusive',
                'DB_USERNAME': 'synthetic', 'DB_PASSWORD': 'ONLY_SYNTHETIC_aB72!_secret',
                'mail_mode': 'local', 'port': 18081}

    def test_dpapi_roundtrip_and_no_plaintext_at_rest(self):
        with tempfile.TemporaryDirectory(prefix='PING native pruebas ñ ') as temp:
            state = launch.private_root(Path(temp) / 'private')
            launch.save_config(state, self.config())
            self.assertEqual(launch.load_config(state), self.config())
            self.assertNotIn(self.config()['DB_PASSWORD'].encode(), (state / 'config.dpapi').read_bytes())

    def test_dpapi_altered_ciphertext_fails_closed(self):
        encrypted = launch.dpapi(b'synthetic-only')
        with self.assertRaises(ops.OpsError):
            launch.dpapi(encrypted[:-10] + b'corrupted!', decrypt=True)

    def test_config_cannot_be_saved_in_clone(self):
        with self.assertRaises(ops.OpsError):
            launch.private_root(launch.ROOT / 'private-forbidden')

    def test_first_setup_prompts_only_missing_and_saves_for_reuse(self):
        config = self.config()
        del config['DB_PASSWORD']
        with tempfile.TemporaryDirectory() as temp, mock.patch.object(launch, 'networks', return_value=[
                {'id': 'wifi-test', 'ip': '192.168.20.10', 'name': 'Wi-Fi', 'type': 71}]), \
                mock.patch.object(launch.getpass, 'getpass', return_value='secret setup test') as hidden, \
                mock.patch('builtins.input', side_effect=AssertionError('Unexpected routine prompt')):
            state = launch.private_root(Path(temp) / 'private')
            launch.configure(state, config)
            self.assertEqual(hidden.call_count, 1)
            self.assertEqual(launch.load_config(state)['DB_PASSWORD'], 'secret setup test')

    def test_multiple_networks_never_choose_first(self):
        rows = [{'id': 'a', 'ip': '192.168.1.1', 'name': 'one'}, {'id': 'b', 'ip': '10.2.3.4', 'name': 'two'}]
        with mock.patch.object(launch, 'networks', return_value=rows):
            with self.assertRaises(ops.OpsError):
                launch.select_network({}, interactive=False)

    def test_saved_adapter_updates_changed_ip(self):
        config = {'network_id': 'same', 'network_ip': '192.168.1.10'}
        with mock.patch.object(launch, 'networks', return_value=[{'id': 'same', 'ip': '192.168.1.11', 'name': 'Wi-Fi'}]):
            self.assertEqual(launch.select_network(config, interactive=False), '192.168.1.11')
            self.assertEqual(config['network_ip'], '192.168.1.11')

    def test_windows_network_api_returns_usable_addresses(self):
        for row in launch.networks():
            self.assertNotIn(row['ip'], ('127.0.0.1', '0.0.0.0'))
            self.assertTrue(row['id'])

    def test_occupied_port_is_not_free(self):
        with socket.socket() as server:
            server.bind(('127.0.0.1', 0))
            server.listen()
            self.assertFalse(launch.free_port(server.getsockname()[1]))

    def test_closed_smtp_does_not_report_ready(self):
        with socket.socket() as reservation:
            reservation.bind(('127.0.0.1', 0))
            config = self.config() | {'mail_port': reservation.getsockname()[1]}
            self.assertFalse(launch.smtp_probe(config)[0])

    def test_local_smtp_real_handshake_and_reuse(self):
        with tempfile.TemporaryDirectory() as temp:
            with socket.socket() as reservation:
                reservation.bind(('127.0.0.1', 0))
                port = reservation.getsockname()[1]
            process = subprocess.Popen([sys.executable, str(launch.ROOT / 'tools/dev-mailbox.py'),
                '--port', str(port), '--output', temp], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            record = launch.process_identity(process.pid)
            try:
                config = self.config() | {'mail_port': port}
                launch.wait_until(lambda: launch.smtp_probe(config)[0], timeout=8, explanation='test mailbox failed')
                self.assertTrue(launch.smtp_probe(config)[0])
                self.assertTrue(launch.smtp_probe(config)[0])
                self.assertEqual(Path(launch.mailbox_path(config)), Path(temp))
                self.assertEqual(list(Path(temp).glob('*.eml')), [])  # NOOP/EHLO did not send messages
            finally:
                launch.terminate_owned(record)
                process.wait(timeout=5)

    def test_process_identity_rejects_reused_pid(self):
        process = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)'])
        record = launch.process_identity(process.pid)
        try:
            self.assertTrue(launch.owned(record))
            fake = record | {'created': record['created'] + 1}
            self.assertFalse(launch.terminate_owned(fake))
            self.assertIsNone(process.poll())
            self.assertTrue(launch.terminate_owned(record))
            process.wait(timeout=5)
        finally:
            if process.poll() is None:
                process.kill()

    def test_redacted_errors_do_not_contain_secret_or_url_encoding(self):
        from urllib.parse import quote
        config = self.config()
        secret = config['DB_PASSWORD']
        sanitized = launch.redactor(config)(f'error {secret} {quote(secret, safe="")} password=hiddenValue')
        self.assertNotIn(secret, sanitized)
        self.assertNotIn('hiddenValue', sanitized)

    def test_public_origin_never_contains_session_or_private_link(self):
        config = self.config()
        self.assertEqual(launch.access_origin(config, '192.168.1.8'), 'http://192.168.1.8:18081')
        config['PING_PUBLIC_BASE_URL'] = 'https://192.168.1.8/account/verify?token=private'
        with self.assertRaises(ops.OpsError):
            launch.access_origin(config, '192.168.1.8')

    def test_https_is_never_silently_downgraded(self):
        for key in ('SERVER_SSL_ENABLED', 'SERVER_SERVLET_SESSION_COOKIE_SECURE'):
            with self.assertRaises(ops.OpsError):
                launch.access_origin(self.config() | {key: 'true'}, '192.168.1.8')

    def test_java_environment_keeps_secret_out_of_argv_and_local_smtp_only(self):
        config = self.config()
        env = launch.app_environment(config, 'http://192.168.1.8:18081')
        self.assertEqual(env['DB_PASSWORD'], config['DB_PASSWORD'])
        self.assertEqual(env['PING_PUBLIC_BASE_URL'], 'http://192.168.1.8:18081')
        self.assertEqual(env['SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED'], 'false')
        real = launch.app_environment(config | {'mail_mode': 'real'}, 'https://192.168.1.8')
        self.assertNotIn('SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED', real)

    def test_maven_cache_reuses_only_matching_source_and_jar(self):
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            java = launch.java_path()
            jar = launch.ROOT / 'target/ping-0.0.1-SNAPSHOT.jar'
            if not jar.exists():
                self.skipTest('No existing build to validate cache')
            fingerprint = launch.source_fingerprint()
            launch.atomic_json(state / 'build.json', {'source': fingerprint, 'jar': ops.file_hash(jar), 'java': str(java)})
            with mock.patch.object(launch, 'logged_process', side_effect=AssertionError('Unexpected rebuild')):
                self.assertEqual(launch.build(state, self.config(), java), (jar, fingerprint))

    def test_current_root_is_independent_of_working_directory(self):
        original = os.getcwd()
        with tempfile.TemporaryDirectory(prefix='otra carpeta con espacios ') as temp:
            try:
                os.chdir(temp)
                self.assertTrue((launch.ROOT / 'pom.xml').is_file())
                self.assertTrue((launch.HERE / 'ping_ops.py').is_file())
            finally:
                os.chdir(original)

    def test_exact_cmd_entry_supports_a_clone_path_with_spaces(self):
        with tempfile.TemporaryDirectory(prefix='PING clon con espacios ñ ') as temp:
            root = Path(temp)
            target = root / 'tools/windows'
            target.mkdir(parents=True)
            import shutil
            for name in ('INICIAR-PING.cmd', 'DETENER-PING.cmd'):
                shutil.copyfile(launch.ROOT / name, root / name)
            for name in ('Launch-Ping.cmd', 'ping_ops.py', 'ping_launcher.py'):
                shutil.copyfile(launch.HERE / name, target / name)
            text = '"' + os.environ['COMSPEC'] + '" /d /s /c ""' + str(root / 'INICIAR-PING.cmd') + '" --help"'
            result = subprocess.run(text, cwd=os.environ['SystemRoot'], stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, timeout=15)
            self.assertEqual(result.returncode, 0, result.stdout.decode(errors='replace'))
            self.assertIn(b'--configurar', result.stdout)

    def test_panel_is_loopback_only_and_does_not_expose_configuration(self):
        from http.server import ThreadingHTTPServer
        import threading
        import urllib.request
        import urllib.error
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            config = self.config()
            server = ThreadingHTTPServer(('127.0.0.1', 0), ping_panel.handler(state, config,
                {'origin': 'http://192.168.1.8:18081'}))
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            url = 'http://127.0.0.1:' + str(server.server_port)
            opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
            try:
                with opener.open(url) as r:
                    body = r.read().decode()
                    self.assertNotIn(config['DB_PASSWORD'], body)
                    self.assertIn('frame-ancestors', r.headers['Content-Security-Policy'])
                with self.assertRaises(urllib.error.HTTPError) as rejected:
                    opener.open(urllib.request.Request(url, headers={'Host': 'host-ajeno.invalid'}))
                self.assertEqual(rejected.exception.code, 403)
                rejected.exception.close()
                with self.assertRaises(urllib.error.HTTPError) as rejected:
                    opener.open(urllib.request.Request(url + '/action', data=b'action=stop'))
                self.assertEqual(rejected.exception.code, 403)
                rejected.exception.close()
            finally:
                server.shutdown()
                server.server_close()


if __name__ == '__main__':
    unittest.main()
