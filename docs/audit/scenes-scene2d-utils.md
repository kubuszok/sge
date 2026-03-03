# Audit: sge.scenes.scene2d.utils

Audited: 21/21 files | Pass: 19 | Minor: 1 | Major: 1
Last updated: 2026-03-03

---

### ActorGestureListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/ActorGestureListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/ActorGestureListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 12 public methods accounted for: `handle`, `touchDown`, `touchUp`, `tap`, `longPress`, `fling`, `pan`, `panStop`, `zoom`, `pinch`, `getGestureDetector`, `getTouchDownTarget`. Both constructors present.
**Renames**: `Gdx` global -> `(implicit sge: Sge)` parameter
**Convention changes**: null -> Nullable[A]; split packages; no return statements; `amount.sub(v)` -> `amount.-(v)`
**Idiom**: Nullable foreach/fold replaces raw null access in GestureAdapter inner callbacks; `stageToLocalAmount` takes explicit actor param instead of accessing mutable outer field
**TODOs**: None
**Issues**: None

---

### ArraySelection.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/ArraySelection.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/ArraySelection.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 public methods accounted for: `choose`, `changed`, `getRangeSelect`, `setRangeSelect`, `validate`. Constructor faithfully ported.
**Renames**: `Array<T>` -> `DynamicArray[T]`; null rangeStart -> `Nullable[T]`
**Convention changes**: null -> Nullable; `choose` requires `(using Sge)` for UIUtils calls; no return; split packages
**Idiom**: `validate` uses collect-then-remove pattern instead of `iter.remove()` to avoid concurrent modification; `DynamicArray.createWithMk` for generic T collection
**TODOs**: None
**Issues**: None

---

### BaseDrawable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/BaseDrawable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/BaseDrawable.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 17 public methods accounted for: `draw`, 6 getter/setter pairs for leftWidth/rightWidth/topHeight/bottomHeight/minWidth/minHeight, `getName`, `setName`, `toString`. Both constructors present.
**Renames**: `ClassReflection.getSimpleName` -> `getClass.getSimpleName`
**Convention changes**: `@Null String` -> `Nullable[String]`; split packages
**Idiom**: `toString` uses `name.getOrElse(getClass.getSimpleName)` instead of null check
**TODOs**: None
**Issues**: None

---

### ChangeListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/ChangeListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/ChangeListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 2 public members: `handle`, `changed`. Inner class `ChangeEvent` present.
**Renames**: None
**Convention changes**: instanceof -> pattern matching; static inner class -> companion object nested class; split packages
**TODOs**: None
**Issues**: None

---

### ClickListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/ClickListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/ClickListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 22 public methods accounted for: `touchDown`, `touchDragged`, `touchUp`, `enter`, `exit`, `cancel`, `clicked`, `isOver(Actor,Float,Float)`, `inTapSquare(Float,Float)`, `inTapSquare()`, `invalidateTapSquare`, `isPressed`, `isVisualPressed`, `setVisualPressed`, `isOver`, `setTapSquareSize`, `getTapSquareSize`, `setTapCountInterval`, `getTapCount`, `setTapCount`, `getTouchDownX`, `getTouchDownY`, `getPressedButton`, `getPressedPointer`, `getButton`, `setButton`. Static `visualPressedDuration` in companion object.
**Renames**: `@Null Actor` -> `Nullable[Actor]` for enter/exit params
**Convention changes**: No return statements; actor.hit returns Nullable -> fold; split packages
**TODOs**: None
**Issues**: None

---

### Cullable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/Cullable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/Cullable.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single method `setCullingArea` faithfully ported.
**Renames**: `@Null Rectangle` -> `Nullable[Rectangle]`
**Convention changes**: Java interface -> Scala trait; split packages
**TODOs**: None
**Issues**: None

---

### Disableable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/Disableable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/Disableable.java` |
| Status | pass |
| Tested | No |

**Completeness**: Both methods `setDisabled` and `isDisabled` faithfully ported.
**Convention changes**: Java interface -> Scala trait; split packages
**TODOs**: None
**Issues**: None

---

