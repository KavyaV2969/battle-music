param([switch]$SkipAudio)
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    $python = if ($env:BATTLEMUSIC_PYTHON) { $env:BATTLEMUSIC_PYTHON } else { Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' }
    if (!(Test-Path -LiteralPath $python)) { $python = 'python' }
    $args = @('scripts/build.py')
    if ($SkipAudio) { $args += '--skip-audio' }
    & $python @args
    if ($LASTEXITCODE -ne 0) { throw 'Battle Music build failed.' }
} finally { Pop-Location }
