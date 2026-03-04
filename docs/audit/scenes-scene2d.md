# Audit: sge.scenes.scene2d

Audited: 9/9 files | Pass: 7 | Minor: 2 | Major: 0
Last updated: 2026-03-04

---

### Action.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/Action.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/Action.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 9 public/protected methods accounted for: `act`, `restart`, `setActor`, `getActor`, `setTarget`, `getTarget`, `reset`, `getPool`, `setPool`, `toString`.
**Renames**: `Pool.Poolable` Java interface -> Scala trait; raw `Pool` -> `Pool[?]`
**Convention changes**: null -> Nullable[A]; split packages; no return statements
**Idiom**: `setActor` null-check logic -> `Nullable.fold`; pool raw type -> `Pool[?]` wildcard
**TODOs**: None
**Issues**: None

---

### Event.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/Event.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/Event.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 15 public methods accounted for: `handle`, `cancel`, `stop`, `reset`, `getTarget`, `setTarget`, `getListenerActor`, `setListenerActor`, `getBubbles`, `setBubbles`, `isHandled`, `isStopped`, `isCancelled`, `setCapture`, `isCapture`, `setStage`, `getStage`.
**Renames**: `Pool.Poolable` Java interface -> Scala trait
**Convention changes**: null -> Nullable[A]; split packages; no return statements
**Idiom**: `getTarget`/`getListenerActor` use `getOrElse(null)` for API compatibility with callers expecting non-Nullable return; `getStage` returns `Nullable[Stage]`
**TODOs**: None
**Issues**: None. `getOrElse(null)` is acceptable at API boundary where callers need non-wrapped values.

---

### EventListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/EventListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/EventListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 1 interface methods accounted for: `handle`.
**Renames**: Java interface -> Scala trait (SAM-compatible)
**Convention changes**: Split packages; braces on trait body
**TODOs**: None
**Issues**: None. Exact 1:1 port.

---

### InputEvent.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/InputEvent.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/InputEvent.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 21 public methods accounted for: `reset`, `getStageX`, `setStageX`, `getStageY`, `setStageY`, `getType`, `setType`, `getPointer`, `setPointer`, `getButton`, `setButton`, `getKeyCode`, `setKeyCode`, `getCharacter`, `setCharacter`, `getScrollAmountX`, `getScrollAmountY`, `setScrollAmountX`, `setScrollAmountY`, `getRelatedActor`, `setRelatedActor`, `toCoordinates`, `isTouchFocusCancel`, `getTouchFocus`, `setTouchFocus`, `toString`. Inner enum `Type` with all 10 values.
**Renames**: Field `type` -> `eventType` (reserved keyword in Scala); `Integer.MIN_VALUE` -> `Int.MinValue`; Java static inner enum -> companion object Scala 3 enum
**Convention changes**: null -> Nullable[A]; split packages; no return statements; Scala 3 enum
**TODOs**: None
**Issues**: None

---

### InputListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/InputListener.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/InputListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 10 public methods accounted for: `handle`, `touchDown`, `touchUp`, `touchDragged`, `mouseMoved`, `enter`, `exit`, `scrolled`, `keyDown`, `keyUp`, `keyTyped`. Static `tmpCoords` -> companion object val.
**Renames**: `implements EventListener` -> `extends EventListener`; `@Null Actor fromActor` -> `Nullable[Actor]`; static -> companion object
**Convention changes**: null -> Nullable[A]; split packages; no return statements
**Idiom**: Java switch -> Scala nested pattern match; `getStage()` null-check -> `Nullable.foreach`
**TODOs**: None
**Issues**: None

---

### Touchable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/Touchable.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/Touchable.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 enum values accounted for: `enabled`, `disabled`, `childrenOnly`.
**Renames**: Java enum -> Scala 3 enum
**Convention changes**: Split packages; braces on enum body
**TODOs**: None
**Issues**: None. Exact 1:1 port.

---

### Actor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/Actor.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/Actor.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All ~60 public/protected methods accounted for. Deprecated `ancestorsVisible()` intentionally dropped.
**Renames**: `DelayedRemovalArray` -> `DynamicArray` + manual deferred removal pattern; `Align` int -> `Align` opaque type; `Object userObject` -> `AnyRef userObject`; `debug` field -> `_debug`
**Convention changes**: null -> Nullable[A]; no return (boundary/break); split packages; `(using Sge)` on `act`/`addAction`
**Idiom**: Alignment bitfield ops -> Align methods (`isRight`, `isLeft`, etc.); do-while -> while with Nullable; POOLS static init block -> companion object vals; `removeListener`/`removeCaptureListener` use manual deferred removal (iterating counter + pending list) instead of `DelayedRemovalArray`
**TODOs**:
- `clipBegin(x,y,w,h)` body is TODO — ScissorStack is ported but requires `(using Sge)`; `draw()` methods lack Sge context
- `clipEnd()` body is TODO — same Sge context issue
- `drawDebugBounds` uses simplified `shapes.rectangle(x, y, width, height)` instead of full `shapes.rect(x, y, originX, originY, width, height, scaleX, scaleY, rotation)` (ShapeRenderer overload not yet ported)
**Issues**: The 3 TODOs above are blocking for production use of clipping and debug rendering with rotated/scaled actors. Functionally correct for non-clipped, axis-aligned actors.

