# Uso: python -I docs/design-proposals/entrada-editorial/tools/revisar_aislado.py  (requiere target/classes compilado;
# dependencias desde C:/Users/river/ping-perf-frontend-evidence/cp.txt, como el arranque de Codex). Abre http://127.0.0.1:8117/
# Arranque local aislado para revisar la integración visual (mismo esquema que target/closure-evidence/start-local.py):
# H2 en memoria, buzón SMTP local, sin MySQL/Render/Aiven. Google con credenciales ficticias solo para dibujar el botón.
from pathlib import Path
import json, os, secrets, socket, subprocess

root = Path(__file__).resolve().parents[4]  # raíz del árbol donde vive este archivo
out = root / "target/visual-review/private"  # fuera de Git
out.mkdir(parents=True, exist_ok=True)
for port in (8117, 1047):
    with socket.socket() as check:
        check.bind(("127.0.0.1", port))
identity = {"email": "organizacion@visual.test", "password": secrets.token_urlsafe(24)}
(out / "secret.json").write_text(json.dumps(identity), encoding="utf-8")
env = os.environ.copy()
env.update(PING_BOOTSTRAP_EMAIL=identity["email"], PING_BOOTSTRAP_PASSWORD=identity["password"], PING_OAUTH_ENABLED="true",
           PING_GOOGLE_CLIENT_ID="revision-local-ficticia", PING_GOOGLE_CLIENT_SECRET="revision-local-ficticia",
           PING_DEMO_ENABLED="false", PING_PUBLIC_BASE_URL="http://127.0.0.1:8117", PING_MAIL_FROM="local@visual.test", PYTHONIOENCODING="utf-8")
mail = subprocess.Popen(["C:/Users/river/AppData/Local/Programs/Python/Python314/python.exe", str(root / "tools/dev-mailbox.py"), "--port", "1047", "--output", str(out / "mail")],
                        stdout=(out / "smtp.log").open("w"), stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW, env=env)
# Plantillas y estáticos desde src/main/resources (el estado integrado), clases compiladas por Codex y dependencias.
cp = ";".join([str(root / "src/main/resources"), str(root / "target/classes"), Path("C:/Users/river/ping-perf-frontend-evidence/cp.txt").read_text().strip()])
args = ["C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin/java.exe", "-cp", cp,
        "-Dspring.profiles.active=visual-review", "-Dserver.address=127.0.0.1", "-Dserver.port=8117",
        "-Dspring.datasource.url=jdbc:h2:mem:visual-review;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "-Dspring.datasource.username=sa", "-Dspring.datasource.password=", "-Dspring.jpa.hibernate.ddl-auto=validate",
        "-Dping.public-base-url=http://127.0.0.1:8117", "-Dspring.mail.host=127.0.0.1", "-Dspring.mail.port=1047",
        "-Dspring.mail.properties.mail.smtp.starttls.enable=false", "-Dspring.mail.properties.mail.smtp.starttls.required=false",
        "-Dping.mail.from=local@visual.test", "-Dping.mail.api.provider=none", "-Dping.demo.enabled=false", "com.ping.PingApplication"]
app = subprocess.Popen(args, cwd=root, env=env, stdout=(out / "app.log").open("w"), stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
(out / "pids.json").write_text(json.dumps({"app": app.pid, "smtp": mail.pid}), encoding="utf-8")
print("H2 y SMTP locales; puertos 8117/1047; privado:", out)
