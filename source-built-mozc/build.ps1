param(
    [string]$DockerExe,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [string]$SourceVolume = 'meltype-mozc',
    [string]$BazelCacheVolume = 'meltype-bazel'
)

$ErrorActionPreference = 'Stop'
$q25Root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$verificationRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'verification'))
$outputRoot = [IO.Path]::GetFullPath($OutputDirectory)
if (-not $outputRoot.StartsWith($verificationRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "OutputDirectory must be a child of $verificationRoot so canonical Mozc inputs cannot be overwritten"
}
if ((Test-Path -LiteralPath $outputRoot) -and (Get-ChildItem -LiteralPath $outputRoot -Force | Select-Object -First 1)) {
    throw "OutputDirectory must be empty: $outputRoot"
}
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
$commit = (Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'metadata\MOZC_COMMIT')).Trim()
if ($commit -ne 'a069a88d4cb5c011de0f9aebb6c149a1c808d904') {
    throw "Unexpected Matchamisù Mozc revision: $commit"
}
if (-not $DockerExe) {
    $dockerCommand = Get-Command docker -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($dockerCommand) { $DockerExe = $dockerCommand.Source }
    else {
        $DockerExe = @(
            (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe'),
            (Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin\docker.exe')
        ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    }
}
if (-not $DockerExe -or -not (Test-Path -LiteralPath $DockerExe)) { throw 'Docker CLI not found' }

& $DockerExe run --rm --platform linux/amd64 `
    --mount "type=bind,source=$($q25Root.Replace('\', '/')),target=/q25,readonly" `
    --mount "type=bind,source=$($outputRoot.Replace('\', '/')),target=/out" `
    --mount "type=volume,source=$SourceVolume,target=/mozc" `
    --mount "type=volume,source=$BazelCacheVolume,target=/root/.cache/bazel" `
    -e "MOZC_SOURCE_VOLUME=$SourceVolume" `
    -e "MOZC_BAZEL_CACHE_VOLUME=$BazelCacheVolume" `
    -e USE_BAZEL_VERSION=9.0.2 `
    -w /q25 meltype-build bash /q25/source-built-mozc/build-in-container.sh
if ($LASTEXITCODE -ne 0) { throw "Mozc build failed (exit $LASTEXITCODE)" }
