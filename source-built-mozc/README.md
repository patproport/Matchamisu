# Q25 source-built Mozc: v0.1.0 corresponding-source recipe

Matchamisù Stable packages official Mozc Android JNI, OSS dictionary data, and generated Java protocol classes. The fixed upstream commit is `a069a88d4cb5c011de0f9aebb6c149a1c808d904` in `google/mozc`; `metadata/MOZC_COMMIT` and both container scripts check it. No AquaMozc or external Meltype checkout is used by these scripts. `meltype-build` and the default volume names are historical local labels. The included `toolchain/Dockerfile` defines the image; its Meltype provenance and GPL notice are in `THIRD_PARTY_NOTICES.md`.

## Distributed inputs and build graph

| App input | Pinned upstream build input | Expected SHA-256 record |
| --- | --- | --- |
| `arm64-v8a/libmozc.so` | `//android/jni:mozc.arm64`, `--config oss_android --config release_build`; upstream `src/android/jni` and Bazel module dependencies | `metadata/hashes.txt` |
| `data/mozc.data` | `//data_manager/oss:mozc_dataset_for_oss`, `--config release_build`; OSS/manual dictionaries, usage data and generated postal data | `metadata/hashes.txt` |
| `app/libs/mozc-proto.jar` | `//protocol:commands_java_proto_lite` and its five class JARs, built from the pinned `src/protocol/*.proto` files | `generated-proto/hashes.txt` (`mozc-proto-official.jar`) |

The native and data scripts also copy `commands.proto` and `candidate_window.proto` and hash them in `metadata/hashes.txt`. The proto script records all five schema and class-JAR hashes in `generated-proto/hashes.txt`. The app supplies `protobuf-javalite:4.34.1` separately. It copies `mozc.data` to `filesDir/source_mozc/mozc.data` after a hash check and passes that path to Mozc's `onPostLoad`. The data file is not an ELF library. The app's Android build and signing recipe is at the repository root; the Docker scripts only regenerate Mozc inputs.

The pinned Mozc [`src/MODULE.bazel`](https://github.com/google/mozc/blob/a069a88d4cb5c011de0f9aebb6c149a1c808d904/src/MODULE.bazel) selects `third_party/ndk/android-ndk-r29` for the Android Bazel toolchain and fixes Abseil `20260107.1` and protobuf `34.1`. `build-in-container.sh` invokes upstream `build_tools/update_deps.py` on a fresh checkout to obtain that NDK and verifies its `source.properties`. [NDK r29 has an official Linux release](https://github.com/android/ndk/releases/tag/r29). The historical Dockerfile additionally downloads NDK r27b to `/opt`; the script clears its `ANDROID_NDK_HOME`/`ANDROID_NDK_ROOT` environment variables before the build so the pinned module's r29 path applies. The scripts force Bazelisk to select Bazel 9.0.2 and record the NDK package revision in isolated build information. The Dockerfile obtains [Bazelisk 1.25.0](https://github.com/bazelbuild/bazelisk/releases/tag/v1.25.0). These selections are established from source/configuration; actual download/build success and the inputs of the existing canonical binary still need a clean run and comparison.

The Mozc source revision also carries `MODULE.bazel`, `.bazelrc`, BUILD files, and Bazel patches. The local integration adds `build.ps1`, `build-in-container.sh`, `build-proto.ps1`, `build-proto-in-container.sh`, this README, and the Dockerfile. Publish all of them with canonical inputs and metadata. Upstream `update_deps.py`, the Bazel module graph, NDK, Bazelisk, base image, apt packages, and remote JDK must remain obtainable. The base image and downloaded tools are not all fixed by immutable digest/checksum in this repository, so a fresh build is the practical verification gate; do not claim bit-for-bit reproducibility from the recipe alone.

## Isolated regeneration on the user's PowerShell machine

These commands write only inside the ignored `source-built-mozc/verification/` directory. They do not replace the hash-matched JNI/data/JAR consumed by Gradle. Run from the repository root with Docker Desktop available; a first run requires network access and substantial disk space. This is a user-side check, not a build performed during the source/notice review.

```powershell
$ErrorActionPreference = 'Stop'
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$output = Join-Path (Resolve-Path '.\source-built-mozc').Path "verification/clean-$stamp"
$sourceVolume = "matchamisu-mozc-$stamp"
$cacheVolume = "matchamisu-bazel-$stamp"
docker build --platform linux/amd64 -t meltype-build -f .\source-built-mozc\toolchain\Dockerfile .
.\source-built-mozc\build.ps1 -OutputDirectory $output -SourceVolume $sourceVolume -BazelCacheVolume $cacheVolume
.\source-built-mozc\build-proto.ps1 -OutputDirectory $output -SourceVolume $sourceVolume -BazelCacheVolume $cacheVolume
```

Both scripts reject paths outside a child of `source-built-mozc/verification/`, and they reject nonempty target folders. The project tree is mounted read-only in the JNI/data container; the proto container mounts only the script directory read-only. Each has a separate writable output mount. `build-proto.ps1` must use the same newly populated source/cache volumes as `build.ps1`.

Compare the outputs before deciding whether the source offer is sufficient:

```powershell
$expected = Get-Content .\source-built-mozc\metadata\hashes.txt
$actual = Get-Content (Join-Path $output 'metadata/hashes.txt')
Compare-Object $expected $actual
$expectedProto = Get-Content .\source-built-mozc\generated-proto\hashes.txt
$actualProto = Get-Content (Join-Path $output 'generated-proto/hashes.txt')
Compare-Object $expectedProto $actualProto
Get-Content (Join-Path $output 'metadata/build-info.txt')
Get-Content (Join-Path $output 'generated-proto/build-info.txt')
```

No `Compare-Object` output means the canonical JNI/data/schema hashes and generated proto JAR/class/schema hashes match. Also inspect the recorded source revision, NDK package, Bazel/protoc versions, dependencies, and any build failures. A mismatch requires investigation; do not overwrite canonical files merely to force a match. SHA-256 identity is useful quality evidence, but GPL corresponding source also requires the actual source, build scripts, configuration, patches, dependency access, and license notices.

## Provenance, licenses, and release boundary

The OSS data target includes the pinned dictionary entries and rules, manual dictionary, Japanese usage dictionary tag `2025-01-25`, and postal data generated from the Japan Post CSV mirror revision `621d059fbcbfae17bfca15b439692bae934268c3`. The existing Bazel cache's nonempty `zip_code.tsv` and hash-matched `mozc.data` support inclusion of postal data. The older generic sentence in the upstream dictionary README saying OSS Mozc excludes postal data does not describe this built artifact. Preserve the postal attribution.

Complete upstream Mozc, the `mozcjni.cc` file header, OSS dictionary, usage dictionary, protobuf, zlib, and `utf8_range` license texts are copied to `third_party/mozc/` and APK `assets/common/licenses/`; Apache-2.0 covers Abseil. `THIRD_PARTY_NOTICES.md` and the in-app notice identify NAIST, ICOT, Okinawa, usage dictionary, Japan Post, and native linked-library terms. The cached link-parameter file for the hash-matched `libmozc.so` identifies Abseil, protobuf (including `utf8_range`), and zlib objects. That evidence does not settle every native source-file notice or the selected final APK dependency graph. Keep these checks open until the clean regeneration and final Stable APK/SBOM review are recorded.
