[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$AndroidSdk = ''
)

$ErrorActionPreference = 'Stop'
$keystorePath = Join-Path $env:USERPROFILE 'Q25Signing\matchamisu-release.p12'
$keystoreType = 'PKCS12'
$keyAlias = 'matchamisu-release'
$expectedPackage = 'com.matchamisu.ime'
$expectedVersionCode = '87'
$expectedVersionName = '0.1.0'
$apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\stable\release\app-stable-release.apk'
$signingNames = @(
    'PASTIERA_KEYSTORE_PATH', 'PASTIERA_KEYSTORE_PASSWORD',
    'PASTIERA_KEY_ALIAS', 'PASTIERA_KEY_PASSWORD'
)

if ($JavaHome) {
    $java = Join-Path $JavaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'Java not found under JAVA_HOME.' }
    $env:JAVA_HOME = $JavaHome
} elseif (-not (Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue)) {
    throw 'JDK 21 is required. Set JAVA_HOME or add java.exe to PATH.'
}

if (-not $AndroidSdk) { $AndroidSdk = $env:ANDROID_SDK_ROOT }
if (-not $AndroidSdk) { $AndroidSdk = $env:ANDROID_HOME }
if (-not $AndroidSdk) { $AndroidSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$buildToolsRoot = Join-Path $AndroidSdk 'build-tools'
if (-not (Test-Path -LiteralPath $buildToolsRoot -PathType Container)) {
    throw 'Android SDK build-tools not found. Set ANDROID_SDK_ROOT or pass -AndroidSdk.'
}
$buildTools = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $buildTools) { throw 'No Android build-tools version is installed.' }
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
$aapt2 = Join-Path $buildTools.FullName 'aapt2.exe'
if (-not (Test-Path -LiteralPath $apksigner -PathType Leaf) -or
    -not (Test-Path -LiteralPath $aapt2 -PathType Leaf)) {
    throw 'apksigner.bat or aapt2.exe is missing from the selected build-tools directory.'
}
if (-not (Test-Path -LiteralPath $keystorePath -PathType Leaf)) {
    throw 'The configured external Stable keystore was not found.'
}

# Gradle gives local keystore properties precedence over environment variables.
# Refuse them so this script cannot silently build with a different key.
$localSigningFiles = @(
    (Join-Path $PSScriptRoot 'release\keystore.properties'),
    (Join-Path $PSScriptRoot 'keystore.properties')
) | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf }
if ($localSigningFiles) {
    throw 'Local Gradle signing properties exist. Remove them from the build environment before using this script; their contents were not read.'
}

try {
    $env:PASTIERA_KEYSTORE_PATH = $keystorePath
    $env:PASTIERA_KEY_ALIAS = $keyAlias
    # PKCS12 signing uses the same password for the store and private-key entry.
    $storeSecure = Read-Host 'Matchamisù PKCS12 password' -AsSecureString
    $storeBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storeSecure)
    try {
        $env:PASTIERA_KEYSTORE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($storeBstr)
        $env:PASTIERA_KEY_PASSWORD = $env:PASTIERA_KEYSTORE_PASSWORD
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($storeBstr)
        $storeSecure.Dispose()
        Remove-Variable storeSecure,storeBstr
    }

    Push-Location $PSScriptRoot
    try {
        & (Join-Path $PSScriptRoot 'gradlew.bat') --no-daemon :app:assembleStableRelease
        if ($LASTEXITCODE -ne 0) { throw 'Stable Release build failed.' }
    } finally {
        Pop-Location
    }

    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw 'Stable Release APK was not produced.' }
    $signature = & $apksigner verify --verbose --print-certs $apk 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $badging = & $aapt2 dump badging $apk 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'APK manifest inspection failed.' }
    $packageLine = @($badging | Where-Object { $_ -match '^package: ' }) | Select-Object -First 1
    $identity = [regex]::Match([string]$packageLine, "^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'")
    if (-not $identity.Success -or $identity.Groups[1].Value -ne $expectedPackage -or
        $identity.Groups[2].Value -ne $expectedVersionCode -or
        $identity.Groups[3].Value -ne $expectedVersionName) {
        throw 'APK application ID or version does not match the Stable release candidate.'
    }
    $certificateLine = @($signature | ForEach-Object { [string]$_ } | Where-Object {
        $_ -match '^Signer #1 certificate SHA-256 digest: '
    }) | Select-Object -First 1
    if (-not $certificateLine) { throw 'Signer certificate SHA-256 was not reported.' }

    Write-Host "Stable APK: $apk"
    Write-Host $packageLine
    Write-Host $certificateLine
    Write-Host "APK SHA-256: $((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash)"
} finally {
    foreach ($name in $signingNames) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
}
