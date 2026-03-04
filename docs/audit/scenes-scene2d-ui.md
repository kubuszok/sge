# Audit: sge.scenes.scene2d.ui

Audited: 35/35 files | Pass: 33 | Minor: 2 | Major: 0
Last updated: 2026-03-04

---

### Widget.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Widget.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Widget.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `setLayoutEnabled`, `validate`, `invalidate`, `invalidateHierarchy`, `pack`, `layout`, `needsLayout`, `sizeChanged`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `getMaxWidth`, `getMaxHeight`.
**Convention changes**: null -> Nullable; split packages; no return statements
**TODOs**: None
**Issues**: None

---

### WidgetGroup.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/WidgetGroup.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/WidgetGroup.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `setLayoutEnabled`, `validate`, `invalidate`, `invalidateHierarchy`, `pack`, `layout`, `needsLayout`, `sizeChanged`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `getMaxWidth`, `getMaxHeight`, `childrenChanged`.
**Convention changes**: null -> Nullable; split packages; boundary/break for layout retry loop
**TODOs**: None
**Issues**: None

---

### Styleable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Styleable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Styleable.java` |
| Status | pass |
| Tested | No |

**Completeness**: Exact match. `setStyle`, `getStyle` methods. Java `Styleable<S>` interface -> Scala `Styleable[S]` trait.
**Convention changes**: Split packages; braces on trait
**TODOs**: None
**Issues**: None

---

### Value.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Value.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Value.java` |
| Status | pass |
| Tested | No |

**Completeness**: All abstract class methods and static fields present: `get`, `zero`, `minWidth`, `minHeight`, `prefWidth`, `prefHeight`, `maxWidth`, `maxHeight`, `percentWidth`, `percentHeight`, `Fixed` inner class with `valueOf` cache.
**Convention changes**: abstract class -> SAM trait (b336fb6); null -> Nullable; `Fixed.valueOf` cache uses Nullable[Fixed]
**TODOs**: None
**Issues**: None

---

### ParticleEffectActor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/ParticleEffectActor.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/ParticleEffectActor.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors, `draw`, `act`, `start`, `isRunning`, `allowCompletion`, `getEffect`, `setPosition`, `setAutoRemove`, `isAutoRemove`, `setResetOnStart`, `getResetOnStart`.
**Convention changes**: `Disposable` -> `AutoCloseable`; `(using Sge)` added to `act()` and one constructor; split packages
**TODOs**: None
**Issues**: None

---

### Stack.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Stack.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Stack.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors, `invalidate`, `layout`, `add`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `getMaxWidth`, `getMaxHeight`.
**Convention changes**: Split packages; null -> Nullable; varargs constructor
**TODOs**: None
**Issues**: None

---

### Image.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Image.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Image.java` |
| Status | pass |
| Tested | No |

**Completeness**: All major methods present including Skin constructor and `setDrawable(Skin, String)`.
**Convention changes**: null -> Nullable; `Scaling` enum -> opaque type/trait; split packages
**TODOs**: None
**Issues**: None

---

### Label.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Label.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Label.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `setStyle`, `getStyle`, `setText`, `getText`, `textEquals`, `getGlyphLayout`, `setWrap`, `getWrap`, `setAlignment`, `getAlignment`, `setFontScale`, `getFontScaleX`, `getFontScaleY`, `setFontScaleX`, `setFontScaleY`, `setEllipsis`, `layout`, `draw`, `getPrefWidth`, `getPrefHeight`, `toString`.
**Convention changes**: null -> Nullable; `DynamicArray[Char]` instead of `CharArray`; split packages
**TODOs**: None
**Issues**: None

---

### Button.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Button.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Button.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods present including `setChecked`, `toggle`, `isPressed`, `isOver`, `getClickListener`, `isDisabled`, `setDisabled`, `setProgrammaticChangeEvents`, `getProgrammaticChangeEvents`, `setStyle`, `getStyle`, `getButtonGroup`, `draw`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`.
**Renames**: `isChecked()` Java getter -> `getIsChecked` (name collision with `isChecked` field)
**Convention changes**: null -> Nullable; split packages; `Disableable` trait
**TODOs**: `// TODO: requestRendering - draw doesn't have 'using Sge' context`
**Issues**: `requestRendering` TODO in `draw()` method. Minor — needs architectural change to add `using Sge` to draw.