### DragAndDrop.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/DragAndDrop.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/DragAndDrop.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 18 public methods accounted for: `addSource`, `removeSource`, `addTarget`, `removeTarget`, `clear`, `cancelTouchFocusExcept`, `setTapSquareSize`, `setButton`, `setDragActorPosition`, `setTouchOffset`, `isDragging`, `getDragActor`, `getDragPayload`, `getDragSource`, `setDragTime`, `getDragTime`, `isDragValid`, `setCancelTouchFocus`, `setKeepWithinStage`. Inner classes `Source`, `Target`, `Payload` all present with full method sets.
**Renames**: `ObjectMap` -> `MutableMap`; `Array` -> `DynamicArray`; `@Null` fields -> `Nullable`; `Payload.object` -> `Payload.obj` (reserved keyword)
**Convention changes**: boundary/break for loop-with-break; no return; split packages; static inner classes -> companion object
**Idiom**: Extensive Nullable foreach/fold chains for null-safe drag/drop logic; Source actor null-check in Java constructor not needed (Scala val param cannot be null)
**TODOs**: None
**Issues**: None

---

### DragListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/DragListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/DragListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 20 public methods accounted for: `touchDown`, `touchDragged`, `touchUp`, `dragStart`, `drag`, `dragStop`, `cancel`, `isDragging`, `setTapSquareSize`, `getTapSquareSize`, `getTouchDownX`, `getTouchDownY`, `getStageTouchDownX`, `getStageTouchDownY`, `getDragStartX`, `setDragStartX`, `getDragStartY`, `setDragStartY`, `getDragX`, `getDragY`, `getDragDistance`, `getDeltaX`, `getDeltaY`, `getButton`, `setButton`.
**Renames**: `dragX`/`dragY` -> `_dragX`/`_dragY` (avoid shadowing)
**Convention changes**: No return; `getDragDistance` uses `Math.sqrt` instead of `Vector2.len` (equivalent); split packages
**TODOs**: None
**Issues**: None

---

### DragScrollListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/DragScrollListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/DragScrollListener.java` |
| Status | **major_issues** |
| Tested | No |

**Completeness**: INCOMPLETE. Constructor taking `ScrollPane`, `drag`, `dragStop`, `isAbove`, `isBelow`, `scroll` methods all commented out. Only `setup`, `getScrollPixels`, `setPadding` are active. `scrollUp`/`scrollDown` Timer.Task fields missing.
**Renames**: Static tmpCoords -> companion object val
**Convention changes**: Split packages
**TODOs**: "TODO: uncomment when ScrollPane is ported" (3 occurrences)
**Issues**: **Major** -- class is a skeleton. Depends on ScrollPane and Timer which are not yet ported. The no-arg constructor is a placeholder; Java has only a `ScrollPane` constructor. Most of the class's core behavior is missing.

---

### Drawable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/Drawable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/Drawable.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 15 methods accounted for: `draw`, 4 getter/setter pairs for leftWidth/rightWidth/topHeight/bottomHeight, 3 `setPadding` overloads, `getMinWidth`, `setMinWidth`, `getMinHeight`, `setMinHeight`, `setMinSize`.
**Convention changes**: Java interface with default methods -> Scala trait with def implementations; split packages
**TODOs**: None
**Issues**: None

---

### FocusListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/FocusListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/FocusListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 public methods: `handle`, `keyboardFocusChanged`, `scrollFocusChanged`. Inner `FocusEvent` class with all 7 methods (`reset`, `isFocused`, `setFocused`, `getType`, `setType`, `getRelatedActor`, `setRelatedActor`) and `Type` enum faithfully ported.
**Renames**: `@Null Actor relatedActor` -> `Nullable[Actor]`
**Convention changes**: Java enum -> Scala 3 enum; static inner class -> companion object; switch -> pattern matching; split packages
**TODOs**: None
**Issues**: None

---

### Layout.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/Layout.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/Layout.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 12 abstract methods accounted for: `layout`, `invalidate`, `invalidateHierarchy`, `validate`, `pack`, `setFillParent`, `setLayoutEnabled`, `getMinWidth`, `getMinHeight`, `getPrefWidth`, `getPrefHeight`, `getMaxWidth`, `getMaxHeight`.
**Convention changes**: Java interface -> Scala trait; split packages
**TODOs**: None
**Issues**: None

---

### NinePatchDrawable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/NinePatchDrawable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/NinePatchDrawable.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 public methods: `draw` (2 overloads), `setPatch`, `getPatch`, `tint`. All 3 constructors present.
**Convention changes**: Copy constructor uses manual property copy (cannot call `super(drawable)` in Scala secondary constructor); `setPatch` null-check -> `Nullable(patch).foreach`; split packages
**TODOs**: None
**Issues**: None

