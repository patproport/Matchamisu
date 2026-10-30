# Open source notices

## Android Open Source Project LatinIME

Pastiera's full virtual keyboard mode uses AOSP LatinIME as a reference for keyboard geometry and includes AOSP-derived keyboard visual assets.

- Project: Android Open Source Project, LatinIME (`platform/packages/inputmethods/LatinIME`)
- Source repository: https://android.googlesource.com/platform/packages/inputmethods/LatinIME
- Pinned source revision used as reference: `127336e9f29d69607eab55982324b210279ae8c5`
- License: Apache License, Version 2.0

The Apache License, Version 2.0 text is included in `assets/common/licenses/Apache-2.0.txt` and in the repository under `third_party/licenses/Apache-2.0.txt`.

Pastiera does not import AOSP dictionaries.

## Official Mozc and its data (Phase 4-E1; release audit still open)

Matchamisù packages `source-built-mozc/arm64-v8a/libmozc.so`, `source-built-mozc/data/mozc.data`, Mozc-generated Java protocol classes in `app/libs/mozc-proto.jar`, and `com.google.protobuf:protobuf-javalite:4.34.1`. The native library, data, and protocol inputs are pinned to [google/mozc revision `a069a88d4cb5c011de0f9aebb6c149a1c808d904`](https://github.com/google/mozc/tree/a069a88d4cb5c011de0f9aebb6c149a1c808d904). The compiled artifacts' hashes and build commands are recorded under `source-built-mozc/metadata/` and `source-built-mozc/generated-proto/`.

| Component and version | Copyright / applicable terms | Attribution and complete text supplied | Primary evidence / unresolved scope |
| --- | --- | --- | --- |
| Google-authored Mozc code and protocol schemas, pinned revision above | Copyright 2010–2018 Google Inc. in the upstream root `LICENSE`; BSD 3-Clause conditions and disclaimer | `third_party/mozc/LICENSE`; APK asset `common/licenses/Mozc-LICENSE.txt` | [Pinned root LICENSE](https://github.com/google/mozc/blob/a069a88d4cb5c011de0f9aebb6c149a1c808d904/LICENSE), `src/android/jni/BUILD.bazel`, `src/protocol/*.proto`. The root license's dictionary sections are additional terms, not a claim that all data is BSD. |
| Android JNI wrapper `src/android/jni/mozcjni.cc` | Copyright 2010–2021 Google Inc.; its file header carries the BSD 3-Clause conditions and disclaimer | Exact header text in `third_party/mozc/mozcjni-LICENSE.txt`; APK asset `common/licenses/Mozc-mozcjni-LICENSE.txt`. Preserve the pinned source file and its own header in the source offer. | [Pinned JNI source](https://github.com/google/mozc/blob/a069a88d4cb5c011de0f9aebb6c149a1c808d904/src/android/jni/mozcjni.cc). This identifies the wrapper's notice, not every object linked into `libmozc.so`. |
| OSS dictionary (IPAdic-derived), `mecab-ipadic-2.7.0-20070801` as documented by pinned Mozc | Copyright 2000–2003 Nara Institute of Science and Technology (NAIST); permission and warranty paragraphs in upstream `LICENSE` and `dictionary_oss/README.txt` | Both unabridged upstream texts above and `third_party/mozc/dictionary_oss-README.txt`; APK assets `Mozc-LICENSE.txt` and `Mozc-dictionary_oss-README.txt` | [Pinned dictionary README](https://github.com/google/mozc/blob/a069a88d4cb5c011de0f9aebb6c149a1c808d904/src/data/dictionary_oss/README.txt), `src/data/dictionary_oss/BUILD.bazel`. Individual entries are not separable from the compiled dataset by this audit. |
| ICOT-origin entries within IPAdic-derived data | ICOT Free Software distribution condition and full `NO WARRANTY` section as reproduced in pinned Mozc `LICENSE` | The full ICOT passage is retained without abbreviation in both Mozc license/README copies | Same pinned primary documents. Do not replace this condition with a generic BSD label. |
| Okinawa dictionary entries | The pinned README and root `LICENSE` reproduce the source's Japanese Public Domain Data statement and permission to use, modify, and distribute | The complete Japanese statement is retained in both upstream text copies | Same pinned primary documents. The source statement does not identify a conventional SPDX license or named individual copyright holder. |
| Japanese usage dictionary, tag `2025-01-25` | Copyright (c) 2011 Hiroyuki Komatsu, Shoji Ogura, Tsuyoshi Horo; two-clause BSD-style license and disclaimer | `third_party/mozc/ja_usage_dict-LICENSE.txt`; APK asset `common/licenses/Mozc-ja_usage_dict-LICENSE.txt` | Pinned Mozc `src/MODULE.bazel` references the tagged archive; `src/data_manager/oss/BUILD.bazel` includes `@ja_usage_dict//:usage_dict.txt`. License copied from that archive in the existing Bazel cache. |
| Japan Post address and large-office postal-code CSV snapshots, mirrored at `hiroyuki-komatsu/japanpost_zipcode` revision `621d059fbcbfae17bfca15b439692bae934268c3` | Japan Post's published conditions say it does not assert copyright in these data and permits redistribution; no SPDX license is assigned here | Attribution and [Japan Post's address-data conditions](https://www.post.japanpost.jp/zipcode/dl/readme.html) / [office-data conditions](https://www.post.japanpost.jp/zipcode/dl/jigyosyo/readme.html) are recorded here; no separate license text exists in the downloaded CSV archives | Pinned Mozc `src/MODULE.bazel` fetches `KEN_ALL.CSV` and `JIGYOSYO.CSV`; `src/dictionary/BUILD.bazel` generates `zip_code.tsv`, and `src/data/dictionary_oss/BUILD.bazel` puts it into the OSS dictionary. The existing 9,599,281-byte generated `zip_code.tsv` has entries, and the cached `mozc.data` hash equals the bundled file's hash. The pinned dictionary README's older statement that OSS Mozc excludes postal data is therefore not reliable for this built artifact. |
| Java classes generated from Mozc `.proto` files; native protobuf selected by Mozc module `34.1`; Android runtime `protobuf-javalite:4.34.1` | Mozc schema authors retain ownership of generated code; protocol buffer support library uses Google's BSD 3-Clause license with its generated-code note | Mozc and protobuf full license texts: `third_party/mozc/LICENSE`, `third_party/mozc/protobuf-LICENSE.txt`; APK assets `Mozc-LICENSE.txt`, `Mozc-protobuf-LICENSE.txt` | Pinned Mozc `src/MODULE.bazel`, `source-built-mozc/generated-proto/build-info.txt`, [protobuf v34.1 LICENSE](https://github.com/protocolbuffers/protobuf/blob/v34.1/LICENSE), and `app/build.gradle.kts`. APK-transitive runtime components still require final verification. |
| Abseil C++ `20260107.1` | Apache License 2.0; individual source-file notices remain applicable | `third_party/licenses/Apache-2.0.txt`; APK asset `common/licenses/Apache-2.0.txt` | Pinned `src/MODULE.bazel`, `src/android/jni/BUILD.bazel`, and the cached link parameter file for the hash-matched `libmozc.so` list `external/abseil-cpp+` objects; [Abseil license](https://github.com/abseil/abseil-cpp/blob/20260107.1/LICENSE). |
| zlib `1.3.1` | Copyright (C) 1995–2022 Jean-loup Gailly and Mark Adler; zlib license with origin/alteration/notice conditions | `third_party/mozc/zlib-LICENSE.txt`; APK asset `common/licenses/Mozc-zlib-LICENSE.txt` | The hash-matched native library's cached link parameters list `external/zlib+` objects; the cached source's `zlib.h` states version `1.3.1`, and its `LICENSE` was copied verbatim. |
| `utf8_range` vendored inside protobuf `34.1` | Copyright (c) 2019 Yibo Cai and Copyright 2022 Google LLC; MIT License | `third_party/mozc/utf8_range-LICENSE.txt`; APK asset `common/licenses/Mozc-utf8_range-LICENSE.txt` | The hash-matched native library's cached link parameters list `external/protobuf+/third_party/utf8_range` object code; license copied verbatim from that pinned cached source. |

The existing cached link-parameter file for `libmozc.so` (SHA-256 `ec350e4096efec81b8c7cdde7507e15828aa91e756b43f8ad7e9dd2af134bc41`) lists external object paths from `abseil-cpp+`, `protobuf+`, and `zlib+`, in addition to the Android NDK; the `utf8_range` object is vendored under `protobuf+`. This narrows the native linked-object inventory, but does **not** establish that every source-file notice inside those projects has been cleared, or which Android/Maven transitive artifacts enter the final release APK. The separately listed Pastiera assets and downloadable dictionaries below have not been re-cleared by Phase 4-E1. Do not mark Phase 4-E complete on the strength of this table.

### Android dependency inventory still requiring final notices

`app/build.gradle.kts` and `gradle/libs.versions.toml` declare Guava `33.4.8-android`, OkHttp `4.11.0`, Kotlinx Serialization JSON/CBOR, Shizuku `13.1.5`, and AndroidX modules. The existing Stable SBOM records the resolved versions, including Kotlinx Serialization `1.7.3` and Compose artifacts `1.7.0`/`1.7.1`, in the repository `docs/stable-release-dependency-pom-inventory.md`. That older resolution is not a final release-APK inventory. Component copyright, version-specific upstream NOTICE files, and the final APK must still be checked before publication.

### Phase 4-E2 direct Maven metadata checks (final release graph still pending)

The existing Stable SBOM/POM inventory has **98** resolved Maven entries: **90** POMs declare Apache-2.0, **four** Shizuku modules declare MIT, and **four** POMs have no license element and are investigated below. The exact component/version mapping is in the repository `docs/stable-release-dependency-pom-inventory.md`. These declarations, the preserved Apache-2.0 and Shizuku MIT texts, and the four specific checks do not establish every artifact's source-file copyright or upstream NOTICE obligation. The final rebuilt APK may select a changed graph and must be compared with that inventory.

- `dev.rikka.shizuku:aidl/api/provider/shared:13.1.5`: all four cached published POMs identify the MIT License. The [Shizuku-API project LICENSE](https://github.com/RikkaApps/Shizuku-API/blob/master/LICENSE), including Copyright (c) 2021 RikkaW, is preserved in `third_party/shizuku/LICENSE` and bundled as `assets/common/licenses/Shizuku-API-LICENSE.txt`. The published POMs point to the project's default-branch license; an immutable source tag for artifact `13.1.5` was not located in this audit.
- `com.google.guava:guava:33.4.8-android`: its cached published parent POM identifies Apache License 2.0. `com.squareup.okhttp3:okhttp:4.11.0` and `org.jetbrains.kotlinx:kotlinx-serialization-json/cbor:1.6.3`: their cached published POMs also identify Apache License 2.0. The complete Apache 2.0 text is already in `third_party/licenses/Apache-2.0.txt` and `assets/common/licenses/Apache-2.0.txt`. Their per-artifact NOTICE files and transitive dependencies have **not** yet been cleared.

These are declarations and cached Maven metadata, not proof that every named artifact is packaged in the final release APK. Component-specific notices and the final APK graph remain to be checked; no blanket license conclusion is made for them.

### Phase 4-E3 additional direct AndroidX POM checks (final release graph still pending)

Cached published POMs for the **declared** versions of `androidx.core:core-ktx:1.17.0`, `androidx.lifecycle:lifecycle-runtime-ktx` and `lifecycle-runtime-compose:2.9.2`, `androidx.activity:activity-compose:1.10.1`, `androidx.compose:compose-bom:2024.09.00`, `androidx.appcompat:appcompat:1.6.1`, `androidx.work:work-runtime-ktx:2.9.1`, `androidx.recyclerview:recyclerview:1.3.2`, and `androidx.emoji2:emoji2`, `emoji2-views`, and `emoji2-views-helper:1.4.0` identify Apache License 2.0. The older resolved SBOM separately records `androidx.compose.material:material-icons-extended-android:1.7.0` with an Apache-2.0 POM declaration. The full Apache text is already supplied in `third_party/licenses/Apache-2.0.txt` and the app asset `common/licenses/Apache-2.0.txt`. These checks do **not** establish the final release APK's selected versions, copyright holders, or artifact-specific NOTICE files; obtain its final resolved graph and inspect each component before publication.

### Four resolved Maven POMs without a license element (artifact-specific check)

- `com.google.guava:guava:33.4.8-android`: [v33.4.8 upstream LICENSE](https://github.com/google/guava/blob/v33.4.8/LICENSE), Apache License 2.0; complete text in `common/licenses/Apache-2.0.txt`.
- `com.google.guava:failureaccess:1.0.3`: resolved JAR `META-INF/LICENSE` is Apache License 2.0; complete text in `common/licenses/Apache-2.0.txt`.
- `com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava`: resolved JAR contains Maven metadata and manifest only, with no class or application resource. It is a dependency-resolution placeholder; no license is inferred for absent code.
- `com.google.protobuf:protobuf-javalite:4.34.1`: [protobuf v34.1 upstream LICENSE](https://github.com/protocolbuffers/protobuf/blob/v34.1/LICENSE), supplied in `common/licenses/Mozc-protobuf-LICENSE.txt`.

Component-specific NOTICE and the remaining resolved Maven artifacts still need review.

### OkHttp 4.11.0 embedded Public Suffix List

The resolved OkHttp JAR and the inspected older Stable APK both contain `okhttp3/internal/publicsuffix/NOTICE` and `publicsuffixes.gz`. The embedded [OkHttp 4.11.0 NOTICE](https://github.com/square/okhttp/blob/parent-4.11.0/okhttp/src/main/resources/okhttp3/internal/publicsuffix/NOTICE) identifies [Public Suffix List](https://publicsuffix.org/list/public_suffix_list.dat) data under [MPL 2.0](https://www.mozilla.org/en-US/MPL/2.0/). The same notice conditions are independently preserved as APK asset `common/licenses/OkHttp-PublicSuffixList-NOTICE.txt` and repository file `third_party/okhttp/PublicSuffixList-NOTICE.txt`; inspect the rebuilt APK for both that asset and the embedded NOTICE/data. The exact source snapshot used by OkHttp 4.11.0 is not yet established; this data has separate terms from OkHttp's Apache-2.0 code.

## EB Garamond

The app bundles `res/font/eb_garamond.ttf` (SHA-256 `ef9512f92f6d579e5dc75af59a5a4b1b8b47d2eda89e00b954d44520e5369027`). EB Garamond is by Georg Duffner, Octavio Pardo, and the EB Garamond Project Authors. Copyright 2017 The EB Garamond Project Authors. It is licensed under the SIL Open Font License 1.1. The complete copyright notice and license text are included at `assets/common/licenses/EB-Garamond-OFL.txt`; the repository copy is `third_party/eb_garamond/OFL.txt`.

## Noto Serif JP

The app bundles `res/font/noto_serif_jp.ttf` and references it in app typography. The upstream font metadata credits Google and Adobe. It is licensed under the SIL Open Font License 1.1. The complete copyright notice and license text are included at `assets/common/licenses/Noto-Serif-JP-OFL.txt`; the repository copy is `third_party/noto_serif_jp/OFL.txt`.

## Google Material Symbols / Material Icons

Pastiera uses icon artwork from Google Material Symbols / Material Icons for parts of the app UI and keyboard chrome.

- Project: Material Symbols / Material Icons
- Project page: https://fonts.google.com/icons
- Source repository: https://github.com/google/material-design-icons
- License: Apache License, Version 2.0

The Apache License, Version 2.0 text is included in `assets/common/licenses/Apache-2.0.txt` and in the repository under `third_party/licenses/Apache-2.0.txt`.

## Unicode CLDR Emoji Annotations

- Project: Unicode Common Locale Data Repository (CLDR)
- Source repository: https://github.com/unicode-org/cldr-json
- Source path used by generator: `cldr-json/cldr-annotations-full/annotations`
- License: Unicode Data Files and Software License / Unicode License

Pastiera's local emoji search assets under `assets/common/emoji_search/*.tsv`
are generated from Unicode CLDR annotation data by `scripts/generate_emoji_search_assets.py`.
The generated TSV files are filtered to the emoji set bundled with Pastiera and normalized
for compact local lookup.

## Leipzig Corpora Collection / Wortschatz Leipzig

- Project: Leipzig Corpora Collection / Wortschatz Leipzig
- Project pages: https://corpora.uni-leipzig.de/ and https://wortschatz.uni-leipzig.de/
- Companion asset repository: https://github.com/palsoftware/pastiera-dict
- License: Creative Commons attribution terms for the downloaded corpus/frequency-list data;
  the Leipzig frequency dictionary word lists are documented as CC-BY 3.0, while current
  downloadable corpus data may carry corpus-specific terms.

Pastiera's bundled base dictionaries under `assets/common/dictionaries/*_base.json`
and the generated serialized dictionaries under `assets/common/dictionaries_serialized/*_base.dict`
are frequency-list derivatives built mainly from Leipzig Corpora Collection / Wortschatz Leipzig
word-frequency data, with project-maintained filtering, truncation, normalization, and additional
entries. The companion `pastiera-dict` repository distributes larger downloadable `.dict` assets
and their manifests.

Provenance note: the maintained dictionary pipeline and current maintainer-provided sources point
mainly to Leipzig/Wortschatz frequency data. Some older bundled entries predate the current
documentation trail, so their exact upstream corpus IDs are not fully reconstructed here.

## eellak GSOC 2019 Greek Morphological Dictionary

- Project: GSOC 2019 - Development of a Greek open source Morphological dictionary and application of it to Greek spelling tools
- Source repository: https://github.com/eellak/gsoc2019-greek-morpho
- Source data used for `el_base.dict`: `data/spell_dict_with_freq.dic`
- License: GPLv3 for source code; CC BY-SA 3.0 for the produced morphological database

The downloadable Greek base dictionary distributed through the companion `pastiera-dict`
repository is derived from the top frequency-ranked, letter-only entries in the eellak
Greek spelling dictionary with Pastiera-specific filtering, truncation, and normalization.