---

### TextButton.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/TextButton.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/TextButton.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setStyle`, `getStyle`, `newLabel`, `getLabel`, `getLabelCell`, `setText`, `getText`, `toString`, `draw`.
**Convention changes**: null -> Nullable; split packages
**TODOs**: None
**Issues**: None

---

### CheckBox.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/CheckBox.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/CheckBox.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `getStyle`, `draw`, `getImage`, `getImageCell`, `getLabel`, `getLabelCell`, `getImageDrawable`.
**Convention changes**: null -> Nullable; split packages
**TODOs**: None
**Issues**: None

---

### ImageButton.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/ImageButton.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/ImageButton.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `newImage`, `setStyle`, `getStyle`, `getImageDrawable`, `updateImage`, `draw`, `getImage`, `getImageCell`, `toString`.
**Convention changes**: null -> Nullable; split packages
**TODOs**: None
**Issues**: None

---

### ImageTextButton.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/ImageTextButton.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/ImageTextButton.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `newImage`, `newLabel`, `setStyle`, `getStyle`, `getImageDrawable`, `updateImage`, `draw`, `getImage`, `getImageCell`, `getLabel`, `getLabelCell`, `setText`, `getText`, `toString`.
**Convention changes**: null -> Nullable; split packages
**TODOs**: None
**Issues**: None

---

### ButtonGroup.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/ButtonGroup.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/ButtonGroup.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `add`, `remove`, `clear`, `setMinCheckCount`, `setMaxCheckCount`, `setUncheckLast`, `canCheck`, `getChecked`, `getCheckedIndex`, `getAllChecked`, `getButtons`, `setButtons`.
**Convention changes**: null -> Nullable; `DynamicArray` with `MkArray.anyRef` cast; split packages; varargs constructor
**TODOs**: None
**Issues**: None

---

### Touchpad.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Touchpad.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Touchpad.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors, `setStyle`, `getStyle`, `draw`, `layout`, `getPrefWidth`, `getPrefHeight`, `isTouched`, `setResetOnTouchUp`, `getKnobX`, `getKnobY`, `getKnobPercentX`, `getKnobPercentY`, `setDeadzone`.
**Convention changes**: null -> Nullable; `(using Sge)` context; split packages
**TODOs**: None
**Issues**: None

---

### Container.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Container.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Container.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present. Large builder API with `size`, `width`, `height`, `minSize`, `minWidth`, `minHeight`, `prefSize`, `prefWidth`, `prefHeight`, `maxSize`, `maxWidth`, `maxHeight`, `pad`, `padTop/Left/Bottom/Right`, `fill`, `fillX`, `fillY`, `align`, `center`, `top`, `left`, `bottom`, `right`, `background`, `clip`, `setActor`, `getActor`, `removeActor`, `layout`, `draw`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `getMaxWidth`, `getMaxHeight`.
**Convention changes**: null -> Nullable; split packages; Align opaque type
**TODOs**: None
**Issues**: None

---

### ProgressBar.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/ProgressBar.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/ProgressBar.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setStyle`, `getStyle`, `draw`, `getMinValue`, `getMaxValue`, `getValue`, `getVisualValue`, `getPercent`, `getVisualPercent`, `setValue`, `setRange`, `setStepSize`, `getStepSize`, `getPrefWidth`, `getPrefHeight`, `setVisualInterpolation`, `setAnimateDuration`, `setAnimateInterpolation`, `isVertical`, `setRound`, `isDisabled`, `setDisabled`, `getBackgroundDrawable`, `getKnobDrawable`, `getKnobBeforeDrawable`, `getKnobAfterDrawable`.
**Convention changes**: null -> Nullable; `(using Sge)` context; split packages; Skin constructors present (not commented)
**TODOs**: None
**Issues**: None

---

### Slider.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Slider.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Slider.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `getSliderStyle`, `isOver`, `getBackgroundDrawable`, `getKnobDrawable`, `getKnobBeforeDrawable`, `getKnobAfterDrawable`, `calculatePositionAndValue`, `snap`, `setSnapToValues`, `getSnapToValues`, `getSnapToValuesThreshold`, `isDragging`, `setButton`, `setVisualInterpolationInverse`, `setVisualPercent`.
**Convention changes**: null -> Nullable; `(using Sge)` with `Sge().input.isKeyPressed`; `Interpolation.linear` SAM trait; split packages
**TODOs**: None
**Issues**: None

