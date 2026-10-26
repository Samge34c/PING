"""Opt-in real Windows launch exercise. Creates only a NEW private MySQL instance.
No habitual schema, application tests, external mail, credentials in arguments, or cleanup trees.
"""
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import sys
import time
import urllib.request

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import ping_launcher as launch


def port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def main():
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    state = launch.private_root(Path(os.environ['LOCALAPPDATA']) / 'PING' / 'launcher-tests' /
                                ('prueba con espacios ' + time.strftime('%Y%m%d-%H%M%S')))
    print('Estado privado del ensayo: ' + str(state), flush=True)
    result = {'state': str(state), 'checks': [], 'started': time.time()}
    database = state / 'mysql-data'
    database.mkdir()
    mysql_home = Path(os.environ['ProgramFiles']) / 'MySQL' / 'MySQL Server 8.4'
    mysqld = mysql_home / 'bin/mysqld.exe'
    mysql = mysql_home / 'bin/mysql.exe'
    db_port, smtp_port, app_port = port(), port(), port()
    db_process = mailbox = None
    native_env = os.environ.copy()
    native_env['PYTHONUTF8'] = '1'
    secret = 'Synthetic-' + secrets.token_urlsafe(32)
    config = {'DB_URL': f'jdbc:mysql://127.0.0.1:{db_port}/ping_launcher_exclusive',
              'DB_USERNAME': 'root', 'DB_PASSWORD': secret, 'port': app_port,
              'mail_mode': 'local', 'mail_port': smtp_port, 'mysql': str(mysql)}
    launch.select_network(config)

    def record(name, value=True):
        assert value, name
        result['checks'].append({'name': name, 'ok': True})
        print('PASS ' + name, flush=True)

    def invoke(command='start'):
        # Exercise the exact root CMD entry, including another current directory and a spaced state path.
        filename = launch.ROOT / ('INICIAR-PING.cmd' if command == 'start' else 'DETENER-PING.cmd')
        # cmd.exe /s /c requires its own enclosing quote pair, not CRT backslash quoting.
        shell_text = '"' + os.environ['COMSPEC'] + '" /d /s /c ""' + str(filename) + '" --state-dir "' + str(state) + '" --no-browser --non-interactive"'
        completed = subprocess.run(shell_text, cwd=state,
            env=native_env, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            timeout=650, creationflags=launch.CREATE_HIDDEN)
        output = completed.stdout.decode('utf-8', errors='replace')
        record('secret absent from command output', secret not in output)
        (state / ('test-' + command + '-' + str(len(result['checks'])) + '.log')).write_text(output, encoding='utf-8')
        if completed.returncode:
            print(output[-2000:], flush=True)
        return completed

    try:
        # Preserve existing test evidence before Maven's normal clean/package build lifecycle.
        reports = launch.ROOT / 'target/surefire-reports'
        if reports.exists():
            shutil.copytree(reports, state / 'previous-surefire-reports')
        initialize = launch.run([mysqld, '--no-defaults', '--initialize-insecure', '--basedir=' + str(mysql_home),
            '--datadir=' + str(database)], timeout=90)
        if initialize.returncode:
            raise RuntimeError('MySQL isolated initialization failed; no habitual database was used')
        db_process = subprocess.Popen([str(mysqld), '--no-defaults', '--basedir=' + str(mysql_home),
            '--datadir=' + str(database), '--port=' + str(db_port), '--bind-address=127.0.0.1',
            '--mysqlx=OFF', '--skip-log-bin', '--log-error=' + str(state / 'mysql.log')],
            stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            creationflags=launch.CREATE_HIDDEN)
        db_identity = launch.process_identity(db_process.pid)

        def ready():
            try:
                with socket.create_connection(('127.0.0.1', db_port), 1):
                    return True
            except OSError:
                return False
        launch.wait_until(ready, timeout=35, explanation='Disposable MySQL did not start')
        statement = "CREATE DATABASE ping_launcher_exclusive CHARACTER SET utf8mb4; ALTER USER 'root'@'localhost' IDENTIFIED BY '" + secret + "';"
        created = launch.run([mysql, '--no-defaults', '--no-login-paths', '--protocol=TCP',
            '--host=127.0.0.1', '--port=' + str(db_port), '--user=root', '--ssl-mode=PREFERRED'],
            input=statement.encode(), timeout=20)
        record('new isolated database configured (password via stdin)', created.returncode == 0)
        bad = config | {'DB_PASSWORD': secret + 'wrong'}
        launch.save_config(state, bad)
        failed = invoke()
        record('wrong password fails without exposing secret', failed.returncode != 0 and b'1045' in failed.stdout)
        launch.save_config(state, config)
        with socket.socket() as occupied:
            occupied.bind(('0.0.0.0', app_port))
            occupied.listen()
            failed = invoke()
            record('occupied port rejected without killing its owner', failed.returncode != 0 and b'ocupado' in failed.stdout and occupied.fileno() >= 0)
        # Existing mailbox fixture: the launcher must recognize SMTP and never stop an unregistered server.
        mailbox = subprocess.Popen([sys.executable, str(launch.ROOT / 'tools/dev-mailbox.py'), '--port',
            str(smtp_port), '--output', str(state / 'existing-mailbox')], stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL, creationflags=launch.CREATE_HIDDEN)
        mail_identity = launch.process_identity(mailbox.pid)
        launch.wait_until(lambda: launch.smtp_probe(config)[0], timeout=10, explanation='Existing mailbox failed')
        first = invoke()
        record('root CMD first real startup, other folder, spaced path', first.returncode == 0)
        running = launch.read_json(state / 'runtime.json')
        record('public response validated on real Wi-Fi origin', launch.ping_response(running['origin'], launch.tls_context(state, launch.load_config(state))))
        result['panel_url'] = running['panel_url']
        result['mobile_origin'] = running['origin']
        result['app_pid'] = running['app']['pid']
        for route in ('', 'health', 'qr.svg', 'status', 'mail'):
            with urllib.request.build_opener(urllib.request.ProxyHandler({})).open(running['panel_url'] + route) as response:
                record('panel HTTP 200 ' + (route or '/'), response.status == 200)
        second = invoke()
        again = launch.read_json(state / 'runtime.json')
        record('already running: same app PID, no duplicate', second.returncode == 0 and running['app'] == again['app'])
        build = launch.read_json(state / 'build.json')
        record('compiled cache tied to sources and jar', build['source'] == launch.source_fingerprint())
        for p in state.glob('*.log'):
            record('secret absent from ' + p.name, secret not in p.read_text(encoding='utf-8', errors='replace'))
        launch.atomic_json(state / 'test-progress.json', result)
        # Leave the panel briefly available for a screenshot; a marker lets the operator release it.
        print('PANEL_CAPTURE_READY ' + running['panel_url'], flush=True)
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline and not (state / 'capture-done').exists():
            time.sleep(.5)
        record('stop root CMD', invoke('stop').returncode == 0)
        record('shared/reused mailbox preserved', launch.owned(mail_identity))
        record('MySQL remains available after stop', launch.owned(db_identity))
        launch.terminate_owned(mail_identity)
        mailbox.wait(timeout=5)
        # Next startup must start its own missing mailbox and reuse the compiled build.
        restart = invoke()
        fresh = launch.read_json(state / 'runtime.json')
        record('second startup saved configuration and missing mailbox', restart.returncode == 0 and launch.owned(fresh.get('mailbox')))
        record('build cache unchanged', launch.read_json(state / 'build.json') == build)
        record('stop owns only tracked processes', invoke('stop').returncode == 0)
        time.sleep(.5)
        record('tracked app and mailbox stopped', not launch.owned(fresh.get('app')) and not launch.owned(fresh.get('mailbox')))
    except Exception as error:
        result['failure_type'] = type(error).__name__
        print('FAIL ' + type(error).__name__ + '; private records retained, no secrets printed', flush=True)
        return 1
    finally:
        try:
            launch.stop(state)
        except Exception:
            pass
        if mailbox and mailbox.poll() is None:
            launch.terminate_owned(mail_identity)
        if db_process and db_process.poll() is None:
            # MySQL on Windows has a watchdog/child: killing only Popen.pid leaks its server.
            # Authenticate to our instance and verify its exact NEW datadir before SHUTDOWN.
            client = launch.ops.MySQL(launch.ops.db_config(config['DB_URL']), str(mysql),
                username=config['DB_USERNAME'], password=secret)
            try:
                actual = Path(client.run('SELECT @@datadir;').decode().strip()).resolve()
                assert actual == database.resolve(), 'Refuse shutdown of any other data directory'
                client.run('SHUTDOWN;')
                result['isolated_mysql_shutdown'] = True
            finally:
                client.close()
            db_process.wait(timeout=15)
        result['finished'] = time.time()
        launch.atomic_json(state / 'test-results.json', result)
        print('Evidence: ' + str(state / 'test-results.json'), flush=True)
        # No directory trees are deleted; isolated files are retained outside the repository.
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
