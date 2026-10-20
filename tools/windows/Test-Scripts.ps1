# Local PowerShell parser check only; does not run PING, change config or require Pester.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$failed = $false
foreach ($file in Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1') {
    $tokens = $null; $parseErrors = $null
    $null = [Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$parseErrors)
    if (@($parseErrors).Count) {
        $failed = $true
        foreach ($problem in $parseErrors) { Write-Warning ($file.Name + ':' + $problem.Extent.StartLineNumber + ' | ' + $problem.Message) }
    } else { Write-Host ('Sintaxis PowerShell: ' + $file.Name + ' OK') }
}
if ($failed) { exit 1 }
exit 0