---

### Tooltip.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Tooltip.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Tooltip.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors, `enter`, `exit`, `setActor`, `getActor`, `setInstant`, `setAlways`, `setTouchable`, `getContainer`, `getManager`.
**Convention changes**: null -> Nullable; `(using Sge)` context; split packages
**TODOs**: None
**Issues**: None

---

### TextTooltip.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/TextTooltip.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/TextTooltip.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `newLabel`, `setStyle`, `getStyle`. `TextTooltipStyle` inner class complete.
**Convention changes**: null -> Nullable; `(using Sge)` context; split packages
**TODOs**: None
**Issues**: None

---

### TooltipManager.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/TooltipManager.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/TooltipManager.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods and fields present: `getInstance`, `instant`, `showAction`, `hideAction`, `initialTime`, `resetTime`, `subsequentTime`, `offsetX`, `offsetY`, `edgeDistance`, `maxWidth`, `animations`, `touchDown`, `enter`, `hide`, `resetTask`.
**Convention changes**: Singleton pattern adapted with Nullable; `Gdx.files` check -> `_defaults.isEmpty` + `_creatingDefaults` guard; split packages
**TODOs**: None
**Issues**: None

---

### Dialog.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Dialog.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Dialog.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `key`, `button` (multiple overloads), `show`, `hide`, `setObject`, `result`, `cancel`, `getContentTable`, `getButtonTable`, `getTitleLabel`, `getTitleTable`.
**Convention changes**: null -> Nullable; `(using Sge)` context; `scala.collection.mutable.Map` for values map; `ObjectMap` -> `mutable.Map`; split packages
**TODOs**: None
**Issues**: None

---

### Window.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Window.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Window.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setStyle`, `getStyle`, `keepWithinStageMethod` (renamed from `keepWithinStage()` to avoid field collision), `draw`, `act`, `setMovable`, `isMovable`, `setModal`, `isModal`, `setResizable`, `isResizable`, `setResizeBorder`, `isDragging`, `isKeepWithinStage`, `setKeepWithinStage`, `getPrefWidth`, `getPrefHeight`, `getTitleLabel`, `getTitleTable`.
**Renames**: `keepWithinStage()` method -> `keepWithinStageMethod()` (avoids collision with `keepWithinStage` Boolean field)
**Convention changes**: null -> Nullable; `(using Sge)` context; split packages; boundary/break
**TODOs**: None
**Issues**: None

---

### SplitPane.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/SplitPane.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/SplitPane.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setStyle`, `getStyle`, `layout`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `setVertical`, `isVertical`, `draw`, `setSplitAmount`, `getSplitAmount`, `clampSplitAmount`, `getMinSplitAmount`, `setMinSplitAmount`, `getMaxSplitAmount`, `setMaxSplitAmount`, `setFirstWidget`, `setSecondWidget`, `addActor` (throws), `addActorAt` (throws), `addActorBefore` (throws), `removeActor`, `removeActorAt`, `isCursorOverHandle`.
**Convention changes**: null -> Nullable; `(using Sge)` context; `GdxRuntimeException` -> `SgeError.GraphicsError`; boundary/break in `touchDown`; split packages
**TODOs**: None
**Issues**: None

---

### SgeList.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/SgeList.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/List.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setStyle`, `getStyle`, `layout`, `draw`, `drawSelection`, `drawBackground`, `drawItem`, `getSelection`, `setSelection`, `getSelected`, `setSelected`, `getSelectedIndex`, `setSelectedIndex`, `getOverItem`, `getPressedItem`, `getItemAt`, `getItemIndexAt`, `setItems`, `clearItems`, `getItems`, `getItemHeight`, `getPrefWidth`, `getPrefHeight`, `itemToString` (renamed from `toString(T)` to avoid conflict), `setCullingArea`, `getCullingArea`, `setAlignment`, `getAlignment`, `setTypeToSelect`, `getKeyListener`.
**Renames**: Class `List<T>` -> `SgeList[T]` (avoids clash with `scala.List`); `toString(T)` -> `itemToString(T)`
**Convention changes**: null -> Nullable; `DynamicArray` with `MkArray.anyRef` cast; `(using Sge)` context; `Cullable` trait; split packages
**Missing**: Varargs `setItems(T*)` overload (Java has `setItems(T... newItems)`, Scala only has `setItems(DynamicArray[T])`)
**TODOs**: None
**Issues**: None. Missing varargs is acceptable since DynamicArray is the primary collection type.

