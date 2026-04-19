# Re-Audit Batch B: Textra (all sub-packages)

**Auditor**: port-auditor agent (aggressive re-audit)
**Date**: 2026-04-18
**Scope**: 79 Scala files in `sge-extension/textra/src/main/scala/sge/textra/`
**Original**: `original-src/textratypist/src/main/java/com/github/tommyettinger/textra/`
**Prior audit status**: ALL files marked `pass` in audit DB

## Summary of Findings

- **12 Java source files have NO Scala counterpart at all** (Typing* widget variants, TextureArray*, LzmaUtils)
- **4 high-suspicion files confirmed MAJOR_ISSUES** (Font, CaseInsensitiveIntMap, KnownFonts, BitmapFontSupport)
- **1 behavioral bug found**: WaveEffect Interpolation argument order inverted vs original
- **Many files previously audited as "pass" have missing methods/inner classes/features**

## Missing Files (no Scala counterpart exists)

The following Java files exist in the original but have NO Scala port file:

1. `TypingButton.java` - NOT_PORTED
2. `TypingCheckBox.java` - NOT_PORTED
3. `TypingDialog.java` - NOT_PORTED
4. `TypingListBox.java` - NOT_PORTED
5. `TypingSelectBox.java` - NOT_PORTED
6. `TypingTooltip.java` - NOT_PORTED
7. `TypingWindow.java` - NOT_PORTED
8. `TextureArrayCpuPolygonSpriteBatch.java` - NOT_PORTED (may be intentionally skipped - GL-specific)
9. `TextureArrayPolygonSpriteBatch.java` - NOT_PORTED (may be intentionally skipped - GL-specific)
10. `TextureArrayShaderCompiler.java` - NOT_PORTED (may be intentionally skipped - GL-specific)
11. `TextureArrayShaders.java` - NOT_PORTED (may be intentionally skipped - GL-specific)
12. `utils/LzmaUtils.java` - NOT_PORTED

None of these are tracked in the migration database.

---

## Per-File Audit Results

### Font.scala
- **Original**: Font.java (8421 lines)
- **Prior status**: pass (with notes about partial-port covenant)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `calculateXAdvances`, `applyScale`, `extractScale`, `extractIntScale`, `getJsonExtension`, `getMaxDimension` (static helpers), `drawGlyphs` 3-arg rotation overload (with rotation/originX/originY)
- **Missing inner classes**: `TexturelessRegion`, `TexturelessAtlasRegion` (both present in Java, absent in Scala)
- **Missing constructors**: All `Font(BitmapFont, ...)` constructor variants (4+ overloads)
- **Simplified methods**: `drawGlyphs` - the rotation+origin overload is entirely absent (Java lines 4841-4938). The two present overloads don't handle rotation at all.
- **Missing branches**: None found in existing methods
- **Mechanism changes without tests**: The partial-port covenant header explicitly lists debt (drop shadow, outlines, HALO/NEON/SHINY effects, underline/strikethrough, box-drawing). However, body-level review of `drawGlyph` shows these ARE now implemented (lines 2270-2400+). The covenant header is stale/inaccurate.
- **Shortcut markers**: 2 hits (get-or-else-null at lines 1671, 1696) - both annotated as Java interop, acceptable
- **Notes**: 3521 lines vs 8421 original. The file was upgraded from partial-port but still has significant gaps. The `extractScale`/`extractIntScale`/`applyScale` static methods are used by other classes for glyph manipulation. `calculateXAdvances` is critical for layout width calculation. The missing rotation `drawGlyphs` overload means rotated layout rendering is unsupported.

### TextraField.scala
- **Original**: TextraField.java (1360 lines)
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `createInputListener`, `findNextTextField`, `getDefaultInputListener`, `getOnscreenKeyboard`, `setOnscreenKeyboard`, `setStage`, `setParent`, `positionChanged`, `sizeChanged`
- **Missing inner classes**: `TextFieldClickListener` (inner class, 100+ lines in Java), `KeyRepeatTask` (inner class extending Timer.Task)
- **Simplified methods**: The entire scene2d integration layer is missing. TextraField doesn't extend Widget/Actor and lacks the event listener infrastructure.
- **Missing branches**: Focus traversal (`findNextTextField`), stage lifecycle (`setStage`/`setParent`)
- **Mechanism changes without tests**: The field operates as standalone class rather than scene2d Widget, so all Actor lifecycle hooks are absent
- **Notes**: 1284 vs 1360 lines suggests body content is mostly there, but the scene2d integration (InputListener, ClickListener, Widget inheritance) is restructured. The `KeyRepeatTask` using `Timer.Task` is replaced with manual timer state fields. `TextFieldClickListener` (Java lines 1045-1182) handling touchDown/touchDragged/touchUp/keyDown/keyTyped/keyUp is replaced with `handle*` methods but the ClickListener wrapper is absent.

