param(
    [string]$ModelPath = (Join-Path $PSScriptRoot '../models/Qwen3-0.6B-Q4_K_M.gguf'),
    [string]$RuntimePath,
    [int]$Port = 8080
)
$ErrorActionPreference = 'Stop'
if (!$RuntimePath) {
    $runtimeCommand = Get-Command llama-server.exe -ErrorAction SilentlyContinue
    if ($runtimeCommand) { $RuntimePath = $runtimeCommand.Source }
    else {
        $RuntimePath = Get-ChildItem -LiteralPath "$env:LOCALAPPDATA/Microsoft/WinGet/Packages" -Filter llama-server.exe -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
    }
}
if (!$RuntimePath -or !(Test-Path -LiteralPath $RuntimePath)) { throw 'Informe -RuntimePath com o caminho do llama-server.exe instalado.' }
$resolvedModel = (Resolve-Path -LiteralPath $ModelPath).Path
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "Porta $Port ocupada. Verifique o servidor existente antes de iniciar outro."
}
$logDirectory = Join-Path $PSScriptRoot '../build/ai-runtime'
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
$runtimeProcess = Start-Process -FilePath $RuntimePath -WindowStyle Hidden -PassThru `
    -ArgumentList @('-m', ('"' + $resolvedModel + '"'), '--host', '127.0.0.1', '--port', "$Port",
        '-c', '2048', '-np', '1', '-t', '4', '-ngl', '0', '--reasoning-budget', '0') `
    -RedirectStandardOutput (Join-Path $logDirectory 'stdout.log') `
    -RedirectStandardError (Join-Path $logDirectory 'stderr.log')
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if ($runtimeProcess.HasExited) { throw 'Runtime terminou. Consulte build/ai-runtime/stderr.log.' }
    try {
        $health = Invoke-RestMethod "http://127.0.0.1:$Port/health" -TimeoutSec 1
        if ($health.status -eq 'ok') {
            Write-Output "IA local pronta. PID=$($runtimeProcess.Id), endpoint=http://127.0.0.1:$Port/v1/chat/completions"
            exit 0
        }
    } catch { }
    Start-Sleep -Milliseconds 500
}
throw 'Runtime ainda nao respondeu. Consulte os logs; nao foi iniciado um segundo processo.'
