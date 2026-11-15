<#
  PING · ensayo local con datos FICTICIOS (cierre final, F7).
  Un único procedimiento reproducible: compila el commit actual, crea o reutiliza una MySQL aislada propia
  (127.0.0.1:3322, carpeta %LOCALAPPDATA%\PING\ensayo), arranca PING con la política indicada y prepara el
  escenario demo (Samuel, Julián, Ana, Carlos) más una recompensa de prueba de 10 puntos.

  No usa la base real, Aiven, Render ni la configuración del lanzador. No cambia firewall, certificados ni planes.
  No depende de PID ni de archivos temporales: los procesos se reconocen por su línea de comandos.

  Uso:
    .\tools\windows\Start-PingEnsayo.ps1                  # política 2 (regla actual), solo este PC
    .\tools\windows\Start-PingEnsayo.ps1 -Lan             # además, accesible desde celulares en la misma red (HTTP)
    .\tools\windows\Start-PingEnsayo.ps1 -Lan -Policy 3   # prueba del escaneo único (DEC-073)
    .\tools\windows\Start-PingEnsayo.ps1 -Reset           # borra los datos ficticios del ensayo y empieza de cero
    .\tools\windows\Start-PingEnsayo.ps1 -Stop            # detiene PING y la MySQL del ensayo
#>
[CmdletBinding()]
param(
  [ValidateSet(2,3)][int]$Policy = 2,
  [ValidateRange(1024,65535)][int]$Port = 8095,
  [switch]$Lan,
  [string]$LanIPv4,
  [switch]$Reset,
  [switch]$Stop,
  [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
)
$ErrorActionPreference = 'Stop'
$Repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$State = Join-Path $env:LOCALAPPDATA 'PING\ensayo'
$Data = Join-Path $State 'mysql-data'
$Ini = Join-Path $State 'my.ini'
$Logs = Join-Path $State 'logs'
$DbPort = 3322
$AdminEmail = 'ensayo-admin@ping.test'
$mysql = Join-Path $MySqlBin 'mysql.exe'; $mysqld = Join-Path $MySqlBin 'mysqld.exe'; $mysqladmin = Join-Path $MySqlBin 'mysqladmin.exe'

# Native tools write banners/warnings to stderr; PowerShell 5.1 would turn them into terminating errors under 'Stop'.
# Run them with 'Continue' and decide success by $LASTEXITCODE.
function Run([scriptblock]$b) { $ErrorActionPreference = 'Continue'; & $b }
function Say([string]$t) { Write-Host "PING ensayo | $t" }
function Secret([string]$name) {
  $f = Join-Path $State "$name.secret"
  if (-not (Test-Path $f)) { $v = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 24 | ForEach-Object {[char]$_}); [IO.File]::WriteAllText($f, $v) }
  return [IO.File]::ReadAllText($f).Trim()
}
function Listening([int]$p) { return [bool](Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue) }
function Procs([string]$name, [string]$needle) {
  @(Get-CimInstance Win32_Process -Filter "Name='$name'" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($needle) })
}
function Sql([string]$q, [string]$db = '') {
  $ErrorActionPreference = 'Continue'
  $env:MYSQL_PWD = Secret 'mysql-root'
  $a = @('--no-defaults','-h','127.0.0.1','-P',[string]$DbPort,'-u','root','--batch','--skip-column-names','--default-character-set=utf8mb4'); if ($db) { $a += $db }
  try { $out = $q | & $mysql @a 2>&1 }
  finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
  if ($LASTEXITCODE -ne 0) { throw "MySQL del ensayo: $($out | Out-String)" }
  return $out
}
function StopAll {
  $ErrorActionPreference = 'Continue'
  foreach ($p in (Procs 'java.exe' (Join-Path $State 'ping-ensayo-'))) { Say "deteniendo PING (proceso del ensayo)"; Stop-Process -Id $p.ProcessId -Force }
  if (Listening $DbPort) {
    $env:MYSQL_PWD = Secret 'mysql-root'
    try { & $mysqladmin --no-defaults -h 127.0.0.1 -P $DbPort -u root shutdown 2>$null | Out-Null } finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    $t = [Diagnostics.Stopwatch]::StartNew(); while ((Listening $DbPort) -and $t.Elapsed.TotalSeconds -lt 30) { Start-Sleep -Milliseconds 500 }
    foreach ($p in (Procs 'mysqld.exe' $Ini)) { Stop-Process -Id $p.ProcessId -Force }
    Say 'MySQL del ensayo detenida'
  }
}

if ($Stop) { StopAll; Say 'listo: nada del ensayo queda en marcha.'; exit 0 }

