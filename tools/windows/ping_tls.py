"""Local HTTPS for Wi-Fi tests: one private CA per Windows user/clone, server certificates for the Wi-Fi IP.

Uses keytool from the JDK 21 already required by PING; no extra download. Private keys stay in the
DPAPI-protected launcher folder. Only the public CA certificate is ever served to the LAN.
TLS verification is never disabled: checks use this CA explicitly with hostname/IP validation.
"""
from __future__ import annotations

import hashlib
import html
from http.server import BaseHTTPRequestHandler
import ipaddress
import json
import os
from pathlib import Path
import secrets
import ssl
import subprocess as sp
import time
from urllib.parse import urlsplit

import ping_ops as ops

CA_DAYS = 3650
# Apple: TLS server certificates issued after 1 July 2019 need EKU serverAuth and at most 825 days.
# 397 days also respects the stricter public-CA limit, so the same certificate stays valid if policies tighten.
SERVER_DAYS = 397
RENEW_BEFORE_DAYS = 30
CREATE_HIDDEN = 0x08000000


def folder(state: Path) -> Path:
    path = state / 'https'
    path.mkdir(exist_ok=True)  # Inherits the private ACL of the launcher folder.
    return path


def keytool(java: Path) -> Path:
    tool = Path(java).parent / 'keytool.exe'
    if not tool.is_file():
        raise ops.OpsError('Falta keytool en el JDK 21. Revisa JAVA_HOME; no se instaló ninguna herramienta.')
    return tool


def run_keytool(java, args, secrets_env):
    """Passwords travel only through this child's environment (-storepass:env), never argv or logs."""
    env = {k: v for k, v in os.environ.items() if not k.startswith('PING_TLS_')}
    env.update(secrets_env)
    result = sp.run([str(keytool(java))] + [str(a) for a in args], stdin=sp.DEVNULL, stdout=sp.PIPE,
                    stderr=sp.PIPE, env=env, timeout=60, creationflags=CREATE_HIDDEN if os.name == 'nt' else 0)
    if result.returncode:
        raise ops.OpsError('keytool no pudo preparar el certificado local. No se activó HTTPS ni se desactivó su validación.')
    return result


def decode(pem: Path) -> dict:
    # CPython's own X.509 decoder (read-only); keytool output is localized and not parsed.
    return ssl._ssl._test_decode_cert(str(pem))


def fingerprint(der: bytes) -> str:
    digest = hashlib.sha256(der).hexdigest().upper()
    return ' '.join(digest[i:i + 2] for i in range(0, len(digest), 2))


def ca_files(state):
    base = folder(state)
    return base / 'ca.p12', base / 'ping-ca.pem', base / 'ping-ca.cer'


def ensure_ca(state, config, java, *, regenerate=False):
    """Create the local CA once. Reused while its keystore and password exist; never per start."""
    store, pem, der = ca_files(state)
    if store.exists() and not regenerate:
        if not config.get('https_ca_password'):
            raise ops.OpsError('Existe una CA local, pero falta su contraseña cifrada (se retiraron credenciales). '
                               'Usa Configurar → HTTPS → 1 para crear otra; el iPhone tendrá que confiar en la nueva.')
        if not (pem.exists() and der.exists()):
            env = {'PING_TLS_CA': config['https_ca_password']}
            run_keytool(java, ['-exportcert', '-rfc', '-alias', 'ping-ca', '-keystore', store, '-storepass:env', 'PING_TLS_CA', '-file', pem], env)
            run_keytool(java, ['-exportcert', '-alias', 'ping-ca', '-keystore', store, '-storepass:env', 'PING_TLS_CA', '-file', der], env)
        return False
    for path in (store.with_suffix('.p12.new'), pem.with_suffix('.pem.new'), der.with_suffix('.cer.new')):
        path.unlink(missing_ok=True)  # Own unfinished temporaries from an interrupted attempt only.
    password = secrets.token_urlsafe(32)
    env = {'PING_TLS_CA': password}
    host = os.environ.get('COMPUTERNAME', 'PC')[:40]
    new_store = store.with_suffix('.p12.new')
    run_keytool(java, ['-genkeypair', '-alias', 'ping-ca', '-keyalg', 'RSA', '-keysize', '3072', '-sigalg', 'SHA256withRSA',
        '-dname', f'CN=PING CA local {host} {time.strftime("%Y-%m-%d")}, O=PING ensayo local',
        '-ext', 'bc:c=ca:true,pathlen:0', '-ext', 'ku:c=keyCertSign,cRLSign', '-validity', str(CA_DAYS),
        '-keystore', new_store, '-storetype', 'PKCS12', '-storepass:env', 'PING_TLS_CA'], env)
    run_keytool(java, ['-exportcert', '-rfc', '-alias', 'ping-ca', '-keystore', new_store, '-storepass:env', 'PING_TLS_CA', '-file', pem.with_suffix('.pem.new')], env)
    run_keytool(java, ['-exportcert', '-alias', 'ping-ca', '-keystore', new_store, '-storepass:env', 'PING_TLS_CA', '-file', der.with_suffix('.cer.new')], env)
    new_store.replace(store)
    pem.with_suffix('.pem.new').replace(pem)
    der.with_suffix('.cer.new').replace(der)
    config['https_ca_password'] = password
    config.pop('https_server', None)
    return True


