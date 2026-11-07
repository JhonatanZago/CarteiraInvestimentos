param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,
    [Parameter(Mandatory = $true)]
    [string]$OutputPath,
    [string]$OsvScannerPath = $env:OSV_SCANNER_PATH
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($OsvScannerPath)) {
    $OsvScannerPath = 'osv-scanner'
}

if (-not (Test-Path -LiteralPath $InputPath)) {
    throw "OSV input does not exist: $InputPath"
}

$resolvedOutput = [IO.Path]::GetFullPath($OutputPath)
New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($resolvedOutput)) | Out-Null

 $tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("osv-sbom-" + [Guid]::NewGuid().ToString('N'))
 $tempInput = Join-Path $tempRoot 'bom.json'
 New-Item -ItemType Directory -Force $tempRoot | Out-Null
 Copy-Item -LiteralPath $InputPath -Destination $tempInput
 try {
     & $OsvScannerPath scan source --format=json --sbom $tempInput --output-file $resolvedOutput
     $exitCode = $LASTEXITCODE
 }
 finally {
     Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
 }
if ($exitCode -gt 1) {
    throw "OSV-Scanner execution failed with exit code $exitCode"
}
exit $exitCode