---

### SelectBox.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/SelectBox.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/SelectBox.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `newScrollPane`, `setMaxListCount`, `getMaxListCount`, `setStyle`, `getStyle`, `setItems` (varargs + DynamicArray), `clearItems`, `getItems`, `layout`, `getBackgroundDrawable`, `getFontColor`, `draw`, `drawItem`, `setAlignment`, `getSelection`, `getSelected`, `setSelected`, `getSelectedIndex`, `setSelectedIndex`, `setSelectedPrefWidth`, `getSelectedPrefWidth`, `getMaxSelectedPrefWidth`, `setDisabled`, `isDisabled`, `getPrefWidth`, `getPrefHeight`, `showList`/`showScrollPane`, `hideList`/`hideScrollPane`, `getList`, `setScrollingDisabled`, `getScrollPane`, `isOver`, `getClickListener`, `onShow`, `onHide`.
Inner class `SelectBoxScrollPane` complete with `show`, `hide`, `draw`, `act`, `getList`, `getSelectBox`.
**Convention changes**: null -> Nullable; `(using Sge)` context; `List` -> `SgeList`; `DynamicArray` + `MkArray.anyRef` cast; split packages; boundary/break
**TODOs**: None
**Issues**: None

---

### Cell.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Cell.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Cell.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present. Full builder API: `size`, `width`, `height`, `minSize`, `minWidth`, `minHeight`, `prefSize`, `prefWidth`, `prefHeight`, `maxSize`, `maxWidth`, `maxHeight`, `space`, `spaceTop/Left/Bottom/Right`, `pad`, `padTop/Left/Bottom/Right`, `fill`, `fillX`, `fillY`, `align`, `center`, `top`, `left`, `bottom`, `right`, `grow`, `growX`, `growY`, `expand`, `expandX`, `expandY`, `colspan`, `uniform`, `uniformX`, `uniformY`, `setActorBounds`, getters/setters for bounds, `getColumn`, `getRow`, value getters, `clear`, `reset`, `set`, `merge`, `toString`, `defaults`.
**Convention changes**: null -> Nullable; `Integer`/`Float`/`Boolean` Java boxed types -> `Nullable[Int]`/`Nullable[Float]`/`Nullable[Boolean]`; `int align` -> `Nullable[Align]`; `Cell.defaults()` pattern adapted; split packages
**TODOs**: None
**Issues**: None. The `defaults()` singleton pattern uses `_creatingDefaults` guard instead of `Gdx.files` check.

---

### Table.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Table.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Table.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present. Core API: `draw`, `setBackground` (Drawable + String), `background`, `getBackground`, `hit`, `clip`, `setClip`, `getClip`, `invalidate`, `add` (Actor, varargs, no-arg, CharSequence x4), `stack`, `removeActor`, `removeActorAt`, `clearChildren`, `reset`, `row`, `columnDefaults`, `getCell`, `getCells`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `defaults`, `pad`/`padTop`/`padLeft`/`padBottom`/`padRight` (Value + Float overloads), `align`, `center`, `top`, `left`, `bottom`, `right`, `setDebug`, `debug`, `debugAll`, `debugTable`, `debugCell`, `debugActor`, `getTableDebug`, getters for pad values, `getAlign`, `getRow`, `setRound`, `getRows`, `getColumns`, `getRowHeight`, `getRowMinHeight`, `getRowPrefHeight`, `getColumnWidth`, `getColumnMinWidth`, `getColumnPrefWidth`, `layout`, `drawDebug`, `getSkin`, `setSkin`.
**Convention changes**: null -> Nullable; `Align` opaque type; `DynamicArray` for cells; split packages; boundary/break; `Debug` enum in companion object; `@targetName("addLabel")` for `add(Nullable[CharSequence])` to avoid erasure conflict with `add(Nullable[Actor])`
**TODOs**: None
**Issues**: None

