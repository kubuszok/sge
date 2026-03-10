# Release Readiness Review

Last updated: 2026-03-09

This document is a rolling dump of findings from a fresh repository review focused on:

- missing tests for specific functionality
- missing end-to-end or cross-module flows
- incorrect assumptions in the Scala port
- logic mismatches introduced during porting from Java
- error-prone Scala usage
- stub-only or placeholder functionality
- blockers for publishing usable releases

It is intentionally updated incrementally as issues are uncovered so findings are not
lost to context pressure. Items may be refined later, but they should be recorded here
as soon as they are identified.

## Scope and method

- Reviewed the active Scala port modules: `sge`, `sge-freetype`, `sge-physics`,
  `sge-tools`, `sge-jvm-platform-api`, `sge-jvm-platform-jdk`,
  `sge-jvm-platform-android`, and `sge-it-tests`.
- Treated the vendored `libgdx/` tree as the Java reference baseline, not as part of
  the Scala product itself.
- Used existing audit material in `docs/audit` and `docs/progress` as prior evidence,
  but not as proof that release readiness has been achieved.

## Early observations

### 1. Existing audit coverage is broad, but release-readiness evidence is still incomplete

The repository already contains a substantial per-file audit corpus in `docs/audit`.
That work is valuable, but it is mostly API parity and source-diff oriented. It does
not by itself prove that the port works end to end on desktop, Android, JS, or native.

Release impact:
- high

Why this matters:
- a source-level audit can miss integration breakage, lifecycle ordering bugs,
  packaging failures, and runtime/backend mismatches
- several docs under `docs/progress` still discuss open integration gaps and quality
  issues, which means the existence of audit docs should not be treated as a sign-off

### 2. Test coverage is concentrated in `sge`; extension modules and publishable flows are comparatively under-tested

Initial structure review shows many unit tests in `sge`, a moderate set of platform and
integration tests in `sge-it-tests`, and much thinner visible test coverage in
`sge-freetype`, `sge-physics`, and `sge-tools`.

Release impact:
- high

Why this matters:
- core library behavior may be locally validated while extension modules remain
  effectively unverified
- packaging and publication targets can fail even when unit tests in the main module
  are green

## Findings

This section is appended throughout the review.

### 2026-03-09 batch 1

#### RR-001. `Gdx2DPixmap` is still stub-only, so Pixmap-backed rendering remains blocked

Files:
- `sge/src/main/scala/sge/graphics/g2d/Gdx2DPixmap.scala`
- `sge/src/main/scala/sge/graphics/Pixmap.scala`
- `docs/progress/integration-test-gaps.md`

Severity:
- blocker

What is wrong:
- `Pixmap` now delegates drawing and allocation operations to `Gdx2DPixmap`, but the
  native methods in `Gdx2DPixmap` are still stub implementations:
  `load`, `loadByteBuffer`, `newPixmap`, `free`, `clear`, `setPixel`, `getPixel`,
  `drawLine`, `drawRect`, `drawCircle`, `fillRect`, `fillCircle`, `fillTriangle`,
  `drawPixmap`, `setBlend`, `setScale`.
- In practice this means the higher-level `Pixmap` API exists, but the backing
  implementation cannot actually allocate or mutate pixel data.

Why this matters:
- breaks pixmap creation from dimensions and encoded data
- blocks texture upload paths that depend on pixmaps
- blocks readback/verification flows for rendering
- makes several asset loader and graphics code paths impossible to validate end to end

Test gap:
- there are no direct tests exercising real pixmap allocation, pixel read/write,
  draw primitives, or pixmap-backed texture upload
- `docs/progress/integration-test-gaps.md` already lists the missing desktop/browser
  rendering checks that depend on this

Release impact:
- desktop/browser/native rendering confidence is incomplete
- any release claiming working pixmap-based rendering would be misleading

#### RR-002. Physics extension links against the wrong native library name

Files:
- `sge-physics/src/main/scalajvm/sge/platform/PhysicsOpsPanama.scala`
- `sge-physics/src/main/scalanative/sge/platform/PhysicsOpsNative.scala`
- `native-components/Cargo.toml`
- `sge-build/src/main/scala/sge/sbt/SgeNativeLibs.scala`

Severity:
- blocker

What is wrong:
- JVM physics code looks up `System.mapLibraryName("sge_physics")`.
- Scala Native physics code uses `@link("sge_physics")`.
- The Rust crate only builds `sge_native_ops` (`[lib] name = "sge_native_ops"`).
- Build tooling and packaging helpers also only know about `sge_native_ops`.