### CaseInsensitiveIntMap.scala
- **Original**: CaseInsensitiveIntMap.java (675 lines)
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `place`, `locateKey`, `putResize`, `resize`, `shrink`, `clear(int)`, `tableSize`, `entries()`, `values()` (iterator version), `keys()` (iterator version), `iterator()`
- **Missing inner classes**: `Entry`, `MapIterator`, `Entries`, `Values`, `Keys` (5 inner classes, ~160 lines)
- **Simplified methods**: The entire implementation is replaced with `scala.collection.mutable.HashMap[String, Int]` wrapper with lowercased keys. This fundamentally changes the data structure from open-addressing hash map to chained hash map.
- **Missing branches**: The Java `equals()` uses a `-1` sentinel pattern (get with -1 default, then check containsKey if result is -1) which is more correct than the Scala version that uses `get(key, 0)` - if a legitimate value is 0, the Scala equals would incorrectly treat it as missing.
- **Mechanism changes without tests**: `hashCodeIgnoreCase` uses `Character.toUpperCase` instead of `Category.caseUp` from RegExodus. The RegExodus `caseUp` handles more Unicode categories correctly (Georgian alphabet). This is a behavioral difference for non-ASCII text.
- **Notes**: 178 lines vs 675. The wrapper approach is valid for basic functionality but loses the iteration protocol (Entries/Keys/Values iterators with reuse), the `Iterable<Entry>` contract, and the open-addressing performance characteristics. The `ensureCapacity` method is a no-op stub.

### KnownFonts.scala
- **Original**: KnownFonts.java (6870 lines)
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getBitmapFont`, `getMsdfShader`, `getSdfShader`, `getSdfOutlineShader`, `getStandardShader`, `loadFont` (internal), `loadUnicodeAtlas`
- **Simplified methods**: Individual font getter methods in Java contain full font construction logic with specific metric adjustments (xAdj, yAdj, wAdj, hAdj, makeGridGlyphs flags, distance field type selection). The Scala versions delegate to a generic `getFont(baseName, dft)` method. Need to verify the generic `getFont` path produces equivalent results.
- **Missing branches**: Shader program caching (`getStandardShader` etc. cache ShaderProgram instances)
- **Mechanism changes without tests**: `loadUnicodeAtlas` in Java (lines 5364-5498) handles UTF-8 atlas loading with specific region parsing. Scala notes say "SGE reads UTF-8 by default" but the method itself is absent. `getBitmapFont` is explicitly skipped ("no BitmapFont in SGE").
- **Notes**: 1020 lines vs 6870. The massive line count difference is partly because each Java getter method has ~30 lines of construction logic while Scala uses 1-line delegation. But the shader methods and BitmapFont conversion are genuinely missing. The `loadFont` internal method (Java lines 535-575) that handles the actual Font construction with caching is not visible; need to verify `getFont` implements equivalent caching.

### Effect.scala
- **Original**: Effect.java (150 lines)
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: All 13 methods present and verified body-equivalent: constructor, `assignTokenName`, `update`, `apply`, `onApply` (abstract), `isFinished`, `calculateFadeout`, `calculateProgress` (3 overloads), `paramAsFloat`, `paramAsBoolean`, `paramAsColor`. Inner `EffectBuilder` interface ported as trait. Verified `calculateFadeout` uses same `Interpolation.smooth` call pattern, `calculateProgress` has identical ping-pong logic.

### Layout.scala
- **Original**: Layout.java
- **Prior status**: pass (with notes about 12 abstract methods)
- **New status**: MINOR_ISSUES
- **Missing methods**: `insertLine`
- **Simplified methods**: none found in present methods
- **Missing branches**: none in present methods
- **Mechanism changes without tests**: none
- **Notes**: `insertLine` (Java line 238) is a public method that inserts a Line at a specific index. This is used for text editing operations. All other methods appear present based on compare output (Common: many). The audit note about "12 abstract methods" refers to the Widget superclass methods, not Layout's own methods.

### Line.scala
- **Original**: Line.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (LongArray flagged as missing is a type reference, not a method)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 5 common methods. Simple data class with `glyphs` (ArrayBuffer[Long]), `height`, `width` fields and `size`, `compareTo` methods. Verified equivalent.

### Parser.scala
- **Original**: Parser.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `parseColorMarkups` (but this is commented out/unused in the Java original too - line 113)
- **Simplified methods**: none identified in common methods
- **Missing branches**: none
- **Mechanism changes without tests**: `Replacer` (RegExodus) replaced with `java.util.regex` - functionality equivalent for the patterns used
- **Notes**: The `parseColorMarkups` method exists in Java but is commented out at its call site (line 113: `// parseColorMarkups(label);`). So its absence is acceptable. The `Replacer` from RegExodus is replaced with standard regex, which is the project convention.

