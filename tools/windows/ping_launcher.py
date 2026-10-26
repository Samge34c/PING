"""Windows launch workflow behind ping_ops/Start-Ping. Standard library, user DPAPI.

Private configuration, logs, process identities and mailbox stay outside the clone.
No changes to application rules, global execution policy, router or shared MySQL.
"""
from __future__ import annotations

import argparse
import ctypes as C
from ctypes import wintypes as W
import email.policy
from email.parser import BytesParser
import getpass
import hashlib
import html
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import smtplib
import socket
import ssl
import subprocess as sp
import sys
import threading
import time
from urllib.parse import urlsplit, quote, parse_qs, urlencode
import urllib.request
import webbrowser
import zipfile

import ping_ops as ops

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
CREATE_HIDDEN = 0x08000000
CREATE_CONSOLE = 0x00000010
SENSITIVE = re.compile(r'PASSWORD|SECRET|TOKEN|CREDENTIAL', re.I)


def windows():
    if os.name != 'nt':
        raise ops.OpsError('El doble clic requiere Windows. No se modificó la configuración.')


def run(args, *, env=None, timeout=20, input=None):
    return sp.run([str(a) for a in args], input=input, stdout=sp.PIPE, stderr=sp.PIPE,
                  env=env, timeout=timeout, creationflags=CREATE_HIDDEN if os.name == 'nt' else 0)


def atomic_json(path, value):
    temp = path.with_suffix('.new')
    temp.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')
    temp.replace(path)


def read_json(path, default=None):
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except FileNotFoundError:
        return {} if default is None else default


def private_root(requested=None):
    windows()
    root = ops.outside_repo(Path(requested) if requested else
        Path(os.environ['LOCALAPPDATA']) / 'PING' / 'launcher' /
        hashlib.sha256(str(ROOT).casefold().encode()).hexdigest()[:16])
    new = not root.exists()
    root.mkdir(parents=True, exist_ok=True)
    if new:
        sid_result = run(['whoami.exe', '/user', '/fo', 'csv', '/nh'])
        import csv
        sid = next(csv.reader(sid_result.stdout.decode(errors='replace').splitlines()))[-1]
        if sid_result.returncode or not re.fullmatch(r'S-1-[0-9-]+', sid):
            raise ops.OpsError('No se pudo identificar al usuario Windows para proteger la configuración.')
        acl = run(['icacls.exe', root, '/inheritance:r', '/grant:r', '*' + sid + ':(OI)(CI)F', '/Q'])
        if acl.returncode:
            raise ops.OpsError('Windows no permitió proteger la carpeta privada. No se guardaron credenciales.')
    return root


class Blob(C.Structure):
    _fields_ = [('size', W.DWORD), ('data', C.POINTER(C.c_ubyte))]


def dpapi(data, *, decrypt=False):
    windows()
    crypt = C.WinDLL('crypt32', use_last_error=True)
    kernel = C.WinDLL('kernel32', use_last_error=True)
    raw = C.create_string_buffer(data)
    source = Blob(len(data), C.cast(raw, C.POINTER(C.c_ubyte)))
    output = Blob()
    function = crypt.CryptUnprotectData if decrypt else crypt.CryptProtectData
    function.argtypes = [C.POINTER(Blob), C.c_void_p, C.c_void_p, C.c_void_p,
                         C.c_void_p, W.DWORD, C.POINTER(Blob)]
    function.restype = W.BOOL
    if not function(C.byref(source), None, None, None, None, 1, C.byref(output)):
        raise ops.OpsError('DPAPI no pudo abrir/proteger la configuración con este usuario Windows. '
                           'Usa Configurar; no copies el archivo desde otro usuario o PC.')
    try:
        return C.string_at(output.data, output.size)
    finally:
        kernel.LocalFree.argtypes = [C.c_void_p]
        kernel.LocalFree(C.cast(output.data, C.c_void_p))


def save_config(state, config):
    data = json.dumps(config).encode()
    protected = dpapi(data)
    if dpapi(protected, decrypt=True) != data:
        raise ops.OpsError('La comprobación de ida y vuelta de DPAPI falló. No se guardó configuración.')
    temporary = state / 'config.dpapi.new'
    temporary.write_bytes(protected)
    temporary.replace(state / 'config.dpapi')


def load_config(state):
    path = state / 'config.dpapi'
    return json.loads(dpapi(path.read_bytes(), decrypt=True)) if path.exists() else {}


class LaunchLock:
    def __init__(self, state):
        kernel = C.WinDLL('kernel32', use_last_error=True)
        self.kernel = kernel
        kernel.CreateMutexW.argtypes = [C.c_void_p, W.BOOL, W.LPCWSTR]
        kernel.CreateMutexW.restype = W.HANDLE
        self.handle = kernel.CreateMutexW(None, False, 'Local\\PING-' +
            hashlib.sha256(str(state).encode()).hexdigest()[:24])
        kernel.WaitForSingleObject.argtypes = [W.HANDLE, W.DWORD]
        code = kernel.WaitForSingleObject(self.handle, 120000)
        if code not in (0, 128):
            raise ops.OpsError('Otro arranque/configuración sigue trabajando. Espera y vuelve a abrir INICIAR-PING.')

    def __enter__(self):
        return self

    def __exit__(self, *unused):
        self.kernel.ReleaseMutex.argtypes = [W.HANDLE]
        self.kernel.CloseHandle.argtypes = [W.HANDLE]
        self.kernel.ReleaseMutex(self.handle)
        self.kernel.CloseHandle(self.handle)


def process_identity(pid):
    """PID + creation FILETIME + executable; no WMI or command-line secrets."""
    kernel = C.WinDLL('kernel32', use_last_error=True)
    kernel.OpenProcess.argtypes = [W.DWORD, W.BOOL, W.DWORD]
    kernel.OpenProcess.restype = W.HANDLE
    handle = kernel.OpenProcess(0x1000, False, pid)
    if not handle:
        return None
    try:
        times = [W.FILETIME() for _ in range(4)]
        kernel.GetProcessTimes.argtypes = [W.HANDLE] + [C.POINTER(W.FILETIME)] * 4
        if not kernel.GetProcessTimes(handle, *[C.byref(t) for t in times]):
            return None
        size = W.DWORD(32768)
        name = C.create_unicode_buffer(size.value)
        kernel.QueryFullProcessImageNameW.argtypes = [W.HANDLE, W.DWORD, W.LPWSTR, C.POINTER(W.DWORD)]
        if not kernel.QueryFullProcessImageNameW(handle, 0, name, C.byref(size)):
            return None
        return {'pid': pid, 'created': times[0].dwHighDateTime * 2**32 + times[0].dwLowDateTime,
                'exe': name.value.casefold()}
    finally:
        kernel.CloseHandle.argtypes = [W.HANDLE]
        kernel.CloseHandle(handle)


