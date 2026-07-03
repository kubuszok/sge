# Plan: R8 + merged ProGuard rules in the Android APK pipeline (minSdk 26)

**Date**: 2026-07-01 (Fable-5 planning window, roadmap Topic 5, near-term half)
**Owner repo**: sge (plan + CI + docs + issue reply) — but the PIPELINE CODE lives in
multiarch-scala (see "where the code actually is" below)
**Companion doc**: `multiarch-scala/docs/plans/2026-07-sbt-android-plugin.md` (structural
extraction — lands AFTER this plan)
**Trigger**: kubuszok/sge#5 — PanamaPort's author (vova7878) states the API-36 floor
claimed in our docs is an artifact of skipping R8 + merged `proguard.txt` processing, and
that minSdk 26 works "with proper configuration".
**Trackers**: sge work → `re-scale db issues add`; multiarch-scala work → GitHub issues.

---

## 0. Context a fresh implementer lacks

### Where the code actually is (roadmap correction)

The roadmap Topic 5 says "in sge's `AndroidBuild.scala`". STALE: the APK pipeline was
extracted to multiarch-scala (commit `9b81523` there) and sge consumes the published
`sbt-multiarch-scala % 0.3.0` (`sge/project/plugins.sbt:5`, `sge/sge-build/build.sbt:60`,
`sge/project/Versions.scala:31`). The files to change are in **multiarch-scala master**:

- `plugin/src/main/scala/multiarch/sbt/AndroidBuild.scala` — the `androidDex` task
  (lines 86-177) runs `java -cp <r8Jar> com.android.tools.r8.D8 --min-api 26 --lib
  android.jar --output <dexDir> <fatJar>` (lines 159-171). It is D8: desugaring only, NO
  shrinking, NO ProGuard-rule processing.
- `plugin/src/main/scala/multiarch/sbt/AndroidDeps.scala` — downloads the 9 PanamaPort
  artifacts, extracts `classes.jar` + `libs/*.jar` from each AAR (lines 92-133) and
  **silently discards `proguard.txt`** — the exact omission the author called out.
- `plugin/src/main/scala/multiarch/sbt/AndroidSdk.scala` — `minSdkVersion = 26`,
  `targetSdkVersion = 35`, `buildToolsVersion = "35.0.0"` (lines 21-27); `r8Jar(sdk)` =
  `build-tools/35.0.0/lib/d8.jar` (lines 86-90; the same JAR contains both the
  `com.android.tools.r8.D8` and `com.android.tools.r8.R8` entry points).

Flow: change multiarch-scala → release `0.5.0` → bump sge pins → sge CI proves it.

> **(COORD-FIX 2026-07-03)** The R8 work releases as **`0.5.0`**, not `0.3.1`: 0.4.0 landed
> the manifest-v2 binary break on master; a 0.3.1 patch cannot be cut from master and no
> 0.3.x maintenance branch is planned; R8 keys/pipeline are additive on top of 0.4.0 → next
> minor 0.5.0 per early-semver.

### Current empirical state (verified 2026-07-01) — read this before believing any doc

- `sge/docs/architecture/android-native-constraints.md` claims an API-36 hard floor caused
  by PanamaPort referencing `Build.VERSION.SDK_INT_FULL` (NoSuchFieldError below 36) and
  calls it "an upstream constraint … not something SGE can work around". **Multiple parts
  of this are wrong today**:
  - The currently-pinned PanamaPort `v0.1.0` (no `-release` classifier — a different
    artifact line than the `v0.1.0/v0.1.1/v0.1.2 -release` ones the doc tested in March)
    does NOT reference `SDK_INT_FULL` at all (verified: grep over every extracted
    `classes.jar` from the cached AARs finds no occurrence; `javap -c` of
    `ArtVersion.class` shows reflection probes `is36p1()`/`is36()`/`is35()`…).
    CAUTION for upgrades: current PanamaPort main/v0.1.3 REVERSES this — `SDK_INT_FULL`
    is the PRIMARY key of `ArtVersion.computeIndex()`, made safe on API <36 only by
    R8/D8's field-get backport (R8 commit `0c417b5`, 2024-11-26, rewrites the read to
    `SDK_INT * 100_000`), which the build-tools-35 `8.6.2-dev` R8 predates. Any future
    PanamaPort version bump REQUIRES this plan's pinned modern R8 first. See
    `docs/plans/research/2026-07-panamaport-emulator-facts.md`.
  - sge CI has been running the emulator at **API 35** since 2026-04-03 (commit
    `ea220355` lowered it from 36) and the smoke suite passes — GL2D/GL3D render through
    PanamaPort FFM on API 35. So the real, proven floor is ≤35 already, without R8.
  - The APK we build ALREADY declares minSdk 26: `androidMinSdk` defaults to
    `AndroidSdk.minSdkVersion = 26` and feeds both `aapt2 link --min-sdk-version` and
    `D8 --min-api`. It would *install* on API 26 today; nobody has ever *run* it there.
  - Nuance on "R8 required": PanamaPort's README never mentions R8/ProGuard, and the
    author's own example app builds with `minifyEnabled false` (= AGP's plain D8 at
    min-api 26, no shrinking). So the load-bearing ingredient for min-api is the
    D8/R8 **desugar/backport pass at `--min-api 26`** — which our D8 step already runs —
    and the merged consumer rules become load-bearing the moment SHRINKING is enabled.
    Consequence: (1) the current D8 pipeline MAY already work at API 26 for the pinned
    v0.1.0 — the API-26 CI leg tests this as a baseline first (§4 step 6); (2) turning
    on R8 shrinking WITHOUT the merged rules would be strictly worse than today —
    never ship that combination.
- The AARs' consumer rules (extracted from the local cache at
  `sge-test/android-smoke/target/jvm-3/streams/_global/unmanagedJars/_global/streams/panama-port-deps/`):
  - `Core`, `VarHandles`, `LLVM`: `proguard.txt` present but **empty** (0 bytes).
  - `Unsafe` (280 bytes): seven `-dontwarn` lines (`sun.misc.Cleaner`,
    `com.v7878.unsafe.{AsynchronousFileChannelBase,AsynchronousSocketChannelBase,DirectByteBuffer$MemoryRef,DirectByteBuffer,HeapByteBuffer}`).
  - `R8Annotations` (4143 bytes): the meat — annotation-driven rules mapping
    `com.v7878.r8.annotations.*` to keeps: `DoNotShrink`/`DoNotShrinkType`/
    `DoNotObfuscate`/`DoNotObfuscateType`/`DoNotOptimize`/`KeepCode` (with `includecode`
    modifier)/`KeepAttributes`/`NoSideEffects` (`-assumenosideeffects`), plus two
    **R8-specific options**: `-alwaysinline` and `-checkdiscard` — both VERIFIED
    accepted by modern R8 (feed addendum 4; the only diagnostics are benign
    `Ignoring modifier: includecode` Info lines). These rules are how PanamaPort
    protects its reflection/hidden-API/runtime-DEX machinery from the shrinker while
    ASKING for aggressive optimization everywhere else.
  - `SunCleanerStub`, `SunUnsafeWrapper`: no `proguard.txt`. `DexFile`/`AndroidMisc` are
    plain JARs (no consumer rules by design).
  - All four PanamaPort AAR manifests declare `android:minSdkVersion="26"`.
