param(
    [string]$CycloneDxNpmVersion = '6.0.1'
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$reportDir = Join-Path $repo 'security/reports'
New-Item -ItemType Directory -Force $reportDir | Out-Null

Push-Location (Join-Path $repo 'frontend')
try {
    & npm.cmd exec --yes --package "@cyclonedx/cyclonedx-npm@$CycloneDxNpmVersion" -- cyclonedx-npm `
        --output-format JSON `
        --output-file (Join-Path $reportDir 'sbom-frontend.json') `
        package.json
    if ($LASTEXITCODE -ne 0) {
        throw "CycloneDX npm failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
