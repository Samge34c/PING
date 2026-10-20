# Shared helpers. No service/global configuration changes on import. Windows PowerShell 5.1+.
Set-StrictMode -Version Latest
$script:PingRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$script:PingHelper = Join-Path $PSScriptRoot 'ping_ops.py'

function Get-PingEnvironmentSnapshot {
    $values = @{}
    foreach ($name in @('DB_URL','DB_USERNAME','DB_PASSWORD','SERVER_PORT','SERVER_ADDRESS','PING_PUBLIC_BASE_URL','SPRING_PROFILES_ACTIVE','SPRING_MAIL_HOST','SPRING_MAIL_PORT','PING_MAIL_FROM')) {
        $values[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    }
    return $values
}
function Restore-PingEnvironment($Values) {
    foreach ($name in $Values.Keys) { [Environment]::SetEnvironmentVariable($name, $Values[$name], 'Process') }
}
function Request-PingDatabase {
    if (-not $env:DB_URL) { $env:DB_URL = Read-Host 'URL JDBC MySQL existente (sin usuario ni contraseña en la URL)' }
    if (-not $env:DB_USERNAME) { $env:DB_USERNAME = Read-Host 'Usuario técnico MySQL (no tu cuenta PING)' }
    if (-not $env:DB_PASSWORD) {
        $secret = Read-Host 'Contraseña MySQL (no se muestra ni se guarda)' -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
        try { $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secret.Dispose() }
    }
}
function Get-PingPython {
    foreach ($name in @('py.exe','python.exe','python3','python')) {
        $command = Get-Command $name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $command) { continue }
        $prefix = @(); if ($name -eq 'py.exe') { $prefix = @('-3') }
        & $command.Source @prefix -c 'import sys; sys.exit(0 if sys.version_info >= (3,10) else 1)' 2>$null
        if ($LASTEXITCODE -eq 0) { return @{ File = $command.Source; Prefix = $prefix } }
    }
    throw 'No encontramos Python 3.10+ ya instalado (también lo usa el buzón). No se descargó nada.'
}
function Invoke-PingOps([string[]]$Arguments) {
    $python = Get-PingPython; $prefix = @($python.Prefix)
    & $python.File @prefix $script:PingHelper @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'La herramienta no completó la operación. Revisa el mensaje anterior; no repitas una restauración sobre su base parcial.' }
}
function Test-PingJava {
    $java = $null
    if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME 'bin\java.exe' }
    else { $cmd = Get-Command java -CommandType Application -ErrorAction SilentlyContinue; if ($cmd) { $java = $cmd.Source } }
    if (-not $java -or -not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'Falta Java 21 o JAVA_HOME apunta a una carpeta incorrecta.' }
    # ProcessStartInfo avoids treating java -version's normal stderr as a PowerShell error.
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $java; $info.Arguments = '-version'; $info.UseShellExecute = $false
    $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true; $info.CreateNoWindow = $true
    $process = New-Object System.Diagnostics.Process; $process.StartInfo = $info
    try {
        $null = $process.Start(); $output = $process.StandardOutput.ReadToEnd() + $process.StandardError.ReadToEnd(); $process.WaitForExit()
        if ($process.ExitCode -ne 0 -or $output -notmatch 'version\s+"21(?:[.\-+\"]|$)') { throw 'Se requiere Java 21. Revisa JAVA_HOME y PATH; no se instalará otra versión automáticamente.' }
        $javac = Join-Path (Split-Path -Parent $java) 'javac.exe'
        if (-not (Test-Path -LiteralPath $javac -PathType Leaf)) { throw 'Java 21 está disponible, pero falta javac del JDK completo en la misma carpeta.' }
        Write-Host 'JDK Java 21: disponible.'
    } finally { $process.Dispose() }
}
function Get-PingIPv4 {
    $rows = @()
    foreach ($adapter in [Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces()) {
        if ($adapter.OperationalStatus -ne 'Up' -or $adapter.NetworkInterfaceType -eq 'Loopback') { continue }
        $props = $adapter.GetIPProperties()
        foreach ($address in $props.UnicastAddresses) {
            $ip = $address.Address
            if ($ip.AddressFamily -ne 'InterNetwork' -or $ip.ToString().StartsWith('169.254.')) { continue }
            $rows += [pscustomobject]@{ IPv4 = $ip.ToString(); Adapter = $adapter.Name; Gateway = (@($props.GatewayAddresses).Count -gt 0) }
        }
    }
    return $rows
}
function Select-PingIPv4([string]$Requested) {
    $rows = @(Get-PingIPv4)
    if (-not $rows.Count) { throw 'No hay una IPv4 activa. Conecta el PC a la red compartida y repite.' }
    if ($Requested) {
        if ($Requested -notin @($rows | ForEach-Object { $_.IPv4 })) { throw 'La IPv4 indicada no pertenece a una interfaz activa de este PC.' }
        return $Requested
    }
    Write-Host 'Elige la conexión que realmente comparte red con el celular; las interfaces virtuales también pueden aparecer:'
    for ($index = 0; $index -lt $rows.Count; $index++) {
        Write-Host ('{0}) {1} | {2} | puerta de enlace: {3}' -f ($index + 1), $rows[$index].IPv4, $rows[$index].Adapter, $rows[$index].Gateway)
    }
    $answer = Read-Host 'Número de conexión (no se elige la primera automáticamente)'
    $selected = 0
    if (-not [int]::TryParse($answer, [ref]$selected) -or $selected -lt 1 -or $selected -gt $rows.Count) { throw 'Selección inválida. No se cambió ninguna conexión.' }
    return $rows[$selected - 1].IPv4
}
function Test-PingPortFree([int]$Port) {
    $listener = New-Object -TypeName System.Net.Sockets.TcpListener -ArgumentList ([Net.IPAddress]::Any, $Port)
    $listener.Server.ExclusiveAddressUse = $true
    try { $listener.Start(); return $true }
    catch [Net.Sockets.SocketException] { return $false }
    finally { $listener.Stop() }
}
function Get-PingPort([int]$Requested = 0, [switch]$Ask) {
    $candidate = 8081
    if ($env:SERVER_PORT) {
        if (-not [int]::TryParse($env:SERVER_PORT, [ref]$candidate)) { throw 'SERVER_PORT no es un número.' }
    }
    if ($Requested) { $candidate = $Requested }
    elseif ($Ask) {
        $answer = Read-Host "Puerto de PING [$candidate]"
        if ($answer -and -not [int]::TryParse($answer, [ref]$candidate)) { throw 'El puerto debe ser un número.' }
    }
    if ($candidate -lt 1 -or $candidate -gt 65535) { throw 'El puerto debe estar entre 1 y 65535.' }
    return $candidate
}
function Get-PingPublicUri {
    if (-not $env:PING_PUBLIC_BASE_URL) { return $null }
    $uri = $null
    if (-not [Uri]::TryCreate($env:PING_PUBLIC_BASE_URL, [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -notin @('http','https') -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
        throw 'PING_PUBLIC_BASE_URL no es un origen HTTP/HTTPS sin credenciales, query ni fragmento. No se mostrará el valor.'
    }
    return $uri
}
# Windows native argv quoting for Start-Process (which otherwise joins arguments with spaces).
function Join-PingNativeArguments([string[]]$Values) {
    $quoted = foreach ($value in $Values) {
        '"' + [regex]::Replace([regex]::Replace($value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
    }
    return ($quoted -join ' ')
}
