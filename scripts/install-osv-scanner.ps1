param(
    [string]$Version = '2.7.0'
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$toolDir = Join-Path $repo ".tools/osv-scanner/v$Version"
$scanner = Join-Path $toolDir 'osv-scanner.exe'
$checksums = Join-Path $toolDir 'SHA256SUMS'
$base = "https://github.com/google/osv-scanner/releases/download/v$Version"

New-Item -ItemType Directory -Force $toolDir | Out-Null
Invoke-WebRequest -Uri "$base/osv-scanner_windows_amd64.exe" -OutFile $scanner
Invoke-WebRequest -Uri "$base/osv-scanner_SHA256SUMS" -OutFile $checksums

$expected = (Select-String -Path $checksums -Pattern 'osv-scanner_windows_amd64\.exe$').Line.Split(' ')[0]
$actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $scanner).Hash.ToLowerInvariant()
if ($actual -ne $expected.ToLowerInvariant()) {
    throw "OSV-Scanner checksum mismatch"
}

Write-Output "Installed and verified OSV-Scanner v$Version at $scanner"