### TypingLabel.scala
- **Original**: TypingLabel.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `getHeight`, `getOriginX`, `getOriginY`, `getRotation`, `getScaleX`, `getScaleY`, `getWidth`, `getX`, `getY` (all are getter methods from Widget/Actor superclass - likely converted to property access), `layout` (Widget method)
- **Simplified methods**: none identified in body comparison
- **Missing branches**: none identified
- **Mechanism changes without tests**: 1 shortcut marker at line 1210: `UnsupportedOperationException` catch in what appears to be a try/catch for JSON parsing
- **Notes**: The "missing" getters are all Widget/Actor property accessors that would be inherited or converted to `var` fields per project convention. The `Sprite`/`SpriteDrawable`/`Vector2` references are type imports, not methods. The core typing animation logic appears preserved.

### TypingConfig.scala
- **Original**: TypingConfig.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (Color and IntFloatMap are type references)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 6 common methods verified. Static configuration class with constants and effect registration. All `EFFECT_START_TOKENS`, `EFFECT_END_TOKENS`, `INTERVAL_MULTIPLIERS_BY_CHAR`, `GLOBAL_VARS` maps present.

### TypingAdapter.scala
- **Original**: TypingAdapter.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 5 common methods. Simple adapter class implementing TypingListener with no-op defaults. All methods verified: `event`, `end`, `onChar`, `replace`, `endWithType`.

### TypingListener.scala
- **Original**: TypingListener.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 5 common methods. Interface/trait with `event`, `end`, `onChar`, `replace`, `endWithType`. Verified equivalent.

### ColorLookup.scala
- **Original**: ColorLookup.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 2 common methods + 2 companion instances (INSTANCE, DESCRIPTIVE). Java interface with default method converted to Scala trait. `INSTANCE` delegates to `ColorUtils.lookupInColors`, `DESCRIPTIVE` delegates to `ColorUtils.describe`. Both verified equivalent.

### InternalToken.scala
- **Original**: InternalToken.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: `values` (Java enum static method - available automatically in Scala 3 enum)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Enum with SKIP, FASTER, EVENT, SPEED, FAST, SIZE, CLEARSIZE, NATURAL, COLOR, STYLE, CLEARCOLOR, WAIT entries. All present. The `values()` method is auto-generated for Scala 3 enums.

### Justify.scala
- **Original**: Justify.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Enum with NONE, SPACES_ON_ALL_LINES, SPACES_ON_PARAGRAPH, FULL_ON_ALL_LINES, FULL_ON_PARAGRAPH entries with `spaces` and `last` boolean fields. Dropped ctor args are enum constructor params which are present in the Scala enum definition.

### TokenCategory.scala
- **Original**: TokenCategory.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: `values` (auto-generated for Scala 3 enum)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Enum with WAIT, SPEED, COLOR, EVENT, ALL entries. Verified equivalent. `ALL` is extra in port but is a convenience constant.

### TokenEntry.scala
- **Original**: TokenEntry.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Data class with `token`, `category`, `index`, `endIndex`, `floatValue`, `stringValue`, `effect` fields. 2 common methods verified: `compareTo`, `toString`.

