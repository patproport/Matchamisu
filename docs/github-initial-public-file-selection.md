# Matchamisù initial GitHub publication file selection

Status: reviewed Stable `0.1.0` source-tree selection and initial Git staging on 2026-10-31; no commit or remote exists. Nightly remains a development variant.

This document defines the intended public source boundary. Use an explicit reviewed file list at staging time; do not use `git add .` as the publication decision. Files omitted by a pattern may still require case-by-case review. Mozc clean regeneration and the current signed Stable APK hash/JNI/data/NOTICE inspection have passed; the user reports Q25 operation. The PSL source-form snapshot is identified in `THIRD_PARTY_NOTICES.md`. The initial 47-path Git staging and focused privacy scan passed; the exact source commit/tag and APK relationship still need recording after the first commit.

## Planned public set

Include the following source and build materials after the final review:

- Root project metadata and build entry points: `README.md`, `LICENSE`, `THIRD_PARTY_NOTICES.md`, `CHANGELOG.md`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`, and `build-stable-release.ps1`.
- `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/`, `app/src/debug/`, and `app/src/test/`, except local-only material below. Retain app `AndroidManifest.xml`, Java/Kotlin source, resources, data assets, in-app `third_party_notices.md`, and all bundled license/notice files.
- `app/libs/mozc-proto.jar`: required Gradle compile input. It duplicates the generated official proto output but the build directly consumes this checked-in path; do not omit it unless the build is changed and independently verified.
- `source-built-mozc/README.md`, build scripts, `toolchain/Dockerfile`, `metadata/` including source revision, hashes, build information and proto inputs, canonical `arm64-v8a/libmozc.so`, `data/mozc.data`, and `generated-proto/mozc-proto-official.jar`. These are required source/build provenance or checked-in build inputs. Their inclusion does not establish GPL corresponding-source sufficiency or reproducibility.
- `third_party/` license texts; `design/icon/` editable source and generator; `convert_dictionaries.py` and reviewed dictionary/emoji build scripts as corresponding source inputs. Do not include the retired signing and publication scripts in the first commit.
- `docs/phase-4-e3-release-verification.md`, `docs/stable-first-release-checklist.md`, `docs/stable-release-component-review.md`, `docs/stable-release-dependency-pom-inventory.md`, `docs/github-release-v0.1.0.md`, and this selection policy. Other `docs/` entries remain ignored until individually reviewed.

## Exclude from the initial public tree

- `CODEX_HANDOFF.md`, `.idea/`, `.vscode/`, `.codex/`, `.agents/`, `.claude/`, `.codex-gradle/`, `.gradle/`, `.kotlin/`, `local.properties`, `AGENTS.md`, `CLAUDE.md`, and user-specific benchmark state.
- Diagnostic logs, logcat/trace/dump/bugreport/screen recording captures; root `input-*`, `window-*`, `q25-*` captures; `dict_backup/`; editor backups; generated APK/AAB/APKS/AAR files; `app/build/` and other build output.
- Keystores, signing properties, private keys, tokens, credentials, and local signing configuration. Do not publish secrets even if ignored.
- `source-built-mozc/generated-proto/class-jars/`, `generated-proto/verification/`, and `source-built-mozc/verification/`: intermediate/test artifacts. Preserve canonical `app/libs/mozc-proto.jar` and `generated-proto/mozc-proto-official.jar` instead.
- `signing/lineages/`, signing-key/certificate attestations, `fastlane/`, and `fdroiddata/` for this initial publication. They are legacy/private distribution evidence and require a separate decision; their exclusion does not imply that signing provenance is cleared.
- `docs/device-archives/`, raw device exports, personal/device screenshots, and old device capture material. Review each documentation image and data file before adding it.
- All `.github/workflows/` files for the first commit. The old release jobs remain on disk but are ignored; CI can be introduced after its permissions, names, and behavior are reviewed.
- `source-built-mozc/backup/`, `tools/prediction_bench/`, and historical signing/publication helpers. These are not required by the selected Stable build path.

## Current-tree scan findings

- No Git repository existed before this pass. `git init` created an empty repository, and the explicit 47-path dry run and actual staging each selected 769 files. Every staged file belongs to the 47-path allowlist; no prohibited path was staged. The index has no commit or remote, and no tracked working-tree change remains. The complete staged filename list is saved privately under ignored `build/publication-audit/staged-files.txt`; review with `git diff --cached --name-only` before committing.
- Present private/local candidates include `local.properties`, `.idea/workspace.xml`, `.kotlin/errors/*.log`, root Q25 diagnostic logs and dumps, `dict_backup/`, debug APKs/build output, the internal handoff, device archives, signing lineages/attestations, and legacy Fastlane/F-Droid files. Keep them out of the initial public set.
- The 2026-10-31 scan of all 769 staged paths found no user-home absolute path, high-confidence private-key/cloud/GitHub/Slack token pattern, or quoted hard-coded secret assignment in text or binary bytes. All three staged JARs were separately inspected as ZIPs: 599 decompressed entries had no sensitive entry name or high-confidence content match. The index includes no signing key, APK, log, dump, verification output, or internal handoff. `scripts/check-initial-publication.ps1` passed on the actual stage. Recheck after any stage change without printing matched values.
- A decompressed scan of all 760 APK ZIP entries found no exact-case high-confidence credential pattern. A broader case-insensitive cloud-key pattern produced dictionary-word false positives in seven serialized dictionaries; this was resolved by checking token case without exposing matched text. Binary metadata and JAR entry names did not show user-home paths or sensitive filenames. The APK itself is excluded from Git and is intended as a separate Release attachment.
- Windows user path patterns occur in internal handoff/workspace material and a benchmark README; these are excluded. The developer's public `patproport` handle and upstream contributor credits are intentional attribution, not private credentials.

## Files intentionally retained pending separate review

- Bundled base dictionaries, icon derivatives, and multi-device layout assets remain runtime assets. The user confirms the icon master was newly generated with ChatGPT without third-party imagery, closing that specific origin item; other assets' provenance and attribution still need review. The two Titan caption-bar tutorial JPEGs and 88 typing-sound OGG files have been removed from the source set; the inspected Stable APK has no matching ZIP entry names.
- `scripts/`, `tools/`, `.github/workflows/ci.yml`, `docs/`, and non-canonical Mozc generated metadata should be selected file by file. This list is not permission to publish every file under those directories.
- Preserve `LICENSE`, both notice locations, original Pastiera attribution, Mozc third-party license files, and all copyright notices. Do not prune these as cleanup.

## Required checks after `git init`, before staging or committing

1. Review `git status --short --ignored` and `git check-ignore -v` for representative secrets, logs, local config, build outputs, backups, and required Mozc inputs.
2. Build a reviewed allowlist, then run `git add --dry-run` with the explicit paths/patterns from that allowlist. Do not stage with `git add .`.
3. Inspect `git status --short`, `git diff --cached --name-only`, and `git diff --cached --stat`; verify that the internal handoff, local config, logs, archives, APKs, signing material, and generated verification artifacts are absent.
4. Scan staged text for personal paths and credential patterns while ensuring output reports filenames/line numbers only, never matched values. Inspect staged binary names and sizes separately.
   Run `powershell -ExecutionPolicy Bypass -File .\scripts\check-initial-publication.ps1` after staging; it checks required paths and high-risk patterns without printing matched values. It is a guardrail, not a complete secret or license review.
5. Confirm the staged set retains `LICENSE`, `THIRD_PARTY_NOTICES.md`, app license assets, `source-built-mozc` build scripts/provenance, canonical native/data/proto artifacts, and Gradle wrapper files.
6. Do not commit or publish until Phase 4-E license/provenance, corresponding-source and Mozc regeneration, release APK/SBOM/signature, and Q25 release verification gates are separately complete.

For the first review, an explicit staging dry run can start with the following list. Inspect its output and add remaining reviewed source/build scripts one by one; this does not stage files:

```powershell
git add --dry-run -- .gitignore README.md LICENSE THIRD_PARTY_NOTICES.md CHANGELOG.md build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat build-stable-release.ps1 convert_dictionaries.py gradle/libs.versions.toml gradle/wrapper app/build.gradle.kts app/proguard-rules.pro app/src/main app/src/debug app/src/test app/libs/mozc-proto.jar design/icon source-built-mozc/README.md source-built-mozc/build.ps1 source-built-mozc/build-in-container.sh source-built-mozc/build-proto.ps1 source-built-mozc/build-proto-in-container.sh source-built-mozc/toolchain/Dockerfile source-built-mozc/metadata source-built-mozc/arm64-v8a/libmozc.so source-built-mozc/data/mozc.data source-built-mozc/generated-proto/build-info.txt source-built-mozc/generated-proto/hashes.txt source-built-mozc/generated-proto/mozc-proto-official.jar third_party docs/github-initial-public-file-selection.md docs/stable-first-release-checklist.md docs/stable-release-component-review.md docs/stable-release-dependency-pom-inventory.md docs/phase-4-e3-release-verification.md docs/github-release-v0.1.0.md scripts/check-initial-publication.ps1 scripts/build_symspell_dict.py scripts/convert_dict_to_cbor.py scripts/generate_emoji_search_assets.py scripts/preprocess_dictionaries.py scripts/preprocess-dictionaries.main.kts scripts/update_emoji_assets.py
```

This is a review starting point, not the complete GPL corresponding-source decision. If any referenced generator, asset input, Dockerfile, or patch is required to rebuild the distributed work, add its reviewed source path before publication. Do not force-add ignored legacy workflows, signing lineages, or internal device archives.

