# Batch T3 -- All Remaining Extension Tests Audit

Audit date: 2026-04-18

## Anim8

Original test directory: `original-src/anim8-gdx/src/test/java/com/github/tommyettinger/`
SGE test directory: `sge-extension/anim8/src/test/scala/sge/anim8/`

### Original Test Analysis

All 20 files in `original-src/anim8-gdx/src/test/` are **demo/benchmark applications** (extend `ApplicationAdapter` with `main` methods) or helper classes. **Zero files contain `@Test` annotations.** Files include:

- `Config.java` -- config helper
- `DifferenceCheck.java`, `FastShaderCaptureDemo.java`, `FastStillImageDemo.java`, `FastVideoConvertDemo.java` -- demos
- `InteractiveFastReducer.java`, `InteractiveQualityReducer.java`, `InteractiveReducer.java` -- interactive demos
- `NQGif.java`, `NeuQuant.java` -- algorithm reference implementations
- `PaletteConvert.java`, `PaletteTwister.java` -- palette tools
- `QualityShaderCaptureDemo.java`, `QualityVideoConvertDemo.java` -- demos
- `ShaderCaptureDemo.java`, `StillImageDemo.java`, `VideoConvertDemo.java` -- demos
- `bench/APNGStartupBench.java`, `bench/GifStartupBench.java`, `bench/PNG8StartupBench.java` -- benchmarks

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `ChunkBufferSuite.scala` | PNG chunk buffer write/endChunk | SGE-original test |
| 2 | `DitherAlgorithmSuite.scala` | DitherAlgorithm enum completeness, legibleName | SGE-original test |
| 3 | `OtherMathSuite.scala` | barronSpline, atan2 approximation | SGE-original test |

**Summary**: Original has 0 unit tests, 20 demos/benchmarks. SGE has 3 independent test suites covering core utilities. **No porting gap** -- SGE exceeds original.

---

## Colorful

Original test directory: `original-src/colorful-gdx/colorful/src/test/java/com/github/tommyettinger/colorful/`
SGE test directory: `sge-extension/colorful/src/test/scala/sge/colorful/`

### Original Unit Tests (files with `@Test` annotations)

Only 3 files out of ~100+ test files contain `@Test` annotations. The rest are all demos, palette generators, or helper classes.

### HSLCorrectnessTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testRgb2hslRogue` | HSL conversion accuracy (rogue algorithm) | NOT_APPLICABLE | -- | Tests a local rogue algorithm variant, not part of the library API |
| 2 | `testRgb2hsl` | FloatColors.rgb2hsl accuracy vs Wikipedia reference | PORTED | `FloatColorsSuite` | SGE tests rgb2hsl roundtrip and known-value conversions |
| 3 | `testRgb2hslInt` | FloatColors.rgb2hslInt accuracy | PARTIALLY_PORTED | `FloatColorsSuite` | SGE tests int-based conversions indirectly |
| 4 | `testHsl2rgb` | FloatColors.hsl2rgb accuracy | PORTED | `FloatColorsSuite` | SGE tests hsl2rgb roundtrip |
| 5 | `testHsl2rgbInt` | FloatColors.hsl2rgbInt accuracy | PARTIALLY_PORTED | `FloatColorsSuite` | Covered indirectly |
| 6 | `testRgb2hclInt` | FloatColors.rgb2hclInt accuracy | NOT_PORTED | -- | HCL int conversion not explicitly tested |
| 7 | `testHcl2rgbInt` | FloatColors.hcl2rgbInt accuracy | NOT_PORTED | -- | HCL int reverse conversion not tested |

### oklab/BasicCheckTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testLimitToGamut` | limitToGamut 4-param vs 1-param consistency | PORTED | `OklabColorToolsSuite` | SGE tests limitToGamut |
| 2 | `testRandomColors` | inGamut consistency between overloads | PARTIALLY_PORTED | `OklabColorToolsSuite` | SGE tests inGamut but not 1M random samples |
| 3 | `testPalette` | FullPalette colors all in gamut | NOT_PORTED | -- | Palette gamut validation not tested |
| 4 | `testBlues` | chromaLimit for blue hue range | NOT_PORTED | -- | Specific chroma limit sweep not tested |

