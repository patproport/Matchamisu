# Matchamisù Phase 4-E3: Windows release verification (not yet executed)

For the **first public release**, use `stable-first-release-checklist.md`: only signed Stable `0.1.0`/code `87` is in scope. The broader Stable/Nightly commands below are retained as the historical Phase 4-E3 procedure for later development verification; they have not been run by Codex.

This checklist is for the person building on Windows. No command below was run during the Codex Phase 4-E3 audit. Use only disposable test text for logcat checks. Keep signing files, dependency reports, extracted APK contents, and logcat captures private until reviewed.

## 1. Configuration and dependency evidence

The source declares Gradle wrapper 8.13, Android Gradle Plugin 8.11.0, Kotlin 2.0.21, compile SDK 36, Java/Kotlin target 11, and project guidance for JDK 21. The selected release dependency graph has **not** been generated in this audit. AndroidX Compose versions are selected through BOM 2024.09.00; direct declarations alone do not specify the resolved transitive versions or final packaged code.

From a PowerShell window in the project root, with the Android SDK and JDK 21 configured:

```powershell
$audit = Join-Path $env:TEMP 'matchamisu-release-audit'
New-Item -ItemType Directory -Force -Path $audit | Out-Null
java -version
.\gradlew.bat --version
.\gradlew.bat :app:dependencies --configuration stableReleaseRuntimeClasspath > (Join-Path $audit 'stableReleaseRuntimeClasspath.txt')
.\gradlew.bat :app:dependencies --configuration nightlyReleaseRuntimeClasspath > (Join-Path $audit 'nightlyReleaseRuntimeClasspath.txt')
.\gradlew.bat :app:dependencyInsight --configuration stableReleaseRuntimeClasspath --dependency protobuf-javalite > (Join-Path $audit 'protobuf-insight.txt')
```

Review every selected artifact/version and its published POM/source license and NOTICE, including transitive AndroidX, Compose, Kotlin, Guava, OkHttp, protobuf, and Shizuku modules. Resolve any `FAILED` lines in the reports. The generated stable SBOM must be compared with this selected graph. A Gradle graph is evidence of resolution, not proof that every class or resource remains after release shrinking.

## 2. Build and package

The following release builds require the existing signing properties or environment variables described in `app/build.gradle.kts`: stable `PASTIERA_KEYSTORE_PATH`, `PASTIERA_KEYSTORE_PASSWORD`, `PASTIERA_KEY_ALIAS`, `PASTIERA_KEY_PASSWORD`; nightly uses the corresponding `PASTIERA_NIGHTLY_*` variables. The ignored `release/keystore.properties` is another supported input. Do not place credentials in commands, captured logs, or the public repository. If signing is unavailable, `-PPASTIERA_UNSIGNED_RELEASE_BUILD=true` can produce an **unsigned inspection APK only**, not a distribution artifact. Nightly Release has a distinct signing check; a stable signing setup does not satisfy it.

```powershell
.\gradlew.bat :app:testNightlyDebugUnitTest :app:assembleNightlyDebug
.\gradlew.bat :app:testStableReleaseUnitTest :app:assembleStableRelease
.\gradlew.bat :app:testNightlyReleaseUnitTest :app:assembleNightlyRelease
```

Expected paths, subject to the actual Gradle output: `app/build/outputs/apk/nightly/debug/app-nightly-debug.apk`, `app/build/outputs/apk/stable/release/app-stable-release.apk`, and `app/build/outputs/apk/nightly/release/app-nightly-release.apk`. Confirm signed/unsigned status; filenames alone do not prove it. `app/build.gradle.kts` generates `assets/matchamisu-sbom.cdx.json` for **stableRelease only**; no corresponding nightly SBOM task is declared. If a build fails, retain the error and fix the cause before publication. Do not switch to an unsigned artifact for distribution.

## 3. Inspect each final APK

Set `$apk` to an APK produced above. Android SDK Command-Line Tools provide `apkanalyzer.bat`; adjust the path if `latest` is not installed.