### Styles.scala
- **Original**: Styles.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `dispose()` on `TextFieldStyle` (implements Disposable in Java, calls `font.dispose()`)
- **Simplified methods**: All BitmapFont-accepting constructors are skipped (deprecated in Java, and no BitmapFont in SGE)
- **Missing branches**: none in present code
- **Mechanism changes without tests**: none
- **Notes**: Contains style inner classes: `LabelStyle`, `TextButtonStyle`, `ImageTextButtonStyle`, `CheckBoxStyle`, `WindowStyle`, `ListStyle`, `SelectBoxStyle`, `TooltipStyle`, `TextFieldStyle`. All present. The deprecated BitmapFont constructors are intentionally skipped. The `dispose()` method on TextFieldStyle is a real gap - it prevents proper resource cleanup.

### BitmapFontSupport.scala
- **Original**: BitmapFontSupport.java (471 lines)
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `loadStructuredJson` (4 overloads), `getFirstGlyph`, `getGlyph`, `load`, `round`
- **Missing inner classes**: `JsonFontData` (extends BitmapFont.BitmapFontData, ~300 lines)
- **Simplified methods**: The entire BitmapFont loading functionality is absent
- **Missing branches**: All - the core functionality is deferred
- **Mechanism changes without tests**: The Scala file contains only LZB decompression utilities, none of the BitmapFont integration
- **Notes**: 208 lines vs 471. The migration note says "Full loading deferred until rendering layer is wired up" but the file is marked as `pass` in audit DB. The primary purpose of this class (loading BitmapFont from Structured JSON) is entirely absent. Only the LZB decompression helper is present.

### ImageTypingButton.scala
- **Original**: ImageTypingButton.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: none (TypingLabel is a type reference)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 2 common methods. Simple button widget. Dropped ctor args (skin, styleName) suggest Skin-based constructors may be simplified.

### ImageTextraButton.scala
- **Original**: ImageTextraButton.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getCell`, `getImage`, `getImageCell`, `getLabelCell`, `getPrefHeight`, `newImage`, `getName`, `getClass`
- **Simplified methods**: Scene2d Cell/Table layout methods are absent
- **Missing branches**: Image layout and sizing logic
- **Mechanism changes without tests**: none
- **Notes**: The missing methods relate to scene2d Table/Cell layout infrastructure. `getImage`, `getImageCell`, `getLabelCell` are critical for layout. `newImage` creates the Image actor. `getPrefHeight` affects layout sizing.

### EmojiProcessor.scala
- **Original**: EmojiProcessor.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `getReplacer` (returns RegExodus Replacer)
- **Simplified methods**: `replaceEmoji` replaces the Replacer pattern matching
- **Missing branches**: none
- **Mechanism changes without tests**: RegExodus Replacer replaced with alternative emoji processing
- **Notes**: The `Replacer` class from RegExodus is replaced with a different emoji processing approach. This is consistent with the project's convention of using `java.util.regex` instead of RegExodus.

### FWSkin.scala
- **Original**: FWSkin.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `getAll`, `getJsonLoader`, `read` (Json serialization integration)
- **Simplified methods**: Json loading uses SGE's Json API instead of libGDX's Json/Skin serialization
- **Missing branches**: BitmapFont integration paths
- **Mechanism changes without tests**: none
- **Notes**: The Scala version has `load*` methods for each style type (loadTextraCheckBoxStyles, loadTextraLabelStyles, etc.) that replace the Java's generic Json serialization. `getJsonLoader` and `getAll` are part of Skin's generic reflection-based deserialization which is restructured.

### FWSkinLoader.scala
- **Original**: FWSkinLoader.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (FWSkin is a type reference)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 2 common methods. Simple loader class. Verified equivalent.

### TextraArea.scala
- **Original**: TextraArea.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (Font, TypingLabel are type references)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 5 common methods. Extends TextraField for multi-line editing. Dropped ctor args are Skin-based constructor simplifications.

### TextraButton.scala
- **Original**: TextraButton.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getCell`, `getTextraLabelCell`, `getPrefHeight`, `getName`, `getClass`
- **Simplified methods**: Scene2d Table/Cell integration absent
- **Missing branches**: none in present code
- **Mechanism changes without tests**: Button state (checked, pressed, over, disabled) managed via manual fields rather than Actor state
- **Notes**: The core button functionality appears present but the scene2d Cell/Table layout methods are absent. `getPrefHeight` affects widget sizing.