### cielab/BasicChecks.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testLimitToGamut` | CIELAB limitToGamut consistency | NOT_APPLICABLE | -- | `@Ignore`d in original |
| 2 | `testInGamut` | CIELAB palette inGamut check | NOT_APPLICABLE | -- | `@Ignore`d in original |
| 3 | `testChroma` | Max chroma brute force search | NOT_PORTED | -- | CIELAB chroma not specifically in SGE scope (SGE ports oklab + rgb) |

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `FloatColorsSuite.scala` | rgb2hsl, hsl2rgb roundtrip, known values | SGE-original |
| 2 | `TrigToolsSuite.scala` | sin_, cos_, atan2_ approximations | SGE-original |
| 3 | `OklabColorToolsSuite.scala` | oklab pack/unpack, limitToGamut, inGamut | SGE-original |
| 4 | `RgbColorToolsSuite.scala` | RGB ColorTools pack/unpack, channel extraction | SGE-original |

**Summary**: Original has 3 unit test files (12 `@Test` methods, 2 `@Ignore`d). 4 PORTED/PARTIALLY, 4 NOT_PORTED, 2 NOT_APPLICABLE. SGE has 4 independent test suites that cover the ported color spaces well.

---

## VisUI

Original test directory: `original-src/vis-ui/ui/src/test/java/com/kotcrab/vis/ui/test/`
SGE test directory: `sge-extension/visui/src/test/scala/sge/visui/`

### GreaterThanValidatorTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testValidateInput` | GreaterThanValidator with/without equals, edge cases | PORTED | `ValidatorsSuite` | SGE tests equivalent scenarios |

### LesserThanValidatorTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testValidateInput` | LesserThanValidator with/without equals, edge cases | PORTED | `ValidatorsSuite` | SGE tests equivalent scenarios |

### TestImageTextButtonOrientation.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| -- | (no @Test) | Manual UI demo for orientation | NOT_APPLICABLE | -- | Visual demo, not unit test |

### USL TemplateBasedParserTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testParserUsingVisUITemplate` | USL parse visui template | NOT_APPLICABLE | -- | USL parser not ported to SGE |
| 2 | `testParserUsingGdxTemplate` | USL parse gdx template | NOT_APPLICABLE | -- | USL parser not ported |
| 3 | `testParserUsingTintedTemplate` | USL parse tinted template | NOT_APPLICABLE | -- | USL parser not ported |
| 4 | `testParserAlias` | USL alias parsing | NOT_APPLICABLE | -- | USL parser not ported |
| 5 | `testParserComments` | USL comments parsing | NOT_APPLICABLE | -- | USL parser not ported |
| 6 | `testMinus` | USL minus parsing | NOT_APPLICABLE | -- | USL parser not ported |

### USL RemoteTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testRemote` | USL remote include | NOT_APPLICABLE | -- | `@Ignore`d, USL not ported |

### Manual test files (17 files)

All files in `manual/` directory are visual demo applications (extend `VisWindow` or `ApplicationAdapter`). **None contain `@Test` annotations.** NOT_APPLICABLE.

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `ValidatorsSuite.scala` | Integer/Float/GreaterThan/LesserThan validators | Covers original test methods |
| 2 | `SizesSuite.scala` | Sizes defaults, copy constructor | SGE-original |
| 3 | `ColorUtilsSuite.scala` | HSVtoRGB, RGBtoHSV conversion | SGE-original |

**Summary**: Original has 2 unit test files with `@Test` (2 methods), plus 6 USL parser tests (NOT_APPLICABLE -- USL not ported). Both validator test methods are PORTED. SGE has 3 test suites exceeding original coverage.

---

## JBump

Original test directory: `original-src/jbump/test/src/com/dongbat/jbump/test/`
SGE test directory: `sge-extension/jbump/src/test/scala/sge/jbump/`

### TestBump.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| -- | (no @Test) | Interactive visual demo app | NOT_APPLICABLE | -- | Extends `ApplicationAdapter`, manual test only |

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `WorldSuite.scala` (14 tests) | add/remove/getRect, move with slide/cross/touch/bounce, queryRect/queryPoint, update, reset, multiple items | Comprehensive coverage |

**Summary**: Original has 0 unit tests (1 interactive demo). SGE has 14 independent unit tests in `WorldSuite`. **No porting gap** -- SGE far exceeds original.

---

## Noise4J

Original test directory: No test directory found
SGE test directory: `sge-extension/noise/src/test/scala/sge/noise/`

**No original tests found.** The `original-src/noise4j/` contains only source files under `src/com/`.

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `CellularAutomataSuite.scala` | CellularAutomataGenerator | SGE-original |
| 2 | `DungeonSuite.scala` | DungeonGenerator | SGE-original |
| 3 | `NoiseSuite.scala` | NoiseGenerator | SGE-original |

