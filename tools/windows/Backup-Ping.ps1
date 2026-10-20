<# All PING tables/blobs. Private new folder, binary mysqldump output, no passwords in argv/files. #>
[CmdletBinding()]
param([string]$Destination, [string]$MySqlPath, [string]$MySqlDumpPath, [string]$SslCa)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Common.ps1')
$saved = Get-PingEnvironmentSnapshot; $exitCode = 1
try {
    Request-PingDatabase
    Write-Host 'Respaldo completo. Evita escrituras y cambios de estructura durante la captura; el script NO detiene PING ni MySQL.'
    $arguments = @('backup')
    if ($Destination) { $arguments += @('--destination',$Destination) }
    if ($MySqlPath) { $arguments += @('--mysql',$MySqlPath) }
    if ($MySqlDumpPath) { $arguments += @('--mysqldump',$MySqlDumpPath) }
    if ($SslCa) { $arguments += @('--ssl-ca',$SslCa) }
    Invoke-PingOps $arguments
    Write-Host 'SQL + checksum + manifiesto guardados fuera de Git. Archivo no vacío/checksum NO demuestran restaurabilidad: usa el ensayo documentado.'
    $exitCode = 0
} catch { Write-Warning $_.Exception.Message }
finally { Restore-PingEnvironment $saved }
exit $exitCode
