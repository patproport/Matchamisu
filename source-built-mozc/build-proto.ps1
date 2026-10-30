param(
    [string]$DockerExe,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [string]$SourceVolume = 'meltype-mozc',
    [string]$BazelCacheVolume = 'meltype-bazel'
)

$ErrorActionPreference = 'Stop'
$verificationRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'verification'))
$outputRoot = [IO.Path]::GetFullPath($OutputDirectory)
if (-not $outputRoot.StartsWith($verificationRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "OutputDirectory must be a child of $verificationRoot so canonical Mozc inputs cannot be overwritten"
}
$generatedProtoOutput = Join-Path $outputRoot 'generated-proto'
if ((Test-Path -LiteralPath $generatedProtoOutput) -and (Get-ChildItem -LiteralPath $generatedProtoOutput -Force | Select-Object -First 1)) {
    throw "Generated proto output must be empty: $generatedProtoOutput"
}
New-Item -ItemType Directory -Force -Path $generatedProtoOutput | Out-Null

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
    --mount "type=bind,source=$($PSScriptRoot.Replace('\', '/')),target=/scripts,readonly" `
    --mount "type=bind,source=$($generatedProtoOutput.Replace('\', '/')),target=/out" `
    --mount "type=volume,source=$SourceVolume,target=/mozc" `
    --mount "type=volume,source=$BazelCacheVolume,target=/root/.cache/bazel" `
    -e "MOZC_SOURCE_VOLUME=$SourceVolume" `
    -e "MOZC_BAZEL_CACHE_VOLUME=$BazelCacheVolume" `
    -e USE_BAZEL_VERSION=9.0.2 `
    -w /mozc/src meltype-build bash /scripts/build-proto-in-container.sh
if ($LASTEXITCODE -ne 0) { throw "Official Mozc proto build failed (exit $LASTEXITCODE)" }
