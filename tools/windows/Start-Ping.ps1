<# Compatible entry point. The same Python workflow also serves INICIAR-PING.cmd.
   The double-click path does not execute PowerShell scripts or change ExecutionPolicy. #>
[CmdletBinding()]
param([ValidateRange(0,65535)][int]$Port = 0, [string]$LanIPv4, [string]$MySqlPath, [string]$SslCa, [switch]$UseLanHttp, [switch]$Configure)
$arguments = @('start')
if ($Port) { $arguments += @('--port', [string]$Port) }
if ($LanIPv4) { $arguments += @('--lan-ip', $LanIPv4) }
if ($MySqlPath) { $arguments += @('--mysql', $MySqlPath) }
if ($SslCa) { $arguments += @('--ssl-ca', $SslCa) }
if ($Configure) { $arguments += '--configurar' }
if ($UseLanHttp) { Write-Host 'El nuevo arranque conserva HTTPS existente. Cambia a HTTP manual solo desde Configurar → HTTPS.' }
& (Join-Path $PSScriptRoot 'Launch-Ping.cmd') @arguments
exit $LASTEXITCODE