def server_needs(state, config, ip, ca_print):
    record = config.get('https_server') or {}
    store = folder(state) / 'server.p12'
    if not store.exists() or not config.get('SERVER_SSL_KEY_STORE_PASSWORD'):
        return 'falta el certificado del servidor'
    if record.get('ip') != ip:
        return 'cambió la IP del Wi-Fi'
    if record.get('ca') != ca_print:
        return 'cambió la CA local'
    if record.get('not_after', 0) - time.time() < RENEW_BEFORE_DAYS * 86400:
        return 'el certificado del servidor caduca pronto'
    return None


def issue_server(state, config, java, ip):
    """Server key + certificate signed by the existing CA; only the server part changes with the IP."""
    base = folder(state)
    store, pem, der = ca_files(state)
    password = config.get('SERVER_SSL_KEY_STORE_PASSWORD') if config.get('https_mode') == 'local' else None
    password = password or secrets.token_urlsafe(32)
    env = {'PING_TLS_CA': config['https_ca_password'], 'PING_TLS_SERVER': password}
    work = base / 'server.p12.new'
    for path in (work, base / 'server.csr.new', base / 'server.pem.new'):
        path.unlink(missing_ok=True)
    run_keytool(java, ['-genkeypair', '-alias', 'ping', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
        '-dname', f'CN={ip}, O=PING ensayo local', '-validity', str(SERVER_DAYS), '-keystore', work,
        '-storetype', 'PKCS12', '-storepass:env', 'PING_TLS_SERVER'], env)
    run_keytool(java, ['-certreq', '-alias', 'ping', '-keystore', work, '-storepass:env', 'PING_TLS_SERVER',
        '-file', base / 'server.csr.new'], env)
    run_keytool(java, ['-gencert', '-alias', 'ping-ca', '-keystore', store, '-storepass:env', 'PING_TLS_CA',
        '-infile', base / 'server.csr.new', '-outfile', base / 'server.pem.new', '-rfc', '-validity', str(SERVER_DAYS),
        '-sigalg', 'SHA256withRSA', '-ext', f'san=ip:{ip}', '-ext', 'eku=serverAuth',
        '-ext', 'ku:c=digitalSignature,keyEncipherment', '-ext', 'bc:c=ca:false'], env)
    run_keytool(java, ['-importcert', '-noprompt', '-alias', 'ping-ca', '-file', pem, '-keystore', work, '-storepass:env', 'PING_TLS_SERVER'], env)
    run_keytool(java, ['-importcert', '-alias', 'ping', '-file', base / 'server.pem.new', '-keystore', work, '-storepass:env', 'PING_TLS_SERVER'], env)
    info = decode(base / 'server.pem.new')
    if ('IP Address', ip) not in info.get('subjectAltName', ()):
        raise ops.OpsError('El certificado generado no incluye la IP del Wi-Fi. No se activó HTTPS.')
    (base / 'server.csr.new').unlink(missing_ok=True)
    work.replace(base / 'server.p12')
    (base / 'server.pem.new').replace(base / 'server.pem')
    config['SERVER_SSL_KEY_STORE_PASSWORD'] = password
    config['https_server'] = {'ip': ip, 'ca': fingerprint(der.read_bytes()),
                              'not_after': ssl.cert_time_to_seconds(info['notAfter']), 'issued': time.time()}


def ensure_local_https(state, config, java, ip):
    """Idempotent: reuses CA and server certificate; reissues only the server part when it no longer fits."""
    if ipaddress.ip_address(ip).is_loopback:
        raise ops.OpsError('HTTPS para el iPhone necesita la IP del Wi-Fi, no localhost.')
    created = ensure_ca(state, config, java)
    store, pem, der = ca_files(state)
    ca_print = fingerprint(der.read_bytes())
    reason = server_needs(state, config, ip, ca_print)
    if reason:
        print('HTTPS local: CA local creada y certificado emitido para ' + ip + '. Prepara el iPhone una vez desde el panel.'
              if created else 'HTTPS local: ' + reason + '. Se emite un certificado de servidor con la CA existente; '
              'el iPhone no necesita confiar otra vez.')
        issue_server(state, config, java, ip)
    config.update(https_mode='local', SERVER_SSL_ENABLED='true', SERVER_SSL_KEY_STORE=(folder(state) / 'server.p12').as_uri(),
                  SERVER_SSL_KEY_STORE_TYPE='PKCS12', SERVER_SSL_KEY_ALIAS='ping', SERVER_SERVLET_SESSION_COOKIE_SECURE='true',
                  https_origin=f"https://{ip}:{config['port']}")
    return {'ca_created': created, 'reissued': reason}