- `androidR8Rules` (`AndroidBuild.scala:28`, `settingKey[Option[File]]`) is **dead**: set
  to `None` at line 45 and never `.value`'d by any task. The scaladoc claims it enables
  shrinking — it does not. This plan makes it real.
- The fat-jar step excludes scribe JARs (`AndroidBuild.scala:103-108`) because D8
  miscompiled its Scala 3 lambdas (`VerifyError: wide register index out of range`); the
  dex task already runs the R8 JAR's D8 entry point for the same robustness reason
  (comment at lines 79-85). Keep the scribe exclusion untouched in this plan; retesting
  it under full R8 is a stretch goal, not a gate.

### What the Android Gradle Plugin does that we must replicate (research summary)

For a release build AGP: (1) extracts `proguard.txt` from every AAR ("consumer rules"),
(2) prepends its own baseline file `proguard-android-optimize.txt` (generated from a
resource inside AGP, `com/android/build/gradle/proguard-android-optimize.txt`; the
non-`-optimize` variant used for debug contains `-dontoptimize`), (3) adds keep rules
generated by `aapt2 link --proguard <file>` for classes referenced from the manifest and
resources, (4) feeds ALL of them plus app rules to R8 with `--min-api minSdk` and
`--lib android.jar`. Merge semantics (VERIFIED, feed addendum 3): rules from all sources
are **UNION/ADDITIVE with no documented precedence** — ordering does NOT create override
semantics. The real hazard is the opposite: a GLOBAL flag inside one library's consumer
rules (e.g. `-dontoptimize`) poisons the whole app; AGP 9.0 therefore FILTERS a banned
list of global options out of library consumer rules, and our pipeline replicates that
filter (§1). R8 then, beyond shrinking:
rewrites/desugars backported methods, outlines references to APIs newer than min-api so
classes still pass ART verification on old devices, and assumes
`Build.VERSION.SDK_INT >= minApi` to dead-code-eliminate version guards. THIS is the
mechanism by which PanamaPort's per-ART-version code paths become safe at minSdk 26.
Precision from feed addendum 4 (empirically established; upstream API-outlining
citation still pending): the SDK_INT dead-branch elimination and method/field
BACKPORTING are desugaring-tier features that work even in plain D8 and `--debug`
(`BackportedMethodList` reports 109 backports at min-api 26) — which is why the D8-only
baseline experiment in §4 step 6 is worth running; what only R8 adds is shrinking,
optimization, and honoring the consumer keep rules that protect PanamaPort once
shrinking is on.

> NOTE for implementer: §2 "Verified R8 reference" below pins the exact CLI flags, the R8
> version policy, and the baseline-rules text. Its load-bearing claims are backed by the
> verified-facts feed at `docs/plans/research/2026-07-r8-verified-facts.md` (peer-session
> research: `R8 --version` executed against official dl.google.com packages, source
> citations). If anything conflicts with the R8 build you actually run, TRUST THE TOOL:
> run `java -cp <r8jar> com.android.tools.r8.R8 --help` / `--version` and record the
> output in the implementation PR.

### What "done" means

1. `androidDex` runs **R8** with a merged rules file (baseline + aapt2-generated +
   consumer `proguard.txt` from every AAR + sge/Scala rules + user rules) when
   `androidUseR8 := true` (new default), D8 otherwise.
2. The smoke APK built this way passes the existing smoke IT on an **API 26** emulator CI
   leg AND the existing API 35 leg.
3. `android-native-constraints.md`, `platform-targets.md`, and the `arch-android` skill
   no longer claim an API-36 floor; issue #5 is answered with findings.
4. No regression: API-35 leg, `packaging/android` scripted test, demos `androidAll`, and
   all non-Android CI stay green.

### What must NOT change

- Public key names (`androidDex/androidPackage/androidSign/androidInstall`,
  `androidMinSdk`, …) — re-exported by sge (`SgePlugin.scala:82-85`), used by CI scripts
  and `.rescale/runners.yaml`.
- `androidR8Rules` keeps its type `Option[File]` (source compat) — it becomes *honored*,
  not retyped.
- The D8 path must remain available behind `androidUseR8 := false` — it is the proven
  configuration and the rollback lever.
- Obfuscation stays OFF in this iteration (`-dontobfuscate` in our baseline): sge is
  reflection-heavy Scala 3, logcat-based smoke checks parse class names, and PanamaPort's
  own rules only *allow* obfuscation, they don't need it. Shrinking+optimization are what
  min-api correctness needs.
- sge covenants on `sge-build/src/main/scala/sge/sbt/SgePlugin{,s}.scala` (headers list
  baseline methods): re-run `re-scale enforce verify --file <path>` after any edit.

---

## 1. Design: the modified pipeline

```
scalac ──► fat classes.jar (unchanged, AndroidBuild.scala:101-151)
                │
aapt2 link --proguard r8/aapt-rules.pro (NEW, manifest keep rules; APK output discarded)
                │
rule files (semantics are UNION/ADDITIVE — no precedence, verified, feed addendum 3;
the fixed order below exists only for deterministic builds, error attribution, and
audit readability):
  1. baseline.pro (NEW resource)        ── vendored AGP proguard-android-optimize.txt
                                            (generated content, see §2) + our additions
                                            (-dontobfuscate, Activity/Application keeps)
  2. aapt-rules.pro                     ── manifest keep rules
  3. <consumer rules from every AAR>    ── proguard.txt AND targeted rules from
                                            classes.jar META-INF/com.android.tools/
                                            r8-from-<X>-upto-<Y>/ + META-INF/proguard/;
                                            sorted by file name; SANITIZED: AGP-9 banned
                                            global options filtered out (see §2)
  4. scala-baseline.pro (NEW resource)  ── -dontwarn scala.**, MODULE$ keep, attribute
                                            keeps, keep mainClass (see §2)
  5. androidR8Rules (user, optional)
  6. diagnostics.pro (generated)        ── -printusage/-printseeds/-printconfiguration
                │
R8 (version pinned via androidR8Version, NOT the floating build-tools -dev build; see §2):
  java -cp <r8 jar> com.android.tools.r8.R8
       --release --min-api 26 --lib platforms/android-35/android.jar
       --output <dexDir>
       --pg-conf 1.pro --pg-conf 2.pro ... --pg-conf 6.pro
       --pg-map-output r8/mapping.txt
       <fatJar>
                │
aapt2 link / zipalign / apksigner / adb  (all unchanged, AndroidBuild.scala:180-359)
```

New keys in `multiarch.sbt.AndroidBuild` (exact declarations):

