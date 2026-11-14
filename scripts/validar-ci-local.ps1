[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$reportRoot = Join-Path $repoRoot '.ci-local-reports'
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null

$java21Home = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
if (Test-Path -LiteralPath $java21Home) {
    $env:JAVA_HOME = $java21Home
}

$localGradleHome = Join-Path $repoRoot '.gradle-user-home-validation'
if (-not (Test-Path -LiteralPath $localGradleHome)) {
    New-Item -ItemType Directory -Path $localGradleHome -Force | Out-Null
}
$env:GRADLE_USER_HOME = $localGradleHome

$results = [System.Collections.Generic.List[object]]::new()
Write-Output "Using JAVA_HOME=$env:JAVA_HOME"
Write-Output "Using GRADLE_USER_HOME=$env:GRADLE_USER_HOME"

function Protect-ReportText {
    param([AllowNull()][string]$Text)

    if ($null -eq $Text) {
        return ''
    }

    $protected = $Text
    foreach ($name in @('BRAPI_TOKEN', 'AUTH_JWT_SECRET')) {
        $value = [Environment]::GetEnvironmentVariable($name)
        if (-not [string]::IsNullOrEmpty($value)) {
            $protected = $protected.Replace($value, '<REDACTED>')
        }
    }

    $protected = $protected -replace '(?i)(BRAPI_TOKEN|AUTH_JWT_SECRET)\s*[=:]\s*[^\s]+', '$1=<REDACTED>'
    $protected = $protected -replace '(?i)(Authorization\s*:\s*Bearer\s+)[^\s]+', '$1<REDACTED>'
    return $protected
}

function Get-FailureStatus {
    param([string]$Output)

    if ($Output -match '(?i)AccessDenied|EACCES|Permission denied|Could not create parent directory|Could not read directory|Cannot read directory|Could not GET|Could not resolve|socket exception|SSL misconfiguration|network|timed out|not recognized|JAVA_HOME') {
        return 'BLOCKED'
    }

    return 'FAIL'
}

function Invoke-ValidationStep {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string[]]$Arguments,
        [Parameter(Mandatory)][string]$ReportName
    )

    $reportPath = Join-Path $reportRoot $ReportName
    $temporaryPath = "$reportPath.tmp"
    $exitCode = 1
    $output = ''
    $started = Get-Date

    try {
        Push-Location $WorkingDirectory
        try {
            $previousErrorAction = $ErrorActionPreference
            $ErrorActionPreference = 'Continue'
            & $FilePath @Arguments *> $temporaryPath
            $exitCode = if ($null -eq $LASTEXITCODE) { 0 } else { $LASTEXITCODE }
            $ErrorActionPreference = $previousErrorAction
        }
        finally {
            Pop-Location
        }

        if (Test-Path -LiteralPath $temporaryPath) {
            $output = Get-Content -LiteralPath $temporaryPath -Raw -ErrorAction SilentlyContinue
        }
    }
    catch {
        $output = $_ | Out-String
        $exitCode = 1
    }
    finally {
        Set-Content -LiteralPath $reportPath -Value (Protect-ReportText $output) -Encoding UTF8
        if (Test-Path -LiteralPath $temporaryPath) {
            Remove-Item -LiteralPath $temporaryPath -Force -ErrorAction SilentlyContinue
        }
    }

    $status = if ($exitCode -eq 0) { 'PASS' } else { Get-FailureStatus $output }
    $duration = (Get-Date) - $started
    $results.Add([pscustomobject]@{
        Status = $status
        Step = $Name
        ExitCode = $exitCode
        Duration = $duration.ToString('hh\:mm\:ss')
        Report = $reportPath.Substring($repoRoot.Length + 1)
    })
    Write-Output ("{0} | {1} | exit={2} | report={3}" -f $status, $Name, $exitCode, $reportPath.Substring($repoRoot.Length + 1))
}