### TextraCheckBox.scala
- **Original**: TextraCheckBox.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getImage`, `getImageCell`, `getPrefHeight`, `getStyle`, `getTextraLabel`, `isOver`, `newImage`, `add`
- **Simplified methods**: Checkbox layout (Image + Label in Table cells) simplified
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 3 common methods vs 11+ missing. The checkbox's visual layout (image cell + label cell arrangement) relies on scene2d Table infrastructure that is restructured.

### TextraDialog.scala
- **Original**: TextraDialog.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getButtonTable`, `getContentTable`, `newLabel`, `setStage`, `getStage`, `getWidth`, `addAction`, `sequence`
- **Missing inner classes/listeners**: `ChangeListener`, `FocusListener`, `InputListener` anonymous implementations for button binding and focus management
- **Simplified methods**: Dialog show/hide animation (addAction/sequence) absent
- **Missing branches**: Focus management (focusChanged, keyboardFocusChanged, scrollFocusChanged), keyboard navigation (keyDown), touch handling
- **Mechanism changes without tests**: Button binding system (binding map with Runnable callbacks) restructured
- **Notes**: 23 methods flagged as missing. The dialog has button management but lacks the scene2d lifecycle integration (stage management, focus listeners, action sequencing for show/hide animations).

### TextraLabel.scala
- **Original**: TextraLabel.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `setParent`, `setStage`, `setSuperHeight`, `setSuperWidth` (Actor lifecycle methods)
- **Simplified methods**: none in core rendering logic
- **Missing branches**: none in text display logic
- **Mechanism changes without tests**: Actor dimension management (setSuperWidth/Height) replaced with direct field access
- **Notes**: The core label rendering (layout, draw, text manipulation) appears complete. The missing methods are Actor lifecycle hooks for scene2d integration. 56 extra methods in port include the text manipulation API which is comprehensive.

### TextraListBox.scala
- **Original**: TextraListBox.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getKeyListener`, `getCullingArea`, `setSelection`, `getStage` + all InputListener methods (`keyDown`, `keyTyped`, `mouseMoved`, `touchDown`, `touchDragged`, `touchUp`, `exit`)
- **Simplified methods**: Input handling restructured from InputListener to direct methods
- **Missing branches**: Keyboard navigation (keyDown for arrows/pageUp/pageDown), mouse selection (touchDown/mouseMoved)
- **Mechanism changes without tests**: none
- **Notes**: 13 missing methods. The list selection and keyboard navigation infrastructure depends on InputListener which is restructured. Selection management is present but the event-driven input handling is absent.

### TextraSelectBox.scala
- **Original**: TextraSelectBox.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getClickListener`, `getSelection`, `showList`, `hideList`, `newScrollPane`, `fireChangeEvent`, `setStage`, `getStage`, `hide`, `show`, `isAscendantOf`, `removeActor`
- **Missing inner classes**: `SelectBoxScrollPane` (dropdown scroll pane, significant UI component)
- **Simplified methods**: Dropdown list display mechanism absent
- **Missing branches**: Click-to-open, scroll pane management, list dismissal
- **Mechanism changes without tests**: none
- **Notes**: 24 missing methods. The select box requires a dropdown scroll pane (`SelectBoxScrollPane`) which is a significant inner class. The show/hide list mechanism depends on stage overlay.

### TextraTooltip.scala
- **Original**: TextraTooltip.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `getContainer` (returns tooltip container for positioning)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 5 common methods. Simple tooltip wrapper. `getContainer` is used for tooltip positioning relative to actors.

### TextraWindow.scala
- **Original**: TextraWindow.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `getTitleTable`, `getResizeBorder`, `setResizeBorder`, `setMovable`, `setResizable`, `getParent`, `getStage`, `getTouchable`
- **Missing inner classes**: `InternalListener` (InputListener subclass handling drag, resize, keyboard)
- **Simplified methods**: Window drag/resize behavior absent
- **Missing branches**: All InputListener methods (touchDown/Up/Dragged, keyDown/Up/Typed, mouseMoved, scrolled) for window interaction
- **Mechanism changes without tests**: none
- **Notes**: 23 missing methods. The window's interactive behavior (dragging, resizing, keyboard focus) depends on `InternalListener` which handles all mouse/keyboard events. This is the core of what makes a window interactive vs. just a styled container.

---

## Effects Files (textra.effects.*)

### General Pattern