---

### HorizontalGroup.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/HorizontalGroup.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/HorizontalGroup.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `invalidate`, `layout`, `getPrefWidth`, `getPrefHeight`, `getRows`, `setRound`, `reverse` (0-arg + boolean), `getReverse`, `wrapReverse` (0-arg + boolean), `getWrapReverse`, `space`, `getSpace`, `wrapSpace`, `getWrapSpace`, `pad` (1-arg + 4-arg), `padTop`, `padLeft`, `padBottom`, `padRight`, `getPadTop`, `getPadLeft`, `getPadBottom`, `getPadRight`, `align`, `center`, `top`, `left`, `bottom`, `right`, `getAlign`, `fill` (0-arg + float), `getFill`, `expand` (0-arg + boolean), `getExpand`, `grow`, `wrap` (0-arg + boolean), `getWrap`, `rowAlign`, `rowCenter`, `rowTop`, `rowLeft`, `rowBottom`, `rowRight`, `drawDebugBounds`.
**Convention changes**: `Align` opaque type with `.isLeft`/`.isRight`/`.isTop`/`.isBottom`; `FloatArray` -> `DynamicArray[Float]`; split packages
**TODOs**: `// TODO: uncomment when ShapeRenderer.rectangle with transform params is ported` (drawDebugBounds uses simplified rectangle call)
**Issues**: None significant. The `drawDebugBounds` uses a simplified rectangle call without transform parameters, noted with TODO.

---

### VerticalGroup.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/VerticalGroup.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/VerticalGroup.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `invalidate`, `layout`, `getPrefWidth`, `getPrefHeight`, `getColumns`, `setRound`, `reverse` (0-arg + boolean), `getReverse`, `space`, `getSpace`, `wrapSpace`, `getWrapSpace`, `pad` (1-arg + 4-arg), `padTop`, `padLeft`, `padBottom`, `padRight`, `getPadTop`, `getPadLeft`, `getPadBottom`, `getPadRight`, `align`, `center`, `top`, `left`, `bottom`, `right`, `getAlign`, `fill` (0-arg + float), `getFill`, `expand` (0-arg + boolean), `getExpand`, `grow`, `wrap` (0-arg + boolean), `getWrap`, `columnAlign`, `columnCenter`, `columnTop`, `columnLeft`, `columnBottom`, `columnRight`, `drawDebugBounds`.
**Convention changes**: `Align` opaque type; `Nullable[DynamicArray[Float]]` for columnSizes (lazy init); split packages
**TODOs**: `// TODO: uncomment when ShapeRenderer.rectangle with transform params is ported` (drawDebugBounds)
**Issues**: None significant. Same drawDebugBounds TODO as HorizontalGroup.

---

### TextField.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/TextField.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/TextField.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setMaxLength`, `getMaxLength`, `setAutocompleteOptions`, `setKeyboardType`, `setPreventAutoCorrection`, `setOnlyFontChars`, `setStyle`, `getStyle`, `draw`, `copy`, `cut`, `next`, `getDefaultInputListener`, `setTextFieldListener`, `setTextFieldFilter`, `getTextFieldFilter`, `setFocusTraversal`, `getFocusTraversal`, `getMessageText`, `setMessageText`, `appendText`, `setText`, `getText`, `setProgrammaticChangeEvents`, `getProgrammaticChangeEvents`, `getSelectionStart`, `getSelection`, `setSelection`, `selectAll`, `clearSelection`, `setCursorPosition`, `getCursorPosition`, `getOnscreenKeyboard`, `setOnscreenKeyboard`, `setClipboard`, `getPrefWidth`, `getPrefHeight`, `setAlignment`, `getAlignment`, `setPasswordMode`, `isPasswordMode`, `setPasswordCharacter`, `setBlinkTime`, `setDisabled`, `isDisabled`.
Inner types: `TextFieldListener`, `TextFieldFilter` (with `DigitsOnlyFilter`), `OnscreenKeyboard`, `DefaultOnscreenKeyboard`, `TextFieldStyle`, `KeyRepeatTask`.
**Convention changes**: null -> Nullable; `(using Sge)` context; boundary/break; split packages; `Clipboard` from `sge`
**TODOs**: None
**Issues**: None