**Summary**: Original has 0 tests. SGE has 3 independent test suites. **No porting gap.**

---

## Controllers

Original test directory: `original-src/gdx-controllers/test/`
SGE test directory: `sge-extension/controllers/src/test/scala/sge/controllers/`

### Original Test Analysis

All 7 files under `original-src/gdx-controllers/test/` are **platform launcher applications** (Android, Desktop, iOS, HTML, LWJGL3) for the interactive `ControllersTest` demo app. **Zero files contain `@Test` annotations.**

- `ControllersTest.java` -- Interactive visual controller testing app (`ApplicationAdapter`)
- `AndroidLauncher.java`, `DesktopLauncher.java`, `Lwjgl3Launcher.java`, `GwtLauncher.java`, `IOSLauncher.java`, `MyUIViewController.java` -- Platform launchers

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `ControllerMappingSuite.scala` | StandardMapping axis/button indices | SGE-original |
| 2 | `ControllersSuite.scala` | Controller lifecycle, listeners, connection/disconnection | SGE-original |

**Summary**: Original has 0 unit tests (7 demo/launcher files). SGE has 2 independent test suites. **No porting gap.**

---

## VFX

Original test directory: No test directory found (only `demo/` and `gdx-vfx/` source)
SGE test directory: `sge-extension/vfx/src/test/scala/sge/vfx/`

**No original unit tests found.** The `original-src/gdx-vfx/` contains only production source (`gdx-vfx/core/src/`, `gdx-vfx/effects/src/`) and demo applications (`demo/`).

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `PrioritizedArraySuite.scala` | PrioritizedArray add/remove/iterate/priority ordering | SGE-original |

**Summary**: Original has 0 unit tests. SGE has 1 independent test suite. **No porting gap.**

---

## Textra (TextraTypist)

Original test directory: `original-src/textratypist/src/test/java/com/github/tommyettinger/textra/`
SGE test directory: `sge-extension/textra/src/test/scala/sge/textra/`

### Original Test Analysis

All 100+ files are **demo/interactive applications** (extend `ApplicationAdapter` with `main` methods). **Zero files contain `@Test` annotations.** Categories:

- **Visual demos**: `AntiAliasingTest`, `AtlasTest`, `BareEmojiTypingLabelTest`, `CJKTypingLabelTest`, `CheckBoxTest`, `ColorWrappingTest`, etc. (50+ files)
- **Issue reproduction**: `Issue6Test`, `Issue13Test`, `Issue14Test`, `Issue16Test`, etc. (15+ files)
- **Preview generators**: `PreviewGenerator`, `PreviewEmojiGenerator`, `PreviewIconGenerator`, etc.
- **UI tests**: `NewTextraUITest`, `SimpleTypingUITest`, `StandardUITest`, etc.
- **Helper classes**: `FreeTypistSkin`, `FreeTypistSkinLoader`, `BitmapFontSupport`, `JsonSkin`, etc.
- **Headless test app**: `HeadlessTest` -- runs headlessly but is still an ApplicationAdapter, no @Test
- **Standalone mains**: `LZBTest`, `NoiseUtilsTest`, `StateTest` -- all have `main()` not `@Test`

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `FontSuite.scala` | GlyphRegion construction, TextureRegion inheritance | SGE-original |
| 2 | `LayoutLineSuite.scala` | Layout defaults, line management | SGE-original |
| 3 | `TypingConfigSuite.scala` | TypingConfig default values | SGE-original |
| 4 | `BlockUtilsSuite.scala` | BOX_DRAWING table completeness | SGE-original |
| 5 | `ColorUtilsSuite.scala` | Color utility conversions | SGE-original |
| 6 | `LZBCompressionSuite.scala` | LZB compress/decompress roundtrip | SGE-original |
| 7 | `PaletteSuite.scala` | Palette color names/values | SGE-original |
| 8 | `StringUtilsSuite.scala` | String utility methods | SGE-original |

**Summary**: Original has 0 unit tests (100+ demo/interactive apps). SGE has 8 independent test suites. **No porting gap** -- SGE far exceeds original.

---

## ScreenManager

Original test directory: `original-src/libgdx-screenmanager/src/test/java/de/eskalon/commons/`
SGE test directory: `sge-extension/screens/src/test/scala/sge/screen/`

### LibgdxUnitTest.java (Base class)

Not a test file itself -- provides `@BeforeAll`/`@AfterAll` for headless LibGDX setup with Mockito.

### ScreenManagerUnitTest.java (Base class)

Not a test file itself -- provides `getMockedScreenManager()` utility and mocked `ScreenFboUtils`.

### ManagedGameTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testConstructor` | ManagedGame has ScreenManager | NOT_APPLICABLE | -- | SGE does not port ManagedGame (different architecture) |
| 2 | `testInputMultiplexer` | ManagedGame input multiplexer setup | NOT_APPLICABLE | -- | SGE architecture differs |
| 3 | `testCorrespondingScreenManagerMethods` | Game lifecycle calls forward to ScreenManager | NOT_APPLICABLE | -- | SGE architecture differs |

### ScreenManagerTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testScreenLifecycle` | Screen show/hide/render lifecycle, input processor registration | PARTIALLY_PORTED | `ScreenManagerSuite` | SGE tests lifecycle basics but without full Mockito-based integration |
| 2 | `testExceptions` | IllegalStateException before init, NullPointerException on null push | NOT_PORTED | -- | Error condition tests not ported |
| 3 | `testApplicationListenerEvents` | pause/resume/resize forwarding to correct screens | NOT_PORTED | -- | Complex lifecycle forwarding not tested |
| 4 | `testDispose` | dispose() disposes all screens and transitions | NOT_PORTED | -- | Disposal chain not tested |

### ScreenManagerTest2.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testScreenLifecycleWhileTransition` | Show/hide/render during transition, input handler unregistration | NOT_PORTED | -- | Complex transition lifecycle not tested |
| 2 | `testIdenticalDoublePush` | Pushing same screen twice is ignored | NOT_PORTED | -- | Edge case not tested |
| 3 | `testRemovingProcessor` | Input processor removed from screen during render | NOT_PORTED | -- | Edge case not tested |

### BasicInputMultiplexerTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `test` | addProcessors, removeProcessors, addProcessor, removeProcessors() | NOT_PORTED | -- | BasicInputMultiplexer API not tested |

### TimedScreenTransitionTest.java

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `test` | Timed transition progress, isDone, reset on show() | PORTED | `TimedTransitionSuite` | SGE fully covers this test |

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `ScreenManagerSuite.scala` | ManagedScreen defaults, BlankScreen, ScreenTransition defaults | Partial coverage of original |
| 2 | `TimedTransitionSuite.scala` | TimedTransition progress, isDone, interpolation, reset, duration validation | Covers original + extras |
| 3 | `SlidingDirectionSuite.scala` | SlidingDirection enum factors | SGE-original |

**Summary**: Original has 5 test files with 12 `@Test` methods (3 NOT_APPLICABLE due to architecture). Of the 9 applicable: 2 PORTED, 1 PARTIALLY_PORTED, 6 NOT_PORTED. This is the **most significant gap** in this batch.

---

## GLTF

Original test directory: `original-src/gdx-gltf/gltf/test/net/mgsx/gltf/`
SGE test directory: `sge-extension/gltf/src/test/scala/sge/gltf/`

### AttributesCompareTest.java (only file with `@Test`)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testFogSame` | FogAttribute.compareTo with equal attrs | NOT_PORTED | -- | Attribute comparison not tested |
| 2 | `testFogDifferent` | FogAttribute.compareTo with different attrs | NOT_PORTED | -- | |
| 3 | `testIridescenceSame` | PBRIridescenceAttribute.compareTo equal | NOT_PORTED | -- | |
| 4 | `testIridescenceDifferent` | PBRIridescenceAttribute.compareTo different | NOT_PORTED | -- | |
| 5 | `testVolumeSame` | PBRVolumeAttribute.compareTo equal | NOT_PORTED | -- | |
| 6 | `testVolumeDifferent` | PBRVolumeAttribute.compareTo different | NOT_PORTED | -- | |
| 7 | `testTextureSame` | PBRTextureAttribute.compareTo with UV transforms | NOT_PORTED | -- | |
| 8 | `testTextureDifferent` | PBRTextureAttribute.compareTo different rotation | NOT_PORTED | -- | |

### Other original test files (no @Test)

- `Benchmark.java` -- Performance benchmark app (ApplicationAdapter)
- `ExportOBJTest.java` -- Export demo app
- `ExportSharedIndexBufferTest.java` -- Export demo app
- `ImportGLTFTest.java` -- Import demo app
- `SharedTextureTest.java` -- Shared texture demo app
- `ProceduralExamples.java` -- Interactive demo app
- `DesktopBatchTestLauncher.java` -- Desktop launcher

### SGE Tests (Independent)

