$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path -Parent $PSScriptRoot
$taskDeliveryRoot = Split-Path -Parent $taskProjectRoot
$taskZipPath = Join-Path $taskDeliveryRoot 'TorqOficina_Grupo04_Pronto_Para_Rodar.zip'
$taskTemporaryZip = Join-Path $taskDeliveryRoot 'TorqOficina_Grupo04_Pronto_Para_Rodar.tmp.zip'

if (-not (Test-Path -LiteralPath (Join-Path $taskProjectRoot 'dist\TorqOficina.jar'))) {
    throw 'Execute compilar.bat antes de gerar o pacote.'
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskArchive = [System.IO.Compression.ZipFile]::Open($taskTemporaryZip, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($taskRootFile in @('INICIAR_SISTEMA.bat', 'LEIA_PRIMEIRO.txt', 'COMO_RODAR_E_APRESENTAR_O_SISTEMA.txt', 'README.md')) {
        $taskFile = Join-Path $taskDeliveryRoot $taskRootFile
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($taskArchive, $taskFile, $taskRootFile)
    }
    $taskIdeConfigurations = Join-Path $taskDeliveryRoot '.run'
    if (Test-Path -LiteralPath $taskIdeConfigurations) {
        Get-ChildItem -LiteralPath $taskIdeConfigurations -File | ForEach-Object {
            [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($taskArchive, $_.FullName, ('.run/' + $_.Name))
        }
    }
    Get-ChildItem -LiteralPath $taskProjectRoot -Recurse -File | ForEach-Object {
        $taskRelative = $_.FullName.Substring($taskProjectRoot.Length + 1).Replace('\', '/')
        if ($taskRelative -match '^(build|target|dados|\.git)/' -or
            $taskRelative -match '(^|/)__pycache__/' -or
            $taskRelative -match '^config\.(local|teste)\.properties$' -or
            $taskRelative -match '\.(log|pyc)$') { return }
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $taskArchive, $_.FullName, ('TorqOficina/' + $taskRelative))
    }
} finally {
    $taskArchive.Dispose()
}

# Ambos os caminhos são fixos e ficam na pasta da entrega.
if (-not ([System.IO.Path]::GetFullPath($taskZipPath)).StartsWith($taskDeliveryRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
    -not ([System.IO.Path]::GetFullPath($taskTemporaryZip)).StartsWith($taskDeliveryRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Caminho de pacote fora da entrega.'
}
Move-Item -LiteralPath $taskTemporaryZip -Destination $taskZipPath -Force
Write-Host "Pacote pronto: $taskZipPath"