---

### TextArea.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/TextArea.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/TextArea.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setStyle`, `setPrefRows`, `getPrefHeight`, `getLines`, `newLineAtEnd`, `moveCursorLine`, `setSelection`, `getCursorLine`, `getFirstLineShowing`, `getLinesShowing`, `getCursorX`, `getCursorY`.
**Convention changes**: null -> Nullable; `(using Sge)` context; boundary/break; split packages
**TODOs**: None
**Issues**: None

---

### ScrollPane.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/ScrollPane.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/ScrollPane.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setScrollbarsVisible`, `cancelTouchFocusMethod` (renamed from `cancelTouchFocus()` to avoid field collision), `cancel`, `clamp`, `setStyle`, `getStyle`, `act`, `layout`, `draw`, `fling`, `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `setActor`, `getActor`, `setWidget`/`getWidget` (aliases), `addActor`, `addActorAt`, `addActorBefore`, `addActorAfter`, `removeActor`, `removeActorAt`, `hit`, `setScrollX`, `getScrollX`, `setScrollY`, `getScrollY`, `updateVisualScroll`, `getVisualScrollX`, `getVisualScrollY`, `getVisualScrollPercentX`, `getVisualScrollPercentY`, `getScrollPercentX`, `setScrollPercentX`, `getScrollPercentY`, `setScrollPercentY`, `setFlickScroll`, `setFlickScrollTapSquareSize`, `scrollTo` (2 overloads), `getMaxX`, `getMaxY`, `getScrollBarHeight`, `getScrollBarWidth`, `getScrollWidth`, `getScrollHeight`, `isScrollX`, `isScrollY`, `setScrollingDisabled`, `isScrollingDisabledX`, `isScrollingDisabledY`, `isLeftEdge`, `isRightEdge`, `isTopEdge`, `isBottomEdge`, `isDragging`, `isPanning`, `isFlinging`, `setVelocityX`, `getVelocityX`, `setVelocityY`, `getVelocityY`, `setOverscroll`, `setupOverscroll`, `getOverscrollDistance`, `setForceScroll`, `isForceScrollX`, `isForceScrollY`, `setFlingTime`, `setClamp`, `setScrollBarPositions`, `setFadeScrollBars`, `setupFadeScrollBars`, `getFadeScrollBars`, `setScrollBarTouch`, `setSmoothScrolling`, `setScrollbarsOnTop`, `getVariableSizeKnobs`, `setVariableSizeKnobs`, `setCancelTouchFocus`, `drawDebug`. `ScrollPaneStyle` inner class complete.
**Renames**: `cancelTouchFocus()` method -> `cancelTouchFocusMethod()` (avoids collision with `cancelTouchFocus` Boolean field)
**Convention changes**: null -> Nullable; `(using Sge)` context; `Interpolation.fade` SAM; split packages; boundary/break
**TODOs**: None
**Issues**: None

---

### Skin.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Skin.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Skin.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All core methods present: constructors, `load`, `addRegions`, `add`, `remove`, `get`, `optional`, `has`, `getAll`, `getColor`, `getFont`, `getRegion`, `getRegions`, `getTiledDrawable`, `getPatch`, `getSprite`, `getDrawable`, `find`, `newDrawable` (multiple overloads), `scale`, `setScale`, `setEnabled` (renamed from `setEnabledReflection`), `getAtlas`, `close` (renamed from `dispose`).
Inner class `TintedDrawable` present.
**Missing**: `getJsonClassTags()` method (Java returns `ObjectMap<String, Class>` for custom JSON class tags); some JSON serialization plumbing differs (Scala uses a direct `readValue`/`readStyleObject` approach instead of libGDX Json serializers).
**Convention changes**: null -> Nullable; `Disposable` -> `AutoCloseable`; `ObjectMap` -> `scala.collection.mutable.Map`; Java reflection for style field setting; `GdxRuntimeException` -> `SgeError.InvalidInput`; split packages; boundary/break
**TODOs**: Style field setting uses Java reflection (`getDeclaredFields`, `getDeclaredField`); needs compile-time approach for JS/Native targets.
**Issues**: JSON loading implementation differs architecturally from Java (uses direct parsing instead of Json serializers). `getJsonClassTags` not ported. Java reflection dependency for style field setting is a known cross-platform concern. Minor since core functionality works on JVM.