```scala
val androidUseR8 = settingKey[Boolean](
  "Run R8 (shrinking+optimization with merged ProGuard rules) instead of plain D8 " +
  "for DEX compilation. Default: true. Set false to restore the pre-R8 pipeline."
)
val androidR8Version = settingKey[String](
  "Pinned standalone R8 version (Google Maven com.android.tools:r8 / " +
  "storage.googleapis.com/r8-releases). Default: \"9.1.31\". The build-tools lib/d8.jar " +
  "fallback ships unversioned -dev bot builds (35.0.0 -> 8.6.2-dev) and is used only " +
  "when the pinned download is unavailable."
)
val androidR8Jar = taskKey[File]("Resolve (download+cache) the pinned R8 jar")
val androidProguardConsumerRules = taskKey[Seq[File]](
  "consumer proguard.txt files collected from every resolved AAR dependency"
)
val androidR8RuleFiles = taskKey[Seq[File]](
  "ProGuard/R8 rule files (baseline, aapt2 keep rules, sanitized consumer rules, " +
  "scala baseline, androidR8Rules, diagnostics) — passed to R8 as repeated --pg-conf " +
  "arguments in this fixed order (semantics additive; order fixed for determinism)"
)
// androidR8Rules (existing, Option[File]) — now actually honored (slot 5).
```

`androidR8Jar` mirrors the `AndroidDeps` download-and-cache pattern: fetch
`https://storage.googleapis.com/r8-releases/raw/<version>/r8lib.jar` into the plugin
cache (`streams cacheDirectory / "r8" / s"r8lib-$version.jar"`), verify it answers
`--version` with the pinned version, log the version into the build output. FALLBACK
(download blocked / URL gone): `AndroidSdk.r8Jar(sdk)` (= `build-tools/<v>/lib/d8.jar`,
which contains the full R8 entry point) with a WARNING that an unpinned `-dev` R8 is in
use; record which path ran.

Diagnostics always on: `diagnostics.pro` (slot 6) contains
`-printusage <target/android/r8/usage.txt>`, `-printseeds <target/android/r8/seeds.txt>`,
`-printconfiguration <target/android/r8/effective.pro>` so every build leaves an audit
trail (cheap, and invaluable for the §4 failure branches). `effective.pro` is also the
proof of the realized merge order.

Rule-collection change in `AndroidDeps.resolveAar` (lines 92-133): alongside
`classes.jar`, collect consumer rules from BOTH locations (feed addendum 3):
(a) the AAR's top-level `proguard.txt` (when present AND non-empty), and (b) targeted
rules inside the extracted `classes.jar` under
`META-INF/com.android.tools/r8-from-<X>-upto-<Y>/` (version-ranged; select ranges
matching the pinned R8 major) with fallbacks `META-INF/com.android.tools/proguard.txt`
and `META-INF/proguard/*` — AGP ≥3.6 consumers read these. Extract each to
`<cacheDir>/<artifactId>-<version>-<origin>-rules.pro`. New method
`def panamaPortProguardRules(cacheDir: File, r8MajorVersion: Int): Seq[File]` (GATE-FIX 2026-07-02: the R8 major is threaded as a parameter so range-matching is implementable in R8-1; R8-2 wires `androidR8Version`'s major into it — for the pinned 9.1.31 that is 9) returns them sorted by file
name (deterministic argument order). `AndroidBuild` wires it into
`androidProguardConsumerRules` — files arrive ALREADY sanitized (GATE-FIX 2026-07-02: the sanitizer lives in ONE place, AndroidDeps.panamaPortProguardRules per §4 Step 1; AndroidBuild consumes pre-sanitized output and must NOT re-sanitize)
before use. Empty rule files are skipped but logged
(`log.info(s"  consumer rules: $name (${bytes}B)")`) — today that means the merge picks
up exactly `Unsafe` + `R8Annotations` (verified: the PanamaPort AARs carry no
`META-INF/com.android.tools` targeted rules; re-check on every PanamaPort upgrade).

Manifest keep rules: `androidR8RuleFiles` runs
`aapt2 link -o <target/android/r8/rules-probe.apk> -I android.jar --manifest <manifest>
--min-sdk-version 26 --target-sdk-version 35 --proguard <target/android/r8/aapt-rules.pro>
--proguard-minimal-keep-rules --no-proguard-location-reference`
and uses only the `.pro` output (the probe APK is deleted). VERIFIED (feed addendum 5,
executed against build-tools 35.0.0 aapt2): all flags exist (`--proguard`,
`--proguard-main-dex`, `--proguard-conditional-keep-rules`,
`--proguard-minimal-keep-rules`, `--no-proguard-location-reference`); manifest
components emit unconditional `-keep class <name> { <init>(); }` rules — expected for
the smoke app: `-keep class sge.smoke.SmokeActivity { <init>(); }`. The two extra flags
mirror what AGP always passes (AaptV2CommandBuilder.kt); `--proguard-main-dex` is
skipped (native multidex at min-api 26). FALLBACK (only if a future aapt2 drops the
flags): generate the keep rules directly by parsing `AndroidManifest.xml` for
`application/activity/service/receiver/provider` `android:name` attributes and emitting
`-keep class <name> { <init>(); }` per class; file a multiarch issue recording which
path shipped.

Also ALWAYS keep the app's entry points (they're loaded reflectively by ART/our loader):

```
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
```

(belt-and-suspenders on top of the aapt2 rules; goes in `baseline.pro`).

---

## 2. Verified R8 reference (research annex)

<!-- VERIFIED-FACTS FEED: load-bearing claims in this section cite
     docs/plans/research/2026-07-r8-verified-facts.md (R8 CLI/versions; peer-session
     research, executed --version against official dl.google.com packages) and
     docs/plans/research/2026-07-panamaport-emulator-facts.md (PanamaPort internals,
     API-26 emulator images, VarHandle floors). Addenda 2-5 in the r8 feed file cover
     Scala pitfalls, AGP merge semantics, the -alwaysinline premise correction, aapt2
     --proguard output shapes, the shrinking-mode matrix, and the class-file version
     ceiling. SOLE remaining open citation: R8 API outlining
     (ExternalSyntheticApiModelOutline*) — empirics in the feed, upstream citation
     pending; verify before relying on outlining specifically. Future addenda land in
     that research/ directory — check for newer files before implementing, and
     re-verify anything marked UNVERIFIED below. -->

- **Min-api backporting is the PanamaPort-critical R8 feature (VERIFIED)**: R8 commit
  `0c417b5` (2024-11-26) backports `Build.VERSION.SDK_INT_FULL` static-field reads to
  `SDK_INT * 100_000` on pre-Baklava devices. Current PanamaPort main/v0.1.3 keys its
  ART-version dispatch on that field, so any PanamaPort upgrade depends on an R8/D8
  containing this commit — build-tools 35.0.0's `8.6.2-dev` predates it; the pinned
  9.1.31 has it. This is the concrete mechanism behind the author's "R8 automatically
  replaces many things for older versions of Android".
- **Shrinking-mode matrix (VERIFIED empirically, feed addendum 5)**:
  `SDK_INT >= minApi` dead-branch elimination needs NO shrinking — plain D8 does it in
  every mode (even `--debug`), driven purely by `--min-api`. Backported-method
  rewriting is likewise a desugaring feature (`BackportedMethodList --min-api 26`
  reports 109 backports; the tool ships in the same d8.jar). Shrinking-only semantics:
  `-whyareyoukeeping` is silent under `-dontshrink`; `-checkdiscard` is vacuous under
  `--no-tree-shaking`. API OUTLINING (`ExternalSyntheticApiModelOutline*`) remains the
  one UNVERIFIED-citation item — do not build arguments on it without checking the feed
  for the pending citation.