def context(state=None, config=None):
    """Verification ON. Local mode trusts exactly its CA; other modes use the Windows/system store."""
    if state and (config or {}).get('https_mode') == 'local':
        return ssl.create_default_context(cafile=str(ca_files(state)[1]))
    return ssl.create_default_context()


def summary(state, config):
    if config.get('https_mode') != 'local':
        return None
    store, pem, der = ca_files(state)
    if not der.exists():
        return None
    ca = decode(pem)
    server = config.get('https_server') or {}
    return {'ca_fingerprint': fingerprint(der.read_bytes()),
            'ca_until': time.strftime('%d/%m/%Y', time.localtime(ssl.cert_time_to_seconds(ca['notAfter']))),
            'server_ip': server.get('ip'),
            'server_until': time.strftime('%d/%m/%Y', time.localtime(server['not_after'])) if server.get('not_after') else None,
            'days_left': int((server.get('not_after', 0) - time.time()) // 86400)}


SETUP_STYLE = ('*{box-sizing:border-box}body{margin:0;background:#15121c;color:#f7f4ef;font:17px/1.5 -apple-system,system-ui,sans-serif}'
    'main{max-width:640px;margin:auto;padding:24px 16px 48px}h1{font-size:28px;line-height:1.2;margin:8px 0 12px}'
    'p,li{color:#d9d2e3}ol{padding-left:22px}li{margin:14px 0}a.button{display:flex;justify-content:center;min-height:52px;'
    'padding:14px 18px;border-radius:14px;background:#f8e836;color:#0d0b12;font-weight:800;text-decoration:none;margin:16px 0}'
    'a.second{background:transparent;color:#f7f4ef;border:1px solid #7a7088}code{font-size:13px;overflow-wrap:anywhere;color:#f7f4ef}'
    '.eyebrow{color:#c2a6ff;font-weight:700;letter-spacing:.08em;font-size:13px}.note{border-left:3px solid #ffb547;padding-left:12px}')


def setup_handler(state, origin):
    """LAN page over plain HTTP: only the PUBLIC CA certificate and instructions. Nothing private, no redirects elsewhere."""
    store, pem, der = ca_files(state)
    public = der.read_bytes()
    print_text = html.escape(fingerprint(public))
    target = html.escape(origin, quote=True)

    class Setup(BaseHTTPRequestHandler):
        server_version = 'PING-setup'
        sys_version = ''

        def log_message(self, *args):
            pass

        def reply(self, body, mime, extra=()):
            self.send_response(200)
            self.send_header('Content-Type', mime)
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.send_header('Referrer-Policy', 'no-referrer')
            self.send_header('Content-Security-Policy', "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
            for key, value in extra:
                self.send_header(key, value)
            self.end_headers()
            if self.command != 'HEAD':
                self.wfile.write(body)

        def do_HEAD(self):
            self.do_GET()

        def do_GET(self):
            path = urlsplit(self.path).path
            if path == '/ping-ca.cer':
                return self.reply(public, 'application/x-x509-ca-cert',
                                  [('Content-Disposition', 'attachment; filename="PING-CA-local.cer"')])
            if path != '/':
                self.send_error(404)
                return
            body = f'''<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Preparar iPhone para PING</title><style>{SETUP_STYLE}</style><main><p class="eyebrow">PING · PREPARACIÓN ÚNICA</p>
<h1>Prepara este iPhone para abrir PING con HTTPS</h1><p>Solo una vez por iPhone. Después abre PING por HTTPS y usa «Escanear QR».</p>
<ol><li><strong>Descarga el certificado público</strong> de la CA local de este PC y pulsa <em>Permitir</em>.<a class="button" href="/ping-ca.cer">Descargar certificado PING</a></li>
<li><strong>Instálalo:</strong> abre Ajustes → <em>Perfil descargado</em> → Instalar. Si no aparece, mira en General → VPN y gestión de dispositivos. iOS lo borra si pasan 8 minutos sin instalarlo.</li>
<li><strong>Activa la confianza:</strong> Ajustes → General → Información → Ajustes de confianza de certificados → activa «PING CA local».</li>
<li><strong>Abre PING por HTTPS</strong> en Safari:<a class="button second" href="{target}/">{target}</a></li>
<li><strong>Cámara:</strong> en PING pulsa «Escanear QR» y elige <em>Permitir</em>.</li></ol>
<p class="note">Antes de confiar, puedes comparar la huella SHA-256 que muestra el iPhone en los detalles del certificado con la del panel del PC:<br><code>{print_text}</code></p>
<p>Esta página solo entrega el certificado público. Las claves privadas no salen del PC.</p></main></html>'''.encode()
            self.reply(body, 'text/html; charset=utf-8')

    return Setup