---

### Tree.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/ui/Tree.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/ui/Tree.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors (including Skin), `setStyle`, `getStyle`, `add`, `insert`, `remove`, `clearChildren`, `invalidate`, `layout`, `draw`, `getNodeAt`, `selectAll`, `getSelection`, `getSelectedNode`, `getSelectedValue`, `getRootNodes`, `getNodes`, `updateRootNodes`, `getOverNode`, `getOverValue`, `setOverNode`, `setPadding` (2 overloads), `setIndentSpacing`, `getIndentSpacing`, `setYSpacing`, `getYSpacing`, `setIconSpacing`, `getPrefWidth`, `getPrefHeight`, `findExpandedValues`, `restoreExpandedValues`, `findNode`, `collapseAll`, `expandAll`, `getClickListener`.
Inner class `Node` complete with: constructors, `setExpanded`, `add`, `addAll`, `insert`, `remove` (self + child), `clearChildren`, `getTree`, `setActor`, `getActor`, `isExpanded`, `getChildren`, `hasChildren`, `updateChildren`, `getParent`, `setIcon`, `getValue`, `setValue`, `getIcon`, `getLevel`, `findNode`, `collapseAll`, `expandAll`, `expandTo`, `isSelectable`, `setSelectable`, `findExpandedValues`, `restoreExpandedValues`, `getHeight`, `isAscendantOf`, `isDescendantOf`.
`TreeStyle` inner class complete.
**Convention changes**: null -> Nullable; `DynamicArray` with `MkArray.anyRef` cast; `(using Sge)` context; `Selection[N]` custom; boundary/break; split packages
**TODOs**: None
**Issues**: None

---

## Summary

### Convention Compliance (all 35 files)

| Convention | Status |
|------------|--------|
| Split packages (`package sge / scenes / scene2d / ui`) | All files compliant |
| No `return` (use `boundary`/`break`) | All files compliant |
| No raw `null` (use `Nullable[A]`) | All files compliant |
| Braces on all trait/class/enum/method | All files compliant |
| No `scala.Enumeration` | All files compliant (enums use Scala 3 enum or opaque types) |
| `AutoCloseable` instead of `Disposable` | Compliant where applicable (Skin) |
| `(using Sge)` instead of `Gdx` static | Compliant where needed |

### Recurring Patterns

1. **Skin constructors**: All UI classes now have working Skin constructors.

2. **Java `List<T>` renamed to `SgeList[T]`**: Avoids clash with `scala.List`. Method `toString(T)` renamed to `itemToString(T)`.

3. **Field name collisions resolved by method rename**: `keepWithinStage()` -> `keepWithinStageMethod()` in Window; `cancelTouchFocus()` -> `cancelTouchFocusMethod()` in ScrollPane; `isChecked()` -> `getIsChecked` in Button.

4. **`DynamicArray` with `MkArray.anyRef` cast**: Used in ButtonGroup, SgeList, SelectBox, Tree for generic `T <: AnyRef` collections.

5. **Align opaque type**: Java `int align` fields use `Align` opaque type with `.isLeft`/`.isRight`/`.isTop`/`.isBottom` accessor methods instead of bitwise `(align & Align.left) != 0` checks.

6. **`Interpolation` and `Value` as SAM traits**: Following the b336fb6 refactor.

### Files by Status

**Pass (33)**: Widget, WidgetGroup, Styleable, Value, ParticleEffectActor, Stack, ButtonGroup, Touchpad, Container, ProgressBar, Slider, Tooltip, TextTooltip, TooltipManager, Dialog, Window, SplitPane, SgeList, SelectBox, Cell, HorizontalGroup, VerticalGroup, TextField, TextArea, ScrollPane, Tree, Image, Label, TextButton, CheckBox, ImageButton, ImageTextButton, Table

**Minor Issues (2)**: Button (requestRendering TODO in draw — needs `using Sge` context), Skin (reflection dependency for style field setting, missing getJsonClassTags)