Why this matters:
- the physics extension cannot load on JVM or Scala Native unless a separate
  `sge_physics` binary is created, which the repository does not currently build
- this is not a theoretical issue; the current build and packaging configuration
  point all consumers at `sge_native_ops`
- for Scala Native the contradiction is direct: source code says `@link("sge_physics")`
  while the shared native-linker helper emits `-lsge_native_ops`

Evidence:
- current `native-components/target/release` contains `libsge_native_ops.*` and no
  `libsge_physics.*`

Release impact:
- `sge-physics` is not actually runnable in its current packaged form

#### RR-003. FreeType and physics symbols are feature-gated out of the default native build

Files:
- `native-components/Cargo.toml`
- `native-components/src/lib.rs`
- `.github/workflows/ci.yml`
- `Justfile`
- `sge-freetype/src/main/scalajvm/sge/platform/FreetypeOpsPanama.scala`

Severity:
- blocker

What is wrong:
- Rust features `freetype_support` and `physics` are optional and not enabled by
  default.
- The default local build recipe `rust-build` and the CI `cargo build --release`
  steps do not enable those features.
- `sge-freetype` and `sge-physics` assume the corresponding native symbols exist.

Why this matters:
- the default built `libsge_native_ops` does not export FreeType or physics symbols
- extension modules can compile while being guaranteed to fail at runtime when they
  first resolve native entry points

Evidence:
- symbol inspection of the current `libsge_native_ops.dylib` shows ETC1 and buffer
  ops exports, but no `sge_ft_*` and no `sge_phys_*` exports

Release impact:
- default build artifacts are incomplete for published extension modules
- CI currently cannot catch breakage in those extensions because it builds the wrong
  native feature set and does not run extension tests

#### RR-004. Physics ray-cast result encoding is incorrect and loses handle information

Files:
- `native-components/src/physics.rs`
- `sge-physics/src/main/scala/sge/physics/PhysicsWorld.scala`
- `sge-physics/src/main/scalajvm/sge/platform/PhysicsOpsPanama.scala`
- `sge-physics/src/main/scalanative/sge/platform/PhysicsOpsNative.scala`

Severity:
- major

What is wrong:
- Rust writes the hit body handle into a float slot using
  `f32::from_bits(body_handle_to_u64(parent) as u32)`.
- This truncates the 64-bit Rapier handle to 32 bits.
- Scala reconstructs the handle with `Float.floatToRawIntBits(rayBuf(5)).toLong`,
  which can never recover the lost upper 32 bits.

Why this matters:
- ray-cast results can identify the wrong body once generations or larger indices
  are involved
- the API surface advertises `Long` handles, but the transport format violates that
  contract

Additional correctness problem:
- the same Rust function sets the reported normal to `(0, 0)` with a comment that
  the full intersection path is not implemented
- `RayCastHit.normalX` / `normalY` therefore do not carry meaningful data

Test gap:
- `sge-physics` has zero test files
- there is no unit or integration test covering ray-cast identity preservation or
  normal correctness

Release impact:
- physics query results are not trustworthy even before broader gameplay logic is built

#### RR-005. Android Panama provider is a compile-time stub, so JVM-side FFI helpers are not Android-ready

Files:
- `sge-jvm-platform-android/src/main/scala/sge/platform/PanamaPortProvider.scala`
- `sge/src/main/scalajvm/sge/platform/Panama.scala`
- `sge/src/main/scalajvm/sge/platform/PlatformOps.scala`
- `sge/src/main/scala/sge/utils/BufferUtils.scala`
- `sge/src/main/scala/sge/graphics/glutils/ETC1.scala`

Severity:
- blocker for Android feature completeness

What is wrong:
- `PanamaPortProvider` is explicitly marked as a compile-time stub and all of its
  methods throw `UnsupportedOperationException`.
- `Panama.detect()` selects this provider whenever `java.lang.foreign.MemorySegment`
  is unavailable, which is exactly the intended Android path.
- `PlatformOps` wires `BufferOpsPanama` and `ETC1OpsPanama` through the detected provider.

Why this matters:
- Android may bootstrap and still fail later when buffer operations or ETC1 paths are used
- this is a hidden runtime landmine because the abstraction layer exists and compiles,
  but the Android implementation is not functional

Test gap:
- current Android smoke coverage is too shallow to prove these FFI-backed paths work