- **Class-file version ceiling (VERIFIED, feed addendum 5)**: R8/D8 8.6.2 hard-fail on
  Java 25 classfiles (major 69, `Unsupported class file major version 69`); Java 17
  (major 61) works. The fat-jar input must stay ≤ the pinned R8's supported major —
  preflight + remediation in §4 branch F7.
- **VarHandle floors (VERIFIED)**: platform `java.lang.invoke.VarHandle` is API 33;
  R8's VarHandle desugaring is partial and OFF by default
  (`InternalOptions.enableVarHandleDesugaring = false`). PanamaPort side-steps both by
  shipping its own `com.v7878.invoke.VarHandle` — no extra R8 configuration needed for
  it at min-api 26. `MethodHandle.invoke/invokeExact` require `--min-api >= 26` (D8
  errors below) — we are exactly at 26; never lower `androidMinSdk` below it.
- **R8 version policy (VERIFIED)**: Google publishes NO build-tools→R8 mapping;
  build-tools ships `-dev` bot builds (35.0.0 → R8 `8.6.2-dev`, 36.0.0 → `8.10.9-dev`,
  36.1.0 → `9.0.3-dev`) and flags drift across versions (`--desugared-lib-pg-conf-output`
  removed after 8.6.2; `--api-database` added later). Therefore: PIN the standalone R8 —
  `com.android.tools:r8` on Google Maven, latest release `9.1.31` (2026-03-20), prebuilt
  at `https://storage.googleapis.com/r8-releases/raw/<version>/r8lib.jar` — via
  `androidR8Version`/`androidR8Jar` (§1), and RECORD the version each build ran
  (`--version` check in the resolver). Build-tools `lib/d8.jar` contains the full R8
  entry point (`com.android.tools.r8.R8`) and stays as the offline fallback only.
- **CLI semantics (VERIFIED)**: `--pg-conf` is repeatable and files parse in
  command-line order (R8Command.Builder appends to an ArrayList); rule SEMANTICS are
  additive/union with no precedence (feed addendum 3), so the §1 ordering is fixed for
  determinism and error attribution, not for overrides. R8's DEFAULT mode is RELEASE
  (opposite of D8, which
  defaults to `--debug`) and default `--min-api` is 1 — pass `--release` and
  `--min-api` explicitly anyway so the invocation is self-describing. `--lib` accepts
  android.jar or a JDK home. Canonical invocation:
  `java -cp r8.jar com.android.tools.r8.R8 --release --min-api <n> --output out
  --pg-conf <files...> --lib <android.jar> input.jar`.
  `--pg-map-output <file>` writes the mapping; `--output <dir>` emits `classes.dex`,
  `classes2.dex`, …; inputs are trailing args (our single fat JAR).
  `--no-tree-shaking` / `--no-minification` exist as blunt switches; we express
  no-obfuscation via `-dontobfuscate` in rules instead (finer-grained, keeps shrinking).
- **Baseline rules (`baseline.pro`, embedded as a plugin resource) — VERIFIED (feed
  addendum 3)**: `proguard-android-optimize.txt` is NOT a static SDK file — AGP's
  `ProguardFiles.java` GENERATES it by concatenating jar resources
  `com/android/build/gradle/proguard-header.txt` + a variant middle +
  `proguard-common.txt` (fragments byte-identical across AGP 8.11.2/8.13.2/9.2.1). Do
  NOT reference the obsolete `$ANDROID_HOME/tools/proguard/` copy. Implementer: VENDOR
  the generated content — extract the two fragments from a pinned
  `com.android.tools.build:gradle` jar (record the AGP version in a comment at the top
  of `baseline.pro`) and concatenate exactly as AGP does. The generated file contains
  REAL rules (7× `-keepattributes`, native methods, View setters, Activity onClick
  handlers, enum `values`/`valueOf`, `Parcelable.CREATOR`, `@JavascriptInterface`,
  legacy `@Keep`, `-dontwarn androidx.**`); the only functional optimize-vs-plain diff
  today is `-allowaccessmodification` vs `-dontoptimize`. Use the OPTIMIZE variant —
  never include `-dontoptimize` (it would disable the optimizations PanamaPort's
  `AlwaysInline`/`NoSideEffects` rules request). Our additions appended after the
  vendored text: `-dontobfuscate` and the Activity/Application keeps from §1.
- **Consumer-rule sanitizer — replicate AGP 9.0 (VERIFIED, feed addendum 3)**: because
  merging is additive, one AAR's global flag can poison the whole app (e.g. a library
  shipping `-dontoptimize`). AGP 9.0 filters these out of library consumer rules; our
  `androidR8RuleFiles` applies the same banned-for-libraries list to every slot-3 file
  (comment the line out + log): `-include -basedirectory -injars -outjars -libraryjars
  -repackageclasses -flattenpackagehierarchy -allowaccessmodification
  -renamesourcefileattribute -ignorewarnings -addconfigurationdebugging
  -printconfiguration -printmapping -printusage -printseeds -applymapping
  -obfuscationdictionary -classobfuscationdictionary -packageobfuscationdictionary`.
  (Today's PanamaPort rules contain NONE of these — the filter is future-proofing; our
  own `-print*` diagnostics live in slot 6, which is app-level and exempt.)
- **Scala rules (`scala-baseline.pro`) — feed addendum 2 (VERIFIED, cited)**:
  ```
  -dontwarn scala.**
  -keepclassmembers class * { ** MODULE$; }        # object singletons via reflection
  -keepattributes Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
  ```
  plus `-keep class <mainClass>` when `Compile / mainClass` is set. Scala 3 lambdas
  (`LambdaMetafactory.altMetafactory`, always-serializable) desugar fine at any
  min-api — no keeps needed. KNOWN NO-FIX pitfall to document in scaladoc: D8/R8
  DELETES `$deserializeLambda$` (LambdaDeserializationMethodRemover) — Java
  lambda-SERIALIZATION is silently broken on Android and no keep rule restores it (sge
  does not serialize lambdas; if a game does, that is out of scope). R8 runs in "full
  mode" by default (no `--pg-compat`): reflection-only classes need explicit `-keep`,
  default constructors are NOT implicitly kept, and `-keepattributes` only applies to
  kept items — R8 only models reflection with CONSTANT arguments. AGP≥8-style
  diagnostics: R8 emits suggested rules for missing classes (missing_rules.txt
  mechanism) — feed those back as targeted `-dontwarn`/`-keep` lines, never blanket.
