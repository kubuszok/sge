# SGE Comprehensive Re-Audit — Consolidated Report

**Date**: 2026-04-18
**Method**: Body-level comparison by 11 parallel agents (8 source, 3 test)
**Scope**: All 1,264 audited files + original test mapping

---

## Executive Summary

This re-audit performed **body-level comparison** (reading both original Java and ported Scala files, comparing every method body) across the entire SGE codebase. Prior audits only checked method-set names via `re-scale enforce compare`.

| Metric | Count |
|--------|-------|
| **Total files audited** | ~1,264 |
| **MAJOR_ISSUES found** | **24** (previously marked "pass") |
| **MINOR_ISSUES found** | **~39** (most previously "pass") |
| **NEEDS_DEEP_REVIEW** | 2 |
| **Unported Java files discovered** | 12 (Textra widgets + batching classes) |
| **Confirmed PASS** | ~1,187 |
| **Files upgraded (minor→pass)** | 3 (g3d light attributes) |

**Key finding**: ~5% of files previously marked "pass" have real functional gaps. The worst areas are **Textra** (13 major issues, 12 unported files) and **VisUI** (6 major issues). Core graphics, math, and most extensions are solid.

---

## MAJOR_ISSUES — Full List (24 files)

### Textra (13 files) — Batch B

| File | Issue |
|------|-------|
| `Font.scala` | Missing rotation `drawGlyphs` overload, `TexturelessRegion`/`TexturelessAtlasRegion` inner classes, `calculateXAdvances`, `extractScale`/`applyScale`, BitmapFont constructors |
| `CaseInsensitiveIntMap.scala` | Entirely reimplemented as HashMap wrapper (178 vs 675 lines), missing 5 inner classes (Entry, MapIterator, Entries, Values, Keys), different sentinel logic |
| `KnownFonts.scala` | Missing shader methods, `getBitmapFont`, `loadUnicodeAtlas` |
| `BitmapFontSupport.scala` | Core `loadStructuredJson` and `JsonFontData` entirely absent |
| `WaveEffect.scala` | Interpolation args inverted `(1f, -1f, progress)` vs Java `(-1, 1, progress)` — **wave direction is opposite** |
| `TextraField.scala` | Missing scene2d integration (InputListener, Cell/Table layout, lifecycle hooks) |
| `TextraButton.scala` | Missing scene2d integration |
| `TextraCheckBox.scala` | Missing scene2d integration |
| `TextraDialog.scala` | Missing scene2d integration |
| `TextraListBox.scala` | Missing scene2d integration |
| `TextraSelectBox.scala` | Missing scene2d integration |
| `TextraWindow.scala` | Missing scene2d integration |
| `Layout.scala` | Missing `insertLine` method |

### VisUI (6 files) — Batch A

| File | Issue |
|------|-------|
| `Dialogs.scala` | `showErrorDialog` silently drops `details` parameter. Missing `showConfirmDialog`, `showDetailsDialog`, 4 dialog inner classes (ConfirmDialog, DetailsDialog, InputDialog, OptionDialog) |
| `Menu.scala` | **Entire menu open/close mechanism missing.** No InputListener on openButton — clicking menu titles in MenuBar does nothing |
| `TabbedPane.scala` | `TabButtonTable` missing InputListener: no middle-click close, no hover feedback, no dragged-up styling, no disabled propagation |
| `VisTextArea.scala` | Extends `TextArea` instead of `VisTextField`, losing readOnly, cursorPercentHeight, backgroundOver, focusField, isEmpty, clearText, I-beam cursor |
| `ScrollableTextArea.scala` | Inherits VisTextArea's gap (wrong parent class) |
| `HighlightTextArea.scala` | Inherits VisTextArea's gap (wrong parent class) |

### GLTF (2 files) — Batch G

| File | Issue |
|------|-------|
| `SceneSkybox.scala` | Missing `SkyboxShader` and `SkyboxShaderProvider` inner classes. `lodBias` is dead code. LOD skybox rendering and SRGB color space conversion silently broken |
| `SceneManager.scala` | `removeScene` has empty TODO body — original Java removes lights from environment. **Lights leak when scenes are removed** |