Release impact:
- Android support should not be considered complete

#### RR-006. Desktop and Android text-input APIs contain shipped no-op or cancel-only behavior

Files:
- `sge/src/main/scaladesktop/sge/DefaultDesktopInput.scala`
- `sge/src/main/scalajvm/sge/AndroidInput.scala`

Severity:
- major usability gap

What is wrong:
- Desktop `getTextInput(...)` immediately calls `listener.canceled()` with a `FIXME`
  comment saying it does nothing.
- Android `openTextInputField(...)` is an empty method with a TODO comment.

Why this matters:
- these APIs are part of the public input surface, but the current behavior is
  incomplete rather than intentionally unsupported at the type level
- application code cannot reliably use native text-entry flows across backends

Test gap:
- no backend tests were found for desktop text input dialog behavior
- no Android tests were found for `openTextInputField`

Release impact:
- input API parity is incomplete

#### RR-007. Extension modules have effectively no direct test suite

Files:
- `sge-freetype/`
- `sge-physics/`
- `sge-tools/`
- `sge-jvm-platform-api/`
- `sge-jvm-platform-jdk/`
- `sge-jvm-platform-android/`

Severity:
- major process risk

What is wrong:
- direct test-file count from repository layout:
  - `sge-freetype`: `prod=11`, `test=0`
  - `sge-physics`: `prod=15`, `test=0`
  - `sge-tools`: `prod=8`, `test=0`
  - `sge-jvm-platform-api`: `prod=32`, `test=0`
  - `sge-jvm-platform-jdk`: `prod=1`, `test=0`
  - `sge-jvm-platform-android`: `prod=26`, `test=0`
- some behavior is indirectly exercised from `sge-it-tests`, but these modules do
  not have their own unit-level safety net

Why this matters:
- the least mature modules are also the least tested
- FFI boundaries, loaders, Android shims, and extension behavior are exactly where
  regression risk is highest

Release impact:
- high regression probability for every release touching extensions or platform code

#### RR-008. CI validates only the core `sge` test task and misses extension-module regressions

Files:
- `.github/workflows/ci.yml`

Severity:
- major process risk

What is wrong:
- the CI workflow runs `sgeJS/test` and `sge/test`
- it does not run `sge-freetype/test`, `sge-physics/test`, `sge-tools/test`, or
  extension-specific compile/test tasks
- it also does not run the desktop, browser, Android, or JVM-platform integration
  test modules under `sge-it-tests`
- the native build jobs also use default cargo features, which excludes FreeType
  and physics from the produced native library

Why this matters:
- even if extension tests are added later, the current workflow would still miss
  them unless explicitly updated
- current green CI would not mean extension modules are functional

Release impact:
- CI cannot be used as a release gate for the full published surface

#### RR-009. `sge-freetype` and `sge-physics` are cross-built for Scala.js even though their JS implementations are runtime stubs

Files:
- `build.sbt`
- `sge-freetype/src/main/scalajs/sge/platform/FreetypeOpsJs.scala`
- `sge-freetype/src/main/scalajs/sge/platform/FreetypePlatform.scala`
- `sge-physics/src/main/scalajs/sge/platform/PhysicsOpsJs.scala`
- `sge-physics/src/main/scalajs/sge/platform/PhysicsPlatform.scala`

Severity:
- major product-definition risk

What is wrong:
- both extension modules define `jsPlatform(...)` in the build
- the Scala.js implementation of FreeType throws `UnsupportedOperationException`
  for every operation
- the Scala.js implementation of physics also throws
  `UnsupportedOperationException` for every operation

Why this matters:
- users can receive apparently supported JS artifacts that fail only at runtime
- this is not just a missing feature in one corner; it is the entire backend surface
  for those modules

Open question:
- if this is intentional, the unsupported platforms should probably not be published
  as normal cross-platform variants, or the unsupported status must be made explicit
  in artifact naming and documentation

Release impact:
- high risk of shipping misleading platform support claims

#### RR-010. The default 3D particle-effect loader is still structurally stubbed and appears unusable

Files:
- `sge/src/main/scala/sge/assets/AssetManager.scala`
- `sge/src/main/scala/sge/graphics/g3d/particles/ParticleEffectLoader.scala`
- `sge/src/main/scala/sge/graphics/g3d/particles/ResourceData.scala`
- `sge/src/main/scala/sge/graphics/g3d/particles/batches/ModelInstanceParticleBatch.scala`