def owned(record):
    return bool(record and process_identity(record['pid']) == record)


def terminate_owned(record):
    if not record:
        return False
    kernel = C.WinDLL('kernel32', use_last_error=True)
    kernel.OpenProcess.argtypes = [W.DWORD, W.BOOL, W.DWORD]
    kernel.OpenProcess.restype = W.HANDLE
    # Keep the handle open while checking creation time and terminating: prevents PID reuse races.
    handle = kernel.OpenProcess(0x1001, False, record['pid'])
    if not handle:
        return False
    try:
        if not owned(record):
            return False
        kernel.TerminateProcess.argtypes = [W.HANDLE, W.UINT]
        return bool(kernel.TerminateProcess(handle, 0))
    finally:
        kernel.CloseHandle.argtypes = [W.HANDLE]
        kernel.CloseHandle(handle)


def networks():
    """IP Helper API works without admin, WMI or PowerShell script policy."""
    class Address(C.Structure):
        _fields_ = [('pointer', C.c_void_p), ('length', C.c_int)]
    class Unicast(C.Structure):
        pass
    Unicast._fields_ = [('alignment', C.c_ulonglong), ('next', C.POINTER(Unicast)), ('address', Address)]
    class Adapter(C.Structure):
        pass
    Adapter._fields_ = [('alignment', C.c_ulonglong), ('next', C.POINTER(Adapter)),
        ('id', C.c_char_p), ('unicast', C.POINTER(Unicast)), ('anycast', C.c_void_p),
        ('multicast', C.c_void_p), ('dns', C.c_void_p), ('suffix', W.LPWSTR),
        ('description', W.LPWSTR), ('name', W.LPWSTR), ('mac', C.c_ubyte * 8),
        ('mac_length', W.DWORD), ('flags', W.DWORD), ('mtu', W.DWORD),
        ('type', W.DWORD), ('status', C.c_int)]
    size = W.ULONG(16384)
    api = C.WinDLL('iphlpapi').GetAdaptersAddresses
    for _ in range(3):
        buffer = C.create_string_buffer(size.value)
        code = api(2, 0, None, buffer, C.byref(size))
        if code != 111:
            break
    if code:
        raise ops.OpsError('Windows no pudo enumerar las redes. Conecta el PC al Wi-Fi y reintenta.')
    results = []
    adapter = C.cast(buffer, C.POINTER(Adapter))
    while adapter:
        a = adapter.contents
        if a.status == 1 and a.type != 24:
            u = a.unicast
            while u:
                address = u.contents.address
                if address.pointer:
                    ip = socket.inet_ntoa(C.string_at(address.pointer + 4, 4))
                    parsed = ipaddress.ip_address(ip)
                    if not parsed.is_loopback and not parsed.is_link_local:
                        results.append({'id': a.id.decode(), 'name': a.name, 'ip': ip, 'type': a.type})
                u = u.contents.next
        adapter = a.next
    return results


def select_network(config, requested=None, interactive=True):
    rows = networks()
    matches = [r for r in rows if r['ip'] == requested] if requested else [
        r for r in rows if r['id'] == config.get('network_id')]
    if len(matches) == 1:
        selected = matches[0]
    elif not requested and len(rows) == 1:
        selected = rows[0]
        print('Única red activa: ' + selected['name'] + ' · ' + selected['ip'])
    else:
        if not rows:
            raise ops.OpsError('No hay una IPv4 activa para el iPhone. Conecta ambos equipos al mismo Wi-Fi.')
        if not interactive:
            raise ops.OpsError('Hay varias redes o cambió la conexión guardada. Abre Configurar y elige el Wi-Fi.')
        print('Elige la conexión que comparte Wi-Fi con el iPhone:')
        for i, row in enumerate(rows, 1):
            print(f"  {i}. {row['name']} · {row['ip']}")
        choice = input('Número: ').strip()
        if not choice.isdigit() or not 1 <= int(choice) <= len(rows):
            raise ops.OpsError('No se eligió una red válida. Ninguna fue seleccionada automáticamente.')
        selected = rows[int(choice) - 1]
    config.update(network_id=selected['id'], network_ip=selected['ip'], network_name=selected['name'])
    return selected['ip']


def ask(prompt, default='', *, hidden=False):
    question = prompt + (f' [{default}]' if default and not hidden else '') + ': '
    value = getpass.getpass(question) if hidden else input(question).strip()
    return value or default


def mysql_greeting(host='127.0.0.1', port=3306):
    try:
        with socket.create_connection((host, port), 2) as sock:
            sock.settimeout(2)
            packet = sock.recv(5)
            return len(packet) == 5 and packet[4] == 10
    except OSError:
        return False


def mailbox_path(config):
    """Only this loopback helper's SMTP extension may identify its private folder."""
    import base64
    try:
        with smtplib.SMTP('127.0.0.1', int(config.get('mail_port', 1025)), timeout=3) as client:
            client.ehlo()
            encoded = client.esmtp_features.get('x-ping-mailbox')
            if encoded:
                return str(ops.outside_repo(Path(base64.urlsafe_b64decode(encoded).decode('utf-8'))))
    except (OSError, ValueError, smtplib.SMTPException):
        pass
    return config.get('mailbox_dir')


