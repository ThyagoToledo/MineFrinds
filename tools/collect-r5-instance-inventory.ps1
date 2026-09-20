[CmdletBinding()]
param(
    [string]$InstancesRoot = 'C:\Users\thyag\curseforge\minecraft\Instances',
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $PSScriptRoot '..\doc\03_context\minecraft-companheiros-r5-inventario-instances.md'
}

function Get-RelativeFileNames {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return @() }
    return @(Get-ChildItem -LiteralPath $Path -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name })
}

function Get-LoaderGuess {
    param($Metadata)
    if ($null -ne $Metadata -and $null -ne $Metadata.baseModLoader) {
        if ($Metadata.baseModLoader.name) { return [string]$Metadata.baseModLoader.name }
        if ($Metadata.baseModLoader.filename) { return [string]$Metadata.baseModLoader.filename }
    }
    return 'nao identificado no minecraftinstance.json'
}

if (-not (Test-Path -LiteralPath $InstancesRoot)) {
    throw "Diretorio de instancias nao encontrado: $InstancesRoot"
}

$rows = foreach ($instance in (Get-ChildItem -LiteralPath $InstancesRoot -Directory | Sort-Object Name)) {
    $modsPath = Join-Path $instance.FullName 'mods'
    $configPath = Join-Path $instance.FullName 'config'
    $savesPath = Join-Path $instance.FullName 'saves'
    $rootFiles = Get-RelativeFileNames -Path $instance.FullName
    $modFiles = Get-RelativeFileNames -Path $modsPath
    $json = Join-Path $instance.FullName 'minecraftinstance.json'
    $minecraftVersion = 'nao informado'
    $metadata = $null
    if (Test-Path -LiteralPath $json) {
        try {
            $metadata = Get-Content -LiteralPath $json -Raw | ConvertFrom-Json
        if ($metadata.gameVersion) { $minecraftVersion = [string]$metadata.gameVersion }
        } catch {
            $minecraftVersion = 'metadata invalido'
        }
    }
    [pscustomobject]@{
        Name = $instance.Name
        MinecraftVersion = $minecraftVersion
        ModCount = $modFiles.Count
        LoaderGuess = Get-LoaderGuess -Metadata $metadata
        HasConfig = Test-Path -LiteralPath $configPath
        HasSaves = Test-Path -LiteralPath $savesPath
        HasMetadata = Test-Path -LiteralPath $json
        RootFiles = ($rootFiles -join ', ')
    }
}

$date = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'
$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('---')
$lines.Add('tags: [minecraft, minefriends, r5, curseforge, inventario]')
$lines.Add("updated: $($date.Substring(0, 10))")
$lines.Add('status: inventario-automatizado')
$lines.Add('---')
$lines.Add('')
$lines.Add('# R5 - Inventario das instancias CurseForge')
$lines.Add('')
$lines.Add("Gerado em **$date** pelo script tools/collect-r5-instance-inventory.ps1.")
$lines.Add('')
$lines.Add('Este relatorio e somente leitura. A presenca de uma instancia nao significa que o mod foi carregado ou homologado nela; os testes funcionais continuam exigindo copia do mundo e execucao do cliente/servidor.')
$lines.Add('')
$lines.Add('| Instancia | Minecraft | Mods | Loader declarado | Config | Saves | Metadata |')
$lines.Add('|---|---:|---:|---|:---:|:---:|:---:|')
foreach ($row in $rows) {
    $config = if ($row.HasConfig) { 'sim' } else { 'nao' }
    $saves = if ($row.HasSaves) { 'sim' } else { 'nao' }
    $metadata = if ($row.HasMetadata) { 'sim' } else { 'nao' }
    $name = $row.Name.Replace('|', '\|')
    $lines.Add("| $name | $($row.MinecraftVersion) | $($row.ModCount) | $($row.LoaderGuess) | $config | $saves | $metadata |")
}
$lines.Add('')
$lines.Add('## Proximo passo de homologacao')
$lines.Add('')
$lines.Add('Para cada instancia, criar uma copia descartavel, instalar o JAR correspondente, iniciar o cliente e preencher os casos R5 em doc/01_plan/R5-homologacao-modpacks-release.md. Registrar loader, Java, hardware, hash do JAR, logs e metricas; nao alterar os saves originais.')

$parent = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $parent | Out-Null
Set-Content -LiteralPath $OutputPath -Value $lines -Encoding UTF8
Write-Output "Relatorio escrito em $OutputPath ($($rows.Count) instancias)."
