<# Read-only diagnosis. Does not prompt for credentials, change env, restart services or edit firewall. #>
[CmdletBinding()]
param([ValidateRange(0,65535)][int]$Port = 0, [string]$LanIPv4, [string]$MySqlPath, [string]$SslCa)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Common.ps1')
Write-Host "PING | diagnóstico de lectura | raíz: $script:PingRoot"
try { Test-PingJava } catch { Write-Warning $_.Exception.Message }
Write-Host ('Maven Wrapper: ' + (Test-Path -LiteralPath (Join-Path $script:PingRoot 'mvnw.cmd')))
try { $python = Get-PingPython; Write-Host 'Python 3.10+: disponible.' } catch { Write-Warning $_.Exception.Message }
foreach ($name in @('DB_URL','DB_USERNAME','DB_PASSWORD','PING_PUBLIC_BASE_URL','SPRING_MAIL_HOST','PING_MAIL_FROM','PING_BOOTSTRAP_PASSWORD','PING_OAUTH_ENABLED')) {
    $present = -not [string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))
    Write-Host ("{0}: {1}" -f $name, $(if ($present) { 'presente (valor oculto)' } else { 'ausente' }))
}
try {
    $chosenPort = Get-PingPort $Port
    $listeners = @([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object { $_.Port -eq $chosenPort })
    Write-Host "Puerto consultado: $chosenPort"
    if ($listeners.Count) { foreach ($item in $listeners) { Write-Host ('Escucha local comprobada: ' + $item.Address.ToString() + ':' + $item.Port) } }
    else { Write-Warning 'No hay una escucha TCP comprobada en ese puerto.' }
    $ip = Select-PingIPv4 $LanIPv4
    Write-Host "IPv4 seleccionada: $ip (debe corresponder a la red del celular)."
    if ($listeners.Count -and -not @($listeners | Where-Object { $_.Address.ToString() -in @('0.0.0.0','::',$ip) }).Count) {
        Write-Warning 'La escucha detectada no incluye esa IPv4. Una escucha solo loopback no admite el celular.'
    }
    $uri = Get-PingPublicUri
    if ($uri) {
        Write-Host ('Base configurada: ' + $uri.AbsoluteUri)
        if ($uri.Host -ne $ip -or ($uri.Scheme -eq 'http' -and $uri.Port -ne $chosenPort)) { Write-Warning 'La base pública no coincide con IPv4/puerto elegidos. Puede requerir un proxy real; no se cambió nada.' }
        if ($uri.Scheme -eq 'https') { Write-Host 'Cámara: HTTPS permite esperar contexto seguro SOLO si el teléfono confía en el certificado y accede a ese origen. No está comprobado aquí.' }
        else { Write-Host 'Cámara del navegador en el celular: HTTP LAN no es contexto seguro. Usa códigos manuales.' }
    } else { Write-Warning 'Sin base pública: no se generan enlaces/QR de entrada; el código manual sigue siendo posible.' }
    $scheme = 'http'; if ($env:SERVER_SSL_ENABLED -eq 'true') { $scheme = 'https' }
    $local = "${scheme}://127.0.0.1:${chosenPort}/login"
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $local -TimeoutSec 5 -MaximumRedirection 0
        if ($response.StatusCode -eq 200 -and $response.Content -match '<title>[^<]*Ping</title>' -and $response.Content -match '/js/theme-init.js') {
            Write-Host "Respuesta pública local comprobada: PING /login HTTP 200 en $local"
        } else { Write-Warning 'Hubo respuesta, pero no se identificó la pantalla pública PING esperada.' }
    } catch { Write-Warning 'No se obtuvo una respuesta pública PING local válida en ese origen. No se ignoraron certificados ni se reinició nada.' }
} catch { Write-Warning $_.Exception.Message }
try {
    if (Get-Command Get-Service -ErrorAction SilentlyContinue) {
        Get-Service -Name '*MySQL*' -ErrorAction SilentlyContinue | ForEach-Object { Write-Host ('Servicio MySQL: ' + $_.Name + ' | ' + $_.Status) }
    }
    if ($env:DB_URL -and $env:DB_USERNAME -and $env:DB_PASSWORD) {
        $arguments = @('check-db'); if ($MySqlPath) { $arguments += @('--mysql',$MySqlPath) }; if ($SslCa) { $arguments += @('--ssl-ca',$SslCa) }
        Invoke-PingOps $arguments
    } else { Write-Warning 'Conexión SQL no comprobada: faltan variables. El diagnóstico no solicita ni cambia credenciales.' }
} catch { Write-Warning $_.Exception.Message }
Write-Host 'Pendiente en el teléfono: misma red efectiva, aislamiento Wi-Fi/router/firewall, navegación, permisos de cámara y Safari. Un HTTP 200 local no los demuestra.'
