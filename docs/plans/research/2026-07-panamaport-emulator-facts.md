# PanamaPort / emulator / VarHandle verified facts (research addendum, 2026-07-01)

Evidence grade: VERIFIED unless marked otherwise — agent read upstream sources (GitHub
raw files + commit history), Google's live SDK repository XML, Android reference pages,
and R8 sources; URLs cited per claim. Feed for `docs/plans/2026-07-android-r8.md`.

## PanamaPort (github.com/vova7878/PanamaPort)

- README states: FFM implementation "for Android 8.0+ (API level 26)"; root
  `build.gradle.kts` sets `minSdk = 26` for every module; requires AGP 8.6.0+,
  compileSdk 35+. Latest tag v0.1.3 (Maven Central has v0.1.0–v0.1.3; `-release`
  classifier present at v0.1.3).
- **R8 is NOT stated as required anywhere in any README revision** (checked full history;
  "R8"/"D8"/"proguard" never appear). The only statement is the author's own
  kubuszok/sge#5. His example app (`PanamaExamples/HelloFromPanama`) builds with
  `minifyEnabled false` + `minSdk 26` — i.e. plain AGP **D8** (desugar/backport at
  min-api, no shrinking) is an expected-working configuration. Rule merging becomes
  load-bearing the moment shrinking is enabled.
- No documented consumer keep rules beyond the AAR-shipped ones. The keep logic lives in
  the transitive `io.github.vova7878:R8Annotations` AAR `proguard.txt` (annotation-driven
  conditional rules). Usage counts in PanamaPort source: `@AlwaysInline` 465,
  `@DoNotShrink` 305, `@DoNotObfuscate` 232, `@DoNotShrinkType` 55, `@NoSideEffects` 49,
  `@DoNotOptimize` 40, `@CheckDiscard` 4, `@KeepCode` 2.
- **ArtVersion evolution** (`Unsafe/.../ArtVersion.java` commit history):
  - ≤ v0.0.8-preview: keyed off normalized `SDK_INT` (`Version.CORRECT_SDK_INT`).
  - v0.0.9-preview (commit 525d8ea, 2025-08-26): first `SDK_INT_FULL` use (top branch
    only; `is36p1()` probed `Files.readString`).
  - current main / v0.1.3: `computeIndex()` starts from `int tmp = SDK_INT_FULL;`
    (PRIMARY key, thresholds in the ×100000 scale) + reflection probes for
    mainline-module drift (`is33` String.isBlank … `is36` DirectMethodHandle by name,
    `is36p1` VirtualThread, `is37` java.lang.foreign.MemoryLayout). Android 17 support
    added.
  - The sge-PINNED unclassified `v0.1.0` artifacts contain **no** `SDK_INT_FULL`
    reference at all (verified locally: `grep` over all extracted classes.jar from the
    cached AARs finds nothing; `javap` shows the probe methods) — an intermediate scheme.
- **The R8 mechanism behind "minapi can be 26"**: R8 commit `0c417b5` (2024-11-26) adds
  *backporting of the `Build.VERSION.SDK_INT_FULL` static field get* (rewritten to
  `SDK_INT * 100_000` on pre-Baklava). D8/R8 min-api backporting is what makes
  SDK_INT_FULL-reading PanamaPort versions loadable on API 26–35. Consequence: upgrading
  PanamaPort past v0.1.0 REQUIRES a DEX step whose R8/D8 includes that commit — the
  build-tools 35.0.0 `8.6.2-dev` build predates it; the pinned standalone R8 9.1.31 has
  it. https://r8.googlesource.com/r8/+/0c417b515e54a0990784e595d173540b30993c8b
- Runtime DEX generation: FFM downcall/upcall machinery generates classes at runtime
  (`BulkLinker`, `Transformers`, `EmulatedStackFrame`, `AccessLinker`, …) via the
  `DexFile` lib, plus hand-written `.raung` bytecode; hidden-API access via raw
  ART-struct flag manipulation (`ArtModifiers.kAccPublicApi`), not
  `setHiddenApiExemptions`. This is what the R8Annotations keeps protect.
- UNVERIFIED (documented nowhere): the exact minimal keep-rule set a non-AGP build must
  reproduce; whether any consumer keeps beyond R8Annotations are needed.

## API-26 emulator CI leg

- `system-images;android-26;google_apis;x86_64` EXISTS (verified against
  `dl.google.com/android/repository/sys-img/google_apis/sys-img2-4.xml`). Also
  `default;{x86,x86_64,arm64-v8a}` and `google_apis_playstore;x86` at 26. No
  `armeabi-v7a`.
- Precedent for the exact combo: dart-lang/native `jnigen.yaml` runs
  `api-level: 26, arch: x86_64` on ubuntu-latest + the same KVM udev step sge already
  uses.
- Quirks: reactivecircus/android-emulator-runner issue #373 — occasional post-test
  emulator shutdown hangs on API 26/28 ("stop: Not implemented" → action timeout);
  mitigations: env `ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL` (default 20s),
  `emulator-boot-timeout` (default 600s), keep `-no-snapshot`. Historic generic boot
  flakiness (#160) mostly on macOS runners.
- Use `x86_64` on KVM runners; never `arm64-v8a` (no accel).

## VarHandle / MethodHandle floors

- `java.lang.invoke.MethodHandle` / `MethodHandles.Lookup`: API 26.
  `Lookup.findVarHandle`: API 33. Platform `java.lang.invoke.VarHandle`: **API 33**.
- D8/R8: VarHandle desugaring is PARTIAL and **off by default**
  (`InternalOptions.enableVarHandleDesugaring = false`; `varHandleApiLevel() == T(33)`).
  `MethodHandle.invoke/invokeExact` require `--min-api >= 26` (D8 errors below).
- PanamaPort therefore ships its OWN `com.v7878.invoke.VarHandle` (module `VarHandles`,
  "backport of java.lang.invoke.VarHandle which didn't exist in android 8.x") — do not
  expect R8 to desugar platform VarHandle for it; nothing extra needed at min-api 26.
