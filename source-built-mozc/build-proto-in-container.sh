#!/usr/bin/env bash
set -euo pipefail

readonly expected_commit=a069a88d4cb5c011de0f9aebb6c149a1c808d904
readonly target=//protocol:commands_java_proto_lite
readonly out=/out
readonly proto_names=(commands candidate_window config engine_builder user_dictionary_storage)
readonly bazel_flags=(--config release_build --java_runtime_version=remotejdk_11 --tool_java_runtime_version=remotejdk_11)

test "$(git -C /mozc rev-parse HEAD)" = "$expected_commit"
git -C /mozc diff --exit-code -- src/protocol
cd /mozc/src

bazel build "$target" "${bazel_flags[@]}"
mapfile -t outputs < <(bazel cquery "$target" "${bazel_flags[@]}" --output=files)
test "${#outputs[@]}" -eq 10

mkdir -p "$out/class-jars"
class_jars=()
for name in "${proto_names[@]}"; do
    class_file="lib${name}_proto-lite.jar"
    source_file="${name}_proto-lite-src.jar"
    class_matches=0
    source_matches=0
    for path in "${outputs[@]}"; do
        if [[ "${path##*/}" == "$class_file" ]]; then
            cp "$path" "$out/class-jars/$class_file"
            class_jars+=("$out/class-jars/$class_file")
            ((class_matches += 1))
        fi
        if [[ "${path##*/}" == "$source_file" ]]; then
            ((source_matches += 1))
        fi
    done
    test "$class_matches" -eq 1
    test "$source_matches" -eq 1
done
test "${#class_jars[@]}" -eq 5

python3 - "$out/mozc-proto-official.jar" "${class_jars[@]}" <<'PY'
import os
import sys
import tempfile
import zipfile

output, *inputs = sys.argv[1:]
classes = {}
for path in inputs:
    with zipfile.ZipFile(path) as source:
        for entry in source.infolist():
            if not entry.filename.endswith('.class'):
                continue
            if entry.filename in classes:
                raise SystemExit(f'Duplicate class: {entry.filename}')
            classes[entry.filename] = source.read(entry)

package = 'org/mozc/android/inputmethod/japanese/protobuf/'
for outer in ('ProtoCommands', 'ProtoCandidateWindow', 'ProtoConfig',
              'ProtoEngineBuilder', 'ProtoUserDictionaryStorage'):
    if package + outer + '.class' not in classes:
        raise SystemExit(f'Missing generated outer class: {outer}')
if not classes or any(not name.startswith(package) for name in classes):
    raise SystemExit('Unexpected generated class package')

fd, temporary = tempfile.mkstemp(prefix='mozc-proto-', suffix='.jar', dir=os.path.dirname(output))
os.close(fd)
try:
    with zipfile.ZipFile(temporary, 'w') as result:
        entries = {'META-INF/MANIFEST.MF': b'Manifest-Version: 1.0\r\n\r\n'}
        entries.update(classes)
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            result.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    os.replace(temporary, output)
finally:
    if os.path.exists(temporary):
        os.unlink(temporary)
print(f'Official generated classes: {len(classes)}')
PY

protoc_path="$(find /root/.cache/bazel -type f -name protoc -path '*prebuilt_protoc.linux_x86_64/bin/protoc' -print -quit)"
test -n "$protoc_path"
protoc_version="$("$protoc_path" --version)"
test "$protoc_version" = 'libprotoc 34.1'
{
    printf 'MOZC_COMMIT=%s\n' "$expected_commit"
    printf 'Bazel target=%s\n' "$target"
    printf 'Bazel version=%s\n' "$(bazel --version)"
    printf 'protoc version=%s\n' "$protoc_version"
    printf 'Java generation=java_lite_proto_library (--java_out=lite)\n'
    printf 'Bazel flags=%s\n' "${bazel_flags[*]}"
    printf 'Source image=meltype-build\nSource volume=%s\nBazel cache volume=%s\n' "${MOZC_SOURCE_VOLUME:-meltype-mozc}" "${MOZC_BAZEL_CACHE_VOLUME:-meltype-bazel}"
    printf 'Proto inputs:\n'
    printf '  protocol/%s.proto\n' "${proto_names[@]}"
    printf 'Class jar inputs:\n'
    printf '  %s\n' "${class_jars[@]##*/}"
    printf 'Rebuild command=source-built-mozc/build-proto.ps1\n'
} > "$out/build-info.txt"

(cd "$out" && sha256sum mozc-proto-official.jar class-jars/*.jar) > "$out/hashes.txt"
for name in "${proto_names[@]}"; do
    sha256sum "protocol/$name.proto"
done >> "$out/hashes.txt"
cat "$out/build-info.txt"
cat "$out/hashes.txt"
