"""Opt-in real Windows HTTPS exercise through the root CMD files. Creates only a NEW private MySQL instance.

Checks local CA reuse, verified TLS on the Wi-Fi IP, the iPhone preparation page, secure cookies, redirects,
mixed content, e-mail links, polling, restart reuse and stop. Optionally leaves an HTTP-mode instance running
(marker file) so a browser can exercise the scanner from a secure localhost origin. No habitual schema,
external mail, certificate-store changes, firewall changes or deleted directory trees.
"""
import hashlib
from http.cookiejar import CookieJar
import json
import os
from pathlib import Path
import re
import secrets
import socket
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import ping_launcher as launch
import ping_tls


def free_pair():
    while True:
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            port = sock.getsockname()[1]
        if port < 65000 and launch.free_port(port) and launch.free_port(port + 1):
            return port


def port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def main():
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    state = launch.private_root(Path(os.environ['LOCALAPPDATA']) / 'PING' / 'launcher-tests' /
                                ('https ' + time.strftime('%Y%m%d-%H%M%S')))
    print('Estado privado del ensayo: ' + str(state), flush=True)
    result = {'state': str(state), 'checks': [], 'started': time.time()}
    database = state / 'mysql-data'
    database.mkdir()
    mysql_home = Path(os.environ['ProgramFiles']) / 'MySQL' / 'MySQL Server 8.4'
    mysqld, mysql = mysql_home / 'bin/mysqld.exe', mysql_home / 'bin/mysql.exe'
    db_port, smtp_port, app_port = port(), port(), free_pair()
    secret = 'Synthetic-' + secrets.token_urlsafe(32)
    general_email, general_password = 'general@ensayo.invalid', 'Ensayo-' + secrets.token_urlsafe(18)
    native_env = os.environ.copy()
    native_env.update(PYTHONUTF8='1', PING_BOOTSTRAP_EMAIL=general_email, PING_BOOTSTRAP_PASSWORD=general_password)
    config = {'DB_URL': f'jdbc:mysql://127.0.0.1:{db_port}/ping_https_exclusive', 'DB_USERNAME': 'root',
              'DB_PASSWORD': secret, 'port': app_port, 'mail_mode': 'local', 'mail_port': smtp_port,
              'mysql': str(mysql), 'PING_DEMO_ENABLED': 'true'}
    launch.select_network(config, interactive=False)
    ip = config['network_ip']
    db_process = None
    secrets_to_hide = [secret, general_password]

    def record(name, value=True, detail=None):
        result['checks'].append({'name': name, 'ok': bool(value), **({'detail': detail} if detail is not None else {})})
        print(('PASS ' if value else 'FAIL ') + name + ('' if detail is None else ' · ' + str(detail)), flush=True)
        assert value, name

    def invoke(command='start'):
        filename = launch.ROOT / ('INICIAR-PING.cmd' if command == 'start' else 'DETENER-PING.cmd')
        shell_text = '"' + os.environ['COMSPEC'] + '" /d /s /c ""' + str(filename) + '" --state-dir "' + str(state) + '" --no-browser --non-interactive"'
        completed = subprocess.run(shell_text, cwd=state, env=native_env, stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=650, creationflags=launch.CREATE_HIDDEN)
        output = completed.stdout.decode('utf-8', errors='replace')
        current = launch.load_config(state)
        for value in secrets_to_hide + [current.get('https_ca_password', '-'), current.get('SERVER_SSL_KEY_STORE_PASSWORD', '-')]:
            if value and value != '-' and value in output:
                record('secret absent from ' + command + ' output', False)
        (state / ('test-' + command + '-' + str(len(result['checks'])) + '.log')).write_text(output, encoding='utf-8')
        if completed.returncode:
            print(output[-2500:], flush=True)
        return completed, output

    def client(tls):
        jar = CookieJar()
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPSHandler(context=tls),
                                             urllib.request.HTTPCookieProcessor(jar), NoRedirect())
        return opener, jar

    def fetch(opener, url, data=None):
        try:
            response = opener.open(url, data=urllib.parse.urlencode(data).encode() if data is not None else None, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        body = response.read().decode('utf-8', errors='replace')
        return response.status if hasattr(response, 'status') else response.code, response.headers, body

    def insecure_references(body):
        # Mixed content or links back to HTTP/localhost in rendered pages (W3C namespace is not a request).
        refs = re.findall(r'(?:src|href|action)\s*=\s*"(http://[^"]*|[^"]*localhost[^"]*|[^"]*127\.0\.0\.1[^"]*)"', body)
        return [r for r in refs if not r.startswith('http://www.w3.org')]

    try:
        initialize = launch.run([mysqld, '--no-defaults', '--initialize-insecure', '--basedir=' + str(mysql_home),
            '--datadir=' + str(database)], timeout=90)
        record('isolated MySQL initialized', initialize.returncode == 0)
        db_process = subprocess.Popen([str(mysqld), '--no-defaults', '--basedir=' + str(mysql_home), '--datadir=' + str(database),
            '--port=' + str(db_port), '--bind-address=127.0.0.1', '--mysqlx=OFF', '--skip-log-bin',
            '--log-error=' + str(state / 'mysql.log')], stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL, creationflags=launch.CREATE_HIDDEN)
        def ready():
            try:
                with socket.create_connection(('127.0.0.1', db_port), 1):
                    return True
            except OSError:
                return False
        launch.wait_until(ready, timeout=35, explanation='Disposable MySQL did not start')
        statement = "CREATE DATABASE ping_https_exclusive CHARACTER SET utf8mb4; ALTER USER 'root'@'localhost' IDENTIFIED BY '" + secret + "';"
        created = launch.run([mysql, '--no-defaults', '--no-login-paths', '--protocol=TCP', '--host=127.0.0.1',
            '--port=' + str(db_port), '--user=root', '--ssl-mode=PREFERRED'], input=statement.encode(), timeout=20)
        record('new isolated database configured (password via stdin)', created.returncode == 0)
        launch.save_config(state, config)

        # 1. First start: local CA + server certificate, HTTPS on the Wi-Fi IP.
        first, output = invoke()
        record('first start through INICIAR-PING.cmd', first.returncode == 0)
        runtime, saved = launch.read_json(state / 'runtime.json'), launch.load_config(state)
        origin = runtime['origin']
        record('origin is HTTPS on the Wi-Fi IP', origin == f'https://{ip}:{app_port}', origin)
        record('PING_PUBLIC_BASE_URL equals the HTTPS origin', saved['PING_PUBLIC_BASE_URL'] == origin)
        files = state / 'https'
        ca_before, server_before = (files / 'ping-ca.cer').read_bytes(), (files / 'server.p12').read_bytes()
        result['ca_fingerprint'] = ping_tls.fingerprint(ca_before)
        info = ping_tls.decode(files / 'server.pem')
        record('server certificate SAN = Wi-Fi IP only', info['subjectAltName'] == (('IP Address', ip),), info['subjectAltName'])
        tls = launch.tls_context(state, saved)
        record('verified HTTPS response with the local CA', launch.ping_response(origin, tls))
        record('Windows store alone does not trust it (verification is real, not skipped)',
               not launch.ping_response(origin, ssl.create_default_context()))
        try:
            with socket.create_connection((ip, app_port), 5) as raw, tls.wrap_socket(raw, server_hostname='192.0.2.1'):
                wrong_host = False
        except ssl.SSLCertVerificationError:
            wrong_host = True
        record('certificate rejected for a different IP', wrong_host)
        with socket.create_connection((ip, app_port), 5) as raw, tls.wrap_socket(raw, server_hostname=ip) as wrapped:
            chain = wrapped.getpeercert()
            record('TLS negotiated', wrapped.version() in ('TLSv1.2', 'TLSv1.3'), wrapped.version())
        status, _, _ = fetch(urllib.request.build_opener(urllib.request.ProxyHandler({})), f'http://{ip}:{app_port}/login')
        record('plain HTTP on the HTTPS port does not serve PING', status == 400, status)

        # 2. iPhone preparation page on the Wi-Fi IP: public certificate only.
        setup = runtime.get('setup_url')
        record('setup page announced', setup == f'http://{ip}:{app_port + 1}/', setup)
        plain = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        status, headers, page = fetch(plain, setup)
        record('setup page reachable on the Wi-Fi IP', status == 200 and origin in page and result['ca_fingerprint'] in page)
        with plain.open(setup + 'ping-ca.cer', timeout=10) as response:
            record('setup page serves exactly the public CA (DER)', response.read() == ca_before and
                   response.headers['Content-Type'] == 'application/x-x509-ca-cert')
        for private in ('ca.p12', 'server.p12', 'https/ca.p12', 'config.dpapi', 'ping-ca.pem'):
            record('private file not served: ' + private, fetch(plain, setup + private)[0] == 404)
        record('no private key material in setup responses', b'PRIVATE KEY' not in ca_before and 'PRIVATE' not in page)

        # 3. Panel: HTTPS address/QR, preparation QR, certificate status.
        panel = runtime['panel_url']
        status, _, body = fetch(plain, panel)
        record('panel shows HTTPS address and preparation', status == 200 and html_escape(origin) in body and
               'Preparar el iPhone' in body and result['ca_fingerprint'] in body)
        for route in ('qr.svg', 'setup.svg'):
            record('panel ' + route, fetch(plain, panel + route)[0] == 200)
        state_json = json.loads(fetch(plain, panel + 'status')[2])
        record('panel HTTPS status verified', state_json.get('https', {}).get('ok') is True, state_json.get('https'))
        record('access QR regenerated for the HTTPS origin', (state / 'acceso.svg').stat().st_mtime >= runtime['started'] - 5)

        # 4. Application over HTTPS: cookies, redirects, mixed content, e-mail link, polling.
        browser, jar = client(tls)
        status, headers, body = fetch(browser, origin + '/login')
        cookies = headers.get_all('Set-Cookie') or []
        record('login page over HTTPS', status == 200)
        record('session/CSRF cookies are Secure and HttpOnly', cookies and all('Secure' in c for c in cookies) and
               any('HttpOnly' in c for c in cookies), [c.split('=')[0] for c in cookies])
        record('HSTS header present on HTTPS', bool(headers.get('Strict-Transport-Security')))
        record('no mixed content on /login', not insecure_references(body), insecure_references(body))
        csrf = re.search(r'name="_csrf" value="([^"]+)"', body).group(1)
        status, headers, _ = fetch(browser, origin + '/login', {'_csrf': csrf, 'email': general_email, 'password': general_password})
        location = headers.get('Location', '')
        record('login redirect stays on the HTTPS origin', status == 302 and (location.startswith(origin) or location.startswith('/'))
               and 'localhost' not in location and not location.startswith('http://'), location)
        for path in ('/organizer', '/events', '/demo/prepare', '/'):
            status, _, body = fetch(browser, origin + path)
            record('HTTPS page ' + path + ' without mixed content', status == 200 and not insecure_references(body), status)
        _, _, body = fetch(browser, origin + '/demo/prepare')
        csrf = re.search(r'name="_csrf" value="([^"]+)"', body).group(1)
        record('form POST over HTTPS (demo preparation)', fetch(browser, origin + '/demo/prepare', {'_csrf': csrf})[0] == 302)
        _, _, body = fetch(browser, origin + '/events/1')
        link = re.search(r'https?://[^"<\s]+/join\?code=[A-Z0-9]+', body)
        record('event entry link uses the HTTPS origin', bool(link) and link.group(0).startswith(origin + '/join?code='))
        record('event QR image served over HTTPS', fetch(browser, origin + '/events/1/qr.png')[0] == 200)
        result['event_link_host'] = urllib.parse.urlsplit(link.group(0)).netloc
        _, _, body = fetch(browser, origin + '/demo')
        participant = re.search(r'<option value="([0-9]+)"', body).group(1)
        csrf = re.search(r'name="_csrf" value="([^"]+)"', body).group(1)
        fetch(browser, origin + '/demo', {'_csrf': csrf, 'participantId': participant})
        status, headers, body = fetch(browser, origin + '/me/state')
        record('polling /me/state over HTTPS', status == 200 and headers.get_content_type() == 'application/json')
        # Verification e-mail through the local mailbox: link must use the HTTPS origin (token never printed).
        visitor, _ = client(tls)
        _, _, body = fetch(visitor, origin + '/register')
        csrf = re.search(r'name="_csrf" value="([^"]+)"', body).group(1)
        password = 'Ensayo-' + secrets.token_urlsafe(16)
        secrets_to_hide.append(password)
        fetch(visitor, origin + '/register', {'_csrf': csrf, 'email': 'nueva@ensayo.invalid', 'password': password, 'repeat': password})
        mailbox = Path(launch.read_json(state / 'runtime.json').get('mailbox_path', str(state / 'mailbox')))
        launch.wait_until(lambda: any(mailbox.glob('*.eml')), timeout=20, explanation='No verification mail in the local mailbox')
        text = '\n'.join(p.read_text(encoding='utf-8', errors='replace') for p in mailbox.glob('*.eml'))
        links = re.findall(r'https?://[^\s"<>]+/account/verify\?token=', text.replace('=\n', ''))
        record('verification e-mail link uses the HTTPS origin', bool(links) and all(l.startswith(origin + '/account/verify') for l in links), len(links))

        # 5. Second start reuses CA, certificate and the running instance.
        second, _ = invoke()
        again = launch.read_json(state / 'runtime.json')
        record('second start reuses the running PING (same identity)', second.returncode == 0 and again['app'] == runtime['app'])
        record('CA and server certificate reused unchanged', (files / 'ping-ca.cer').read_bytes() == ca_before and
               (files / 'server.p12').read_bytes() == server_before)

        # 6. Stop: own processes only, preparation port closed, isolated MySQL still up.
        stopped, _ = invoke('stop')
        time.sleep(1)
        record('stop through DETENER-PING.cmd', stopped.returncode == 0 and not launch.owned(again['app']) and not launch.owned(again['worker']))
        record('preparation port closed after stop', launch.free_port(app_port + 1, ip))
        record('isolated MySQL untouched by stop', ready())

        # 7. Optional: HTTP-mode instance for browser scanner checks from a secure localhost origin.
        if os.environ.get('PING_HTTPS_SCANNER_PHASE') == '1':
            plain_config = launch.load_config(state)
            for key in list(plain_config):
                if key.startswith('SERVER_SSL_') or key in ('https_origin', 'https_server', 'SERVER_SERVLET_SESSION_COOKIE_SECURE', 'PING_PUBLIC_BASE_URL'):
                    plain_config.pop(key)
            plain_config['https_mode'] = 'http'
            launch.save_config(state, plain_config)
            scanner, _ = invoke()
            record('HTTP-mode instance for scanner checks', scanner.returncode == 0)
            running = launch.read_json(state / 'runtime.json')
            (state / 'scanner.json').write_text(json.dumps({'lan': running['origin'], 'local': f'http://127.0.0.1:{app_port}',
                # Synthetic, per-run credential inside the private (ACL-restricted) test folder only.
                'email': general_email, 'password': general_password}), encoding='utf-8')
            print('SCANNER_READY ' + str(state), flush=True)
            deadline = time.monotonic() + 1800
            while time.monotonic() < deadline and not (state / 'scanner-done').exists():
                time.sleep(1)
            invoke('stop')
            # Back to local HTTPS: the existing CA must be reused (no new trust on the iPhone).
            back = launch.load_config(state)
            back['https_mode'] = 'local'
            launch.save_config(state, back)
            third, _ = invoke()
            record('back to HTTPS reuses the same CA', third.returncode == 0 and (files / 'ping-ca.cer').read_bytes() == ca_before)
            invoke('stop')
        for p in state.glob('*.log'):
            content = p.read_text(encoding='utf-8', errors='replace')
            record('secrets absent from ' + p.name, not any(s in content for s in secrets_to_hide))
    except Exception as error:
        result['failure_type'] = type(error).__name__
        result['failure'] = str(error)[:300]
        print('FAIL ' + type(error).__name__ + ': ' + str(error)[:300], flush=True)
        return 1
    finally:
        try:
            launch.stop(state)
        except Exception:
            pass
        if db_process and db_process.poll() is None:
            client_db = launch.ops.MySQL(launch.ops.db_config(config['DB_URL']), str(mysql), username='root', password=secret)
            try:
                actual = Path(client_db.run('SELECT @@datadir;').decode().strip()).resolve()
                assert actual == database.resolve(), 'Refuse shutdown of any other data directory'
                client_db.run('SHUTDOWN;')
                result['isolated_mysql_shutdown'] = True
            finally:
                client_db.close()
            db_process.wait(timeout=20)
        result['finished'] = time.time()
        launch.atomic_json(state / 'test-results.json', result)
        print('Evidence: ' + str(state / 'test-results.json'), flush=True)
    return 0


def html_escape(value):
    import html
    return html.escape(value, quote=True)


if __name__ == '__main__':
    raise SystemExit(main())