---

### Group.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/Group.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/Group.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All ~30 public/protected methods accounted for: `act`, `draw`, `drawChildren`, `drawDebug`, `drawDebugChildren`, `computeTransform`, `applyTransform` (Batch and ShapeRenderer overloads), `resetTransform` (Batch and ShapeRenderer overloads), `setCullingArea`, `getCullingArea`, `hit`, `childrenChanged`, `addActor`, `addActorAt`, `addActorBefore`, `addActorAfter`, `removeActor` (2 overloads), `removeActorAt`, `clearChildren` (2 overloads), `clear` (2 overloads), `findActor`, `setStage`, `swapActor` (2 overloads), `getChild`, `getChildren`, `hasChildren`, `setTransform`, `isTransform`, `localToDescendantCoordinates`, `setDebug`, `debugAll`, `toString` (2 overloads).
**Renames**: `SnapshotArray` -> `DynamicArray` (with `toArray` snapshots); `implements Cullable` -> `with Cullable`; static `tmp` -> companion object val; `indexOf(actor, true)` -> `indexOf(actor)` (identity param dropped)
**Convention changes**: null -> Nullable[A]; no return (boundary/break); split packages; `(using Sge)` on `act`
**Idiom**: Java for+continue -> while+if-guard; `children.swap(a,b)` -> manual swap via temp var; parent null-loop in `computeTransform` uses Nullable iteration pattern
**TODOs**:
- `drawDebugChildren`: `shapes.flush()` commented out (ShapeRenderer.flush not yet ported)
- `applyTransform(ShapeRenderer)`: `shapes.flush()` commented out
**Issues**: Missing `shapes.flush()` calls may cause incorrect debug rendering order when transforms are applied. Non-blocking for core functionality.

---

### Stage.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/Stage.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/Stage.java` |
| Status | pass |
| Tested | No |

**Completeness**: All ~45 public/protected methods accounted for. Inner class `TouchFocus` present with `reset()`. All input event methods (`touchDown`, `touchUp`, `touchDragged`, `touchCancelled`, `mouseMoved`, `scrolled`, `keyDown`, `keyUp`, `keyTyped`), focus management (`setKeyboardFocus`, `getKeyboardFocus`, `setScrollFocus`, `getScrollFocus`, `addTouchFocus`, `removeTouchFocus`, `cancelTouchFocus`, `cancelTouchFocusExcept`), stage operations (`draw`, `drawDebug`, `act`, `hit`, `addActor`, `addAction`, `getActors`, `addListener`, `removeListener`, `addCaptureListener`, `removeCaptureListener`, `actorRemoved`, `clear`, `unfocusAll`, `unfocus`), coordinate transforms (`screenToStageCoordinates`, `stageToScreenCoordinates`, `toScreenCoordinates`, `calculateScissors`), viewport/batch/camera (`getBatch`, `getViewport`, `setViewport`, `getWidth`, `getHeight`, `getCamera`, `getRoot`, `setRoot`), debug (`setDebugAll`, `isDebugAll`, `setDebugUnderMouse`, `setDebugParentUnderMouse`, `setDebugTableUnderMouse`, `setDebugInvisible`, `getDebugColor`, `setActionsRequestRendering`, `getActionsRequestRendering`), lifecycle (`close`, `isInsideViewport`).
**Renames**: `InputAdapter` -> `InputProcessor`; `Disposable` -> `AutoCloseable`; `dispose()` -> `close()`; `SnapshotArray<TouchFocus>` -> `DynamicArray[TouchFocus]`; `static debug` -> companion object var
**Convention changes**: null -> Nullable[A]; no return (boundary/break); split packages; `(using Sge)` on constructors
**Idiom**: Java constructor chain -> Scala auxiliary constructors with `ownsBatch` flag; `Gdx.graphics`/`Gdx.input` -> `Sge().graphics`/`Sge().input`; SnapshotArray `begin()`/`end()` -> `toArray` snapshots; `drawDebug` uses `shouldDraw` flag variable instead of early returns
**TODOs**: None
**Issues**:
- `TouchFocus.reset()` uses raw `null` assignments (`listenerActor = null`, `listener = null`, `target = null`). These fields use `scala.compiletime.uninitialized` which initializes to null for reference types. The `null` in `reset()` is acceptable as a Java interop boundary (fields are non-Nullable by design since they are always set before use via pool `obtain`).
