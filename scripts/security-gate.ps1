param(
    [string]$SemgrepVersion = '1.178.0',
    [string]$GitleaksImage = 'zricethezav/gitleaks:v8.30.1'
)

$ErrorActionPreference = 'Stop'
$repo = (Get-Location).Path
$reportDir = Join-Path $repo 'security/reports'
New-Item -ItemType Directory -Force $reportDir | Out-Null
$failures = [System.Collections.Generic.List[string]]::new()
$env:PYTHONUTF8 = '1'
$osvScannerPath = $env:OSV_SCANNER_PATH
if ([string]::IsNullOrWhiteSpace($osvScannerPath)) {
    $osvScannerPath = Join-Path $repo '.tools/osv-scanner/v2.7.0/osv-scanner.exe'
}

Write-Host '== 1/5 SAST: Semgrep =='
& uvx "semgrep==$SemgrepVersion" scan --metrics=off `
    --config p/java --config p/secrets --config p/owasp-top-ten `
    --config semgrep-rules/ --error --sarif `
    --output (Join-Path $reportDir 'semgrep.sarif') src/ frontend/src/
if ($LASTEXITCODE -ne 0) { $failures.Add('semgrep') }

Write-Host '== 2/5 Segredos: Gitleaks =='
$targets = @(
    @{ Path = 'src'; Name = 'src' },
    @{ Path = 'frontend/src'; Name = 'frontend-src' },
    @{ Path = 'docs'; Name = 'docs' },
    @{ Path = 'scripts'; Name = 'scripts' },
    @{ Path = '.gitignore'; Name = 'gitignore' },
    @{ Path = 'env.exemple'; Name = 'env-exemple' },
    @{ Path = '.env.example'; Name = 'env-example' },
    @{ Path = 'docker-compose.yml'; Name = 'docker-compose' },
    @{ Path = 'build.gradle'; Name = 'build-gradle' }
)
foreach ($target in $targets) {
    $report = "/repo/security/reports/gitleaks-$($target.Name).sarif"
    docker run --rm -v "${repo}:/repo" $GitleaksImage dir "/repo/$($target.Path)" `
        --no-banner --redact --report-format sarif --report-path $report
    if ($LASTEXITCODE -ne 0) { $failures.Add("gitleaks:$($target.Name)") }
}

Write-Host '== 3/5 SBOM: CycloneDX backend + frontend =='
& .\gradlew.bat cyclonedxBom --console=plain
if ($LASTEXITCODE -ne 0) { $failures.Add('cyclonedx-backend') }
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\generate-frontend-sbom.ps1
if ($LASTEXITCODE -ne 0) { $failures.Add('cyclonedx-frontend') }

Write-Host '== 4/5 SCA: OSV-Scanner =='
$sbomInputs = @('security/reports/sbom-backend.json', 'security/reports/sbom-frontend.json')
foreach ($sbom in $sbomInputs) {
    try {
        $document = Get-Content -Raw $sbom | ConvertFrom-Json
        if ($document.bomFormat -ne 'CycloneDX' -or $null -eq $document.components) {
            $failures.Add("sbom-invalid:$sbom")
        }
    }
    catch {
        $failures.Add("sbom-invalid:$sbom")
    }
}
if (-not (Test-Path -LiteralPath $osvScannerPath)) {
    $failures.Add('osv:tool-missing')
}
$osvInputs = @(
    @{ Path = 'security/reports/sbom-backend.json'; Name = 'backend' },
    @{ Path = 'security/reports/sbom-frontend.json'; Name = 'frontend' }
)
if (Test-Path -LiteralPath $osvScannerPath) {
    foreach ($input in $osvInputs) {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-osv-scanner.ps1 `
            -InputPath $input.Path -OutputPath (Join-Path $reportDir "osv-$($input.Name).json") `
            -OsvScannerPath $osvScannerPath
        if ($LASTEXITCODE -eq 1) { $failures.Add("osv:$($input.Name)-vulnerabilities") }
        elseif ($LASTEXITCODE -gt 1) { $failures.Add("osv:$($input.Name)-execution") }
    }
}

Write-Host '== 5/5 Bytecode: SpotBugs + FindSecBugs =='
& .\gradlew.bat spotbugsMain --console=plain
if ($LASTEXITCODE -ne 0) { $failures.Add('spotbugs') }

if ($failures.Count -gt 0) {
    Write-Error "REPROVADO: $($failures -join ', '). Consulte security/reports/."
    exit 1
}

Write-Host 'APROVADO: Semgrep, Gitleaks e SpotBugs/FindSecBugs não encontraram achados bloqueantes nos escopos configurados.'
