#!/usr/bin/env bash
set -euo pipefail

readonly expected_commit=a069a88d4cb5c011de0f9aebb6c149a1c808d904
readonly source_dir=/mozc
readonly src=/mozc/src
readonly out=/out

recorded_commit="$(tr -d '[:space:]' </q25/source-built-mozc/metadata/MOZC_COMMIT)"
test "$recorded_commit" = "$expected_commit"

# Use the named source volume and Docker image. Bootstrap pinned official Mozc
# if the volume is empty; repeated builds reuse the existing checkout.
if [[ ! -d "$source_dir/.git" ]]; then
    git -C "$source_dir" init -q
    git -C "$source_dir" remote add origin https://github.com/google/mozc.git
    git -C "$source_dir" fetch -q --depth 1 origin "$expected_commit"
    git -C "$source_dir" checkout -q FETCH_HEAD
fi
actual_commit="$(git -C "$source_dir" rev-parse HEAD)"
test "$actual_commit" = "$expected_commit"
git -C "$source_dir" diff --exit-code -- src/android/jni src/protocol src/data_manager/oss
git -C "$source_dir" submodule update -q --init --recursive --depth 1
# The copied historical image exports r27b. Use the NDK path fixed by Mozc's
# pinned MODULE.bazel instead of allowing those environment variables to win.
unset ANDROID_NDK_HOME ANDROID_NDK_ROOT
ndk_properties="$src/third_party/ndk/android-ndk-r29/source.properties"
if [[ ! -f "$ndk_properties" ]]; then
    (cd "$src" && python3 build_tools/update_deps.py)
fi
test -f "$ndk_properties"

# Both labels come from the pinned Mozc BUILD.bazel; no custom target is added.
cd "$src"
mkdir -p "$out/arm64-v8a" "$out/data" "$out/metadata/proto"
bazel build //data_manager/oss:mozc_dataset_for_oss --config release_build
data_source="$(bazel cquery //data_manager/oss:mozc_dataset_for_oss --config release_build --output=files | grep '/mozc.data$')"
test -f "$data_source"
cp "$data_source" "$out/data/mozc.data"
chmod u+w "$out/data/mozc.data"

android_flags=(--config oss_android --config release_build)
bazel build //android/jni:mozc.arm64 "${android_flags[@]}"
library_source="$(bazel cquery //android/jni:mozc.arm64 "${android_flags[@]}" --output=files | grep '/libmozc.so$')"
test -f "$library_source"
cp "$library_source" "$out/arm64-v8a/libmozc.so"
chmod u+w "$out/arm64-v8a/libmozc.so"

cp "$src/protocol/commands.proto" "$out/metadata/proto/commands.proto"
cp "$src/protocol/candidate_window.proto" "$out/metadata/proto/candidate_window.proto"
printf '%s\n' "$actual_commit" >"$out/metadata/MOZC_COMMIT"
cat >"$out/metadata/build-info.txt" <<EOF
Source: google/mozc $actual_commit
Source volume: ${MOZC_SOURCE_VOLUME:-meltype-mozc}
Bazel cache volume: ${MOZC_BAZEL_CACHE_VOLUME:-meltype-bazel}
Build image: meltype-build
Targets: //data_manager/oss:mozc_dataset_for_oss, //android/jni:mozc.arm64
Data command: bazel build //data_manager/oss:mozc_dataset_for_oss --config release_build
JNI command: bazel build //android/jni:mozc.arm64 --config oss_android --config release_build
JNI source: src/android/jni/mozcjni.cc
ABI: arm64-v8a
Bazel: $(bazel --version)
NDK selection: src/MODULE.bazel configures third_party/ndk/android-ndk-r29
NDK package: $(grep '^Pkg.Revision' "$ndk_properties")
EOF
(cd "$out" && sha256sum arm64-v8a/libmozc.so data/mozc.data metadata/proto/commands.proto metadata/proto/candidate_window.proto) >"$out/metadata/hashes.txt"
echo "libmozc.so: $(stat -c %s "$out/arm64-v8a/libmozc.so") bytes"
echo "mozc.data: $(stat -c %s "$out/data/mozc.data") bytes"
cat "$out/metadata/hashes.txt"