| # | SGE Test Suite | Tests What | Notes |
|---|---------------|-----------|-------|
| 1 | `GLTFDataSuite.scala` | GLTF data model defaults, asset, scenes, nodes, meshes, extensions | SGE-original |
| 2 | `GLTFJsonParserSuite.scala` | JSON parsing: minimal, scenes, materials, buffers, animations, extensions, errors | SGE-original |
| 3 | `GLTFTypesSuite.scala` | Type constants, mapPrimitiveMode, accessorTypeSize, Vector3/Quaternion mapping, interpolation | SGE-original |

**Summary**: Original has 1 unit test file with 8 `@Test` methods (all AttributesCompareTest). All 8 are NOT_PORTED. SGE has 3 independent test suites covering data model and parsing, but not attribute comparison.

---

# Overall Summary

## Per-Library Totals

| Library | Original @Test Methods | Applicable | Ported | Partially | Not Ported | Not Applicable | SGE-Only Suites |
|---------|----------------------|------------|--------|-----------|------------|----------------|-----------------|
| Anim8 | 0 | 0 | 0 | 0 | 0 | 0 | 3 |
| Colorful | 12 (2 @Ignore) | 8 | 2 | 2 | 4 | 2 | 4 |
| VisUI | 9 | 2 | 2 | 0 | 0 | 7 | 3 |
| JBump | 0 | 0 | 0 | 0 | 0 | 0 | 1 (14 tests) |
| Noise4J | 0 | 0 | 0 | 0 | 0 | 0 | 3 |
| Controllers | 0 | 0 | 0 | 0 | 0 | 0 | 2 |
| VFX | 0 | 0 | 0 | 0 | 0 | 0 | 1 |
| Textra | 0 | 0 | 0 | 0 | 0 | 0 | 8 |
| ScreenManager | 12 | 9 | 2 | 1 | 6 | 3 | 3 |
| GLTF | 8 | 8 | 0 | 0 | 8 | 0 | 3 |
| **TOTAL** | **41** | **27** | **6** | **3** | **18** | **12** | **31** |

## Priority List of Tests to Port

### Priority 1 (High) -- ScreenManager: 6 missing tests

These are real integration tests with meaningful coverage of lifecycle management:

1. `ScreenManagerTest.testExceptions` -- Error condition validation
2. `ScreenManagerTest.testApplicationListenerEvents` -- pause/resume/resize forwarding
3. `ScreenManagerTest.testDispose` -- Disposal chain
4. `ScreenManagerTest2.testScreenLifecycleWhileTransition` -- Transition lifecycle
5. `ScreenManagerTest2.testIdenticalDoublePush` -- Double-push guard
6. `BasicInputMultiplexerTest.test` -- Input multiplexer API

Note: These require Mockito-style mocking (original uses `HeadlessApplication` + `Mockito`). SGE would need either a mock framework or test-only screen manager subclass.

### Priority 2 (Medium) -- GLTF AttributesCompareTest: 8 missing tests

All test `Attribute.compareTo()` for PBR attributes (Fog, Iridescence, Volume, Texture). These are pure unit tests with no GL dependency -- straightforward to port.

1. `testFogSame` / `testFogDifferent`
2. `testIridescenceSame` / `testIridescenceDifferent`
3. `testVolumeSame` / `testVolumeDifferent`
4. `testTextureSame` / `testTextureDifferent`

### Priority 3 (Low) -- Colorful: 4 missing tests

These are color math accuracy tests:

1. `HSLCorrectnessTest.testRgb2hclInt` -- HCL int conversion accuracy
2. `HSLCorrectnessTest.testHcl2rgbInt` -- HCL int reverse accuracy
3. `BasicCheckTest.testPalette` -- FullPalette gamut validation
4. `BasicCheckTest.testBlues` -- chromaLimit blue hue sweep

## Libraries With Zero Original Tests

The following 7 out of 10 libraries have **no original unit tests** (`@Test` methods). All "test" files in these libraries are interactive demo applications, benchmarks, or platform launchers:

1. **Anim8** -- 20 demo/benchmark files, 0 `@Test`
2. **JBump** -- 1 interactive demo, 0 `@Test`
3. **Noise4J** -- No test directory at all
4. **Controllers** -- 7 demo/launcher files, 0 `@Test`
5. **VFX** -- No test directory (only demo apps)
6. **Textra** -- 100+ demo/interactive files, 0 `@Test`
7. **VisUI** (manual tests) -- 17 visual demo files, 0 `@Test` (only 2 actual unit tests exist)

SGE has created independent test suites for ALL of these libraries (31 total suites), significantly exceeding the original test coverage.