Severity:
- blocker for that feature area

What is wrong:
- `AssetManager` registers `sge.graphics.g3d.particles.ParticleEffectLoader` as a
  default loader.
- `ParticleEffectLoader.getDependencies(...)` is explicitly stubbed and always
  returns an empty descriptor list.
- `ParticleEffectLoader.save(...)` collects data but never serializes it because the
  JSON bridge is missing.
- `loadSync(...)` expects a cached `ResourceData` entry in `items`, but the current
  `getDependencies(...)` implementation never populates that cache, so `loadSync(...)`
  falls into `RuntimeException("No ResourceData found ...")`.

Why this matters:
- this is not just an incomplete optional export path; the default registered loader
  itself does not appear able to execute the normal dependency/load flow
- consumers will only discover the failure at runtime

Related gaps:
- `ResourceData` explicitly omits the original JSON serialization behavior
- `ModelInstanceParticleBatch.save/load` are empty, which further weakens the
  particle-effect persistence path even after JSON support is added

Test gap:
- no end-to-end loader tests were found for 3D particle effects

Release impact:
- the 3D particle asset pipeline should be considered non-release-ready

#### RR-011. `TextureData.Factory.loadFromFile` uses a dummy `100x100` pixmap for ordinary image files

Files:
- `sge/src/main/scala/sge/graphics/TextureData.scala`
- `sge/src/main/scala/sge/graphics/Texture.scala`
- `sge/src/main/scala/sge/assets/loaders/TextureLoader.scala`
- `sge/src/main/scala/sge/graphics/glutils/FacedCubemapData.scala`
- `sge/src/main/scala/sge/graphics/glutils/FileTextureArrayData.scala`

Severity:
- blocker

What is wrong:
- `TextureData.Factory.loadFromFile(...)` handles `.cim`, `.etc1`, `.ktx`, and
  `.zktx` specially.
- For every other file type it currently constructs
  `FileTextureData(file, Pixmap(100, 100, ...), ...)` instead of leaving the pixmap
  empty for deferred file decoding or decoding the file immediately.
- Because `FileTextureData` sees a preloaded pixmap, its `prepare()` path will use
  that dummy pixmap instead of reading the actual image file.

Why this matters:
- regular texture files such as PNG/JPG/BMP are likely loaded as blank `100x100`
  images instead of the real asset contents
- the same factory is used by texture construction, `TextureLoader`, texture arrays,
  and cubemap face loading
- because `Pixmap(width, height, format)` itself currently depends on stubbed
  `Gdx2DPixmap` allocation, this fallback path may also fail immediately instead of
  even producing the dummy image

Test gap:
- no tests were found that exercise `TextureData.Factory.loadFromFile(...)` against
  a real ordinary image file and verify resulting dimensions/pixels

Release impact:
- core texture loading cannot be trusted in its current form

#### RR-012. `TextFormatter` is still a simplified placeholder, so `I18NBundle.format` semantics diverge from LibGDX

Files:
- `sge/src/main/scala/sge/utils/TextFormatter.scala`
- `sge/src/main/scala/sge/utils/I18nBundle.scala`

Severity:
- major

What is wrong:
- `TextFormatter.format(...)` currently performs repeated `String.replace("{n}", value)`
  substitution.
- The source comment explicitly says it is a placeholder and does not implement the
  original `MessageFormat`-based behavior, brace escaping, or the error checking
  from LibGDX's simple formatter path.

Why this matters:
- localized message formatting can behave differently from LibGDX for escaped braces,
  repeated placeholders, locale-sensitive formatting, and malformed patterns
- the discrepancy surfaces through `I18NBundle.format(...)`, which is part of the
  public API

Test gap:
- no tests were found covering formatter compatibility against real I18N patterns

Release impact:
- i18n output parity is not reliable

#### RR-013. `SgeNativesLoader.load()` is a compatibility shell that marks natives as loaded without actually loading anything

Files:
- `sge/src/main/scala/sge/utils/SgeNativesLoader.scala`

Severity:
- moderate API-trust issue

What is wrong:
- `load()` only flips `nativesLoaded = true`
- the original LibGDX class exists specifically to ensure native libraries are loaded
- the Scala port keeps the API surface but not the behavior

Why this matters:
- callers familiar with the LibGDX contract can reasonably assume `load()` performs
  real work, when in fact it is a no-op
- that increases the chance of confusing runtime failures if external code tries to
  rely on this compatibility layer