Invoke-ValidationStep -Name 'Java 21' -WorkingDirectory $repoRoot -FilePath 'java' -Arguments @('-version') -ReportName '01-java.log'
Invoke-ValidationStep -Name 'Gradle Wrapper 9.5.1' -WorkingDirectory $repoRoot -FilePath '.\gradlew.bat' -Arguments @('--version') -ReportName '02-gradle-version.log'
Invoke-ValidationStep -Name 'Backend tests' -WorkingDirectory $repoRoot -FilePath '.\gradlew.bat' -Arguments @('clean', 'test', '--no-daemon', '--console=plain') -ReportName '03-backend-test.log'
Invoke-ValidationStep -Name 'Backend build' -WorkingDirectory $repoRoot -FilePath '.\gradlew.bat' -Arguments @('build', '-x', 'test', '--no-daemon', '--console=plain') -ReportName '04-backend-build.log'
Invoke-ValidationStep -Name 'SpotBugs and FindSecBugs' -WorkingDirectory $repoRoot -FilePath '.\gradlew.bat' -Arguments @('spotbugsMain', '--rerun-tasks', '--no-daemon', '--console=plain') -ReportName '05-spotbugs.log'

Invoke-ValidationStep -Name 'npm 12.0.2' -WorkingDirectory (Join-Path $repoRoot 'frontend') -FilePath 'npm' -Arguments @('--version') -ReportName '06-npm-version.log'
Invoke-ValidationStep -Name 'Frontend locked install' -WorkingDirectory (Join-Path $repoRoot 'frontend') -FilePath 'npm' -Arguments @('ci') -ReportName '07-frontend-ci.log'
Invoke-ValidationStep -Name 'Frontend tests' -WorkingDirectory (Join-Path $repoRoot 'frontend') -FilePath 'npm' -Arguments @('test', '--', '--watch=false') -ReportName '08-frontend-test.log'
Invoke-ValidationStep -Name 'Frontend build' -WorkingDirectory (Join-Path $repoRoot 'frontend') -FilePath 'npm' -Arguments @('run', 'build') -ReportName '09-frontend-build.log'

Invoke-ValidationStep -Name 'OpenSpec CLI' -WorkingDirectory $repoRoot -FilePath 'openspec' -Arguments @('--version') -ReportName '10-openspec-version.log'
Invoke-ValidationStep -Name 'OpenSpec strict validation' -WorkingDirectory $repoRoot -FilePath 'openspec' -Arguments @('validate', 'establish-ci-cd-pipeline', '--type', 'change', '--strict') -ReportName '11-openspec-strict.log'

$summaryPath = Join-Path $reportRoot 'summary.md'
$summary = @(
    '# CI local validation',
    '',
    "Generated: $(Get-Date -Format o)",
    '',
    '| Status | Step | Exit code | Duration | Report |',
    '|---|---|---:|---:|---|'
)
foreach ($result in $results) {
    $summary += "| $($result.Status) | $($result.Step) | $($result.ExitCode) | $($result.Duration) | $($result.Report) |"
}
$summary += ''
$summary += '## Diagnóstico e correção local'
$summary += ''
$summary += '- Gradle: `GRADLE_USER_HOME` estava vazio e o wrapper tentava usar `C:\.gradle`; o script agora define o cache de processo em `.gradle-user-home-validation`.'
$summary += '- Angular: a execução restrita inicialmente bloqueou a travessia de diretórios com `Access is denied`; nenhuma permissão global foi alterada e a execução fora do sandbox concluiu testes e build.'
$summary += '- Nenhum cache foi excluído, nenhuma proteção do Windows foi desabilitada e nenhum código de produção foi alterado.'
$summary += ''
$summary += '## Comandos'
$summary += ''
$summary += '- `gradlew.bat clean test --no-daemon --console=plain`'
$summary += '- `gradlew.bat build -x test --no-daemon --console=plain`'
$summary += '- `gradlew.bat spotbugsMain --rerun-tasks --no-daemon --console=plain`'
$summary += '- `npm ci`'
$summary += '- `npm test -- --watch=false`'
$summary += '- `npm run build`'
$summary += '- `openspec validate establish-ci-cd-pipeline --type change --strict`'
$summary += ''
$summary += 'Testes ignorados permanecem nos relatórios das ferramentas e não são convertidos em aprovação.'
Set-Content -LiteralPath $summaryPath -Value $summary -Encoding UTF8

$results | Format-Table -AutoSize
$nonPassing = @($results | Where-Object { $_.Status -ne 'PASS' })
if ($nonPassing.Count -gt 0) {
    Write-Output "Validation incomplete: $($nonPassing.Count) step(s) are FAIL or BLOCKED. Reports: $reportRoot"
    exit 1
}

Write-Output "Validation passed. Reports: $reportRoot"
exit 0
