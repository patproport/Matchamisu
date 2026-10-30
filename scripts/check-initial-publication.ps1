param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $root
try {
    & git rev-parse --show-toplevel *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Initialize Git before checking the staged publication set.' }
    $staged = @(& git diff --cached --name-only --diff-filter=ACMR)
    if ($LASTEXITCODE -ne 0 -or $staged.Count -eq 0) { throw 'No staged files to review.' }

    $required = @('.gitignore', 'README.md', 'LICENSE', 'THIRD_PARTY_NOTICES.md',
        'build-stable-release.ps1', 'app/build.gradle.kts', 'app/proguard-rules.pro',
        'app/src/main/AndroidManifest.xml',
        'app/src/main/assets/common/third_party_notices.md',
        'app/src/main/assets/common/licenses/OkHttp-PublicSuffixList-NOTICE.txt',
        'app/src/main/assets/common/licenses/Mozc-mozcjni-LICENSE.txt',
        'app/libs/mozc-proto.jar', 'gradlew', 'gradlew.bat',
        'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties',
        'source-built-mozc/arm64-v8a/libmozc.so', 'source-built-mozc/data/mozc.data',
        'source-built-mozc/README.md', 'source-built-mozc/build.ps1',
        'source-built-mozc/build-in-container.sh', 'source-built-mozc/build-proto.ps1',
        'source-built-mozc/build-proto-in-container.sh',
        'source-built-mozc/toolchain/Dockerfile',
        'source-built-mozc/metadata/MOZC_COMMIT',
        'source-built-mozc/metadata/hashes.txt',
        'source-built-mozc/generated-proto/hashes.txt',
        'source-built-mozc/generated-proto/mozc-proto-official.jar',
        'third_party/mozc/mozcjni-LICENSE.txt',
        'third_party/okhttp/PublicSuffixList-NOTICE.txt',
        'scripts/check-initial-publication.ps1')
    $problems = [System.Collections.Generic.List[string]]::new()
    foreach ($path in $required) {
        if ($path -notin $staged) { $problems.Add("Required file absent: $path") }
    }

    $forbiddenPath = '(?i)(^CODEX_HANDOFF\.md$|^local\.properties$|^\.github/workflows/|^docs/device-archives/|^signing/|^fastlane/|^fdroiddata/|^tools/prediction_bench/|^source-built-mozc/(backup|verification)/|^source-built-mozc/generated-proto/(class-jars|verification)/|(^|/)(build|\.gradle|\.idea|\.codex|dict_backup)/|\.(apk|aab|apks|p12|pfx|jks|keystore|pem|key|log|trace|dump|bak|orig)$)'
    $shortHome = 'PAT' + 'PRO~1'
    $sensitiveText = '(?i)([A-Z]:\\Users\\[^\\\s]+|/(Users|home)/[A-Za-z0-9._-]+|' + $shortHome + '|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{20,}|ghp_[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{25,})'
    $textExtensions = @('.ps1','.bat','.cmd','.sh','.kts','.kt','.java','.xml','.md','.txt','.json','.toml','.properties','.gradle','.py','.yml','.yaml','.html','.css')
    foreach ($path in $staged) {
        if ($path -match $forbiddenPath) { $problems.Add("Forbidden staged path: $path"); continue }
        if ([IO.Path]::GetExtension($path) -notin $textExtensions) { continue }
        $lineNumber = 0
        foreach ($line in [IO.File]::ReadLines((Join-Path $root $path))) {
            $lineNumber++
            if ($line -match $sensitiveText) {
                $problems.Add("Sensitive pattern: ${path}:$lineNumber")
            }
        }
    }
    Write-Host "Staged files reviewed: $($staged.Count)"
    if ($problems.Count -gt 0) {
        $problems | ForEach-Object { Write-Warning $_ }
        throw 'Publication staging check failed. No matched values were printed.'
    }
    Write-Host 'Staging path and high-risk text checks passed. License, asset rights, APK, and source-offer gates still require manual review.'
} finally {
    Pop-Location
}
