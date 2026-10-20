<# Restore only into a NEW exclusive database. Never drops/cleans databases or changes habitual DB_URL. #>
[CmdletBinding()]
param([string]$Backup, [string]$TargetDatabase, [string]$MySqlPath, [string]$SslCa)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Common.ps1')
$saved = Get-PingEnvironmentSnapshot; $exitCode = 1
try {
    if (-not $Backup) { $Backup = Read-Host 'Ruta del archivo SQL privado (con sus .sha256 y .json al lado)' }
    if (-not $TargetDatabase) { $TargetDatabase = 'ping_restore_exclusive_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '_' + [Guid]::NewGuid().ToString('N').Substring(0,8) }
    Request-PingDatabase
    Write-Host 'Se necesitan permisos locales para crear una base y un usuario temporal limitado a esa base. Ninguna cuenta PING concede esos permisos MySQL.'
    $arguments = @('restore','--backup',$Backup,'--target',$TargetDatabase)
    if ($MySqlPath) { $arguments += @('--mysql',$MySqlPath) }
    if ($SslCa) { $arguments += @('--ssl-ca',$SslCa) }
    # The helper validates checksum, shows origin/destination and asks for the exact new name before creation.
    Invoke-PingOps $arguments
    Write-Host 'Ensayo importado y comparado. La base NUEVA se conserva; PING continúa con su configuración habitual.'
    $exitCode = 0
} catch { Write-Warning $_.Exception.Message }
finally { Restore-PingEnvironment $saved }
exit $exitCode