Context:
- the repository appears to prefer `java.library.path` plus direct `libraryLookup(...)`
  instead of a `SharedLibraryLoader` strategy
- if that is the permanent direction, this compatibility object should likely be
  explicitly deprecated or documented as inert

Test gap:
- no tests were found covering native-library loading behavior through this API

#### RR-014. `TimSort` still contains placeholder gallop implementations

Files:
- `sge/src/main/scala/sge/utils/TimSort.scala`
- `sge/src/main/scala/sge/utils/Sort.scala`
- `sge/src/test/scala/sge/utils/SortTest.scala`

Severity:
- moderate quality/performance risk

What is wrong:
- `TimSort.gallopLeft` and `TimSort.gallopRight` are marked as placeholder
  implementations and currently do linear scans
- LibGDX/Java TimSort relies on galloping to preserve its intended adaptive
  performance characteristics during merges

Why this matters:
- the current implementation may still sort correctly, but it is not a faithful
  performance-equivalent port
- this can become visible on large arrays or hot sorting paths where TimSort was
  chosen specifically for its adaptive behavior

Test gap:
- existing `SortTest` checks correctness on small inputs
- no tests or benchmarks were found that would catch this placeholder behavior on
  larger or adversarial inputs

#### RR-015. Binary `.g3db` model loading is missing because `UBJsonReader` has not been ported

Files:
- `sge/src/main/scala/sge/assets/AssetManager.scala`
- `sge/src/main/scala/sge/graphics/g3d/loader/G3dModelLoader.scala`
- `libgdx/gdx/src/com/badlogic/gdx/assets/AssetManager.java`

Severity:
- major missing feature

What is wrong:
- LibGDX registers both `.g3dj` and `.g3db` model loaders by default.
- This port explicitly skips `.g3db` with the comment `UBJsonReader not ported`.
- `G3dModelLoader` only supports `.g3dj` text JSON input.

Why this matters:
- `.g3db` is a real upstream asset format used in LibGDX sample/test content
- applications ported from LibGDX that rely on binary G3D assets will not load
  without asset conversion

Test gap:
- no tests were found for `.g3db` compatibility or for a migration path from
  `.g3db` to `.g3dj`

Release impact:
- 3D model format support is incomplete relative to LibGDX expectations

#### RR-016. Multiple important end-to-end loader flows are still explicitly untested

Files:
- `sge/src/main/scala/sge/assets/loaders/BitmapFontLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/CubemapLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/FileHandleResolver.scala`
- `sge/src/main/scala/sge/assets/loaders/I18NBundleLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/ModelLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/MusicLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/ParticleEffectLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/PixmapLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/ShaderProgramLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/SkinLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/SoundLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/TextureAtlasLoader.scala`
- `sge/src/main/scala/sge/assets/loaders/TextureLoader.scala`

Severity:
- major test gap

What is missing:
- real loader flows for fonts, textures, pixmaps, cubemaps, sounds, music, shader
  programs, particle effects, models, skins, i18n bundles, and resolution-based
  file handling are all still called out in source comments as needing tests

Why this matters:
- these are not tiny helper methods; they are the integration points users rely on
- loader code is where many porting mismatches only appear with real assets and
  real backend state

Release impact:
- asset pipeline regressions are likely to survive until runtime

#### RR-017. Several canonical real-asset decoding flows are still missing end-to-end tests

Files:
- `sge/src/test/scala/sge/graphics/g3d/loader/G3dModelLoaderTest.scala`
- `sge/src/test/scala/sge/maps/tiled/BaseTiledMapLoaderTest.scala`
- `sge/src/test/scala/sge/scenes/scene2d/ui/SkinStyleReaderTest.scala`
- `sge/src/main/scala/sge/graphics/g3d/loader/ObjLoader.scala`
- `sge/src/main/scala/sge/maps/tiled/TideMapLoader.scala`

Severity:
- major test gap

What is missing:
- decoding a real `.g3dj` model through `G3dModelLoader`
- decoding a real `.obj/.mtl` pair through `ObjLoader`
- decoding real `.tmx`, `.tmj`, and `.tiled-project` files through the tiled loaders
- decoding a real `.skin` file through `Skin.load`
- decoding a real `.tide` map through `TideMapLoader`

Why this matters:
- these tests are the fastest way to catch subtle mismatches between the Scala port
  and the original LibGDX asset expectations
- they also protect against future parser regressions that pure unit tests miss

Release impact:
- core content-import flows remain under-validated