---

### ScissorStack.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/ScissorStack.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/ScissorStack.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 public methods: `pushScissors`, `popScissors`, `peekScissors`, `calculateScissors` (2 overloads), `getViewport`. Private `fix` method present.
**Renames**: `Gdx.gl`/`Gdx.graphics` -> `Sge().graphics` with `(using Sge)`; `Array<Rectangle>` -> `DynamicArray[Rectangle]`
**Convention changes**: Java class with static methods -> Scala object; `@Null Rectangle` -> `Nullable[Rectangle]`; no return; split packages
**TODOs**: None
**Issues**: None

---

### Selection.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/Selection.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/Selection.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods accounted for: `setActor`, `choose`, `notEmpty`, `isEmpty`, `size`, `items`, `first`, `set`, `setAll`, `add`, `addAll`, `remove`, `removeAll`, `clear`, `changed`, `fireChangeEvent`, `contains`, `getLastSelected`, `iterator`, `toArray` (2 overloads), `setDisabled`, `isDisabled`, `getToggle`, `setToggle`, `getMultiple`, `setMultiple`, `getRequired`, `setRequired`, `setProgrammaticChangeEvents`, `getProgrammaticChangeEvents`, `toString`.
**Renames**: `OrderedSet<T>` -> `LinkedHashSet[T]`; `Array<T>` -> `DynamicArray[T]`; `@Null` -> `Nullable`
**Convention changes**: Deprecated `hasItems()` omitted; `choose` requires `(using Sge)`; null item checks removed (Scala type safety); split packages
**Idiom**: `fireChangeEvent` uses `Actor.POOLS.obtain/free` pattern; `toArray` uses `DynamicArray.createWithMk`
**TODOs**: None
**Issues**: **Minor** -- `_isDisabled`, `multiple`, `required`, `lastSelected` fields are `var` with no access modifier (effectively public) rather than `private` with getters/setters. The Java source uses package-private access for `isDisabled`, `multiple`, `required`, `lastSelected` which is more restrictive. These should ideally be `private` with only getter/setter access.

---

### SpriteDrawable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/SpriteDrawable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/SpriteDrawable.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 public methods: `draw` (2 overloads), `setSprite`, `getSprite`, `tint`. All 3 constructors present.
**Convention changes**: Copy constructor uses manual property copy; instanceof -> pattern matching for AtlasSprite; split packages
**TODOs**: None
**Issues**: None

---

### TextureRegionDrawable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/TextureRegionDrawable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/TextureRegionDrawable.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 public methods: `draw` (2 overloads), `setRegion`, `getRegion`, `tint`. All 4 constructors present.
**Convention changes**: Copy constructor uses manual property copy; `setRegion` null-check -> `Nullable(region).foreach`; instanceof -> pattern matching for AtlasRegion; split packages
**TODOs**: None
**Issues**: None

---

### TiledDrawable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/TiledDrawable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/TiledDrawable.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 8 public methods: `draw` (2 overloads), `getColor`, `setScale`, `getScale`, `getAlign`, `setAlign`, `tint`. Static `draw` method in companion object. All 3 constructors present.
**Renames**: `int align` -> `Align` opaque type
**Convention changes**: Copy constructor uses manual property copy; `Align.isLeft/isRight` static methods -> extension methods; static draw -> companion object; for loops -> while loops; split packages
**TODOs**: None
**Issues**: None

---

### TransformDrawable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/TransformDrawable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/TransformDrawable.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single method `draw` (transform overload) faithfully ported.
**Convention changes**: Java interface -> Scala trait; split packages
**TODOs**: None
**Issues**: None

---

### UIUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/utils/UIUtils.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/utils/UIUtils.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 16 public members accounted for: 5 platform flags (`isAndroid`, `isMac`, `isWindows`, `isLinux`, `isIos`), and 6 pairs of no-arg/int-arg methods (`left`, `right`, `middle`, `shift`, `ctrl`, `alt`).
**Renames**: `SharedLibraryLoader.os`/`Os` enum -> `System.getProperty("os.name")` detection; `Gdx.input` -> `Sge().input`
**Convention changes**: Java final class with static methods -> Scala object; no-arg methods require `(using Sge)`; split packages
**TODOs**: None
**Issues**: None
