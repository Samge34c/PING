#!/bin/sh
# PING en Render: puerto dinámico y conexión MySQL con validación TLS obligatoria de la CA del proveedor.
# Sin secretos en argumentos ni registros. El almacén solo contiene el certificado PÚBLICO de la CA.
set -eu
umask 077

export SERVER_PORT="${PORT:-10000}"

: "${DB_URL:?Falta DB_URL (jdbc:mysql://HOST:PUERTO/BASE)}"
: "${DB_USERNAME:?Falta DB_USERNAME}"
: "${DB_PASSWORD:?Falta DB_PASSWORD}"
: "${PING_PUBLIC_BASE_URL:?Falta PING_PUBLIC_BASE_URL (https://SERVICIO.onrender.com)}"

ca=/tmp/db-ca.pem
if [ -n "${DB_CA_FILE:-}" ] && [ -r "${DB_CA_FILE}" ]; then
  cp "${DB_CA_FILE}" "$ca"
elif [ -n "${DB_CA_PEM:-}" ]; then
  printf '%s\n' "$DB_CA_PEM" > "$ca"
else
  echo "PING: falta la CA de MySQL (DB_CA_FILE o DB_CA_PEM). No se conecta sin validar TLS." >&2
  exit 1
fi
grep -q 'BEGIN CERTIFICATE' "$ca" || { echo "PING: la CA de MySQL no es un certificado PEM." >&2; exit 1; }
rm -f /tmp/db-ca.p12
keytool -importcert -noprompt -alias db-ca -file "$ca" -keystore /tmp/db-ca.p12 -storetype PKCS12 \
  -storepass ping-ca-publica >/dev/null
case "$DB_URL" in *sslMode=*|*trustCertificateKeyStore*) echo "PING: DB_URL no debe fijar sslMode ni almacenes; los añade este arranque." >&2; exit 1;; esac
case "$DB_URL" in *\?*) sep='&';; *) sep='?';; esac
# VERIFY_IDENTITY por defecto (CA + nombre del servidor). VERIFY_CA solo por decisión expresa tras diagnosticar host y CA (ver docs/DEPLOY_RENDER_AIVEN.md); nunca REQUIRED ni DISABLED.
mode="${DB_SSL_MODE:-VERIFY_IDENTITY}"
case "$mode" in VERIFY_IDENTITY|VERIFY_CA) ;; *) echo "PING: DB_SSL_MODE debe ser VERIFY_IDENTITY o VERIFY_CA." >&2; exit 1;; esac
export DB_URL="${DB_URL}${sep}sslMode=${mode}&trustCertificateKeyStoreUrl=file:/tmp/db-ca.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=ping-ca-publica"

exec java -jar /app/ping.jar