- **`-alwaysinline` / `-checkdiscard` — VERIFIED SAFE (feed addendum 4, premise
  correction)**: `-alwaysinline` was NEVER removed from R8 — it parses in the regular
  option chain and IS applied from 8.2 through main (empirically exit 0 on 8.2.47,
  8.6.2, 8.13.19), though undocumented/officially unsupported. The "removed around 8.2"
  folklore confuses it with the TESTING-gated options (`-neverinline`,
  `-alwaysclassinline`, …), which hard-error without
  `-Dcom.android.tools.r8.allowTestProguardOptions=true`. The full PanamaPort
  R8Annotations consumer rules run on standalone R8 8.13.19 with exit 0 — the only
  diagnostics are two benign Info lines `Ignoring modifier: includecode`. So: pass the
  consumer rules through UNMODIFIED (they are also not on the AGP-9 banned list).
  `-checkdiscard` is ENFORCED (build fails if the named item survives) but vacuous
  under `--no-tree-shaking`. `-assumenosideeffects` is supported (removes calls whose
  results are unused; it does NOT substitute default values the way ProGuard's
  `-assumevalues` does). §4 branch F1 remains only as the contingency for a FUTURE R8
  changing this; the step-3 dry run on the pinned 9.1.31 is the record.
- **Unknown-option taxonomy (VERIFIED, feed addendum 4)** — for triaging R8 config
  errors: silently ignored (`-optimizations`, `-dontpreverify`, `-verbose`,
  `-dontusemixedcaseclassnames`, `-overloadaggressively`, … — several sit in the
  vendored AGP baseline; harmless, keep them for fidelity), Info
  (`-optimizationpasses`), warned (`-outjars`, `-addconfigurationdebugging`,
  `-assumenoexternal*`), hard error (`-skipnonpubliclibraryclasses` and any truly
  unknown option). Only the hard-error class can fail a build.
- **Diagnostics semantics (VERIFIED, feed addendum 4)**: `-printusage`/`-printseeds`/
  `-printconfiguration` take an optional filename (stdout otherwise;
  `-printconfiguration` output carries per-file provenance comments — that is the merge
  audit trail). `-whyareyoukeeping` prints to stdout only and produces NO OUTPUT under
  `-dontshrink` — never combine the two when debugging keeps.

---

## 3. Cross-repo change list

| # | Repo | File | Change |
|---|---|---|---|
| 1 | multiarch-scala | `plugin/.../AndroidDeps.scala:92-133` | extract + expose consumer `proguard.txt` |
| 2 | multiarch-scala | `plugin/.../AndroidBuild.scala` | new keys (§1); `androidR8Jar` pinned-download resolver; `androidR8RuleFiles`; `androidDex` branches on `androidUseR8`: R8 command from §2 vs existing D8 lines 159-171 |
| 3 | multiarch-scala | `plugin/src/main/resources/multiarch/android/{baseline,scala-baseline}.pro` | NEW resources (§2) |
| 4 | multiarch-scala | release | tag `0.5.0` (COORD-FIX 2026-07-03; was 0.3.1), `ci-release` |
| 5 | sge | `project/plugins.sbt:5`, `sge-build/build.sbt:60`, `project/Versions.scala:31` | bump `0.3.0` → `0.5.0` (COORD-FIX 2026-07-03) |
| 6 | sge | `.github/workflows/ci.yml` (test-android / test-android-it, lines 678-765) | API-26 matrix leg (§4 step 6) |
| 7 | sge | `docs/architecture/android-native-constraints.md`, `docs/architecture/platform-targets.md:98`, `.claude/skills/arch-android/SKILL.md` | corrections (§6) |
| 8 | sge | issue #5 | reply (§7) |

---

## 4. Steps, exact commands, expected outputs, failure branches

### Step 1 — multiarch-scala: consumer-rule extraction (change #1)

Edit `resolveAar`: after the `classes.jar` extraction block (lines 104-110), add
extraction of consumer rules from BOTH locations (§1): the AAR's top-level
`proguard.txt` (when the entry exists and `entry.getSize > 0`) AND, inside the
extracted `classes.jar`, `META-INF/com.android.tools/r8-from-<X>-upto-<Y>/*`
(range-match against the pinned R8 major) with fallbacks
`META-INF/com.android.tools/proguard.txt` and `META-INF/proguard/*` — extract each to
`<artifactId>-<version>-<origin>-rules.pro`. Add
`def panamaPortProguardRules(cacheDir: File, r8MajorVersion: Int): Seq[File]` scanning the cache for
`*-rules.pro`, sorted, plus the AGP-9 banned-global-options sanitizer from §2.
Unit-test with the plugin module's munit setup (`build.sbt:175-187`; tests run on the
2.12 axis only): fixture AAR built in-test with `java.util.zip` covering (a) a
non-empty `proguard.txt`, (b) an empty one, (c) a `classes.jar` with a
`META-INF/com.android.tools/r8-from-8.0/` rules file (GATE-FIX 2026-07-02: range-dir semantics — `r8-from-<X>` with no `-upto-` matches any major >= X; `r8-from-<X>-upto-<Y>` matches X <= major < Y; classes.jar-sourced rules use origin tag `aarjar` in the `<artifactId>-<version>-<origin>-rules.pro` scheme; TEST SEAM — extraction logic must be exposed as a package-private `private[sbt] def extractRulesFromAar(aar: File, cacheDir: File, artifactId: String, version: String): Seq[File]` that `resolveAar` delegates to, so the fixture AARs (a)/(b)/(c) are testable offline without the hardcoded Maven URLs), (d) the sanitizer: a consumer
file containing `-repackageclasses` (banned → stripped+logged) alongside `-dontwarn x.**`
(kept); keep the test's banned-list fixture in sync with the §2 list. (Note the feed's
AGP-9 list does NOT include `-dontoptimize` even though it is the canonical poison
example — replicate the list as-is, do not extend it unilaterally.)

```
sbt> +plugin/compile ; plugin/test
```
Expected: both axes compile; new `AndroidDepsSpec` green.
Fail (2.12 vs 3.8.4 syntax): same rule as everywhere in this repo — write 2.12-compatible
code; the `Compat` objects exist for API differences.

### Step 2 — multiarch-scala: R8 merge + invocation (changes #2, #3)

Implement §1 exactly. `androidDex` structure: fat-jar assembly UNCHANGED (lines 96-151);
then

```scala
val useR8 = androidUseR8.value
if (useR8) {
  val r8jar     = androidR8Jar.value               // pinned 9.1.31, fallback build-tools d8.jar
  val ruleFiles = androidR8RuleFiles.value         // ORDERED, §1 slots 1-6
  val pgConf    = ruleFiles.flatMap(f => Seq("--pg-conf", f.getAbsolutePath))
  runJava(r8jar, "com.android.tools.r8.R8",
    Seq("--release", "--min-api", minApi.toString,
        "--lib", androidJarPath,
        "--output", dexDir.getAbsolutePath) ++ pgConf ++
    Seq("--pg-map-output", (r8Dir / "mapping.txt").getAbsolutePath,
        fatJar.getAbsolutePath))
} else { /* existing D8 block, lines 158-173, verbatim */ }
```

(Rule semantics are additive — the fixed `--pg-conf` order is for determinism and error
attribution only, see §2/feed addendum 3. Do not concatenate the files into one
merged.pro; per-file `--pg-conf` keeps R8's error messages pointing at the offending
source file.)

