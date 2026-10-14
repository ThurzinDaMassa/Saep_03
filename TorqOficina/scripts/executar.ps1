param(
    [ValidateSet('demo', 'mysql', 'preparar', 'compilar', 'diagnostico')]
    [string]$Acao = 'demo',
    [string]$Configuracao = '',
    [switch]$SemAbrir
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$taskProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskProjectRoot

function Encontrar-Java([bool]$PrecisaJdk) {
    $taskCandidates = New-Object System.Collections.Generic.List[string]
    if ($env:JAVA_HOME) { $taskCandidates.Add((Join-Path $env:JAVA_HOME 'bin\java.exe')) }
    foreach ($taskBase in @(
        "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Amazon Corretto",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
    )) {
        if (Test-Path -LiteralPath $taskBase) {
            Get-ChildItem -LiteralPath $taskBase -Directory | Sort-Object Name -Descending | ForEach-Object {
                $taskCandidates.Add((Join-Path $_.FullName 'bin\java.exe'))
            }
        }
    }
    Get-Command java.exe -All -ErrorAction SilentlyContinue | ForEach-Object { $taskCandidates.Add($_.Source) }
    foreach ($taskCandidate in ($taskCandidates | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $taskCandidate)) { continue }
        if ($PrecisaJdk -and -not (Test-Path -LiteralPath (Join-Path (Split-Path -Parent $taskCandidate) 'javac.exe'))) { continue }
        $taskInfo = New-Object System.Diagnostics.ProcessStartInfo
        $taskInfo.FileName = $taskCandidate
        $taskInfo.Arguments = '-version'
        $taskInfo.UseShellExecute = $false
        $taskInfo.CreateNoWindow = $true
        $taskInfo.RedirectStandardError = $true
        $taskInfo.RedirectStandardOutput = $true
        $taskProbe = New-Object System.Diagnostics.Process
        $taskProbe.StartInfo = $taskInfo
        try {
            [void]$taskProbe.Start()
            if (-not $taskProbe.WaitForExit(8000)) { $taskProbe.Kill(); continue }
            $taskVersion = $taskProbe.StandardError.ReadToEnd() + $taskProbe.StandardOutput.ReadToEnd()
            if ($taskProbe.ExitCode -eq 0 -and $taskVersion -match 'version "(\d+)') {
                if ([int]$Matches[1] -ge 17) { return $taskCandidate }
            }
        } catch { continue }
        finally { $taskProbe.Dispose() }
    }
    throw 'Java 17 ou superior não encontrado. Instale um JDK 17/21, extraia o ZIP inteiro e abra novamente INICIAR_SISTEMA.bat. Link: https://adoptium.net/temurin/releases/'
}

function Compilar-Projeto([string]$Java) {
    $taskJavaBin = Split-Path -Parent $Java
    $taskJavac = Join-Path $taskJavaBin 'javac.exe'
    $taskJar = Join-Path $taskJavaBin 'jar.exe'
    if (-not (Test-Path -LiteralPath $taskJavac) -or -not (Test-Path -LiteralPath $taskJar)) {
        throw 'Para recompilar, instale um JDK 17 ou superior. Para executar o programa pronto, use INICIAR_SISTEMA.bat.'
    }
    New-Item -ItemType Directory -Path 'build', 'dist' -Force | Out-Null
    $taskSources = @(Get-ChildItem -LiteralPath 'src\main\java' -Recurse -Filter '*.java' |
        ForEach-Object { '"' + $_.FullName.Substring($taskProjectRoot.Length + 1).Replace('\', '/') + '"' })
    if ($taskSources.Count -eq 0) { throw 'Fontes Java ausentes. Extraia o pacote completo.' }
    [System.IO.File]::WriteAllLines((Join-Path $taskProjectRoot 'build\fontes.txt'), $taskSources, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host 'Compilando todas as classes Java...'
    & $taskJavac --release 17 -encoding UTF-8 -d build '@build/fontes.txt'
    if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou. Veja os erros acima.' }
    # Inclui somente os pacotes atuais, evitando classes antigas na distribuição.
    & $taskJar '-J-Djava.io.tmpdir=build' --create --file 'dist\TorqOficina.jar' --main-class oficina.App -C build oficina
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível gerar o JAR.' }
    Write-Host 'Compilação concluída: dist\TorqOficina.jar'
}

try {
    $taskPrecisaJdk = $Acao -eq 'compilar' -or -not (Test-Path -LiteralPath 'dist\TorqOficina.jar')
    $taskJava = Encontrar-Java $taskPrecisaJdk
    if ($Acao -eq 'compilar') { Compilar-Projeto $taskJava; exit 0 }
    if (-not (Test-Path -LiteralPath 'dist\TorqOficina.jar')) { Compilar-Projeto $taskJava }
    if (-not $Configuracao) {
        if ($Acao -eq 'demo') { $Configuracao = 'config.demo.properties' }
        elseif (Test-Path -LiteralPath 'config.local.properties') { $Configuracao = 'config.local.properties' }
        else { $Configuracao = 'config.properties' }
    }
    if (-not (Test-Path -LiteralPath $Configuracao)) { throw "Configuração não encontrada: $Configuracao" }
    $taskConfigText = Get-Content -LiteralPath $Configuracao -Raw
    $taskIsLocal = $taskConfigText -match '(?m)^\s*db.engine\s*=\s*h2\s*$'
    $taskDriver = if ($taskIsLocal) { 'lib\h2-2.5.252.jar' } else { 'lib\mysql-connector-j-8.4.0.jar' }
    if (-not (Test-Path -LiteralPath $taskDriver)) { throw "Driver ausente: $taskDriver. Extraia o ZIP inteiro." }
    # Caminhos longos do TEMP podem impedir o Java de criar sua conexão interna no Windows.
    $taskShortTemp = (& cmd.exe /d /c 'for %I in ("%TEMP%") do @echo %~sI').Trim()
    $taskWindowsTemp = Join-Path $env:SystemRoot 'Temp'
    $taskProbeFile = Join-Path $taskWindowsTemp ([System.IO.Path]::GetRandomFileName())
    try {
        # Um nome físico sem acentos evita falhas do socket interno de algumas versões do JDK.
        [System.IO.File]::WriteAllText($taskProbeFile, '')
        [System.IO.File]::Delete($taskProbeFile)
        $taskShortTemp = $taskWindowsTemp
    } catch {
        # Sem acesso à pasta temporária do Windows, usa o TEMP do próprio usuário.
    }
    $taskArguments = @('-Dfile.encoding=UTF-8', "-Djdk.net.unixdomain.tmpdir=$taskShortTemp",
        "-Dtorq.config=$Configuracao", '-cp', 'dist\TorqOficina.jar;lib\*', 'oficina.App')
    if ($Acao -eq 'diagnostico') { $taskArguments += '--diagnostico' }
    elseif ($Acao -eq 'preparar') { $taskArguments += '--init' }
    else {
        if ($Acao -eq 'mysql') {
            & $taskJava @taskArguments --init
            if ($LASTEXITCODE -ne 0) { throw 'Não foi possível preparar o MySQL. Confira a configuração e inicie o servidor do banco.' }
        }
        if (-not $SemAbrir) { $taskArguments += '--abrir' }
    }
    Write-Host "Java encontrado: $taskJava"
    & $taskJava @taskArguments
    exit $LASTEXITCODE
} catch {
    Write-Host ''
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
