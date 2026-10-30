# Matchamisù first Stable Release: Windows verification

Candidate only: `versionName 0.1.0`, `versionCode 87`, application ID `com.matchamisu.ime`. Nightly ID remains `com.matchamisu.ime.nightly` and Nightly is not an initial release artifact. Use JDK 21 and the configured Android SDK. Run these commands in PowerShell from the project root. Keep all reports, logcat, signing inputs, and diagnostic output private until reviewed. The recommended final APK name is `matchamisu-0.1.0-stable.apk`; a filename alone does not establish its identity.

The user reports that the latest signed Stable APK works on Q25 after the scoped R8/Protobuf fix. The inspected APK SHA-256 is `429be283c3f3e2329821dad9d4d6607fff77aead558de25b94d73dee2db168c1`; its Mozc JNI/data hashes match the canonical records. The existing APK SBOM has 98 Maven components plus four hardcoded asset entries, matching the [POM inventory](stable-release-dependency-pom-inventory.md). [github-release-v0.1.0.md](github-release-v0.1.0.md) is a release-note and publication draft, not an approval to upload. Preserve evidence of the exact source state, privacy review, and Q25 results with the release record.

The user confirms the Matchamisù icon was newly generated with ChatGPT without third-party imagery. Its origin/attribution item is closed on that statement. The inspected latest APK includes the new OkHttp PSL and Mozc JNI license assets, and contains no removed typing OGG or Titan caption-bar resources.

## 0. Verify Mozc corresponding-source regeneration

**Clean regeneration completed by the user on 2026-10-30.** Codex compared `source-built-mozc/verification/clean-20261030194646` against both canonical hash manifests and found no differences. The isolated build records Bazel 9.0.2, protoc 34.1, Mozc commit `a069a88d4cb5c011de0f9aebb6c149a1c808d904`, and module-selected NDK r29 package `29.0.14206865`. JNI, data, and generated proto JAR SHA-256 match the canonical files; the user reports 282 generated classes. Keep the isolated result private and excluded from the public tree. The clean-regeneration gate is complete, while full corresponding-source and license clearance still require the checks below.

The repeatable isolated PowerShell sequence is in [source-built-mozc/README.md](../source-built-mozc/README.md). Preserve the successful build information and both complete hash comparisons as release evidence. Hash identity alone does not establish GPL corresponding-source sufficiency. Keep the exact upstream source, local scripts/Dockerfile, configuration/patches, dependency access, and applicable notices available with the release source.

The existing 98-component POM inventory is the starting point for dependency review: 90 Apache-2.0 declarations, four Shizuku MIT declarations, and four POM gaps with specific evidence in the public/in-app NOTICE. Review versioned artifact-specific NOTICE and copyright evidence only where still missing; do not count the old POM audit as a completed license clearance. OkHttp 4.11.0's Public Suffix List is MPL-2.0 data; the repository NOTICE now identifies a fixed official source snapshot and records the APK/OkHttp/source comparisons.

The current signed APK contains OkHttp's embedded NOTICE/data, three AndroidX `META-INF/.../LICENSE.txt` entries, and all 12 app license assets including the new OkHttp PSL and Mozc JNI texts. All 12 assets and the in-app notice match their source files byte for byte. Do not demand a separate NOTICE from each of the 98 POM entries solely because it is transitively resolved; distinguish packaged code/data from build-only tools and metadata placeholders. The PSL source-form availability check is complete: the fixed official commit's text is byte-identical to OkHttp 4.11.0's retained source snapshot, and its 9,499 ordinary/wildcard rules plus eight exception rules match the APK's decoded data. The compressed file also matches the official OkHttp tag and resolved Maven JAR byte for byte. See `THIRD_PARTY_NOTICES.md` for hashes, fixed URLs, and the limit of this evidence.

The initial Git repository was created from an empty state on 2026-10-31. Its explicit 47-path stage contains 769 files and no forbidden paths, APK, signing material, internal handoff, verification outputs, logs, or old workflows. `scripts/check-initial-publication.ps1` passed; a staged-file text/binary pattern scan and all 599 decompressed entries of the three staged JARs found no high-confidence secret or personal-home-path match. The full staged list is private under ignored `build/publication-audit/staged-files.txt`. No commit or remote exists yet. After any stage change, rerun the checker and review `git diff --cached --name-only`; the first commit/tag and the exact APK relationship must be recorded before Push/Release.