def configure(state, config, *, section=None):
    if section == 'all':
        print('¿Qué quieres cambiar? 1 MySQL · 2 Correo · 3 Red · 4 Puerto · 5 HTTPS · 6 Retirar credenciales')
        selected = ask('Número')
        section = {'1': 'db', '2': 'mail', '3': 'network', '4': 'port', '5': 'https', '6': 'forget'}.get(selected)
        if not section:
            raise ops.OpsError('Configuración cancelada; conserva los valores guardados.')
        if section == 'forget':
            if ask('Escribe RETIRAR para borrar solo las credenciales guardadas') == 'RETIRAR':
                (state / 'config.dpapi').unlink(missing_ok=True)
                print('Credenciales retiradas. No se borraron cuentas, datos, mensajes ni MySQL.')
            return None
        for key in {'db': ['DB_URL', 'DB_USERNAME', 'DB_PASSWORD'],
                    'mail': ['mail_mode', 'SPRING_MAIL_HOST', 'SPRING_MAIL_USERNAME', 'SPRING_MAIL_PASSWORD', 'PING_MAIL_FROM'],
                    'network': ['network_id'], 'port': ['port'], 'https': []}[section]:
            config.pop(key, None)
    env_names = ['DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'PING_PUBLIC_BASE_URL',
        'SPRING_MAIL_HOST', 'SPRING_MAIL_PORT', 'SPRING_MAIL_USERNAME', 'SPRING_MAIL_PASSWORD',
        'PING_MAIL_FROM', 'SERVER_SSL_ENABLED', 'SERVER_SSL_KEY_STORE', 'SERVER_SSL_KEY_STORE_PASSWORD',
        'SERVER_SSL_KEY_STORE_TYPE', 'SERVER_SSL_KEY_ALIAS', 'SERVER_SERVLET_SESSION_COOKIE_SECURE',
        'SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH', 'SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE',
        'SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE', 'SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED']
    env_names += ['PING_OAUTH_ENABLED'] + [key for key in os.environ if key.startswith('SPRING_SECURITY_OAUTH2_CLIENT_')]
    for key in env_names:
        if not section and key not in config and os.environ.get(key):
            config[key] = os.environ[key]
    if not config.get('DB_URL'):
        print('Usaremos una base MySQL existente. No se crea otra base ni se prepara la demo.')
        if mysql_greeting() and ask('MySQL local detectado en 3306. Enter para usarlo, O para otro servidor', '').lower() != 'o':
            host, port = '127.0.0.1', '3306'
        else:
            host = ask('Servidor MySQL', '127.0.0.1')
            port = ask('Puerto MySQL', '3306')
        name = ask('Nombre de la base existente')
        config['DB_URL'] = f'jdbc:mysql://{host}:{port}/{ops.database_name(name)}'
    ops.db_config(config['DB_URL'])
    if any(SENSITIVE.search(k) for k, _ in __import__('urllib.parse', fromlist=['parse_qsl']).parse_qsl(urlsplit(config['DB_URL'][5:]).query)):
        raise ops.OpsError('La URL MySQL contiene secretos. Separa la contraseña de la URL; no se guardó.')
    if not config.get('DB_USERNAME'):
        config['DB_USERNAME'] = ask('Usuario MySQL (entrada oculta)', hidden=True)
    if not config.get('DB_PASSWORD'):
        config['DB_PASSWORD'] = ask('Contraseña MySQL (entrada oculta)', hidden=True)
    if not config.get('port'):
        if os.environ.get('SERVER_PORT'):
            config['port'] = int(os.environ['SERVER_PORT'])
        elif section != 'port' and free_port(8081):
            config['port'] = 8081
            print('PING usará el puerto 8081 disponible. Puedes cambiarlo desde el panel.')
        else:
            config['port'] = int(ask('Puerto de PING', '8081'))
    if not 1 <= config['port'] <= 65535:
        raise ops.OpsError('El puerto de PING debe estar entre 1 y 65535.')
    select_network(config)
    if not config.get('mail_mode'):
        if config.get('SPRING_MAIL_HOST') and config.get('PING_MAIL_FROM'):
            config['mail_mode'] = 'local' if config['SPRING_MAIL_HOST'] in ('127.0.0.1', 'localhost') else 'real'
            print('Reutilizaremos el transporte de correo ya configurado; se comprobará sin enviar mensajes.')
        else:
            mode = ask('Correo: 1 buzón LOCAL de ensayo (no sale a Internet), 2 correo real', '1')
            if mode not in ('1', '2'):
                raise ops.OpsError('Elige buzón local o correo real.')
            config['mail_mode'] = 'local' if mode == '1' else 'real'
    if config['mail_mode'] == 'local':
        config.setdefault('mail_port', int(config.get('SPRING_MAIL_PORT', 1025)))
        if not free_port(config['mail_port'], '127.0.0.1'):
            if not smtp_probe(config)[0]:
                config['mail_port'] = int(ask('El puerto del buzón está ocupado por otro servicio. Puerto SMTP local alternativo', '1026'))
            elif not mailbox_path(config):
                config['mailbox_dir'] = str(ops.outside_repo(Path(ask('Carpeta de mensajes del buzón antiguo ya abierto', str(Path.home() / 'PING-dev-mailbox')))))
    else:
        for key, prompt, default in [('SPRING_MAIL_HOST', 'Servidor SMTP', ''),
            ('SPRING_MAIL_PORT', 'Puerto SMTP', '587'), ('PING_MAIL_FROM', 'Remitente autorizado', ''),
            ('SPRING_MAIL_USERNAME', 'Usuario SMTP (entrada oculta)', ''),
            ('SPRING_MAIL_PASSWORD', 'Contraseña SMTP (entrada oculta)', '')]:
            if not config.get(key):
                config[key] = ask(prompt, default, hidden=key.endswith(('USERNAME', 'PASSWORD')))
        config.setdefault('SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH', 'true')
        if config['SPRING_MAIL_PORT'] == '465':
            config.setdefault('SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE', 'true')
            # Implicit TLS is already encrypted; STARTTLS is a different protocol.
            config.setdefault('SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE', 'false')
            config.setdefault('SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED', 'false')
    if section == 'https':
        mode = ask('1 HTTPS local automático para la cámara del iPhone (recomendado), 2 HTTP Wi-Fi con código manual, '
                   '3 HTTPS con un PKCS12 existente', '1')
        if mode in ('1', '2'):
            for key in list(config):
                if key.startswith('SERVER_SSL_') or key in ('https_origin', 'https_server', 'SERVER_SERVLET_SESSION_COOKIE_SECURE', 'PING_PUBLIC_BASE_URL'):
                    config.pop(key)
        if mode == '1':
            config['https_mode'] = 'local'
            if (state / 'https' / 'ca.p12').exists() and config.get('https_ca_password'):
                if ask('Ya existe la CA local que confía el iPhone. Escribe NUEVA solo para sustituirla (habrá que confiar otra vez)', '') == 'NUEVA':
                    config['https_regenerate_ca'] = True
            save_config(state, config)
            print('HTTPS local guardado. INICIAR-PING preparará o reutilizará la CA y el certificado de la IP del Wi-Fi.')
            return config
        if mode == '2':
            config['https_mode'] = 'http'
            save_config(state, config)
            print('Elegiste HTTP de ensayo y códigos manuales. La cámara requiere HTTPS: vuelve a Configurar → HTTPS → 1.')
            return config
        if mode != '3':
            raise ops.OpsError('Configuración HTTPS cancelada.')
        config['https_mode'] = 'external'
        print('HTTPS necesita un almacén PKCS12 existente con certificado para la IP/nombre del Wi-Fi. '
              'El iPhone debe instalar y confiar su CA. No se omite la validación de certificados.')
        config['SERVER_SSL_ENABLED'] = 'true'
        config['SERVER_SSL_KEY_STORE'] = Path(ask('Ruta al PKCS12 existente')).expanduser().resolve().as_uri()
        config['SERVER_SSL_KEY_STORE_PASSWORD'] = ask('Contraseña PKCS12', hidden=True)
        config['SERVER_SSL_KEY_STORE_TYPE'] = 'PKCS12'
        config['SERVER_SERVLET_SESSION_COOKIE_SECURE'] = 'true'
        config['https_origin'] = ask('Origen HTTPS que cubre ese certificado (sin ruta ni credenciales)',
                                     f"https://{config['network_ip']}:{config['port']}")
    save_config(state, config)
    print('Configuración guardada y comprobada con DPAPI del usuario Windows actual.')
    return config


