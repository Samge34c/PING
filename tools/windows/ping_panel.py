"""Loopback-only access panel. Never serves the repository or exposes secrets to LAN."""
from http.server import BaseHTTPRequestHandler
import html
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import time
from urllib.parse import parse_qs, urlsplit
from email.parser import BytesParser
from email import policy


STYLE = '''*{box-sizing:border-box}body{margin:0;background:#15121c;color:#f7f4ef;font:16px/1.5 system-ui,sans-serif}main{max-width:1040px;margin:auto;padding:32px 24px}header{display:flex;align-items:center;justify-content:space-between;gap:16px}h1{font-size:36px;letter-spacing:-.04em;margin:20px 0 8px}h2{font-size:22px;margin-top:0}p{color:#bdb5ca}.eyebrow{color:#c2a6ff;font-weight:700;letter-spacing:.08em}.grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin:24px 0}.card{background:#1d1926;border:1px solid #3a3347;border-radius:18px;padding:24px}.phone{display:grid;grid-template-columns:1fr 170px;gap:24px;align-items:center}.phone img{width:170px;height:170px;border-radius:12px}.url{display:block;color:#f7f4ef;font-size:20px;overflow-wrap:anywhere;margin:14px 0}.button,button{display:inline-flex;min-height:48px;padding:12px 18px;border:1px solid #7a7088;border-radius:12px;color:#f7f4ef;background:transparent;text-decoration:none;font:inherit;font-weight:700;align-items:center;cursor:pointer;margin:4px 8px 4px 0}.primary{background:#f8e836;color:#0d0b12;border-color:#f8e836}a{color:#c2a6ff}a:focus-visible,button:focus-visible{outline:3px solid #f8e836;outline-offset:4px}.statuses{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.status{background:#262131;border-radius:12px;padding:16px}.ok{color:#7fd9a8}.bad{color:#ffb547}.note{border-left:3px solid #ffb547;padding-left:16px}.muted{font-size:14px;color:#bdb5ca}li{margin:12px 0}details{padding:14px 0;border-top:1px solid #3a3347}summary{cursor:pointer;min-height:44px;display:flex;align-items:center}form{display:inline}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#262131;padding:20px;border-radius:12px}footer{margin-top:32px;border-top:1px solid #3a3347;padding-top:20px}@media(max-width:680px){main{padding:20px 16px}h1{font-size:30px}.grid,.statuses{grid-template-columns:1fr}.phone{grid-template-columns:1fr}.phone img{width:200px;height:200px}.card{padding:20px}header{align-items:start}}'''

SCRIPT = '''async function refresh(){try{const r=await fetch('/status');if(!r.ok)throw Error();const s=await r.json();for(const k of ['app','db','mail','https']){const e=document.getElementById(k);if(!e)continue;e.textContent=s[k].text;e.className=s[k].ok?'ok':'bad'}document.getElementById('checked').textContent='Última comprobación: '+new Date().toLocaleTimeString()}catch(e){document.getElementById('checked').textContent='El panel no responde. Reabre INICIAR-PING.cmd.'}}refresh();setInterval(refresh,30000);'''


