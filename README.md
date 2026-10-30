# Matchamisù

Matchamisù is a Japanese and English input method for the Zinwa Q25 physical keyboard. It is an independent derivative of [Pastiera](https://github.com/palsoftware/pastiera) and uses official Mozc for Japanese conversion.

Matchamisù developer: [patproport](https://github.com/patproport).

This repository is a development fork. A public release URL and update endpoint have not been assigned. The stable Android application ID is `com.matchamisu.ime`; nightly builds use `com.matchamisu.ime.nightly`. The source namespace remains `it.palsoftware.pastiera` for compatibility with existing code and does not identify the installed application.

## Features

- Japanese conversion using a pinned official Mozc build, with English input and suggestions.
- Compact candidate bar and JP/EN mode indication for Q25.
- Physical-key shortcuts, modifiers, Trackpad options, QuickLauncher, and app-specific Enter behavior inherited from Pastiera and adapted for Q25.
- System, Light, and Dark IME themes, plus Japanese and English app display languages.

## Current status

The first public Stable candidate is `0.1.0` (versionCode `87`). The user reports a signed Stable build and Q25 device check of the latest APK, including the R8/Protobuf crash fix. Nightly remains a development variant and is not part of the first publication. The Back-after-candidate-bar Trackpad pointer-range issue remains open. The [Stable release checklist](docs/stable-first-release-checklist.md) records the remaining publication checks.

## Building and release

The Android project uses Gradle 8.13, Android Gradle Plugin 8.11.0, Kotlin 2.0.21, compile SDK 36, and JDK 21 for the documented Windows build. Stable ID is `com.matchamisu.ime`; Nightly ID is `com.matchamisu.ime.nightly`. Stable Release requires the separately configured Stable signing key. Never put signing credentials in source. The recommended first APK attachment name is `matchamisu-0.1.0-stable.apk`.

Official Mozc is pinned at `a069a88d4cb5c011de0f9aebb6c149a1c808d904`. The checked-in native library, dataset, and proto JAR are build inputs; their hashes, Bazel targets, and isolated PowerShell regeneration commands are in [`source-built-mozc/README.md`](source-built-mozc/README.md). A clean user-side Docker run regenerated all three with matching SHA-256; the build record identifies Bazel 9.0.2, protoc 34.1 and the pinned Mozc module's NDK r29 package `29.0.14206865`. The repository includes the GPL-tagged Dockerfile and generation scripts; no sibling Meltype checkout is required. The final published source set must still be checked against the exact APK.

The [Stable first-release checklist](docs/stable-first-release-checklist.md) gives the Windows commands for dependency resolution, signed build, APK/SBOM inspection, and Q25 checks. The [component review](docs/stable-release-component-review.md) separates confirmed evidence from unresolved licenses and asset provenance. The source revision used for the signed APK must be published with that APK and its SHA-256. No GitHub repository URL or update endpoint has been assigned; publication is manual after all gates pass.

## License and credits

Matchamisù is derived from Pastiera, which is distributed under GPL-3.0. The original copyright notices and license text are retained in [`LICENSE`](LICENSE), [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md), bundled notices, and the in-app About credits. Pastiera was created by Andrea Palumbo (PalSoftware), with Patrick Zauner and other contributors credited in the app. Official Mozc and other bundled dependencies retain their respective notices. Matchamisù is not an official Pastiera release.

The [Pastiera forking policy](https://github.com/palsoftware/pastiera#forking-policy) asks distributed forks to use their own name, application ID, branding, and update endpoints. This project does not use an upstream update endpoint.
