<# Guided HTTP LAN run. Changes only this process environment while the child runs; restores it afterwards. #>
[CmdletBinding()]
param([ValidateRange(0,65535)][int]$Port = 0, [string]$LanIPv4, [string]$MySqlPath, [string]$SslCa, [switch]$UseLanHttp)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Common.ps1')
$saved = Get-PingEnvironmentSnapshot
$mailbox = $null; $exitCode = 1
try {
    Write-Host "PING | arranque guiado | raíz: $script:PingRoot"
    Test-PingJava
    $wrapper = Join-Path $script:PingRoot 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf) -or -not (Test-Path -LiteralPath (Join-Path $script:PingRoot '.mvn\wrapper\maven-wrapper.properties'))) { throw 'Falta Maven Wrapper del repositorio. Recupera sus archivos desde Git; no se instalará Maven global.' }
    if ($env:SPRING_PROFILES_INCLUDE -match '(^|[,;\s])bootstrap([,;\s]|$)') { throw 'SPRING_PROFILES_INCLUDE incorpora bootstrap. Retíralo explícitamente para un arranque real mysql; el asistente no modifica esa configuración.' }
    $python = Get-PingPython
    $chosenPort = Get-PingPort $Port -Ask
    while (-not (Test-PingPortFree $chosenPort)) {
        if ($Port) { throw 'El puerto indicado está ocupado. Repite con -Port libre; no se detuvo ningún proceso.' }
        Write-Warning "El puerto $chosenPort está ocupado. Elige otro; no se tocará ese proceso."
        $chosenPort = Get-PingPort -Ask
    }
    $ip = Select-PingIPv4 $LanIPv4
    if ($env:SERVER_SSL_ENABLED -eq 'true' -or $env:SERVER_SERVLET_SESSION_COOKIE_SECURE -eq 'true') { throw 'Este asistente es HTTP LAN. Tu configuración exige TLS o cookies seguras: usa su arranque existente; no se desactivarán certificados/TLS automáticamente.' }
    $lanUrl = "http://${ip}:${chosenPort}"
    $existing = Get-PingPublicUri
    if ($existing -and $existing.AbsoluteUri.TrimEnd('/') -ne $lanUrl -and -not $UseLanHttp) {
        Write-Host ('Base pública existente: ' + $existing.AbsoluteUri)
        Write-Host "Propuesta HTTP LAN para ESTA ejecución: $lanUrl"
        if ((Read-Host 'Escribe LAN para usarla temporalmente, o Enter para cancelar') -cne 'LAN') { throw 'Arranque cancelado. Conservamos la configuración existente.' }
    }
    Request-PingDatabase
    Invoke-PingOps @('config','--for-startup')
    $arguments = @('check-db'); if ($MySqlPath) { $arguments += @('--mysql',$MySqlPath) }; if ($SslCa) { $arguments += @('--ssl-ca',$SslCa) }
    Invoke-PingOps $arguments
    $runArguments = '--spring.profiles.active=mysql'
    if ((Read-Host '¿Usar el buzón LOCAL de ensayo sin envío a Internet? [s/N]') -match '^[sS]$') {
        $mailRoot = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'PING-dev-mailbox'
        if (Test-PingPortFree 1025) {
            $values = @($python.Prefix) + @((Join-Path $script:PingRoot 'tools\dev-mailbox.py'),'--output',$mailRoot,'--port','1025')
            $mailbox = Start-Process -FilePath $python.File -ArgumentList (Join-PingNativeArguments $values) -PassThru
            for ($attempt = 0; $attempt -lt 20 -and -not $mailbox.HasExited -and (Test-PingPortFree 1025); $attempt++) { Start-Sleep -Milliseconds 250 }
            if ($mailbox.HasExited -or (Test-PingPortFree 1025)) { throw 'El buzón no inició en loopback:1025. Revisa su ventana; no se arrancó PING con un correo ficticio.' }
        } elseif ((Read-Host '1025 ya está ocupado. Solo si es tu buzón PING existente, escribe USAR') -cne 'USAR') { throw 'No se usó ni detuvo el servicio desconocido de1025.' }
        $env:SPRING_MAIL_HOST = '127.0.0.1'; $env:SPRING_MAIL_PORT = '1025'; $env:PING_MAIL_FROM = 'pruebas@ping.test'
        # Exceptions apply only to the explicitly chosen loopback mailbox, not to remote SMTP.
        $runArguments += ' --spring.mail.properties.mail.smtp.auth=false --spring.mail.properties.mail.smtp.starttls.enable=false --spring.mail.properties.mail.smtp.starttls.required=false'
        Write-Host "Lee los mensajes .eml PRIVADOS en: $mailRoot. No los subas a Git ni al chat."
    } elseif (-not $env:SPRING_MAIL_HOST -or -not $env:PING_MAIL_FROM) {
        Write-Warning 'Sin correo configurado no habrá registro/recuperación por correo. Las cuentas existentes pueden entrar.'
    }
    $env:SERVER_PORT = [string]$chosenPort; $env:SERVER_ADDRESS = '0.0.0.0'; $env:PING_PUBLIC_BASE_URL = $lanUrl
    Write-Host "En el celular de la misma red, abre EXACTAMENTE: $lanUrl"
    Write-Host 'HTTP permite códigos manuales. El escáner interno requiere HTTPS y certificado confiado por el teléfono; este script no los configura.'
    Write-Host 'No se modifican firewall, ExecutionPolicy, router ni servicios ajenos. Mantén esta ventana abierta; espera Started PingApplication.'
    Push-Location $script:PingRoot
    try { & $wrapper 'spring-boot:run' "-Dspring-boot.run.arguments=$runArguments"; $exitCode = $LASTEXITCODE }
    finally { Pop-Location }
    if ($exitCode -ne 0) { Write-Warning 'PING no completó el arranque o terminó con error. Revisa el primer error del registro; no se atribuye automáticamente a la red.' }
} catch { Write-Warning $_.Exception.Message; $exitCode = 1 }
finally {
    if ($mailbox) {
        try { if (-not $mailbox.HasExited) { Stop-Process -Id $mailbox.Id -ErrorAction Stop } } catch { Write-Warning 'No pudimos cerrar el buzón iniciado por este script. Ciérralo en su ventana.' }
        $mailbox.Dispose()
    }
    Restore-PingEnvironment $saved
}
exit $exitCode