### Core Utils (1 file) — Batch E

| File | Issue |
|------|-------|
| `TextFormatter.scala` | `replaceEscapeChars` and `java.text.MessageFormat` integration entirely missing. `format()` always uses `simpleFormat`, losing locale-aware formatting |

### AI (1 file) — Batch F

| File | Issue |
|------|-------|
| `DefaultTimepiece` (in `Timepiece.scala`) | Missing `maxDeltaTime` field and clamping. AI systems see unbounded delta times after pauses, causing erratic behavior |

### Anim8 (1 file) — Batch H

| File | Issue |
|------|-------|
| `PaletteReducer.scala` | Multi-pixmap `analyze()` only processes first frame instead of aggregating. `analyzeHueWise()` is no-op delegation. SNUGGLY palette data differs from original |

---

## Unported Java Files (12 — discovered by Batch B)

These Java source files have **no Scala counterpart at all** and are not tracked in the migration database:

| Original Java File | Library | Impact |
|-------------------|---------|--------|
| `TypingButton.java` | Textra | Widget variant with typing animation |
| `TypingCheckBox.java` | Textra | Widget variant with typing animation |
| `TypingDialog.java` | Textra | Widget variant with typing animation |
| `TypingListBox.java` | Textra | Widget variant with typing animation |
| `TypingSelectBox.java` | Textra | Widget variant with typing animation |
| `TypingTooltip.java` | Textra | Widget variant with typing animation |
| `TypingWindow.java` | Textra | Widget variant with typing animation |
| `TextureArrayBatch*.java` (4 files) | Textra | TextureArray batching for multi-texture rendering |
| `LzmaUtils.java` | Textra | LZMA compression utility |

---

## MINOR_ISSUES — Summary (~39 files)

### Batch A (VisUI) — 2 files
- `VisTextField.scala` — missing 5-arg style constructor
- `VisUI.scala` — missing `getSizesName`/`setSkipGdxVersionCheck`

### Batch B (Textra) — 12 files
- `Layout.scala` — missing `insertLine`
- `Styles.scala` — missing `dispose()`
- `StringUtils.scala` — missing `shuffleWords`, `decompressCategory`, BitSet constants
- 9 other files with minor method gaps

### Batch E (Utils/Math/Core) — ~14 files
- `Timer.scala` — missing `threadLock.notifyAll()`, error handling catches instead of rethrowing
- `BufferUtils.scala` — missing `clear(ByteBuffer, Int)`, `asByteBuffer` throws for non-ByteBuffer
- `Pool.scala` — missing null check in `freeAll`
- `ArrayMap.scala` — missing `firstKey`, `firstValue`, `insert`, `getKey`, `setValue`, `setKey`, `equalsIdentity`
- `ObjectMap/ObjectSet/OrderedMap/OrderedSet` — iterator inner classes eliminated (deliberate but API-breaking)
- `Log.scala` — global singleton loses per-tag log levels
- `AssetManager.scala` — some 3D model loaders not registered

### Batch F (AI) — 3 files
- `BehaviorTreeParser.scala` — reflection-based castValue replaced by TaskRegistry
- `HierarchicalPathFinder.scala` — inherited FIXME preserved
- `Formation.scala` — Vector2.mul replaced with rotateRad shortcut

### Batch G (GLTF/Maps/Net) — 1 file
- `TideMapLoader.scala` — null texture silently skipped instead of throwing

### Batch H (Remaining extensions) — 6 files
- `PNG8.scala` — missing 5-arg write overloads
- `ImmutableArray.scala` — missing equals/hashCode/toArray/toString
- `Path.scala` — missing removeAll/retainAll/removeIf
- `VfxFrameBuffer.scala` — missing BatchRendererAdapter/ShapeRendererAdapter inner classes
- `PrioritizedArray.scala` — pool/iterator validation absent
- `DefaultControllerManager.scala` — polling-based, no vibration

---

## Clean Batches (confirmed PASS)