def handler(state, config, runtime):
    import ping_launcher as launch
    import ping_tls
    token = secrets.token_urlsafe(32)
    origin = runtime['origin']
    escaped = html.escape(origin, quote=True)
    tls = launch.tls_context(state, config)
    certificate = ping_tls.summary(state, config)
    setup_url = runtime.get('setup_url')
    mailbox = Path(runtime.get('mailbox_path', str(state / 'mailbox')))
    smtp_checked = [0, (True, runtime.get('mail_status', 'Pendiente'))]

    def document(title, body):
        return ('<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" '
                'content="width=device-width,initial-scale=1"><title>' + html.escape(title) +
                '</title><link rel="stylesheet" href="/style.css"><main>' + body + '</main></html>').encode()

    class Panel(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass  # Requests may refer to private messages. No access log.

        def allowed(self):
            expected = f'127.0.0.1:{self.server.server_port}'
            return self.headers.get('Host') == expected and self.headers.get('Sec-Fetch-Site') != 'cross-site'

        def reply(self, body, mime='text/html; charset=utf-8', status=200):
            self.send_response(status)
            self.send_header('Content-Type', mime)
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.send_header('Referrer-Policy', 'no-referrer')
            self.send_header('Content-Security-Policy', "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'none'")
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self):
            if not self.allowed():
                return self.reply(b'Acceso local solamente', 'text/plain', 403)
            path = urlsplit(self.path).path
            if path == '/health':
                return self.reply(b'PING local access', 'text/plain')
            if path == '/style.css':
                return self.reply(STYLE.encode(), 'text/css')
            if path == '/panel.js':
                return self.reply(SCRIPT.encode(), 'text/javascript')
            if path == '/qr.svg':
                return self.reply((state / 'acceso.svg').read_bytes(), 'image/svg+xml')
            if path == '/setup.svg' and setup_url and (state / 'preparar.svg').is_file():
                return self.reply((state / 'preparar.svg').read_bytes(), 'image/svg+xml')
            if path == '/status':
                app_ok = launch.ping_response(origin, tls)
                try:
                    launch.database_probe(config)
                    db_ok = True
                except Exception:
                    db_ok = False
                if time.monotonic() - smtp_checked[0] > 300:
                    smtp_checked[:] = [time.monotonic(), launch.smtp_probe(config)]
                mail_ok, mail_text = smtp_checked[1]
                data = {'app': {'ok': app_ok, 'text': 'Responde · acceso comprobado' if app_ok else 'No responde · abre el registro'},
                        'db': {'ok': db_ok, 'text': 'Conexión SQL comprobada' if db_ok else 'No disponible · revisa MySQL'},
                        'mail': {'ok': mail_ok, 'text': mail_text}}
                if origin.startswith('https://'):
                    # app_ok already proves a verified TLS handshake (CA + IP); never a skipped check.
                    days = (certificate or {}).get('days_left')
                    data['https'] = {'ok': app_ok and (days is None or days > 0),
                                     'text': ('Certificado verificado con la CA local · ' + (f'{days} días de vigencia' if days is not None else 'vigente'))
                                     if app_ok else 'No verificado · revisa el registro y la IP del Wi-Fi'}
                return self.reply(json.dumps(data).encode(), 'application/json')
            if path == '/mail':
                mailbox = Path(launch.read_json(state / 'runtime.json').get('mailbox_path', str(state / 'mailbox')))
                if config['mail_mode'] != 'local':
                    return self.reply(document('Correo', '<h1>Correo real configurado</h1><p>Consulta tu bandeja habitual.</p><a href="/">Volver</a>'))
                files = sorted(mailbox.glob('*.eml'), key=lambda p: p.stat().st_mtime, reverse=True)[:100] if mailbox.exists() else []
                rows = ''
                for p in files:
                    if not re.fullmatch(r'[a-f0-9]{32}\.eml', p.name):
                        continue
                    message = BytesParser(policy=policy.default).parsebytes(p.read_bytes())
                    rows += '<li><a href="/mail/' + p.stem + '">' + html.escape(str(message['subject'] or 'Mensaje de ensayo')) + '</a> · ' + html.escape(str(message['to'] or '')) + '</li>'
                return self.reply(document('Buzón local · privado', '<a href="/">← Panel</a><h1>Buzón local de ensayo</h1><p>Estos mensajes permanecen en este PC. No llegan al correo externo. No compartas sus enlaces privados.</p><ul>' + (rows or '<li>Aún no hay mensajes en la carpeta registrada.</li>') + '</ul><a class="button" href="/mail">Actualizar</a>'))
            if path.startswith('/mail/'):
                mailbox = Path(launch.read_json(state / 'runtime.json').get('mailbox_path', str(state / 'mailbox')))
                identity = path.rsplit('/', 1)[-1]
                if not re.fullmatch('[a-f0-9]{32}', identity):
                    return self.reply(b'No encontrado', 'text/plain', 404)
                file = mailbox / (identity + '.eml')
                if not file.is_file():
                    return self.reply(b'No encontrado', 'text/plain', 404)
                message = BytesParser(policy=policy.default).parsebytes(file.read_bytes())
                part = message.get_body(preferencelist=('plain',))
                text = part.get_content() if part else 'Mensaje sin texto plano. Abre el archivo .eml desde la carpeta privada.'
                # Links remain private and restricted to this app's verification/recovery origin.
                body = html.escape(text)
                expression = re.escape(escaped) + r'/account/(?:verify|reset)\?token=[A-Za-z0-9_-]+'
                body = re.sub(expression, lambda m: '<a rel="noreferrer" href="' + m[0] + '">' + m[0] + '</a>', body)
                return self.reply(document('Mensaje local privado', '<a href="/mail">← Buzón</a><h1>Mensaje de ensayo</h1><p>No publiques este enlace personal.</p><pre>' + body + '</pre>'))
            if path != '/':
                return self.reply(b'No encontrado', 'text/plain', 404)
            https = origin.startswith('https://')
            camera = ('El PC comprobó HTTPS sin ignorar certificados. Safari necesita además confiar en la CA y aceptar el permiso de cámara.' if https else
                      'Acceso por HTTP: puedes probar PING y usar los códigos manuales. La cámara de Safari necesita HTTPS y un certificado confiado por el iPhone.')
            open_label = 'Abrir PING por HTTPS' if https else 'Abrir PING'
            https_status = '<section class="status"><strong>HTTPS</strong><p id="https">Comprobando…</p></section>' if https else ''
            if certificate and setup_url:
                setup_escaped = html.escape(setup_url, quote=True)
                iphone = f'''<section id="iphone" class="card"><h2>Preparar el iPhone · una sola vez</h2>
<div class="phone"><div><p>Con el iPhone en el mismo Wi-Fi, escanea este QR con la app Cámara o abre en Safari:</p><a class="url" href="{setup_escaped}">{setup_escaped}</a>
<p class="muted">Esa página entrega solo el certificado público de la CA local de este PC. Las claves privadas no salen de la carpeta privada.</p></div><img src="/setup.svg" alt="QR de la página para preparar el iPhone"></div>
<ol><li><strong>Descargar:</strong> en esa página pulsa «Descargar certificado PING» y después <em>Permitir</em>.</li>
<li><strong>Instalar:</strong> Ajustes → <em>Perfil descargado</em> → Instalar (código del iPhone). Si no aparece: General → VPN y gestión de dispositivos. iOS lo descarta si pasan 8 minutos.</li>
<li><strong>Confiar:</strong> Ajustes → General → Información → Ajustes de confianza de certificados → activa «PING CA local». Sin este paso Safari no confía en PING.</li>
<li><strong>Abrir:</strong> escanea el QR de acceso de arriba ({escaped}). Safari debe mostrar la página sin aviso de seguridad.</li>
<li><strong>Cámara:</strong> en PING pulsa «Escanear QR» y elige <em>Permitir</em>.</li></ol>
<p class="muted">Huella SHA-256 de la CA (compárala en el iPhone: perfil → Más detalles):<br><code>{html.escape(certificate['ca_fingerprint'])}</code><br>
CA válida hasta {html.escape(certificate['ca_until'])}. Certificado del servidor para {html.escape(str(certificate['server_ip']))}, válido hasta {html.escape(str(certificate['server_until']))}; se renueva solo, con la misma CA, si cambia la IP o se acerca la fecha.</p>
<details><summary>Si Safari no muestra la cámara</summary><p><strong>Denegaste el permiso:</strong> en Safari, con PING abierto, toca el menú de la página (aA o el icono a la izquierda de la dirección) → Ajustes del sitio web → Cámara → Preguntar o Permitir; recarga y pulsa «Escanear QR». También en Ajustes → Apps → Safari → Cámara.</p>
<p><strong>«La cámara requiere HTTPS»:</strong> abriste la dirección http:// o el certificado no está en confianza. Usa la dirección https:// del QR y revisa el paso 3.</p>
<p><strong>Cámara ocupada o sin imagen:</strong> cierra FaceTime/Cámara, vuelve a PING y pulsa «Escanear QR» otra vez. El código manual sigue disponible siempre.</p>
<p><strong>Cambió la IP del Wi-Fi:</strong> reabre INICIAR-PING. El certificado del servidor se reemite con la misma CA; el iPhone no tiene que repetir la preparación.</p></details>
<details><summary>Abrir PING por HTTPS en este PC</summary><p>El navegador de Windows mostrará un aviso hasta que este usuario confíe en la misma CA local. Es opcional y no afecta al iPhone.</p>{{trust}}</details></section>'''
            else:
                iphone = ''
            mail_link = '<a class="button" href="/mail">Abrir mensajes de ensayo</a><p class="muted">El buzón de ensayo es privado y solo abre en este PC. No envía mensajes a Internet.</p>' if config['mail_mode'] == 'local' else '<p>Correo real configurado. Consulta tu bandeja habitual. No se enviaron mensajes externos de prueba.</p>'
            def action(name, label):
                return '<form method="post" action="/action"><input type="hidden" name="csrf" value="' + token + '"><button name="action" value="' + name + '">' + label + '</button></form>'
            iphone = iphone.replace('{trust}', action('trust-pc', 'Confiar en este PC · Windows pide confirmar'))
            legacy_https = '' if iphone else '''<details><summary>Preparación inicial de HTTPS</summary><p>Necesitas un certificado de servidor que incluya la IP o el nombre local elegido y su clave en un almacén PKCS12. Desde Configurar → HTTPS puedes reutilizarlo; Windows debe confiar en su CA para que la comprobación sea válida.</p><p>En el iPhone instala únicamente el certificado público de la CA de tu entorno local: Ajustes → perfil descargado → instalar; después General → Información → Ajustes de confianza de certificados → confiar en esa CA. Nunca transfieras el PKCS12 ni la clave privada al teléfono. Safari pedirá permiso de cámara al abrir el lector. Este programa no puede instalar el perfil, confiar la CA ni aceptar permisos en tu iPhone.</p><p>Mientras preparas ese paso, HTTP y el código manual siguen disponibles. Cambiar «http» por «https» no crea un certificado.</p></details>'''
            body = f'''<header><span class="eyebrow">PING · ACCESO LOCAL</span><span class="muted">Solo en este PC</span></header>
<h1>Tu PING, a un doble clic.</h1><p>Abre la aplicación o escanea el acceso desde el iPhone. Las cuentas siguen usando su inicio de sesión habitual.</p>
<div class="grid"><section class="card"><h2>En este PC</h2><p>La versión del proyecto actual, sin adoptar una identidad automáticamente.</p><a class="button primary" href="{escaped}">{open_label} →</a><a class="button" href="#prueba">Recorrido de prueba</a></section>
<section class="card phone"><div><h2>Abrir en el celular</h2><p>Conecta el iPhone al mismo Wi-Fi que el PC.{" Primera vez: prepara el iPhone (abajo)." if iphone else ""}</p><a class="url" href="{escaped}">{escaped}</a><p class="muted">El QR contiene únicamente esta dirección pública de acceso, sin sesión ni código privado.</p></div><img src="/qr.svg" alt="QR con la dirección de acceso a PING desde el iPhone"></section></div>
<div class="statuses"><section class="status"><strong>Aplicación</strong><p id="app">Comprobando…</p></section><section class="status"><strong>Base de datos</strong><p id="db">Comprobando…</p></section><section class="status"><strong>Correo</strong><p id="mail">Comprobando…</p></section>{https_status}</div><p class="muted" id="checked">Estado comprobado al arrancar; actualizando…</p>
{iphone}<section class="card"><h2>Correo para la prueba</h2>{mail_link}</section>
<section id="prueba" class="card"><h2>Prueba breve, con tus datos</h2><ol><li><a href="{escaped}/login?next=/organizer">Entra con una cuenta autorizada de organización</a>. Abre tu evento existente y busca <strong>Entrada de participantes / Código y QR</strong>. El panel no inventa un código ni crea una demo.</li><li>En dos sesiones independientes o dos celulares: entra con ese código, crea/verifica tu cuenta si hace falta y elige alias, avatar y gustos. Con buzón local, abre aquí el mensaje de ensayo.</li><li>En Conectar, ambos activan disponibilidad; busca y acepta la propuesta desde las dos sesiones.</li><li>Llega al punto, registra ambas llegadas, intercambia códigos personales y confirma después de hacer la actividad.</li><li>Revisa puntos y Beneficios. Para entregar un canje hace falta otra cuenta autorizada del evento.</li></ol></section>
<section class="card"><h2>Wi-Fi, HTTPS y cámara</h2><p class="note">{camera}</p><details><summary>Si el iPhone no abre la dirección</summary><p>Comprueba el mismo Wi-Fi, desactiva solo para la prueba la VPN que cambie la ruta y evita redes de invitados con aislamiento. Reabre INICIAR-PING si cambió la IP. Un HTTP 200 en este PC no demuestra acceso desde el teléfono.</p><p>Si Windows bloquea conexiones entrantes, el siguiente botón pide autorización únicamente para Java/PING en una red privada y el puerto configurado. No cambia el perfil de la red, no abre el router y no eleva PING.</p>{action('firewall','Permitir Wi-Fi privado · pide autorización Windows')}<p>Si tu Wi-Fi de confianza figura como red pública, cámbialo a privada desde Configuración de Windows antes de probar esta regla.</p></details>{legacy_https}</section>
<footer><h2>Configuración y errores</h2><p>Configuración cifrada por Windows fuera del proyecto. Cambiarla no reinicia una sesión en uso; vuelve a abrir INICIAR-PING para aplicarla.</p>{action('configure','Configurar / retirar credenciales')}{action('logs','Abrir registros')}{action('stop','Detener mi PING')}<p class="muted">Detener conserva MySQL y los datos; solo alcanza procesos registrados por estas herramientas.</p></footer><script src="/panel.js"></script>'''
            self.reply(document('PING · acceso local', body))

        def do_POST(self):
            expected_origin = f'http://127.0.0.1:{self.server.server_port}'
            if not self.allowed() or self.headers.get('Origin') not in (None, expected_origin) or self.path != '/action':
                return self.reply(b'Acceso rechazado', 'text/plain', 403)
            size = int(self.headers.get('Content-Length', '0'))
            if not 0 < size < 2048:
                return self.reply(b'Peticion no valida', 'text/plain', 400)
            data = parse_qs(self.rfile.read(size).decode())
            if not secrets.compare_digest(data.get('csrf', [''])[0], token):
                return self.reply(b'Acceso rechazado', 'text/plain', 403)
            action = data.get('action', [''])[0]
            if action == 'logs':
                os.startfile(state)
                message = 'Se abrió la carpeta privada de registros en este PC.'
            elif action in ('configure', 'stop', 'firewall', 'trust-pc'):
                env = os.environ.copy()
                env['PING_KEEP_CONSOLE'] = '1'
                subprocess.Popen([sys.executable, str(launch.HERE / 'ping_ops.py'), action,
                    '--state-dir', str(state)], cwd=launch.ROOT, env=env,
                    creationflags=launch.CREATE_CONSOLE if action in ('configure', 'trust-pc') else launch.CREATE_HIDDEN)
                message = ('Se abrió la configuración en una ventana de este PC.' if action == 'configure' else
                    'Windows pedirá autorización solo para permitir Java/PING en la red privada.' if action == 'firewall' else
                    'Se abrió una ventana: Windows pedirá confirmar la CA local de PING para este usuario.' if action == 'trust-pc' else
                    'Se está deteniendo el PING registrado. MySQL y los datos se conservan.')
            else:
                return self.reply(b'Accion no valida', 'text/plain', 400)
            self.reply(document('PING · herramientas', '<h1>' + message + '</h1><a href="/">Volver al panel</a>'))
    return Panel
