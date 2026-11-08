[CmdletBinding()]
param(
    [string]$Target = 'http://host.docker.internal:8081/swagger-ui/index.html',
    [string]$ZapImage = 'ghcr.io/zaproxy/zaproxy:2.17.0',
    [int]$SpiderMinutes = 1
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$reports = Join-Path $repo 'security\reports'
New-Item -ItemType Directory -Force -Path $reports | Out-Null

$uri = [Uri]$Target
$allowedHosts = @('localhost', '127.0.0.1', 'host.docker.internal', '::1', '[::1]')
if ($uri.Scheme -ne 'http' -or $uri.UserInfo -or $allowedHosts -notcontains $uri.Host) {
    throw "DAST target must be local HTTP only (localhost, 127.0.0.1, host.docker.internal, or ::1)."
}
if ($SpiderMinutes -lt 1 -or $SpiderMinutes -gt 5) {
    throw 'SpiderMinutes must be between 1 and 5 for this baseline scope.'
}

docker version | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Engine is unavailable; ZAP Baseline was not executed.'
}

$volume = "${repo}:/zap/wrk/:rw"
$arguments = @(
    'run', '--rm',
    '--add-host=host.docker.internal:host-gateway',
    '-v', $volume,
    '-t', $ZapImage,
    'zap-baseline.py',
    '-t', $Target,
    '-m', $SpiderMinutes,
    '-r', 'security/reports/zap-baseline.html',
    '-J', 'security/reports/zap-baseline.json',
    '-x', 'security/reports/zap-baseline.xml'
)

Write-Host "Running OWASP ZAP Baseline against local target: $Target"
Write-Host "Image: $ZapImage"
& docker @arguments
$exitCode = $LASTEXITCODE
if ($exitCode -gt 2) {
    throw "OWASP ZAP execution failed with exit code $exitCode."
}
if ($exitCode -eq 2) {
    Write-Warning 'OWASP ZAP completed with WARN alerts; inspect the generated reports before accepting the result.'
}
Write-Host "OWASP ZAP Baseline completed with exit code $exitCode; inspect security/reports/zap-baseline.*."
exit $exitCode