```powershell
$apk = (Resolve-Path 'app/build/outputs/apk/stable/release/app-stable-release.apk').Path
$apkanalyzer = Join-Path $env:LOCALAPPDATA 'Android\Sdk\cmdline-tools\latest\bin\apkanalyzer.bat'
& $apkanalyzer apk summary $apk
& $apkanalyzer manifest debuggable $apk
& $apkanalyzer files list $apk | Select-String 'third_party_notices|licenses/|mozc.data|libmozc.so|sbom|font|icon'
Get-FileHash -LiteralPath $apk -Algorithm SHA256
```

Expected stable application ID: `com.matchamisu.ime`; expected nightly ID: `com.matchamisu.ime.nightly`. The default source version is `0.86`/code `86`, but Gradle properties may override it. Inspect the manifest labels and launcher resources visually on the device; a resource filename cannot establish the rendered icon. Confirm `manifest debuggable` is false for release. Use Android SDK `apksigner.bat verify --print-certs <APK>` and record the certificate digest before publishing.

The next snippet reads APK ZIP entries **without extracting them or changing source files**. Run once for stable and once for nightly. Match the hashes against `source-built-mozc/metadata/hashes.txt` and `source-built-mozc/generated-proto/hashes.txt` as applicable.

```powershell
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($apk)
try {
    $zip.Entries.FullName | Where-Object { $_ -match '^assets/common/(third_party_notices\.md|licenses/)|^assets/(mozc\.data|matchamisu-sbom\.cdx\.json)$|^lib/arm64-v8a/libmozc\.so$' } | Sort-Object
    foreach ($name in @('lib/arm64-v8a/libmozc.so', 'assets/mozc.data')) {
        $entry = $zip.GetEntry($name)
        if ($null -eq $entry) { throw "Missing APK entry: $name" }
        $stream = $entry.Open()
        $sha = [System.Security.Cryptography.SHA256]::Create()
        try { "$name $(([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '').ToLowerInvariant())" }
        finally { $sha.Dispose(); $stream.Dispose() }
    }
    $sbomEntry = $zip.GetEntry('assets/matchamisu-sbom.cdx.json')
    if ($sbomEntry) {
        $reader = [System.IO.StreamReader]::new($sbomEntry.Open())
        try { $sbom = $reader.ReadToEnd() | ConvertFrom-Json }
        finally { $reader.Dispose() }
        $sbom.metadata.component
        $sbom.components | Select-Object group,name,version
    } else { 'SBOM entry missing (expected for stableRelease; currently not generated for nightlyRelease)' }
}
finally { $zip.Dispose() }
```

The two OFL files, Shizuku license, Mozc license/data texts, and public/in-app NOTICE content should be present and readable. A license file being in the APK does not by itself clear every transitive dependency or resource.

## 4. Privacy and Q25 behavior

On the Q25, test the **release** package with a unique harmless string, in both Matchamisù's test field and an external editor such as LINE. Capture logcat locally while entering and deleting it; inspect all app tags and the string, and do not publish the raw capture. Also check the About license/NOTICE and SBOM actions. Confirm Japanese Mozc input, Space conversion, candidate tap continuation, partial Enter, LINE send tests A–E, English suggestions, GRAVE/Alt+GRAVE, SYM/Ctrl/Alt, Trackpad, settings language switching, themes, and Home/icon rendering. The Back-after-candidate-bar pointer restriction is an existing unresolved Known Issue; do not mark it fixed by this check.

Record `Get-FileHash` of the final signed APK and the source revision identifier used to build it. Publish the matching source state and checksums together. A hash match for the three Mozc outputs is useful provenance evidence; a byte-for-byte reproducible **whole APK** is a separate, stronger goal. Do not claim either without performing the corresponding test.

## 5. Publication gate

Before the first GitHub Release, review the exact publication file list and history for diagnostics, credentials, signing material, local paths, old AquaMozc artifacts, and legacy Pastiera metadata. `.gitignore` does not remove already tracked files. The stable release workflow is disabled, while `.github/workflows/debug.yml` remains manually runnable and defaults to publishing a Pastiera-branded nightly pre-release if triggered. Avoid running it as-is. Manual release upload of the reviewed signed APK, checksum, matching source state, GPL/third-party notices, and known limitations is a viable initial path once the audit gates above pass.