def access_origin(config, ip):
    tls = config.get('SERVER_SSL_ENABLED', '').lower() == 'true'
    existing = config.get('https_origin') or config.get('PING_PUBLIC_BASE_URL', '')
    if existing.startswith('https://'):
        p = urlsplit(existing)
        if p.username or p.password or p.query or p.fragment or p.path not in ('', '/'):
            raise ops.OpsError('La base HTTPS debe ser un origen sin ruta, credenciales, query ni fragmento.')
        if p.hostname in ('localhost', '127.0.0.1'):
            raise ops.OpsError('La base HTTPS guardada usa localhost. Configura el nombre/IP accesible desde el iPhone.')
        if ip not in socket.gethostbyname_ex(p.hostname)[2]:
            raise ops.OpsError('El origen HTTPS ya no apunta al Wi-Fi elegido. Reconfigura su certificado/nombre; no se rebaja a HTTP.')
        return existing.rstrip('/')
    if tls or config.get('SERVER_SERVLET_SESSION_COOKIE_SECURE', '').lower() == 'true':
        raise ops.OpsError('Existe TLS/cookie Secure configurada. Abre Configurar → HTTPS y completa su origen; no se desactiva automáticamente.')
    return f"http://{ip}:{config['port']}"


def free_port(port, host='0.0.0.0'):
    with socket.socket() as s:
        if os.name == 'nt':
            s.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
        try:
            s.bind((host, port))
            return True
        except OSError:
            return False


def smtp_probe(config):
    local = config['mail_mode'] == 'local'
    host = '127.0.0.1' if local else config['SPRING_MAIL_HOST']
    port = int(config.get('mail_port', 1025) if local else config['SPRING_MAIL_PORT'])
    try:
        use_ssl = not local and config.get('SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE', 'false').lower() == 'true'
        transport = smtplib.SMTP_SSL(timeout=5, context=ssl.create_default_context()) if use_ssl else smtplib.SMTP(timeout=5)
        with transport as client:
            greeting = client.connect(host, port)[1]
            client.ehlo()
            if local and b'PING DEVELOPMENT MAILBOX' not in greeting.upper():
                return False, 'El puerto SMTP pertenece a otro servicio; no se adopta como buzón PING.'
            if not local and not use_ssl:
                client.starttls(context=ssl.create_default_context())
                client.ehlo()
            if not local and config.get('SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH', 'true') == 'true':
                client.login(config['SPRING_MAIL_USERNAME'], config['SPRING_MAIL_PASSWORD'])
            if client.noop()[0] != 250:
                return False, 'SMTP no respondió correctamente a NOOP.'
        return True, 'SMTP local comprobado · no envía correo externo' if local else 'SMTP/TLS comprobado · no se envió correo de prueba'
    except (OSError, smtplib.SMTPException):
        return False, 'SMTP no disponible o autenticación/certificado rechazados. Revisa Correo en Configurar.'


def database_probe(config):
    client = ops.MySQL(ops.db_config(config['DB_URL']), config.get('mysql'), config.get('ssl_ca'),
                       username=config['DB_USERNAME'], password=config['DB_PASSWORD'])
    try:
        client.run('SELECT 1;', ops.db_config(config['DB_URL'])['database'])
    finally:
        client.close()


def https_mode(config):
    """Local CA by default; explicit HTTP or an existing PKCS12 are kept when chosen."""
    if config.get('https_mode') in ('local', 'http', 'external'):
        return config['https_mode']
    return 'external' if config.get('SERVER_SSL_ENABLED', '').lower() == 'true' else 'local'


def ping_response(origin, tls=None):
    try:
        # Explicitly bypass system proxies for a local origin. TLS verification remains ON
        # (local mode passes a context that trusts exactly the PING CA and checks the IP).
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPSHandler(context=tls or ssl.create_default_context()))
        with opener.open(origin + '/login', timeout=4) as response:
            body = response.read(500000).decode('utf-8', errors='replace')
            return response.status == 200 and ('theme-init' in body) and ('PING' in body or 'Ping' in body)
    except (OSError, ValueError):
        return False


def source_fingerprint():
    digest = hashlib.sha256()
    paths = [ROOT / 'pom.xml', ROOT / 'mvnw.cmd']
    for base in [ROOT / 'src' / 'main', ROOT / '.mvn']:
        paths += sorted(p for p in base.rglob('*') if p.is_file())
    for p in paths:
        digest.update(p.relative_to(ROOT).as_posix().encode())
        digest.update(b'\0')
        digest.update(p.read_bytes())
    return digest.hexdigest()


def java_path():
    import shutil
    choices = [Path(os.environ['JAVA_HOME']) / 'bin' / 'java.exe'] if os.environ.get('JAVA_HOME') else []
    found = shutil.which('java.exe')
    if found:
        choices.append(Path(found))
    choices += sorted(Path(os.environ.get('ProgramFiles', r'C:\Program Files')).glob('Eclipse Adoptium/jdk-21*/bin/java.exe'))
    for p in choices:
        if p.is_file() and (p.parent / 'javac.exe').is_file():
            r = run([p, '-version'])
            if r.returncode == 0 and re.search(rb'version "21[.\-+"]', r.stderr + r.stdout):
                return p
    raise ops.OpsError('Falta el JDK 21 completo. Revisa JAVA_HOME; no se instalaron herramientas.')