# 0 · Requisitos
foreach ($f in $mysql, $mysqld, $mysqladmin) { if (-not (Test-Path $f)) { throw "No existe $f. Indica -MySqlBin con la carpeta bin de MySQL 8.4." } }
$java = (Get-Command java -ErrorAction Stop).Source
$jv = Run { & $java -version 2>&1 | Out-String }; if ($jv -notmatch 'version "21') { throw "Se requiere Java 21 en el PATH. Encontrado: $jv" }
New-Item -ItemType Directory -Force $State, $Logs | Out-Null
& icacls.exe $State /inheritance:r /grant:r "$($env:USERNAME):(OI)(CI)F" /Q | Out-Null

# 1 · Siempre parte de un estado conocido: detiene lo que hubiera del ensayo (nunca otros procesos)
StopAll
if ($Reset -and (Test-Path $Data)) { Say 'borrando los datos FICTICIOS del ensayo (-Reset)'; Remove-Item -Recurse -Force $Data }
if (Listening $Port) { throw "El puerto $Port está en uso por otro proceso. Libéralo o usa -Port." }

# 2 · Compila el commit actual (o reutiliza el JAR ya compilado de ese mismo commit)
$sha = (git -C $Repo rev-parse HEAD).Trim(); $short = $sha.Substring(0,7)
$dirty = git -C $Repo status --porcelain -- src pom.xml
if ($dirty) { Write-Warning 'Hay cambios sin commit en src/pom.xml: el JAR no corresponderá exactamente a un SHA.' }
$jar = Join-Path $State "ping-ensayo-$short.jar"
if ($dirty -or -not (Test-Path $jar)) {
  Say "compilando $short (1–2 min)…"
  Push-Location $Repo
  try { Run { & .\mvnw.cmd -B -q -DskipTests package *> (Join-Path $Logs "compilacion-$short.log") } } finally { Pop-Location }
  if ($LASTEXITCODE -ne 0) { throw "La compilación falló. Revisa $(Join-Path $Logs "compilacion-$short.log")" }
  Copy-Item (Join-Path $Repo 'target\ping-0.0.1-SNAPSHOT.jar') $jar -Force
} else { Say "reutilizando el JAR ya compilado de $short" }

