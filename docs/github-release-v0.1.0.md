# Matchamisù v0.1.0 — manual GitHub release draft

**Repository description:** Japanese and English physical-keyboard IME for the Zinwa Q25, based on Pastiera and official Mozc.

Suggested repository name: `Matchamisu`. Confirm the name and destination before creating the repository; no repository URL is assumed here.

## Release notes (draft)

Matchamisù v0.1.0 is the first Stable release for the Zinwa Q25. It supports Japanese conversion with official Mozc, English input and suggestions, Q25 physical-key shortcuts, candidate selection, and a compact settings and home interface in Japanese and English.

The Stable application ID is `com.matchamisu.ime` (`versionCode` 87). The bundled official Mozc build is pinned to revision `a069a88d4cb5c011de0f9aebb6c149a1c808d904`. Matchamisù is an independent GPL-3.0 derivative of Pastiera; see `LICENSE` and `THIRD_PARTY_NOTICES.md` for attribution and component terms.

**Known issue:** On Q25 Keyboard mode, pressing Back after the candidate bar is shown can leave pointer movement restricted. Manually switching Edge Trackpad to Mouse mode clears the restriction. No automatic mode switch is implemented.

The current signed Stable candidate at `app/build/outputs/apk/stable/release/app-stable-release.apk` has SHA-256 `429be283c3f3e2329821dad9d4d6607fff77aead558de25b94d73dee2db168c1`. Its JNI/data and bundled notices were inspected on 2026-10-31, and the user reports Q25 operation. The OkHttp PSL fixed source-form snapshot and staged-source privacy check are recorded in `THIRD_PARTY_NOTICES.md` and `github-initial-public-file-selection.md`. Confirm the copied release attachment has this exact hash and record its relationship to the first source commit/tag before publication.

## Intended release attachments

1. `matchamisu-0.1.0-stable.apk` — signed Stable APK copied from the final verified build.
2. `matchamisu-0.1.0-stable.apk.sha256` — lowercase SHA-256 of the exact attached APK.
3. GitHub source archive of the exact `v0.1.0` tag, with GPL-3.0, notices, build inputs, and corresponding-source instructions. Do not upload a separately assembled source ZIP that differs from the tag.
4. `matchamisu-0.1.0-sbom.cdx.json` — the SBOM extracted from that same APK after content review.

Do not attach signing material, diagnostic logs, local dependency reports, unsigned builds, or Nightly artifacts.

After the final APK passes the Stable checklist, extract its own SBOM for attachment and review the component list before upload:

```powershell
Add-Type -AssemblyName System.IO.Compression
$apk = (Resolve-Path '.\app\build\outputs\apk\stable\release\app-stable-release.apk').Path
$zip = [IO.Compression.ZipFile]::OpenRead($apk)
try {
    $entry = $zip.GetEntry('assets/matchamisu-sbom.cdx.json')
    if ($null -eq $entry) { throw 'APK SBOM missing' }
    $output = [IO.File]::Create((Join-Path $env:TEMP 'matchamisu-0.1.0-sbom.cdx.json'))
    $input = $entry.Open()
    try { $input.CopyTo($output) } finally { $input.Dispose(); $output.Dispose() }
} finally { $zip.Dispose() }
```

## First commit and publication sequence (user-run after final review)

The empty Git repository has already been initialized and the explicit 47-path allowlist staged. No commit, remote, Push, or repository creation has occurred. From the project root in PowerShell, review the staged file list and checker output once more after any change. Then, when ready to publish:

```powershell
git diff --cached --name-only
git diff --cached --stat
powershell -ExecutionPolicy Bypass -File .\scripts\check-initial-publication.ps1
# Review the full staged list and resolve all findings before committing.
git commit -m "Release Matchamisù v0.1.0 source"
git branch -M main
$repoUrl = 'PASTE_CONFIRMED_REPOSITORY_URL_HERE'
git remote add origin $repoUrl
git push -u origin main
git tag -a v0.1.0 -m "Matchamisù v0.1.0"
git push origin v0.1.0
```

Create the GitHub repository without an auto-generated README/license so the local initial commit remains the source. Create the `v0.1.0` GitHub Release manually from that tag, paste the reviewed notes above, and upload only the final attachments. Record the source commit, APK hash, APK certificate fingerprint, Mozc hashes, and final Q25 result in the release review record before publication.

After publication, download the APK and checksum into a separate directory and verify them locally:

```powershell
$expected = ((Get-Content .\matchamisu-0.1.0-stable.apk.sha256 -Raw).Trim() -split '\s+')[0]
$actual = (Get-FileHash .\matchamisu-0.1.0-stable.apk -Algorithm SHA256).Hash
if ($actual -ine $expected) { throw 'Downloaded APK SHA-256 mismatch' }
"APK SHA-256 verified: $actual"
```

This hash check proves the downloaded APK matches the published checksum. Keep the signing certificate verification and app identity checks from the Stable checklist in the final release record as separate evidence.