## 1. Resolve release dependencies and build the signed Stable APK

The Stable signing key is the dedicated Matchamisù PKCS12 keystore at `$env:USERPROFILE\Q25Signing\matchamisu-release.p12`, Alias `matchamisu-release`. The script resolves this path for the current Windows user. The password is requested interactively by `build-stable-release.ps1`; for PKCS12, the script sets Gradle's store and key password variables to the same in-memory value. Do not place it in a properties file, source, command line, or report. The former Edge Customizer key and Meltype signing material are not used.

```powershell
$ErrorActionPreference = 'Stop'
$audit = Join-Path $env:TEMP 'matchamisu-stable-release-audit'
New-Item -ItemType Directory -Force -Path $audit | Out-Null
java -version
.\gradlew.bat --version
.\gradlew.bat :app:dependencies --configuration stableReleaseRuntimeClasspath > (Join-Path $audit 'stableReleaseRuntimeClasspath.txt')
.\gradlew.bat :app:dependencyInsight --configuration stableReleaseRuntimeClasspath --dependency protobuf-javalite > (Join-Path $audit 'protobuf-insight.txt')
.\gradlew.bat :app:generateStableReleaseSbom
```

Run `powershell -ExecutionPolicy Bypass -File .\build-stable-release.ps1` from the project root. The script fixes the external PKCS12 path and alias, requests the password once without echo, passes it only through process environment variables expected by existing Gradle configuration, forces `--no-daemon`, clears those variables in `finally`, and checks APK signature, application ID, version, certificate SHA-256, and APK SHA-256. It refuses local Gradle signing property files because they take precedence over environment variables and could silently select another key. It builds only `:app:assembleStableRelease`.

Do not put values on command lines or in source. `PASTIERA_UNSIGNED_RELEASE_BUILD=true` and `PASTIERA_FDROID_BUILD=true` are inspection paths, not first-release distribution modes. Review every selected artifact and license/NOTICE against [stable-release-component-review.md](stable-release-component-review.md); resolve any `FAILED` lines.

## 2. Inspect identity, SBOM, notices, Mozc inputs, and signature

```powershell
$apk = (Resolve-Path 'app/build/outputs/apk/stable/release/app-stable-release.apk').Path
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$apkanalyzer = Join-Path $sdk 'cmdline-tools/latest/bin/apkanalyzer.bat'
$apksigner = Join-Path $sdk 'build-tools/36.0.0/apksigner.bat'
& $apkanalyzer apk summary $apk
& $apkanalyzer manifest debuggable $apk
& $apkanalyzer files list $apk | Select-String 'third_party_notices|licenses/|mozc.data|libmozc.so|matchamisu-sbom|font|icon'
& $apksigner verify --verbose --print-certs $apk
Get-FileHash -LiteralPath $apk -Algorithm SHA256
```

Adjust SDK tool paths to installed versions. Confirm application ID `com.matchamisu.ime`, version `0.1.0`/`87`, `debuggable=false`, and the intended Stable signing certificate digest. Compare that digest with the chosen update/signing identity; do not use an old Pastiera key by accident. Confirm the manifest label and launcher icon on device.

To inspect the existing key's public certificate and alias locally, `keytool` prompts for the store password; this lists certificate metadata and does not export a private key:

```powershell
keytool -list -v -keystore (Join-Path $env:USERPROFILE 'Q25Signing\matchamisu-release.p12') -storetype PKCS12 -alias 'matchamisu-release'
```

Compare its certificate SHA-256 fingerprint to `apksigner verify --print-certs` output from the built APK. Do not capture or publish password prompts or key material.

Read the APK as a ZIP without extracting files into the source tree:

```powershell
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($apk)
try {
    $names = $zip.Entries.FullName
    $names | Where-Object { $_ -match '^assets/common/(third_party_notices\.md|licenses/)|^assets/(mozc\.data|matchamisu-sbom\.cdx\.json)$|^lib/arm64-v8a/libmozc\.so$' } | Sort-Object
    foreach ($name in @('lib/arm64-v8a/libmozc.so', 'assets/mozc.data')) {
        $entry = $zip.GetEntry($name)
        if ($null -eq $entry) { throw "Missing APK entry: $name" }
        $stream = $entry.Open()
        $sha = [System.Security.Cryptography.SHA256]::Create()
        try { "$name $(([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '').ToLowerInvariant())" }
        finally { $sha.Dispose(); $stream.Dispose() }
    }
    $sbomEntry = $zip.GetEntry('assets/matchamisu-sbom.cdx.json')
    if ($null -eq $sbomEntry) { throw 'Stable SBOM is missing' }
    $reader = [System.IO.StreamReader]::new($sbomEntry.Open())
    try { $sbom = $reader.ReadToEnd() | ConvertFrom-Json }
    finally { $reader.Dispose() }
    $sbom.metadata.component
    $sbom.components | Select-Object group,name,version
}
finally { $zip.Dispose() }
```

The 2026-10-31 inspection confirmed both APK Mozc hashes against `source-built-mozc/metadata/hashes.txt`, the proto JAR against `generated-proto/hashes.txt`, all 12 license assets and the in-app notice against source, the OkHttp embedded NOTICE/data, and no removed typing OGG or Titan caption-bar entries. Java classes may be dexed rather than appear as a JAR entry in the APK. The SBOM's bundled asset list is incomplete and must not be used alone as a license clearance. Repeat these checks only if the APK changes.

## 3. Install and verify on Q25

```powershell
$adb = Join-Path $sdk 'platform-tools/adb.exe'
& $adb devices
& $adb install -r $apk
```

If installation fails due to a certificate mismatch, stop and decide the update/fresh-install policy; do not silently uninstall existing data. On the signed Stable app, check Japanese Mozc input and conversion, Space and full candidate retention, candidate tap continuation, partial Enter, LINE send A–E, English suggestions, GRAVE/Alt+GRAVE, SYM/Ctrl/Alt, Trackpad DPAD, app-specific Enter behavior, Japanese/English display switching, System/Light/Dark theme persistence, home actions, About/NOTICE/SBOM, `patproport` developer link, and launcher/adaptive icon. Verify physical and software keys produce no typing sound, the remaining haptics settings work, and existing saved sound settings do not crash the app. Record each result and the exact APK hash. The Back-after-candidate-bar Trackpad pointer restriction remains a disclosed Known Issue unless separately fixed and retested.

For release logcat privacy, use a unique harmless test string. Start capture before entering it in Matchamisù's test field and in LINE, then stop capture and search locally. Avoid real messages or passwords.

```powershell
$log = Join-Path $audit 'stable-release-logcat.txt'
& $adb logcat -c
# Enter and delete the disposable string on device, then capture:
& $adb logcat -d > $log
Select-String -LiteralPath $log -Pattern 'YOUR_DISPOSABLE_TEST_STRING'
```

Also inspect app-tagged status/error lines for input-derived content, not only the exact test string. Keep raw logcat private. An absence of the string alone is not a full privacy guarantee.

## 4. Match APK, source, and manual GitHub Release

After the above gates pass and a reviewed Git repository exists, record the exact source commit/tag used for the APK, the APK SHA-256, certificate digest, Mozc hashes, Gradle dependency report, and SBOM. Make the corresponding Matchamisù source and required external Mozc build prerequisites available. The clean Mozc regeneration and NDK/toolchain evidence are already recorded; do not claim bit-for-bit whole-APK reproducibility from matching Mozc files.

Create a release copy only after signing and inspection:

```powershell
$releaseApk = Join-Path $audit 'matchamisu-0.1.0-stable.apk'
Copy-Item -LiteralPath $apk -Destination $releaseApk
$hash = (Get-FileHash -LiteralPath $releaseApk -Algorithm SHA256).Hash.ToLowerInvariant()
"$hash  matchamisu-0.1.0-stable.apk" | Set-Content -LiteralPath (Join-Path $audit 'matchamisu-0.1.0-stable.apk.sha256') -Encoding ascii
```

Manual GitHub Release attachments: signed Stable APK, matching SHA-256 file, reviewed source archive or tag, GPL license/third-party notices, SBOM, and release notes including known limitations. Do not attach Nightly, unsigned APKs, private logs, signing material, or local build reports. Use the exact reviewed source revision; no repository URL has yet been selected. Do not publish until component licenses/assets, corresponding source, signed APK/SBOM, privacy, and Q25 regression checks are complete.