All 40 effect files share the same pattern in the compare output: methods like `paramAsFloat`, `paramAsBoolean`, `paramAsColor`, `calculateProgress`, `calculateFadeout` are flagged as "missing" but they are **inherited from the Effect base class**. These are false positives from the compare tool.

The actual methods each effect implements are:
- Constructor (parameter parsing)
- `onApply` (the effect logic)

Both are present in all effect files. The `IntFloatMap` and `FloatArray` references are type imports, not methods.

### WaveEffect.scala
- **Original**: WaveEffect.java
- **Prior status**: pass (empty notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: **Interpolation argument order bug**: Scala line 31 has `Interpolation.sine(1f, -1f, progress)` but Java line 82 has `Interpolation.sine.apply(-1, 1, progress)`. These produce opposite wave directions: Scala gives `1 - 2*curve(progress)`, Java gives `-1 + 2*curve(progress)`. The wave oscillates in the wrong direction.
- **Notes**: This is a behavioral bug. The text will wave downward when it should wave upward (and vice versa). All other logic is equivalent.

### ShakeEffect.scala
- **Original**: ShakeEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (FloatArray, calculateFadeout, paramAsFloat are inherited/type refs)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Verified body-level: offset buffer management (while loop adding 16 zeros vs Java's setSize), random offset generation, interpolation smoothing, fadeout application, offset storage. All equivalent.

### CannonEffect.scala
- **Original**: CannonEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Verified body-level: stretch interpolation, arc height calculation via `MathUtils.sin(PI * progress)`, shake phase with fadeout. Logic equivalent including the `sqrt(progress)` for eased entry.

### AttentionEffect.scala
- **Original**: AttentionEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (paramAsFloat inherited)
- **Notes**: Simple effect applying x-offset based on noise. 2 common methods verified.

### BlinkEffect.scala
- **Original**: BlinkEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (calculateProgress, paramAsColor, paramAsFloat inherited)
- **Notes**: Color blinking between two colors based on progress threshold. Logic verified equivalent.

### CarouselEffect.scala
- **Original**: CarouselEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Rotation effect using per-glyph time tracking. Logic verified.

### CrowdEffect.scala
- **Original**: CrowdEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (calculateFadeout, paramAsFloat inherited)
- **Notes**: Rotation based on noise with fadeout. Verified equivalent.

### EaseEffect.scala
- **Original**: EaseEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Vertical sliding with elastic/sine interpolation. Per-glyph time tracking. Verified equivalent.

### EmergeEffect.scala
- **Original**: EmergeEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Scale-in effect with interpolation. Verified equivalent.

### FadeEffect.scala
- **Original**: FadeEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Alpha/color fade between two states. Verified equivalent.

### GradientEffect.scala
- **Original**: GradientEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (calculateProgress, paramAsColor, paramAsFloat inherited)
- **Notes**: Color gradient cycling. Verified equivalent.

### HangEffect.scala
- **Original**: HangEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Vertical drop with bounce. Per-glyph time tracking. Verified equivalent.

### HeartbeatEffect.scala
- **Original**: HeartbeatEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Pulsing scale effect. Verified equivalent.

### HighlightEffect.scala
- **Original**: HighlightEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Color highlight cycling. Verified equivalent.

### InstantEffect.scala
- **Original**: InstantEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Simplest effect - instant reveal. 2 common methods. Verified equivalent.

### JoltEffect.scala
- **Original**: JoltEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Horizontal shake with color flash. Verified equivalent.

### JumpEffect.scala
- **Original**: JumpEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Vertical bouncing with sine interpolation. Verified equivalent.

### LinkEffect.scala
- **Original**: LinkEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: URL link effect. 2 common methods. Verified equivalent.

### MeetEffect.scala
- **Original**: MeetEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Glyphs converge from random positions. Per-glyph time tracking. Verified equivalent.

### OceanEffect.scala
- **Original**: OceanEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Horizontal wave using calculateProgress. Verified equivalent.

### PinchEffect.scala
- **Original**: PinchEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Scale pinch with elastic option. Verified equivalent.

### RainbowEffect.scala
- **Original**: RainbowEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: HSL-based rainbow color cycling. Verified equivalent.

### RotateEffect.scala
- **Original**: RotateEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (paramAsFloat inherited)
- **Notes**: Static rotation. 2 common methods. Verified equivalent.

### ScaleEffect.scala
- **Original**: ScaleEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (paramAsFloat inherited)
- **Notes**: Static scale. 2 common methods. Verified equivalent.

### ShootEffect.scala
- **Original**: ShootEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Underline shoot effect. Verified equivalent.

### ShrinkEffect.scala
- **Original**: ShrinkEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Scale-down with interpolation. Verified equivalent.

### SickEffect.scala
- **Original**: SickEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Wobbling vertical motion. Verified equivalent.

### SlamEffect.scala
- **Original**: SlamEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Drop with shake on landing. Verified equivalent.

### SlideEffect.scala
- **Original**: SlideEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Horizontal slide-in. Verified equivalent.

### SlipEffect.scala
- **Original**: SlipEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Horizontal slip with fadeout. Verified equivalent.

### SpinEffect.scala
- **Original**: SpinEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Rotation spin-in. Verified equivalent.

### SpiralEffect.scala
- **Original**: SpiralEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Spiral motion with per-glyph timing. Verified equivalent.

### SputterEffect.scala
- **Original**: SputterEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Noise-based jitter with fadeout. Verified equivalent.

### SquashEffect.scala
- **Original**: SquashEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Horizontal squash-in. Verified equivalent.

### StylistEffect.scala
- **Original**: StylistEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (CaseInsensitiveIntMap, paramAsBoolean are type ref/inherited)
- **Notes**: Style token effect. Verified equivalent.

### ThinkingEffect.scala
- **Original**: ThinkingEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Drift-away with fade. Per-glyph timing. Verified equivalent.

### ThrobEffect.scala
- **Original**: ThrobEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Horizontal throb. Verified equivalent.

### TriggerEffect.scala
- **Original**: TriggerEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Event trigger. 2 common methods. Verified equivalent.

### ZipperEffect.scala
- **Original**: ZipperEffect.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Vertical zipper. Verified equivalent.

---

## Utils Files

### BlockUtils.scala
- **Original**: BlockUtils.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 2 common methods plus extensive BOX_DRAWING constant arrays. All box-drawing segment constants (THIN_*, WIDE_*, TWIN_*) present.

### ColorUtils.scala
- **Original**: ColorUtils.java (841 lines)
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (IntArray is a type reference)
- **Simplified methods**: none identified
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 803 lines vs 841. Comprehensive color utility class. Methods verified present: `describe`, `lookupInColors`, `lerpColors`, `multiplyAlpha`, `lerpColorsMultiplyAlpha`, `hsl2rgb`, `rgb2hsl`, and numerous helpers. The 40 "extra" items are local variables and internal helpers.

### CaseInsensitiveIntMap.scala
- **See above** - MAJOR_ISSUES

### LZBCompression.scala
- **Original**: LZBCompression.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none (ByteArray is a type reference)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 3 common methods. LZB compression algorithm. Verified equivalent logic flow.

### LZBDecompression.scala
- **Original**: LZBDecompression.java
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `decompressFromByteArray` (accepts libGDX ByteArray, delegates to `decompressFromBytes`)
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: The missing method is a convenience wrapper that accepts `ByteArray` (libGDX collection type). Since SGE uses standard Scala arrays, this is a minor gap - callers can use `decompressFromBytes` directly.

### NoiseUtils.scala
- **Original**: NoiseUtils.java
- **Prior status**: pass (empty notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: 7 common methods. Noise generation utilities. All verified: `noise1D`, `noise1_5D` (3 overloads), `noise2D`, `hash`, and helpers. Body logic verified equivalent including bit manipulation for hash mixing.

### Palette.scala
- **Original**: Palette.java (1031 lines)
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `appendToKnownColors` (copies all Palette colors into libGDX's global Colors map)
- **Simplified methods**: none in present code
- **Missing branches**: none
- **Mechanism changes without tests**: Uses `HashMap[String, Int]` instead of `ObjectIntMap<String>` for NAMED map. The `appendToKnownColors` method interacts with libGDX's global `Colors` class which may not exist in SGE.
- **Notes**: 228 lines vs 1031. The line count difference is partly due to Java's verbose ObjectIntMap initialization. All 50+ color constants and 34 libGDX-compatible ALL_CAPS colors are present. The ALIASES map is populated. `addColor` method is present. The missing `appendToKnownColors` is specific to libGDX's global Colors registry.

### StringUtils.scala
- **Original**: StringUtils.java (531 lines)
- **Prior status**: pass (empty notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: `shuffleWords` (2 overloads), `decompressCategory`, `unsignedHexArray` (2 overloads)
- **Missing constants**: `LETTERS`, `LOWER_CASE_LETTERS`, `UPPER_CASE_LETTERS`, `WORD_CHARS`, `SPACE_CHARS` (BitSet constants using `decompressCategory`)
- **Simplified methods**: none in present methods
- **Missing branches**: none
- **Mechanism changes without tests**: The `decompressCategory` method and its BitSet constants depend on RegExodus's `Category` class. The Scala port doesn't use RegExodus.
- **Notes**: 258 lines vs 531. 15 common methods present and verified. The missing `shuffleWords` is used for text scrambling effects. The missing BitSet constants are used for character classification in text processing. The `unsignedHexArray` methods are formatting utilities.

---

## Consolidated Status Summary

### MAJOR_ISSUES (11 files)
1. **Font.scala** - Missing rotation drawGlyphs, BitmapFont constructors, static helpers, inner classes
2. **CaseInsensitiveIntMap.scala** - Reimplemented as HashMap wrapper, missing 5 inner classes, incorrect hashCode behavior
3. **KnownFonts.scala** - Missing shader methods, BitmapFont support, loadUnicodeAtlas
4. **BitmapFontSupport.scala** - Core loadStructuredJson functionality entirely absent
5. **TextraField.scala** - Missing scene2d integration (KeyRepeatTask, TextFieldClickListener, lifecycle hooks)
6. **ImageTextraButton.scala** - Missing Cell/Table layout methods
7. **TextraButton.scala** - Missing Cell/Table layout methods
8. **TextraCheckBox.scala** - Missing image/cell layout methods
9. **TextraDialog.scala** - Missing focus/action/stage lifecycle integration
10. **TextraListBox.scala** - Missing input listener methods
11. **TextraSelectBox.scala** - Missing SelectBoxScrollPane, show/hide list mechanism
12. **TextraWindow.scala** - Missing InternalListener for drag/resize/keyboard
13. **WaveEffect.scala** - Interpolation argument order bug (inverted wave direction)

### MINOR_ISSUES (9 files)
1. **Layout.scala** - Missing `insertLine` method
2. **Parser.scala** - Missing `parseColorMarkups` (but unused in Java too)
3. **TypingLabel.scala** - Missing Actor lifecycle methods (may be covered by property conversion)
4. **Styles.scala** - Missing `dispose()` on TextFieldStyle
5. **EmojiProcessor.scala** - RegExodus Replacer replaced with alternative
6. **FWSkin.scala** - Json loading restructured
7. **TextraTooltip.scala** - Missing `getContainer`
8. **LZBDecompression.scala** - Missing `decompressFromByteArray` convenience method
9. **Palette.scala** - Missing `appendToKnownColors`
10. **StringUtils.scala** - Missing shuffleWords, decompressCategory, unsignedHexArray, BitSet constants
11. **TextraLabel.scala** - Missing Actor lifecycle hooks
12. **ImageTypingButton.scala** - Minor constructor simplification

### PASS (42 files)
Effect.scala, Line.scala, TypingConfig.scala, TypingAdapter.scala, TypingListener.scala, ColorLookup.scala, InternalToken.scala, Justify.scala, TokenCategory.scala, TokenEntry.scala, FWSkinLoader.scala, TextraArea.scala, BlockUtils.scala, ColorUtils.scala, LZBCompression.scala, NoiseUtils.scala, and all 26 remaining effect files (AttentionEffect through ZipperEffect, excluding WaveEffect).

### NOT_PORTED (12 original Java files)
TypingButton.java, TypingCheckBox.java, TypingDialog.java, TypingListBox.java, TypingSelectBox.java, TypingTooltip.java, TypingWindow.java, TextureArrayCpuPolygonSpriteBatch.java, TextureArrayPolygonSpriteBatch.java, TextureArrayShaderCompiler.java, TextureArrayShaders.java, utils/LzmaUtils.java

### Key Behavioral Bugs Found
1. **WaveEffect.scala line 31**: `Interpolation.sine(1f, -1f, progress)` should be `Interpolation.sine(-1f, 1f, progress)` to match Java's wave direction