def redactor(config):
    from urllib.parse import quote as urlquote
    values = [str(v) for k, v in {**os.environ, **config}.items() if SENSITIVE.search(k) and v]
    variants = set(values + [urlquote(v, safe='') for v in values])
    def clean(line):
        for value in sorted(variants, key=len, reverse=True):
            line = line.replace(value, '[secreto oculto]')
        return re.sub(r'(?i)(password|secret|token)\s*[=:]\s*[^\s,;]+', r'\1=[oculto]', line)
    return clean


def logged_process(args, state, log_name, config, *, env=None):
    process = sp.Popen([str(a) for a in args], cwd=ROOT, env=env, stdin=sp.DEVNULL,
                       stdout=sp.PIPE, stderr=sp.STDOUT, creationflags=CREATE_HIDDEN)
    clean = redactor(config)
    def drain():
        with (state / log_name).open('a', encoding='utf-8') as log:
            for raw in iter(process.stdout.readline, b''):
                log.write(clean(raw.decode('utf-8', errors='replace')))
                log.flush()
        process.stdout.close()
    thread = threading.Thread(target=drain, daemon=True)
    thread.start()
    return process, thread


def build(state, config, java):
    fingerprint = source_fingerprint()
    record = read_json(state / 'build.json')
    jar = ROOT / 'target' / 'ping-0.0.1-SNAPSHOT.jar'
    if jar.is_file() and record.get('source') == fingerprint and record.get('jar') == ops.file_hash(jar) and record.get('java') == str(java):
        print('Compilación vigente comprobada por hashes; se reutiliza.')
        return jar, fingerprint
    print('Preparando la versión actual. Maven compila y empaqueta; no ejecuta la suite de pruebas.')
    environment = {k: v for k, v in os.environ.items() if not SENSITIVE.search(k) and not k.startswith(('DB_', 'SPRING_', 'PING_'))
                   and k not in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS')}
    environment['JAVA_HOME'] = str(java.parent.parent)
    environment['PATH'] = str(java.parent) + os.pathsep + os.environ.get('PATH', '')
    # Some shells set this; cmd would then refuse to run the project's own mvnw.cmd from its folder.
    for name in [k for k in environment if k.casefold() == 'nodefaultcurrentdirectoryinexepath']:
        environment.pop(name)
    # Cmd is used only for this fixed wrapper command; no user value or credential is shell code.
    process, thread = logged_process([os.environ.get('COMSPEC', r'C:\Windows\System32\cmd.exe'),
        '/d', '/c', 'mvnw.cmd -B --no-transfer-progress -Dmaven.test.skip=true clean package'],
        state, 'compilacion.log', config, env=environment)
    try:
        code = process.wait(timeout=600)
    except sp.TimeoutExpired:
        raise ops.OpsError('La compilación supera 10 minutos. Consulta compilacion.log; no se anuncia lista.') from None
    thread.join(5)
    if code or not jar.exists():
        raise ops.OpsError('Maven no pudo compilar. Abre compilacion.log; revisa el primer ERROR y vuelve a iniciar.')
    if source_fingerprint() != fingerprint:
        raise ops.OpsError('Las fuentes cambiaron mientras Maven compilaba. Espera a que termine la edición y reintenta.')
    atomic_json(state / 'build.json', {'source': fingerprint, 'jar': ops.file_hash(jar), 'java': str(java)})
    return jar, fingerprint


def make_qr(state, java, jar, origin, name='acceso.svg'):
    with zipfile.ZipFile(jar) as archive:
        names = [n for n in archive.namelist() if re.fullmatch(r'BOOT-INF/lib/core-[0-9.]+\.jar', n)]
        if len(names) != 1:
            raise ops.OpsError('No se encontró el ZXing declarado por PING para generar el QR local.')
        core = state / 'zxing-core.jar'
        core.write_bytes(archive.read(names[0]))
    result = run([java, '--class-path', core, HERE / 'AccessQr.java', origin], timeout=30)
    if result.returncode or not result.stdout.startswith(b'<svg'):
        raise ops.OpsError('No se pudo generar el QR de acceso con el JDK/ZXing local. Revisa el JDK 21.')
    (state / name).write_bytes(result.stdout)


def setup_address(config):
    """Plain-HTTP page on the Wi-Fi IP that hands out only the public CA certificate."""
    port = int(config.get('setup_port') or int(config['port']) + 1)
    if not 1 <= port <= 65535:
        raise ops.OpsError('El puerto de preparación del iPhone no es válido. Usa Configurar → Puerto.')
    return port, f"http://{config['network_ip']}:{port}/"


def app_environment(config, origin):
    env = os.environ.copy()
    for name in list(env):
        if name.startswith(('DB_', 'SPRING_', 'SERVER_', 'PING_')) or name in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'):
            env.pop(name, None)
    for key, value in config.items():
        if key.startswith(('DB_', 'SPRING_', 'SERVER_', 'PING_')):
            env[key] = str(value)
    env.update(SPRING_PROFILES_ACTIVE='mysql', SERVER_ADDRESS='0.0.0.0', SERVER_PORT=str(config['port']),
        PING_PUBLIC_BASE_URL=origin, SPRING_MAIL_PROPERTIES_MAIL_DEBUG='false')
    # Explicit one-time bootstrap remains possible; never invent or persist that identity.
    for key in ('PING_BOOTSTRAP_EMAIL', 'PING_BOOTSTRAP_PASSWORD'):
        if os.environ.get(key):
            env[key] = os.environ[key]
    if config['mail_mode'] == 'real':
        env['SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_CHECKSERVERIDENTITY'] = 'true'
    if config['mail_mode'] == 'local':
        env.update(SPRING_MAIL_HOST='127.0.0.1', SPRING_MAIL_PORT=str(config.get('mail_port', 1025)),
            PING_MAIL_FROM='ensayo@ping.test', SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH='false',
            SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE='false',
            SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED='false')
        env.pop('SPRING_MAIL_PASSWORD', None)
        env.pop('SPRING_MAIL_USERNAME', None)
    return env


def tls_context(state, config):
    import ping_tls
    return ping_tls.context(state, config)


def wait_until(predicate, *, timeout, explanation):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        if predicate():
            return
        time.sleep(.25)
    raise ops.OpsError(explanation)


def ensure_mysql(config, interactive):
    try:
        database_probe(config)
        return
    except ops.OpsError as error:
        db = ops.db_config(config['DB_URL'])
        with socket.socket() as sock:
            sock.settimeout(3)
            unavailable = sock.connect_ex((db['host'], db['port'])) != 0
        if not unavailable or db['host'] not in ('127.0.0.1', 'localhost'):
            raise error
    # Only offer a local service, never stop/reconfigure shared MySQL.
    result = run(['sc.exe', 'query', 'type=', 'service', 'state=', 'all'])
    names = re.findall(r'(?im)^SERVICE_NAME:\s*(\S*mysql\S*)', result.stdout.decode(errors='replace'))
    if interactive and len(names) == 1 and ask(f'MySQL no responde. ¿Iniciar solo el servicio {names[0]}? s/N', 'N').lower() == 's':
        # UAC is requested only for the fixed service-start action.
        shell = C.WinDLL('shell32', use_last_error=True).ShellExecuteW
        shell.argtypes = [W.HWND, W.LPCWSTR, W.LPCWSTR, W.LPCWSTR, W.LPCWSTR, C.c_int]
        shell.restype = C.c_void_p
        code = shell(None, 'runas', 'sc.exe', 'start ' + names[0], None, 0)
        if not code or code <= 32:
            raise ops.OpsError('Windows no autorizó iniciar MySQL. No se elevó PING ni se cambiaron servicios.')
        def available():
            try:
                with socket.create_connection((db['host'], db['port']), 1):
                    return True
            except OSError:
                return False
        wait_until(available, timeout=30,
            explanation='MySQL no respondió después de iniciar su servicio. Consulta Servicios de Windows.')
        database_probe(config)
    else:
        raise ops.OpsError('MySQL no responde. Inicia su servicio existente y reintenta. No se creó ni cambió ninguna base.')


def start(state, options):
    with LaunchLock(state):
        config = load_config(state)
        if options.configure:
            configure(state, config, section='all')
            return 0
        for arg, key in [('mysql', 'mysql'), ('ssl_ca', 'ssl_ca'), ('port', 'port')]:
            if getattr(options, arg, None):
                config[key] = getattr(options, arg)
        if not config or not all(config.get(k) for k in ('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'port', 'mail_mode')):
            if options.non_interactive:
                raise ops.OpsError('Falta configuración privada. Abre INICIAR-PING una vez para completarla.')
            config = configure(state, config)
        ip = select_network(config, options.lan_ip, not options.non_interactive)
        java = java_path()
        if https_mode(config) == 'local':
            import ping_tls
            if config.pop('https_regenerate_ca', False):
                ping_tls.ensure_ca(state, config, java, regenerate=True)
            ping_tls.ensure_local_https(state, config, java, ip)
        origin = access_origin(config, ip)
        config['PING_PUBLIC_BASE_URL'] = origin
        save_config(state, config)
        tls = tls_context(state, config)
        if not (ROOT / 'mvnw.cmd').is_file():
            raise ops.OpsError('Falta mvnw.cmd en el proyecto. Recupera el Maven Wrapper; no se instala Maven global.')
        runtime = read_json(state / 'runtime.json')
        current = source_fingerprint()
        signature = hashlib.sha256(json.dumps(config, sort_keys=True).encode()).hexdigest()
        same = runtime.get('source') == current and runtime.get('config') == signature
        if owned(runtime.get('app')):
            if same:
                if not ping_response(origin, tls):
                    raise ops.OpsError('La instancia registrada existe pero no responde en el acceso configurado. Abre el registro; no se inicia otra.')
                database_probe(config)
                if not smtp_probe(config)[0]:
                    if config['mail_mode'] == 'local' and free_port(config.get('mail_port', 1025), '127.0.0.1'):
                        with (state / 'buzon.log').open('ab') as log:
                            mailbox = sp.Popen([sys.executable, str(ROOT / 'tools/dev-mailbox.py'), '--port',
                                str(config.get('mail_port', 1025)), '--output', str(state / 'mailbox')],
                                stdin=sp.DEVNULL, stdout=log, stderr=log, creationflags=CREATE_HIDDEN)
                        runtime['mailbox'] = process_identity(mailbox.pid)
                        runtime['mailbox_path'] = str(state / 'mailbox')
                        atomic_json(state / 'runtime.json', runtime)
                        wait_until(lambda: smtp_probe(config)[0], timeout=15,
                            explanation='El buzón local no respondió a SMTP. Abre buzon.log.')
                    else:
                        raise ops.OpsError('PING está abierto, pero SMTP no responde. Revisa el buzón o el correo; no se inicia otra aplicación.')
                ensure_panel(state, runtime, options.no_browser)
                print('PING ya estaba abierto y responde. No se inició otra instancia.')
                return 0
            print('Cambió la versión, configuración o IP del Wi-Fi. Se reiniciará únicamente el PING registrado por estas herramientas.')
            terminate_owned(runtime['app'])
            wait_until(lambda: not owned(runtime['app']), timeout=10, explanation='El PING propio no terminó; no se inició otro.')
        if not free_port(config['port']):
            raise ops.OpsError(f"El puerto {config['port']} está ocupado por un proceso no registrado. No se detuvo. "
                               'Cierra esa aplicación o usa Configurar → Puerto.')
        try:
            ensure_mysql(config, not options.non_interactive)
        except ops.OpsError as error:
            if options.non_interactive:
                raise
            print(str(error))
            if ask('¿Corregir usuario/contraseña MySQL ahora? s/N', 'N').lower() != 's':
                raise
            config['DB_USERNAME'] = ask('Usuario MySQL (entrada oculta)', hidden=True)
            config['DB_PASSWORD'] = ask('Contraseña MySQL (entrada oculta)', hidden=True)
            ensure_mysql(config, False)
            save_config(state, config)
            signature = hashlib.sha256(json.dumps(config, sort_keys=True).encode()).hexdigest()
        print('MySQL comprobado mediante SELECT 1 en la base elegida; no se cambió de base.')
        jar, fingerprint = build(state, config, java)
        make_qr(state, java, jar, origin)
        if https_mode(config) == 'local':
            make_qr(state, java, jar, setup_address(config)[1], 'preparar.svg')
        # The worker owns the log pipes for the lifetime of the application, after this console exits.
        runtime.update(source=fingerprint, config=signature, origin=origin, java=str(java),
                       status='starting', started=time.time())
        runtime.pop('error', None)
        atomic_json(state / 'runtime.json', runtime)
        if owned(runtime.get('worker')):
            terminate_owned(runtime['worker'])
        worker = sp.Popen([sys.executable, str(HERE / 'ping_ops.py'), 'serve', '--state-dir', str(state)],
            cwd=ROOT, stdin=sp.DEVNULL, stdout=sp.DEVNULL, stderr=sp.DEVNULL, creationflags=CREATE_HIDDEN)
        runtime['worker'] = process_identity(worker.pid)
        # Worker writes app identity; do not overwrite its state after spawn.
        wait_until(lambda: read_json(state / 'runtime.json').get('status') in ('ready', 'error'), timeout=150,
            explanation='PING no respondió dentro del plazo. Consulta aplicacion.log y vuelve a iniciar; no se anuncia listo.')
        runtime = read_json(state / 'runtime.json')
        if runtime.get('status') != 'ready':
            raise ops.OpsError(runtime.get('error', 'No se completó el arranque. Consulta aplicacion.log.'))
        ensure_panel(state, runtime, options.no_browser)
        print('PING responde; MySQL y SMTP comprobados. Panel: ' + runtime['panel_url'])
        return 0


def ensure_panel(state, runtime, no_browser):
    url = runtime.get('panel_url')
    if not url:
        raise ops.OpsError('Falta el panel local. Reabre INICIAR-PING para recuperar el acceso.')
    try:
        with urllib.request.build_opener(urllib.request.ProxyHandler({})).open(url + 'health', timeout=3) as r:
            if r.read() != b'PING local access':
                raise OSError()
    except OSError:
        raise ops.OpsError('El panel local no responde. Usa DETENER-PING y vuelve a iniciar; no se abre un servicio ajeno.') from None
    if not no_browser:
        if not webbrowser.open(url):
            raise ops.OpsError('El panel responde, pero Windows no abrió un navegador predeterminado. '
                               'Configura uno en Aplicaciones predeterminadas y vuelve a abrir INICIAR-PING.')


def stop(state):
    with LaunchLock(state):
        runtime = read_json(state / 'runtime.json')
        for role in ('app', 'mailbox', 'worker'):
            record = runtime.get(role)
            if record:
                result = terminate_owned(record)
                print(f'{role}: ' + ('detenido' if result else 'ya cerrado o identidad diferente; no se tocó'))
        runtime.update(status='stopped')
        atomic_json(state / 'runtime.json', runtime)
    print('MySQL y los datos se conservan. Solo se detuvieron procesos propios con PID, fecha y ejecutable coincidentes.')
    return 0


def serve(state):
    config = load_config(state)
    runtime = read_json(state / 'runtime.json')
    runtime['worker'] = process_identity(os.getpid())
    atomic_json(state / 'runtime.json', runtime)
    children = []
    try:
        if config['mail_mode'] == 'local':
            mail_port = config.get('mail_port', 1025)
            if free_port(mail_port, '127.0.0.1'):
                mailbox, reader = logged_process([sys.executable, ROOT / 'tools' / 'dev-mailbox.py',
                    '--output', state / 'mailbox', '--port', str(mail_port)], state, 'buzon.log', config)
                runtime['mailbox'] = process_identity(mailbox.pid)
                children.append(('mailbox', mailbox))
                runtime['mailbox_path'] = str(state / 'mailbox')
                wait_until(lambda: smtp_probe(config)[0], timeout=15,
                    explanation='El buzón local no respondió al protocolo SMTP. Revisa buzon.log y el puerto configurado.')
            elif not smtp_probe(config)[0]:
                raise ops.OpsError('El puerto del buzón está ocupado y no respondió como SMTP local. No se detuvo su proceso.')
            elif not runtime.get('mailbox_path'):
                existing_path = mailbox_path(config)
                if not existing_path:
                    raise ops.OpsError('El buzón antiguo responde pero no anuncia su carpeta. Usa Configurar → Correo para indicarla; no se adivinan mensajes.')
                runtime['mailbox_path'] = existing_path
                runtime['mailbox_reused'] = True
        smtp_ok, smtp_message = smtp_probe(config)
        if not smtp_ok:
            raise ops.OpsError(smtp_message)
        runtime['mail_status'] = smtp_message
        runtime['python'] = sys.executable
        runtime.pop('setup_url', None)
        if https_mode(config) == 'local':
            import ping_tls
            port, url = setup_address(config)
            try:
                # Bound to the chosen Wi-Fi IP only; serves the public CA certificate and instructions.
                setup = ThreadingHTTPServer((config['network_ip'], port), ping_tls.setup_handler(state, runtime['origin']))
            except OSError:
                raise ops.OpsError(f'El puerto {port} para preparar el iPhone está ocupado por otro programa. '
                                   'No se detuvo. Cierra esa aplicación o cambia el puerto de PING en Configurar.') from None
            threading.Thread(target=setup.serve_forever, kwargs={'poll_interval': .5}, daemon=True).start()
            runtime['setup_url'] = url
        tls = tls_context(state, config)
        app, reader = logged_process([runtime['java'], '-jar', ROOT / 'target' / 'ping-0.0.1-SNAPSHOT.jar'],
            state, 'aplicacion.log', config, env=app_environment(config, runtime['origin']))
        runtime['app'] = process_identity(app.pid)
        children.append(('app', app))
        atomic_json(state / 'runtime.json', runtime)
        wait_until(lambda: ping_response(runtime['origin'], tls) if app.poll() is None else
                   (_ for _ in ()).throw(ops.OpsError('PING terminó durante el arranque. Abre aplicacion.log y revisa el primer ERROR.')),
                   timeout=110, explanation='PING no respondió con su pantalla de acceso. Revisa aplicacion.log; no se ignoran certificados HTTPS.')
        runtime.update(status='ready', checked=time.time())
        launch_panel(state, config, runtime)
    except Exception as error:
        runtime.update(status='error', error=str(error) if isinstance(error, ops.OpsError) else
            'No se completó el arranque local. Abre el registro y vuelve a Configurar.')
        atomic_json(state / 'runtime.json', runtime)
        for role, process in children:
            if process.poll() is None:
                terminate_owned(runtime.get(role))
        return 1
    return 0


def launch_panel(state, config, runtime):
    from ping_panel import handler
    server = ThreadingHTTPServer(('127.0.0.1', 0), handler(state, config, runtime))
    runtime['panel_url'] = f'http://127.0.0.1:{server.server_port}/'
    atomic_json(state / 'runtime.json', runtime)
    server.serve_forever(poll_interval=.5)


def request_wifi_rule(state):
    runtime = read_json(state / 'runtime.json')
    config = load_config(state)
    java = Path(runtime.get('java', ''))
    if not java.is_file() or not 1 <= int(config['port']) <= 65535:
        raise ops.OpsError('Primero inicia PING para identificar su Java y puerto; no se cambió el firewall.')
    name = 'PING local ' + hashlib.sha256(str(state).encode()).hexdigest()[:12] + ' ' + str(config['port'])
    print('Windows solicitará autorización SOLO para permitir este Java/puerto en la red PRIVADA y subred local. '
          'PING y MySQL continúan sin elevarse. No se abre el router.', flush=True)
    def rule(rule_name, program, port):
        return ['advfirewall', 'firewall', 'add', 'rule', 'name=' + rule_name, 'dir=in', 'action=allow',
                'program=' + str(program), 'protocol=TCP', 'localport=' + str(port),
                'remoteip=LocalSubnet', 'profile=private', 'enable=yes']
    netsh = str(Path(os.environ['SystemRoot']) / 'System32/netsh.exe')
    executable, parameters = netsh, sp.list2cmdline(rule(name, java, config['port']))
    python = Path(runtime.get('python', ''))
    if https_mode(config) == 'local' and python.is_file():
        # Same single authorization: also the plain-HTTP page that hands out only the public CA certificate.
        port = setup_address(config)[0]
        executable = str(Path(os.environ['SystemRoot']) / 'System32/cmd.exe')
        parameters = '/d /c ' + sp.list2cmdline([netsh] + rule(name, java, config['port'])) + ' & ' + \
            sp.list2cmdline([netsh] + rule(name + ' preparar iPhone', python, port))
        print(f'Incluye el puerto {port} de la página que entrega solo el certificado público al iPhone.', flush=True)
    shell = C.WinDLL('shell32', use_last_error=True).ShellExecuteW
    shell.argtypes = [W.HWND, W.LPCWSTR, W.LPCWSTR, W.LPCWSTR, W.LPCWSTR, C.c_int]
    shell.restype = C.c_void_p
    result = shell(None, 'runas', executable, parameters, None, 0)
    if not result or result <= 32:
        raise ops.OpsError('Windows no autorizó la regla. Puedes seguir usando PING en el PC; no se elevó la aplicación.')
    print('Solicitud enviada a Windows. Comprueba el acceso desde el iPhone; no se afirma que el teléfono ya pueda conectar.')
    return 0


def trust_on_this_pc(state):
    """Optional: add only the public PING CA to the CURRENT USER's root store. Windows asks for confirmation."""
    import ping_tls
    certificate = ping_tls.ca_files(state)[2]
    if not certificate.is_file():
        raise ops.OpsError('Aún no existe la CA local. Inicia PING con HTTPS local primero.')
    print('Windows mostrará una advertencia de seguridad y pedirá confirmar la CA local de PING para este usuario. '
          'Comprueba que la huella coincide con la del panel. No requiere administrador ni afecta al iPhone.', flush=True)
    result = sp.run([str(Path(os.environ['SystemRoot']) / 'System32/certutil.exe'), '-user', '-addstore', 'Root', str(certificate)],
                    stdin=sp.DEVNULL, stdout=sp.PIPE, stderr=sp.PIPE, timeout=300)
    if result.returncode:
        raise ops.OpsError('Windows no añadió la CA (cancelado o rechazado). PING sigue funcionando; el iPhone no cambia.')
    print('CA local añadida para este usuario de Windows. Reabre «Abrir PING» en el navegador del PC.')
    return 0


def main(argv=None):
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    parser = argparse.ArgumentParser(description='PING · acceso local con doble clic')
    parser.add_argument('command', choices=['start', 'stop', 'configure', 'forget', 'serve', 'firewall', 'trust-pc'])
    parser.add_argument('--state-dir', help=argparse.SUPPRESS)
    parser.add_argument('--no-browser', action='store_true')
    parser.add_argument('--non-interactive', action='store_true')
    parser.add_argument('--configurar', dest='configure', action='store_true')
    parser.add_argument('--port', type=int)
    parser.add_argument('--lan-ip')
    parser.add_argument('--mysql')
    parser.add_argument('--ssl-ca')
    args = parser.parse_args(argv)
    state = None
    try:
        state = private_root(args.state_dir)
        if args.command == 'start':
            return start(state, args)
        if args.command == 'stop':
            return stop(state)
        if args.command == 'serve':
            return serve(state)
        if args.command == 'firewall':
            return request_wifi_rule(state)
        if args.command == 'trust-pc':
            return trust_on_this_pc(state)
        with LaunchLock(state):
            if args.command == 'forget':
                (state / 'config.dpapi').unlink(missing_ok=True)
                print('Credenciales retiradas. Datos y cuentas conservados.')
            else:
                configure(state, load_config(state), section='all')
                print('Vuelve a abrir INICIAR-PING para aplicar los cambios. No se reinició una sesión en uso.')
        return 0
    except Exception as error:
        message = str(error) if isinstance(error, ops.OpsError) else 'No se completó la configuración/operación. Revisa los datos y permisos del usuario Windows.'
        print('\nPING no está listo: ' + message, flush=True)
        if state:
            with (state / 'arranque.log').open('a', encoding='utf-8') as log:
                log.write(time.strftime('%Y-%m-%d %H:%M:%S') + ' ' + message + '\n')
            print('Registros privados: ' + str(state))
            if not args.non_interactive and sys.stdin.isatty():
                try:
                    action = input('Pulsa C para corregir configuración, R para abrir registros, o Enter para conservar este mensaje: ').strip().upper()
                    if action == 'R':
                        os.startfile(state)
                    elif action == 'C':
                        with LaunchLock(state):
                            try:
                                config = load_config(state)
                            except ops.OpsError:
                                config = {}
                            configure(state, config, section='all')
                        print('Configuración actualizada. Vuelve a abrir INICIAR-PING.cmd.')
                except (EOFError, OSError):
                    pass
        return 1
    finally:
        if os.environ.get('PING_KEEP_CONSOLE') == '1' and sys.stdin.isatty():
            try:
                input('Pulsa Enter para cerrar esta ventana. ')
            except (EOFError, OSError):
                pass


if __name__ == '__main__':
    raise SystemExit(main())
