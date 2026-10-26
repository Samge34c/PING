"""Local HTTPS checks with the real JDK keytool. Temporary private folders only; no app, MySQL or network changes."""
import os
from pathlib import Path
import socket
import ssl
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from unittest import mock
import urllib.error
import urllib.request

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import ping_launcher as launch
import ping_tls
import ping_ops as ops


@unittest.skipUnless(os.name == 'nt', 'Windows-native launcher checks')
class LocalHttpsChecks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.java = launch.java_path()

    def config(self):
        return {'DB_URL': 'jdbc:mysql://127.0.0.1:18306/ping_launcher_exclusive', 'DB_USERNAME': 'synthetic',
                'DB_PASSWORD': 'ONLY_SYNTHETIC_aB72!_secret', 'mail_mode': 'local', 'port': 18443,
                'network_ip': '192.168.20.10'}

    def test_ca_is_created_once_and_ip_change_reissues_only_the_server(self):
        with tempfile.TemporaryDirectory(prefix='PING https ñ ') as temp:
            state = launch.private_root(Path(temp) / 'private')
            config = self.config()
            first = ping_tls.ensure_local_https(state, config, self.java, '192.168.20.10')
            ca = (state / 'https' / 'ping-ca.cer').read_bytes()
            server = (state / 'https' / 'server.p12').read_bytes()
            self.assertTrue(first['ca_created'])
            self.assertEqual(config['https_origin'], 'https://192.168.20.10:18443')
            self.assertEqual(launch.access_origin(config, '192.168.20.10'), 'https://192.168.20.10:18443')
            info = ping_tls.decode(state / 'https' / 'server.pem')
            self.assertIn(('IP Address', '192.168.20.10'), info['subjectAltName'])
            lifetime = ssl.cert_time_to_seconds(info['notAfter']) - ssl.cert_time_to_seconds(info['notBefore'])
            self.assertLessEqual(lifetime, 398 * 86400)  # Apple accepts at most 825 days; we stay at 397.
            # Second start: nothing regenerated.
            again = ping_tls.ensure_local_https(state, config, self.java, '192.168.20.10')
            self.assertEqual(again, {'ca_created': False, 'reissued': None})
            self.assertEqual((state / 'https' / 'server.p12').read_bytes(), server)
            # Wi-Fi IP changed: same CA (no new trust on the iPhone), new server certificate for the new IP.
            moved = ping_tls.ensure_local_https(state, config, self.java, '192.168.20.77')
            self.assertEqual(moved['reissued'], 'cambió la IP del Wi-Fi')
            self.assertFalse(moved['ca_created'])
            self.assertEqual((state / 'https' / 'ping-ca.cer').read_bytes(), ca)
            self.assertIn(('IP Address', '192.168.20.77'), ping_tls.decode(state / 'https' / 'server.pem')['subjectAltName'])
            self.assertNotIn(('IP Address', '192.168.20.10'), ping_tls.decode(state / 'https' / 'server.pem')['subjectAltName'])
            # Expiring soon: renewed with the same CA.
            config['https_server']['not_after'] = time.time() + 5 * 86400
            self.assertEqual(ping_tls.ensure_local_https(state, config, self.java, '192.168.20.77')['reissued'],
                             'el certificado del servidor caduca pronto')
            self.assertEqual((state / 'https' / 'ping-ca.cer').read_bytes(), ca)
            # Passwords live only in the (DPAPI) config, never in the public files.
            for public in ('ping-ca.cer', 'ping-ca.pem', 'server.pem'):
                data = (state / 'https' / public).read_bytes()
                self.assertNotIn(b'PRIVATE KEY', data)
                for secret in (config['https_ca_password'], config['SERVER_SSL_KEY_STORE_PASSWORD']):
                    self.assertNotIn(secret.encode(), data)

    def test_missing_ca_password_fails_closed_instead_of_creating_a_silent_new_ca(self):
        with tempfile.TemporaryDirectory() as temp:
            state = launch.private_root(Path(temp) / 'private')
            config = self.config()
            ping_tls.ensure_local_https(state, config, self.java, '192.168.20.10')
            ca = (state / 'https' / 'ping-ca.cer').read_bytes()
            config.pop('https_ca_password')
            with self.assertRaises(ops.OpsError):
                ping_tls.ensure_local_https(state, config, self.java, '192.168.20.10')
            self.assertEqual((state / 'https' / 'ping-ca.cer').read_bytes(), ca)

    def test_loopback_is_never_used_as_the_iphone_address(self):
        # The real verified TLS handshake against PING is exercised by verify_https_windows.py.
        with tempfile.TemporaryDirectory() as temp:
            state = launch.private_root(Path(temp) / 'private')
            with self.assertRaises(ops.OpsError):
                ping_tls.ensure_local_https(state, self.config(), self.java, '127.0.0.1')

    def test_https_mode_defaults_and_explicit_choices(self):
        self.assertEqual(launch.https_mode({}), 'local')
        self.assertEqual(launch.https_mode({'https_mode': 'http'}), 'http')
        self.assertEqual(launch.https_mode({'SERVER_SSL_ENABLED': 'true'}), 'external')
        # Plain configurations keep their previous HTTP origin (the launcher adds HTTPS before this call).
        self.assertEqual(launch.access_origin(self.config(), '192.168.20.10'), 'http://192.168.20.10:18443')

    def test_setup_page_serves_only_the_public_ca(self):
        from http.server import ThreadingHTTPServer
        with tempfile.TemporaryDirectory() as temp:
            state = launch.private_root(Path(temp) / 'private')
            config = self.config()
            ping_tls.ensure_local_https(state, config, self.java, '192.168.20.10')
            server = ThreadingHTTPServer(('127.0.0.1', 0), ping_tls.setup_handler(state, config['https_origin']))
            threading.Thread(target=server.serve_forever, daemon=True).start()
            url = f'http://127.0.0.1:{server.server_port}'
            opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
            try:
                with opener.open(url + '/') as page:
                    body = page.read().decode()
                    self.assertIn('https://192.168.20.10:18443', body)
                    self.assertIn(ping_tls.fingerprint((state / 'https' / 'ping-ca.cer').read_bytes()), body)
                with opener.open(url + '/ping-ca.cer') as certificate:
                    self.assertEqual(certificate.headers['Content-Type'], 'application/x-x509-ca-cert')
                    self.assertEqual(certificate.read(), (state / 'https' / 'ping-ca.cer').read_bytes())
                for private in ('/ca.p12', '/server.p12', '/https/ca.p12', '/../https/ca.p12', '/ping-ca.pem', '/config.dpapi'):
                    with self.assertRaises(urllib.error.HTTPError) as missing:
                        opener.open(url + private)
                    self.assertEqual(missing.exception.code, 404)
                    missing.exception.close()
                for secret in (config['https_ca_password'], config['SERVER_SSL_KEY_STORE_PASSWORD'], config['DB_PASSWORD']):
                    self.assertNotIn(secret, body)
            finally:
                server.shutdown()
                server.server_close()

    def test_keystore_passwords_never_reach_keytool_arguments(self):
        calls = []
        real = subprocess.run
        def spy(args, **kwargs):
            calls.append((args, kwargs.get('env', {})))
            return real(args, **kwargs)
        with tempfile.TemporaryDirectory() as temp, mock.patch.object(ping_tls.sp, 'run', side_effect=spy):
            state = launch.private_root(Path(temp) / 'private')
            config = self.config()
            ping_tls.ensure_local_https(state, config, self.java, '192.168.20.10')
        secrets = (config['https_ca_password'], config['SERVER_SSL_KEY_STORE_PASSWORD'])
        self.assertTrue(calls)
        for args, env in calls:
            self.assertFalse(any(secret in str(a) for a in args for secret in secrets))


if __name__ == '__main__':
    unittest.main()
