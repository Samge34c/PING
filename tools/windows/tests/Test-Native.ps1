# Windows-native process-boundary tests. Only disposable fixtures; no Java/MySQL/network required.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ($env:OS -ne 'Windows_NT') { throw 'These native checks require Windows; they cannot be certified on Linux.' }

function Assert-PingTest([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

$source = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([IO.Path]::GetTempPath()) ('PING pruebas á ñ ' + [Guid]::NewGuid().ToString('N'))
$savedTest = @{}
foreach ($name in @('JAVA_HOME','PING_TEST_CLIENT_FAIL','PING_TEST_CA')) {
    $savedTest[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$saved = $null
try {
    $clone = Join-Path $temp 'clon con espacios ñ'
    $helpers = Join-Path $clone 'tools\windows'
    $null = New-Item -ItemType Directory -Path $helpers -Force
    foreach ($file in @('Common.ps1','ping_ops.py')) { Copy-Item -LiteralPath (Join-Path $source $file) -Destination $helpers }
    . (Join-Path $helpers 'Common.ps1')
    $saved = Get-PingEnvironmentSnapshot
    Assert-PingTest ($script:PingRoot -eq $clone) 'Root must resolve from the copied script, including spaces and Unicode.'
    Write-Host 'PASS root resolution from a non-ASCII clone path'

    $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
    Assert-PingTest (Test-Path -LiteralPath $compiler) 'Windows runner must provide the inbox .NET Framework compiler.'
    $probe = Join-Path $temp 'native double á.exe'
    & $compiler /nologo "/out:$probe" (Join-Path $PSScriptRoot 'fixtures\NativeDouble.cs')
    Assert-PingTest ($LASTEXITCODE -eq 0) 'Could not compile the local native double.'
    $expected = @('', 'simple', 'dos palabras', 'acción ñ', 'a"b', 'C:\ruta con espacio\', 'dos\\', '&|%$()')
    $out = Join-Path $temp 'argv stdout.txt'; $err = Join-Path $temp 'argv stderr.txt'
    $process = Start-Process -FilePath $probe -ArgumentList (Join-PingNativeArguments (@('__argv__') + $expected)) -RedirectStandardOutput $out -RedirectStandardError $err -Wait -PassThru
    try { Assert-PingTest ($process.ExitCode -eq 0) 'Native argv double failed.' } finally { $process.Dispose() }
    $actual = @(Get-Content -LiteralPath $out | ForEach-Object {
        Assert-PingTest ($_.StartsWith('ARG:')) 'Unexpected native probe output.'
        [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_.Substring(4)))
    })
    Assert-PingTest ($actual.Count -eq $expected.Count) 'Native argv count changed, including an empty argument.'
    for ($i = 0; $i -lt $expected.Count; $i++) { Assert-PingTest ($actual[$i] -ceq $expected[$i]) 'Native argv value changed across the process boundary.' }
    Write-Host 'PASS native argv: empty, spaces, Unicode, quotes, trailing backslashes and shell metacharacters'

    $jdk = Join-Path $temp 'JDK ficticio á\bin'
    $null = New-Item -ItemType Directory -Path $jdk -Force
    Copy-Item -LiteralPath $probe -Destination (Join-Path $jdk 'java.exe')
    Copy-Item -LiteralPath $probe -Destination (Join-Path $jdk 'javac.exe')
    $env:JAVA_HOME = Split-Path -Parent $jdk
    $null = Test-PingJava
    Remove-Item -LiteralPath (Join-Path $jdk 'javac.exe')
    $rejected = $false
    try { Test-PingJava } catch { $rejected = $_.Exception.Message -like '*javac*' }
    Assert-PingTest $rejected 'The Java probe must reject a JRE without javac.'
    Write-Host 'PASS Java stderr handling and missing-javac guard with a native double'

    $fixtureSecret = 'SYNTHETIC_ONLY_NOT_A_REAL_PASSWORD_52'
    $env:DB_URL = 'jdbc:mysql://127.0.0.1:1/ping_ci_fixture?sslMode=VERIFY_CA'
    $env:DB_USERNAME = 'ci_fixture'
    $env:DB_PASSWORD = $fixtureSecret
    $env:PING_TEST_CA = Join-Path $temp 'certificado ficticio á.pem'
    Set-Content -LiteralPath $env:PING_TEST_CA -Value 'fixture, not a real certificate' -Encoding UTF8
    $env:PING_TEST_CLIENT_FAIL = $null
    $arguments = @('check-db','--mysql',$probe,'--ssl-ca',$env:PING_TEST_CA)
    $text = (Invoke-PingOps $arguments | Out-String)
    Assert-PingTest (($text | ConvertFrom-Json).connection -eq $true) 'Helper must parse the native client result.'
    Assert-PingTest (-not $text.Contains($fixtureSecret)) 'Helper output exposed a synthetic password.'
    Assert-PingTest ($env:DB_PASSWORD -ceq $fixtureSecret) 'Child invocation changed the parent credential.'
    Write-Host 'PASS PowerShell to Python to native client: paths, TLS arguments and child-only credentials'

    $env:PING_TEST_CLIENT_FAIL = '1'
    $records = New-Object 'System.Collections.Generic.List[string]'
    $rejected = $false
    try { Invoke-PingOps $arguments *>&1 | ForEach-Object { $records.Add([string]$_) } }
    catch { $records.Add($_.Exception.Message); $rejected = $true }
    $text = $records -join "`n"
    Assert-PingTest $rejected 'Nonzero native client exit must fail the PowerShell operation.'
    Assert-PingTest ($text.Contains('1045')) 'Sanitized error must retain the useful numeric MySQL code.'
    Assert-PingTest (-not $text.Contains($fixtureSecret) -and -not $text.Contains('synthetic private diagnostic')) 'Raw server diagnostics or password escaped redaction.'
    Write-Host 'PASS native failure propagation and output redaction'

    $env:SERVER_PORT = 'not-a-port'
    $rejected = $false
    try { Get-PingPort } catch { $rejected = $true }
    Assert-PingTest $rejected 'Invalid port must fail before operational startup.'
    Restore-PingEnvironment $saved
    foreach ($name in $saved.Keys) {
        Assert-PingTest ([Environment]::GetEnvironmentVariable($name, 'Process') -ceq $saved[$name]) 'Process environment was not restored.'
    }
    Write-Host ('PASS environment restore; native checks complete on PowerShell ' + $PSVersionTable.PSVersion.ToString())
} finally {
    if ($saved) { Restore-PingEnvironment $saved }
    foreach ($name in $savedTest.Keys) { [Environment]::SetEnvironmentVariable($name, $savedTest[$name], 'Process') }
    # Only the unique directory created above, never user files or repository data.
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
# Expected negative subprocess tests leave LASTEXITCODE nonzero. Only a completed suite exits0.
exit 0