| Batch | Scope | Files | Result |
|-------|-------|-------|--------|
| **C** | Graphics3D, GLutils, Profiling | ~140 | **All PASS.** 3 files upgraded from minor→pass. Particle serialization fully covered by 47 codecs. |
| **D** | Graphics root, G2D, Scene2D | 149 | **148 PASS, 1 minor.** All high-suspicion files (ParticleEmitter, GlyphLayout, Actor, Group, Skin, Button) verified clean. |

---

## Test Porting Gap Summary

### Core LibGDX Tests (T1)
- 211 original test methods → 90 ported, 4 partial, 13 not ported, 104 N/A
- **Excluding N/A: 84.1% ported**
- Priority gap: ObjectMap stress tests (hash correctness validation)

### Ashley/AI/Graphs Tests (T2)
- 120 original test methods → 91 ported, 7 partial, 22 not ported
- **Ashley: 84%** — missing ComponentOperationHandler, FamilyManager, SystemManager internals
- **AI: 45%** — missing multi-step Parallel orchestrator tests (Resume vs Join behavior)
- **Graphs: 75%** — missing disconnect, sortVertices, sortEdges

### Other Extension Tests (T3)
- 27 applicable original test methods → 6 ported, 3 partial, 18 not ported
- **ScreenManager (6 missing)**: error conditions, lifecycle forwarding, disposal chains
- **GLTF (8 missing)**: PBR attribute compareTo tests (pure unit tests, easy to port)
- **Colorful (4 missing)**: HCL int conversion accuracy tests
- 7 of 10 libraries had zero original unit tests (SGE created 31 independent suites)

---

## Priority Recommendations

### P0 — Critical functional bugs (fix immediately)
1. **VisUI Menu.scala** — clicking menu titles does nothing (entire mechanism missing)
2. **GLTF SceneManager.removeScene** — lights leak when scenes are removed
3. **Textra WaveEffect.scala** — wave direction is inverted
4. **AI DefaultTimepiece** — unbounded delta times after pauses

### P1 — Major missing functionality (fix before release)
5. **Textra Font.scala** — missing rotation drawGlyphs, inner classes, BitmapFont constructors
6. **Textra CaseInsensitiveIntMap** — reimplemented as simple wrapper, missing collection semantics
7. **VisUI Dialogs.scala** — error dialog drops details, missing confirm/details/input/option dialogs
8. **VisUI TabbedPane.scala** — tab button interaction broken (no middle-click close, no hover)
9. **VisUI VisTextArea.scala** — wrong parent class (cascading to ScrollableTextArea, HighlightTextArea)
10. **GLTF SceneSkybox.scala** — LOD and SRGB rendering broken
11. **TextFormatter.scala** — locale-aware formatting broken
12. **PaletteReducer.scala** — animation palette quality broken (first-frame only)
13. **12 unported Textra files** — Typing* widgets and TextureArray batching

### P2 — Minor gaps and test coverage
14. All ~39 MINOR_ISSUES files
15. Port missing test methods (ObjectMap stress, AI orchestrator, ScreenManager lifecycle)

### P3 — Architecture divergences (document, don't fix)
16. Collection iterator inner classes → Scala foreach (deliberate)
17. Net HTTP → SgeHttpClient (deliberate)
18. JSON → typed codecs (deliberate)

---

## Batch Reports

Detailed per-file findings are in:
- `agents/re-audit-batch-A.md` — VisUI
- `agents/re-audit-batch-B.md` — Textra
- `agents/re-audit-batch-C.md` — Graphics3D/GLutils/Profiling
- `agents/re-audit-batch-D.md` — Graphics2D/Scene2D
- `agents/re-audit-batch-E.md` — Utils/Math/Core
- `agents/re-audit-batch-F.md` — AI
- `agents/re-audit-batch-G.md` — GLTF/Maps/Net
- `agents/re-audit-batch-H.md` — Remaining extensions
- `agents/re-audit-batch-T1.md` — Core LibGDX test mapping
- `agents/re-audit-batch-T2.md` — Ashley/AI/Graphs test mapping
- `agents/re-audit-batch-T3.md` — Other extension test mapping
