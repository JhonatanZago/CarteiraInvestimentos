[CmdletBinding()]
param(
    [switch]$Offline,
    [switch]$SkipRegression,
    [string]$GradleUserHome
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jdk21 = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$wrapper = Join-Path $projectRoot 'gradlew.bat'

function Invoke-Gradle {
    param([Parameter(Mandatory)][string[]]$Arguments)
    Push-Location $projectRoot
    try {
        & $wrapper @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle falhou com exit code ${LASTEXITCODE}: $($Arguments -join ' ')"
        }
    }
    finally {
        Pop-Location
    }
}

function Test-WritableDirectory {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) { return $false }
    $probe = Join-Path $Path ('.codex-write-probe-' + [guid]::NewGuid().ToString('N'))
    try {
        [System.IO.File]::WriteAllText($probe, 'probe')
        return $true
    }
    catch {
        return $false
    }
    finally {
        if (Test-Path -LiteralPath $probe) {
            Remove-Item -LiteralPath $probe -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "Projeto: $projectRoot"
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    throw "Gradle Wrapper nao encontrado: $wrapper"
}

if (-not (Test-Path -LiteralPath $jdk21 -PathType Container)) {
    throw "JDK 21 nao encontrado no caminho identificado: $jdk21"
}

$env:JAVA_HOME = $jdk21
$env:Path = (Join-Path $jdk21 'bin') + [IO.Path]::PathSeparator + $env:Path

Write-Host "JAVA_HOME configurado: $env:JAVA_HOME"
java -version

$defaultCache = Join-Path $projectRoot '.gradle-user-home-validation'
if ([string]::IsNullOrWhiteSpace($GradleUserHome)) {
    $cache = $defaultCache
}
else {
    $cache = $GradleUserHome
}

if (-not (Test-Path -LiteralPath $cache -PathType Container)) {
    throw "GRADLE_USER_HOME nao encontrado: $cache"
}
if (-not (Test-WritableDirectory $cache)) {
    throw "GRADLE_USER_HOME nao gravavel: $cache"
}

$env:GRADLE_USER_HOME = $cache
Write-Host "GRADLE_USER_HOME selecionado: $env:GRADLE_USER_HOME"

Write-Host 'Parando apenas daemons Gradle associados a este cache...'
Invoke-Gradle @('--stop', '--console=plain')

Write-Host 'Verificando o Gradle Wrapper...'
Invoke-Gradle @('--version', '--console=plain')

$resolutionArgs = @('dependencies', '--configuration', 'testCompileClasspath', '--console=plain', '--no-daemon')
if ($Offline) { $resolutionArgs += '--offline' }
Write-Host "Testando resolucao de dependencias (Offline=$Offline)..."
Invoke-Gradle $resolutionArgs

$focusedTests = @(
    'com.example.carteirainvestimento.service.AuthTokenServiceSecurityTest',
    'com.example.carteirainvestimento.api.JwtContextSecurityTest'
)
foreach ($testClass in $focusedTests) {
    $testArgs = @('test', '--tests', $testClass, '--console=plain', '--no-daemon')
    if ($Offline) { $testArgs += '--offline' }
    Write-Host "Executando teste focalizado: $testClass"
    Invoke-Gradle $testArgs
}

if (-not $SkipRegression) {
    $regressionCommands = @(
        @('clean', 'test', '--console=plain', '--no-daemon'),
        @('build', '--console=plain', '--no-daemon'),
        @('spotbugsMain', '--rerun-tasks', '--console=plain', '--no-daemon')
    )
    foreach ($command in $regressionCommands) {
        if ($Offline) { $command += '--offline' }
        Write-Host "Executando regressao: $($command -join ' ')"
        Invoke-Gradle $command
    }
}

Write-Host 'Validacao Gradle concluida sem falhas.'