# 3 · MySQL aislada del ensayo
$fresh = -not (Test-Path (Join-Path $Data 'mysql'))
@"
[mysqld]
basedir="$($MySqlBin.Replace('\bin','').Replace('\','/'))"
datadir="$($Data.Replace('\','/'))"
port=$DbPort
bind-address=127.0.0.1
mysqlx=OFF
local_infile=OFF
"@ | Set-Content -Path $Ini -Encoding ascii
if ($fresh) {
  Say 'creando una MySQL nueva y vacía para el ensayo…'
  Run { & $mysqld "--defaults-file=$Ini" --initialize-insecure *> (Join-Path $Logs 'mysql-init.log') }
  if ($LASTEXITCODE -ne 0) { throw "No se pudo inicializar MySQL. Revisa $(Join-Path $Logs 'mysql-init.log')" }
  Remove-Item (Join-Path $State 'mysql-root.secret'), (Join-Path $State 'app-db.secret') -ErrorAction SilentlyContinue
}
Start-Process -FilePath $mysqld -ArgumentList "--defaults-file=`"$Ini`"" -WindowStyle Hidden
$t = [Diagnostics.Stopwatch]::StartNew(); while (-not (Listening $DbPort)) { if ($t.Elapsed.TotalSeconds -gt 60) { throw 'MySQL del ensayo no respondió en 60 s.' }; Start-Sleep -Milliseconds 500 }
$rootPw = Secret 'mysql-root'; $appPw = Secret 'app-db'
if ($fresh) {
  Run { "ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPw';" | & $mysql --no-defaults -h 127.0.0.1 -P $DbPort -u root 2>&1 | Out-Null }
  Sql "CREATE DATABASE ping_ensayo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER 'ping_ensayo'@'127.0.0.1' IDENTIFIED BY '$appPw'; GRANT ALL ON ping_ensayo.* TO 'ping_ensayo'@'127.0.0.1';" | Out-Null
}
Say "MySQL del ensayo lista en 127.0.0.1:$DbPort ($(if ($fresh) {'nueva'} else {'datos de ensayos anteriores'}))"

# 4 · Dirección para los celulares (solo con -Lan)
$bind = '127.0.0.1'; $origin = "http://127.0.0.1:$Port"
if ($Lan) {
  if (-not $LanIPv4) {
    $route = Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue | Sort-Object RouteMetric, InterfaceMetric | Select-Object -First 1
    if ($route) { $LanIPv4 = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceIndex $route.InterfaceIndex -ErrorAction SilentlyContinue | Select-Object -First 1).IPAddress }
  }
  if (-not $LanIPv4) { throw 'No encontré la IPv4 de la red local. Indícala con -LanIPv4.' }
  $bind = '0.0.0.0'; $origin = "http://${LanIPv4}:$Port"
}

# 5 · Arranca PING (las credenciales viajan por variables de entorno del proceso hijo, nunca por argumentos)
$adminPw = Secret 'admin'
$log = Join-Path $Logs ("app-{0}-politica{1}-{2}.log" -f $short, $Policy, (Get-Date -Format 'yyyyMMdd-HHmmss'))
$vars = @{ DB_URL = "jdbc:mysql://127.0.0.1:$DbPort/ping_ensayo?sslMode=DISABLED&allowPublicKeyRetrieval=true"; DB_USERNAME = 'ping_ensayo'; DB_PASSWORD = $appPw
           SPRING_PROFILES_ACTIVE = 'mysql'; PING_DEMO_ENABLED = 'true'; PING_PUBLIC_BASE_URL = $origin; PING_BOOTSTRAP_EMAIL = $AdminEmail; PING_BOOTSTRAP_PASSWORD = $adminPw }
foreach ($k in $vars.Keys) { Set-Item "Env:$k" $vars[$k] }
try {
  Start-Process -FilePath $java -ArgumentList @('-jar', "`"$jar`"", "--server.port=$Port", "--server.address=$bind", "--ping.journey.encounter-policy=$Policy") -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError "$log.err"
} finally { foreach ($k in $vars.Keys) { Remove-Item "Env:$k" -ErrorAction SilentlyContinue } }
Say "arrancando PING $short con política $Policy…"
$t = [Diagnostics.Stopwatch]::StartNew(); $up = $false
while ($t.Elapsed.TotalSeconds -lt 180) {
  try { if ((Invoke-WebRequest "http://127.0.0.1:$Port/login" -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200) { $up = $true; break } } catch { }
  if (Select-String -Path $log -Pattern 'APPLICATION FAILED' -Quiet -ErrorAction SilentlyContinue) { break }
  Start-Sleep -Seconds 2
}
if (-not $up) { throw "PING no arrancó. Revisa $log" }

# 6 · Escenario demo y recompensa de prueba (idempotente), con la cuenta ficticia de administración
$base = "http://127.0.0.1:$Port"
function Csrf($html) { $m = [regex]::Match($html, 'name="_csrf"[^>]*value="([^"]+)"'); if (-not $m.Success) { throw 'Sin token CSRF.' }; $m.Groups[1].Value }
$r = Invoke-WebRequest "$base/login" -UseBasicParsing -SessionVariable web
Invoke-WebRequest "$base/login" -Method Post -UseBasicParsing -WebSession $web -Body @{ email = $AdminEmail; password = $adminPw; _csrf = (Csrf $r.Content) } | Out-Null
$r = Invoke-WebRequest "$base/demo/prepare" -UseBasicParsing -WebSession $web
Invoke-WebRequest "$base/demo/prepare" -Method Post -UseBasicParsing -WebSession $web -Body @{ _csrf = (Csrf $r.Content) } | Out-Null
$event = (Sql 'SELECT event_id FROM demo_scenarios WHERE id=1;' 'ping_ensayo' | Select-Object -First 1).ToString().Trim()
$has = (Sql "SELECT COUNT(*) FROM benefits WHERE event_id=$event AND name='Snack ficticio de ensayo';" 'ping_ensayo' | Select-Object -First 1).ToString().Trim()
if ($has -eq '0') {
  $r = Invoke-WebRequest "$base/events/$event/rewards/new" -UseBasicParsing -WebSession $web
  Invoke-WebRequest "$base/events/$event/rewards" -Method Post -UseBasicParsing -WebSession $web -Body @{ _csrf = (Csrf $r.Content); name = 'Snack ficticio de ensayo'; claimLocation = 'Mesa de ensayo'; claimInstructions = 'Muestra tu código de recibo al personal.'; pointsCost = '10'; active = 'true' } | Out-Null
}
$rewards = (Sql "SELECT COUNT(*) FROM benefits WHERE event_id=$event AND name='Snack ficticio de ensayo';" 'ping_ensayo' | Select-Object -First 1).ToString().Trim()
$frozen = (Sql "SELECT verification_policy, COUNT(*) FROM ping_matches GROUP BY verification_policy;" 'ping_ensayo') -join '; '

Write-Host ''
Say "LISTO · commit $sha · política $Policy para los encuentros NUEVOS"
Say "URL: $origin   (este PC también: http://127.0.0.1:$Port)"
Say "Cuenta ficticia: $AdminEmail · contraseña en $(Join-Path $State 'admin.secret')"
Say "Evento demo: SOFA 2026 (id $event) · participantes Samuel, Julián, Ana, Carlos · recompensa de prueba: $rewards"
if ($frozen) { Say "Encuentros ya existentes por política: $frozen (no se reinterpretan)" }
Say "Registro: $log"
Say 'Detener todo: .\tools\windows\Start-PingEnsayo.ps1 -Stop'
if ($Lan) { Say 'HTTP en la red local: la cámara del navegador NO se abre sin HTTPS de confianza; usa «Escribir su código».' }