Local proof (no emulator): use any consumer project — the fastest is sge's scripted
fixture. From a local checkout with multiarch `publishLocal`-ed
(`sbt '+plugin/publishLocal'`, note the version it prints, wire it into the scripted
fixture's `project/plugins.sbt`):

```
cd sge/sge-build && sbt 'scripted packaging/android'
```
Expected: `checkAndroidSign` passes (valid signed APK containing `classes.dex`). The
scripted fixture's HelloGame has no PanamaPort usage, so this proves pipeline mechanics
only — PanamaPort proof is the emulator legs (steps 5-6).
Also assert rule collection happened: the scripted log must show
`consumer rules: Unsafe-v0.1.0-aar-rules.pro (280B)` and
`consumer rules: R8Annotations-v1.0.0-aar-rules.pro (4143B)` lines — names per Step 1's
`<artifactId>-<version>-<origin>-rules.pro` scheme; if the implementation picks
different origin tags, update BOTH the log line and this gate together (add these log
lines in `androidR8RuleFiles`; make the scripted `test` script grep for them).

**Failure branches:**
- **F1 — a FUTURE R8 rejects `-alwaysinline`/`-checkdiscard`** — NOT expected on the
  pinned 9.1.31: feed addendum 4/5 verified the full R8Annotations rules run with
  exit 0 on 8.2.47/8.6.2/8.13.19 (`-alwaysinline` silently accepted and applied;
  `includecode` produces a benign Info). Distinguish two signatures: (a) parse-time
  "unknown/unsupported option" error naming the consumer rules file — THIS branch;
  (b) `Error: Discard checks failed.` / `Item ... was not discarded` — that is
  `-checkdiscard` WORKING as designed (an expected-inlined item survived); treat it as
  F4 (something blocked optimization), not F1. For (a): extend the sanitizer in
  `androidR8RuleFiles` (a sanitized copy of the offending consumer file replaces the
  original in slot 3) that comments out lines starting with `-alwaysinline` or
  `-checkdiscard` (and their `class`-block continuation lines up to the closing `}` or
  next `-` directive), logging each. IMPORTANT: strip BOTH or NEITHER — `-checkdiscard`
  FAILS the build when the named item survives, and stripping only `-alwaysinline` can
  leave `@CheckDiscard`-annotated items (4 uses in PanamaPort) un-inlined and therefore
  un-discarded. These options only *tune* optimization; correctness keeps come from the
  other rules. Record in the PR which options were stripped. If R8
  rejects anything ELSE from consumer rules, STOP — do not sanitize further without
  filing a multiarch issue with the exact R8 error, because eating arbitrary consumer
  rules can silently break PanamaPort.
- **F2 — R8 fails on missing library classes** (`Missing class …` errors referencing
  `sun.misc.*`, `javax.*`, Scala classes — hard errors in modern R8): R8's diagnostics
  include SUGGESTED rules for exactly the missing references (the AGP `missing_rules.txt`
  mechanism; on the CLI they appear in the error output). Feed those back as targeted
  `-dontwarn <exact.pkg>.**` lines into `scala-baseline.pro` — for each REPORTED package
  only. NEVER add a blanket `-ignorewarnings`. If more than ~10 distinct packages need
  it, STOP and attach the full R8 stderr to an issue.
- **F3 — R8 OOM/slow on the fat jar** (smoke fat-jar is tens of MB of Scala): bump the
  forked JVM: run via `Fork.java` / add `-Xmx4g` to the `java` invocation (the command is
  a plain `SysProcess` — insert `"-Xmx4g"` after `"java"`). Not expected below 1M methods.
- **F7 — `Unsupported class file major version N`** (VERIFIED constraint, feed
  addendum 5): R8/D8 8.6.2 hard-fail on Java 25 classfiles (major 69); Java 17
  (major 61) is fine. Trigger: some jar on the Android classpath compiled with a new
  JDK target. Fixes, in order: (1) confirm the PINNED 9.1.31 is actually being used
  (the fallback build-tools R8 has the older reader — check the resolver log line);
  (2) if the offender is an sge/consumer module, compile it with `-release 17` (sge
  already does this for its jvm-platform modules — `sge/build.sbt:404,426`; apply the
  same to the offending module's Android axis); (3) if a third-party dep, exclude it
  from the fat jar via `androidDexJarExclude`-style filtering and file an issue.
  PREFLIGHT (cheap, do it in Step 2): `androidDex` logs the max class-file major found
  while building the fat jar (read bytes 6-7 of each `.class` header) and warns above
  61 — turns this failure from an R8 stack trace into a named jar.

### Step 3 — first R8'd smoke APK + local emulator proof at API 35

In sge (multiarch pinned to the locally published version for this step only):

```
re-scale runner android-smoke-build          # sbt --client 'sge-android-smoke/androidSign'
re-scale runner android-it                   # local emulator (sge-test-avd), API per local AVD
```
Expected: `AndroidSmokeTest` passes — all 13 frame-phase checks
(`BOOTSTRAP,GL2D,GL3D,FILEIO,JSON_XML,AUDIO,INPUT,PREFERENCES,CLIPBOARD,DISPLAY,FILEHANDLE_TYPES,SENSORS,TOUCH_SETUP`)
reported, ≥3 frame markers, no FATAL (see `sge-test/it-android/.../AndroidSmokeTest.scala:300-346`
for the exact pass condition).
**This is the critical gate**: it proves R8 shrinking did not strip anything PanamaPort
or sge reaches reflectively, before any minSdk questions.

**Failure branch F4 — R8 stripped something reached reflectively** (logcat shows
`ClassNotFoundException`/`NoSuchMethodError`/`NoSuchFieldError`, typically under
`com.v7878.*`, `sge.*`, or `scala.*`). Diagnosis ladder — run in order, stop at first fix:
1. Identify the missing member M from the crash stack (the IT prints full logcat).
2. Check `target/.../android/r8/usage.txt` (`-printusage` output = everything REMOVED).
   M listed ⇒ shrunk away; M absent but crash persists ⇒ suspect optimization
   (inlining/member-value propagation), go to rung 4.
3. Add the narrowest keep to the sge-side `androidR8Rules` file first (create
   `sge-test/android-smoke/r8-extra.pro`, wire `androidR8Rules := Some(file(...))` in
   `build.sbt`'s smoke project): `-keep class <exact.Class> { <exact member>; }`. Re-run
   step 3. Ask WHY it was dropped with a one-off:
   append `-whyareyoukeeping class <exact.Class>` to the extra rules and read the R8
   stdout (explains the retained-graph path; if nothing references it, nothing keeps it —
   reflection needs explicit keeps). Caveat (feed addendum 5): `-whyareyoukeeping`
   emits NOTHING when shrinking is disabled — do not combine the probe with a
   `-dontshrink`/`--no-tree-shaking` experiment.
4. If crash persists with the keep: disable specific optimizations for the class:
   `-keep,allowshrinking class <exact.Class> { *; }` won't help against inlining — use
   `-keepclassmembers class <exact.Class> { *; }` plus, if still broken,
   `-dontoptimize` as a TEMPORARY global probe. If `-dontoptimize` fixes it, bisect which
   member needed protection, replace the global flag with a targeted rule, and file a
   multiarch issue documenting it.
5. Mode probe: re-run with `--pg-compat` (ProGuard-compatible mode — R8's CLI default is
   "full mode", which keeps strictly less: no implicit default-constructor keeps,
   attributes only on kept items; see §2). If compat mode fixes it, the difference
   pinpoints a missing explicit keep — find it with `-whyareyoukeeping` under compat,
   add the targeted rule, and RETURN to full mode; shipping `--pg-compat` permanently is
   allowed but must be recorded as a setting comment + issue note.
6. Escalation ceiling: `-keep class com.v7878.** { *; }` (kills most size benefit for
   PanamaPort but preserves min-api rewriting). If EVEN THAT fails, set
   `androidUseR8 := false` (pipeline reverts to D8 exactly), file the sge issue with
   logcat + usage.txt + effective.pro attached, and post the findings to issue #5 asking the
   author — do NOT ship a half-working R8 config.
Every rung's evidence (rule added, resulting logcat) goes into the implementation issue's
resolution notes.

### Step 4 — wire `androidR8Rules` for consumers + release

- Honor `androidR8Rules` in the merge (append last, so user rules win notes-wise).
- Scaladoc on `androidUseR8`/`androidR8Rules` updated to describe the real behavior
  (current scaladoc lies — see §0).
- multiarch-scala: PR, review, tag `0.5.0` (COORD-FIX 2026-07-03; was 0.3.1), `ci-release`.

```
sbt> +plugin/publishLocal   # for final local verification
# then: git tag 0.5.0 && push → CI ci-release publishes to Maven Central  (COORD-FIX 2026-07-03; was 0.3.1)
```
Expected: Sonatype release visible; `com.kubuszok:sbt-multiarch-scala:0.5.0` resolvable. (COORD-FIX 2026-07-03; was 0.3.1)
Fail (release pipeline): this repo's standard release flow is git-tag-driven
(`build.sbt:57-71`); any failure here is handled per that repo's norms, not this plan.

### Step 5 — sge: pin bump PR

Bump the three pins (§3 row 5). Full sweep:

```
re-scale build compile --all
re-scale test verify
(cd sge-build && sbt 'scripted packaging/browser packaging/browser-rapier2d packaging/asset-manifest packaging/android')
re-scale runner android-smoke-build && re-scale runner android-it
(cd demos && sbt --client androidAll)     # 11 APKs must still build
```
Expected: all green. The demos consume the published sge-build plugin — run
`(cd sge-build && sbt publishLocal)` first (per sge CLAUDE.md).
Failure: anything red that mentions R8 → back to steps 2-3 branches; anything red
unrelated to Android → rebase/flake triage as usual, it is not this plan's scope.

### Step 6 — sge CI: API-26 emulator leg

Edit `.github/workflows/ci.yml`. Convert `test-android` (lines 678-713) to a matrix:

```yaml
  test-android:
    name: Android tests (API ${{ matrix.api-level }})
    strategy:
      fail-fast: false
      matrix:
        api-level: [26, 35]
    ...
      - name: Android smoke tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: ${{ matrix.api-level }}
          target: google_apis
          arch: x86_64
          force-avd-creation: false
          emulator-options: -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim -no-metrics
          disable-animations: true
          script: sbt 'sge-android-smoke/testFull'
```

`test-android-it` (lines 717-765) keeps `needs: [test-android]` and stays at 35 (one IT
leg is enough; the smoke matrix is the min-api proof). System image:
`system-images;android-26;google_apis;x86_64` — VERIFIED to exist in Google's sys-img
repository (also `default;x86_64`; there is NO `armeabi-v7a` at 26), and the exact
`api-level: 26, arch: x86_64, ubuntu-latest + KVM` combo is used in the wild
(dart-lang/native `jnigen.yaml`). The runner action installs the image from the
`api-level`/`target`/`arch` inputs automatically. Known runner quirk at API 26
(reactivecircus issue #373): occasional POST-test emulator shutdown hang ("stop: Not
implemented" → action timeout) — if observed, set env
`ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL: 60` on the step and/or raise
`emulator-boot-timeout` from its 600s default; these are retry-level mitigations, not
plan changes. (Sources: research feed, panamaport-emulator-facts.)

**Run the leg TWICE, in this order:**
1. **Baseline experiment — current D8 pipeline at API 26** (before the sge pin bump, or
   with `androidUseR8 := false`): PanamaPort's author ships his example app un-shrunk
   (`minifyEnabled false`), so the pinned v0.1.0 + D8-at-min-api-26 may already work.
   Whatever happens is pure signal: green ⇒ our old doc claim is doubly dead and R8's
   value is shrinking + upgrade-headroom; red ⇒ logcat shows exactly what D8-only lacks.
   Record the result in the issue notes and in the §7 reply.
2. **The R8 pipeline** (after the pin bump) — this one is the gate.

Expected: **treat the first runs as an experiment, not a gate.** Boot + install must
succeed and BOOTSTRAP/GL2D/GL3D/FILEIO/AUDIO/DISPLAY must PASS (on run 2). The
excused-check sets in `AndroidSmokeTest.scala` (`knownFailures`, line 397; missing-check
enforcement, lines 300-316) were calibrated on API 35 — API 26 may legitimately differ
(older WebView-less image, different sensor stack, OpenSL ES quirks).
**Failure branch F5 — API-26-specific check failures**: for each failing check decide:
(a) app bug at min-api → fix in sge (that's the point of the leg); (b) emulator-image
capability gap → extend the excusal mechanism to be API-aware: the test reads
`ro.build.version.sdk` via `adb shell getprop` and applies a per-API excusal map (new
code in `AndroidSmokeTest`; keep the API-35 sets exactly as-is). Every excusal needs the
same honesty treatment as the existing ones: comment + issue reference. If the emulator
itself won't boot at 26 on the runner (rare; image exists for x86_64), try
`target: default` instead of `google_apis`; if still dead after 2 attempts, STOP, keep
the leg `continue-on-error: true`, and file an sge issue with the runner logs.
**Failure branch F6 — PanamaPort genuinely broken at 26 even under R8**: capture logcat,
set the 26 leg `continue-on-error: true` (honest-red, non-required — same doctrine as
ISS-538 clause 2), and post the evidence on issue #5 asking the author for the missing
configuration. Do NOT silently drop the leg.

### Step 7 — docs corrections (§6) and issue reply (§7) — after the 26 leg is green.

---

## 5. Verification gates (orchestrator re-runs independently)

1. multiarch: `sbt '+plugin/compile; plugin/test'` green.
2. sge scripted: `sbt 'scripted packaging/android'` green AND its log contains the two
   `consumer rules:` lines (proves the merge actually ingested AAR rules — guards the
   silent-skip failure mode).
3. `re-scale runner android-it` green locally; CI `test-android` green on BOTH matrix
   legs; `test-android-it` green.
4. Artifact audit: `unzip -l` on the signed smoke APK shows `classes*.dex` count and
   total size REDUCED vs the D8 build (R8 shrinking evidence); `target/.../r8/usage.txt`
   exists and is non-empty. Record both numbers in the resolution notes.
5. Ratchet: `re-scale enforce verify --all` unchanged; no covenant regressions.
6. Opus dry-run gate (roadmap Topic 8) before 2026-07-06: fresh Opus agent restates the
   steps and executes Step 1 in a scratch worktree; plan defects found are fixed here.

---

## 6. Documentation corrections (exact quotes → replacements)

`sge/docs/architecture/android-native-constraints.md`:

| Current (WRONG) | Replacement |
|---|---|
| "**API 36 (Android 16)** is the minimum supported Android version for SGE." (line 9) | "**API 26 (Android 8.0)** is the minimum supported Android version for SGE (matching PanamaPort's own floor). Verified in CI on API 26 and API 35 emulators. The DEX step must run R8 with the merged consumer ProGuard rules — see `docs/plans/2026-07-android-r8.md`; with plain D8 the practically proven floor was API 35." |
| "All tested versions (v0.1.0, v0.1.1, v0.1.2) reference `Build.VERSION.SDK_INT_FULL`, a field that only exists on API 36+. Running on older APIs causes `NoSuchFieldError` at class-load time." (lines 11-14) | "Older PanamaPort artifact lines (`-release`-classifier v0.1.x) referenced `Build.VERSION.SDK_INT_FULL` (API 36+). The current unclassified `v0.1.0` artifacts detect the ART version via reflection probes in `com.v7878.unsafe.ArtVersion` and declare `minSdkVersion=26` in their AAR manifests." |
| "This is an **upstream constraint** in PanamaPort's `ArtVersion.computeIndex()` — not something SGE can work around without forking or patching the library." (lines 16-17) | "Per the library author (kubuszok/sge#5), API 26+ support requires processing each AAR's consumer `proguard.txt` through R8 — which the SGE pipeline now does." |
| "- Emulators must target API 36+ (system image: `system-images;android-36;google_apis;arm64-v8a`)" and the two bullets after it (lines 21-24) | "- CI emulators: `system-images;android-{26,35};google_apis;x86_64`\n- `minSdkVersion` 26 / `targetSdkVersion` 35 (defaults from `multiarch.sbt.AndroidSdk`)\n- NDK cross-compilation targets API 26, aligned with the runtime floor" |
| "1. **PanamaPort requires API 36+**: cannot support older Android versions without upstream fix" (line 113) | "1. **PanamaPort requires API 26+ and an R8-processed APK**: the consumer rules shipped in its AARs must reach R8; plain-D8 builds are only proven on recent API levels" |

Also update the header **Date** and add a changelog line citing issue #5.

`sge/docs/architecture/platform-targets.md:98`: "- API 36+ minimum (PanamaPort
constraint)" → "- API 26+ minimum (PanamaPort floor; requires the R8 pipeline — see
android-native-constraints.md)".

`sge/.claude/skills/arch-android/SKILL.md` (line 9): "Key constraints: API 36+ minimum,
…" → "Key constraints: API 26+ minimum (R8 pipeline required), …".

`multiarch-scala` scaladoc lies fixed in Step 4 (androidR8Rules/androidDex docstrings).

---

## 7. Reply to kubuszok/sge#5 (post after the API-26 leg is green; adjust if F5/F6 hit)

> Thank you — you were right on all counts, and this is now fixed.
>
> What we found while implementing it:
> - Our docs' "API 36 floor" was measured against the old `-release`-classifier v0.1.x
>   artifacts (`SDK_INT_FULL` at class load). The current unclassified `v0.1.0` line
>   probes ART reflectively, and our D8-only build already ran fine on an API 35
>   emulator — the docs were stale on top of the pipeline being wrong.
> - The pipeline now: collects `proguard.txt` from every AAR (for the current PanamaPort
>   set that's `Unsafe` + `R8Annotations`; `Core`/`VarHandles`/`LLVM` ship empty files),
>   prepends an AGP-equivalent baseline (optimize variant, plus `-dontobfuscate` for
>   now), adds aapt2-generated manifest keep rules, and runs R8 with `--release
>   --min-api 26 --lib android.jar`. Implemented in
>   [multiarch-scala](https://github.com/kubuszok/multiarch-scala) (our sbt Android
>   plugin), consumed by SGE.
> - CI now runs the smoke suite on `system-images;android-26;google_apis;x86_64` and
>   API 35. [link the green run] [Also report the step-6 baseline experiment result:
>   whether the old D8-only build already ran at API 26 with the v0.1.0 line — your
>   HelloFromPanama example builds with `minifyEnabled false`, which suggested it
>   might.]
> - We pinned standalone R8 9.1.31 rather than the build-tools `-dev` builds — mostly
>   because newer PanamaPort keys `ArtVersion` on `SDK_INT_FULL`, whose field-get
>   backport only landed in R8 after the 8.6.x line that build-tools 35 ships.
>
> Two questions, if you have a moment:
> 1. `R8Annotations`' rules include `-alwaysinline`/`-checkdiscard`. [Report what the
>    pinned R8 9.1.31 did with them — accepted/warned/rejected + what we did about it.]
>    Is running without them acceptable, or do any PanamaPort code paths rely on them
>    for correctness rather than size?
> 2. Is `-dontobfuscate` + full shrinking/optimization a configuration you test, or
>    should we expect issues vs the fully-minified setup AGP defaults to?
>
> The incorrect statements in `docs/architecture/android-native-constraints.md` are
> corrected in the same change. Thanks again for taking the time to file this.

---

## 8. Issue decomposition (one implementer session each)

multiarch-scala (GitHub issues):

| ID | Title | Plan refs | Depends |
|---|---|---|---|
| R8-1 | AndroidDeps: extract + expose consumer proguard.txt (+ unit test) | §4 step 1 | — |
| R8-2 | AndroidBuild: androidUseR8/androidR8Version/androidR8Jar/androidR8RuleFiles + pinned-R8 resolver + R8 invocation + baseline .pro resources + honor androidR8Rules | §1, §2, §4 steps 2+4 | R8-1 |
| R8-3 | Release 0.5.0 (COORD-FIX 2026-07-03; was 0.3.1 — 0.4.0 landed the manifest-v2 binary break on master, a 0.3.1 patch cannot be cut from master and no 0.3.x maintenance branch is planned, R8 keys/pipeline are additive on top of 0.4.0 → next minor 0.5.0 per early-semver) | §4 step 4 | R8-2 |

sge (`re-scale db issues add`, self-contained bodies quoting the plan sections):

| ID | Title | Plan refs | Depends |
|---|---|---|---|
| R8-4 | Bump sbt-multiarch-scala to 0.5.0 (COORD-FIX 2026-07-03; was 0.3.1) + full verification sweep + scripted grep for consumer-rules lines | §4 steps 3+5, gates 2-4 | R8-3 |
| R8-5 | CI: API-26 emulator matrix leg (+ API-aware excusal map in AndroidSmokeTest if F5 hits) | §4 step 6 | R8-4 |
| R8-6 | Docs corrections (3 files) + arch-android skill | §6 | R8-5 green |
| R8-7 | Reply on issue #5 | §7 | R8-5 outcome known |

Failure-mode issues (F1-F6) are filed as they occur, referencing the branch letter.

---

## 9. Rollback

`androidUseR8 := false` restores byte-for-byte the current D8 pipeline (Step 2 keeps that
code path verbatim). sge-side rollback = revert the pin bump. The CI API-26 leg is
removable independently. No data migration, no covenant impact.
