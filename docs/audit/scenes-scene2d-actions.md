# Audit: sge.scenes.scene2d.actions

Audited: 34/34 files | Pass: 32 | Minor: 2 | Major: 0
Last updated: 2026-03-03

---

### Actions.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/Actions.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/Actions.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 44 factory methods accounted for: `action`, `registerAction`, `addAction` (x2), `removeAction` (x2), `moveTo` (x3), `moveToAligned` (x3), `moveBy` (x3), `sizeTo` (x3), `sizeBy` (x3), `scaleTo` (x3), `scaleBy` (x3), `rotateTo` (x3), `rotateBy` (x3), `color` (x3), `alpha` (x3), `fadeOut` (x2), `fadeIn` (x2), `show`, `hide`, `visible`, `touchable`, `removeActor` (x2), `delay` (x2), `timeScale`, `sequence` (x7 + varargs + no-arg), `parallel` (x7 + varargs + no-arg), `repeat`, `forever`, `run`, `layout`, `after`, `addListener` (x2), `removeListener` (x2), `targeting`. Static `ACTION_POOLS` val.
**Renames**: Java static class -> Scala object; `GdxRuntimeException` -> `SgeError.InvalidInput`; `PoolSupplier<T>` -> `() => T`; `setTarget(actor)` -> `setTarget(Nullable(actor))`; `@Null Interpolation` -> `Nullable[Interpolation]`; static init block -> top-level object statements
**Convention changes**: null -> Nullable[A]; split packages
**TODOs**: None
**Issues**: None. All 27 registered pool types match Java. All factory method signatures match 1:1.

---

### AddAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/AddAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/AddAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 methods: `act`, `getAction`, `setAction`, `restart`, `reset`.
**Renames**: `action` field -> `actionToAdd` (avoids conflict with `Action` base class)
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: Java `target.addAction(action)` -> `target.foreach { t => actionToAdd.foreach { a => a.setActor(Nullable(t)); t.getActions += a } }`. The Scala version manually sets the actor and appends to the actions array rather than calling `addAction`. Functionally equivalent.
**TODOs**: None
**Issues**: None

---

### AddListenerAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/AddListenerAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/AddListenerAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods: `act`, `getListener`, `setListener`, `getCapture`, `setCapture`, `reset`.
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: target null-access -> `target.foreach + listener.foreach`
**TODOs**: None
**Issues**: None

---

### AfterAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/AfterAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/AfterAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods: `setTarget`, `restart`, `delegate`.
**Renames**: `Array<Action>` -> `DynamicArray[Action]`
**Convention changes**: null -> Nullable[A]; no return; split packages
**Idiom**: `target != null` -> `newTarget.foreach`; `indexOf(action, true) == -1` -> `!contains(action)`; `waitForActions.size > 0` -> `waitForActions.nonEmpty`; for loop -> while loop (reverse iteration)
**TODOs**: None
**Issues**: None

---

### AlphaAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/AlphaAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/AlphaAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods: `begin`, `update`, `reset`, `getColor`, `setColor`, `getAlpha`, `setAlpha`.
**Renames**: `end` -> `_end`
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: `if (color == null) color = target.getColor()` -> `Nullable.getOrElse(target.fold(new Color())(_.getColor))`. Note: Java caches color reference in `begin()` and reuses in `update()`; Scala re-resolves on each `update()` call. Functionally equivalent since target color reference does not change between frames.
**TODOs**: None
**Issues**: None

---

### ColorAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/ColorAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/ColorAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods: `begin`, `update`, `reset`, `getColor`, `setColor`, `getEndColor`, `setEndColor`.
**Renames**: 4 separate `startR/startG/startB/startA` floats -> single `startColor: Color` object; `end` -> `endColor`
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: Same color resolution pattern as AlphaAction. Uses `startColor.set(c)` in begin instead of storing 4 floats.
**TODOs**: None
**Issues**: None

---

### CountdownEventAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/CountdownEventAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/CountdownEventAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 1 method: `handle`. Constructor params: `eventClass`, `count`.
**Renames**: package-private `int count` -> `val count` (constructor param, immutable)
**Convention changes**: split packages
**TODOs**: None
**Issues**: None

---

### DelayAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/DelayAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/DelayAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 methods: `delegate`, `finish`, `restart`, `getTime`, `setTime`, `getDuration`, `setDuration`.
**Renames**: `implements FinishableAction` -> `with FinishableAction`
**Convention changes**: no return; split packages
**Idiom**: `if (action == null) return true; return action.act(delta)` -> `action.fold(true)(_.act(actionDelta))`. The Scala version correctly computes actionDelta = time - duration for the overflow case.
**TODOs**: None
**Issues**: None

---

### DelegateAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/DelegateAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/DelegateAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 8 methods: `setAction`, `getAction`, `delegate`, `act`, `restart`, `reset`, `setActor`, `setTarget`, `toString`.
**Convention changes**: null -> Nullable[A]; no return; split packages
**Idiom**: `action != null` checks -> `action.foreach`; `setPool(null)` -> `setPool(Nullable.empty)`; `action == null ? "" : "(" + action + ")"` -> `action.fold("")(a => ...)`
**TODOs**: None
**Issues**: None

---

### EventAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/EventAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/EventAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods: `restart`, `setTarget`, `handle`, `act`, `isActive`, `setActive`. Inner anonymous `EventListener` listener. Constructor param `eventClass`.
**Renames**: `ClassReflection.isInstance(eventClass, event)` -> `eventClass.isInstance(event)`; `(T)event` -> `eventClass.cast(event)`
**Convention changes**: null -> Nullable[A]; no return; split packages
**Idiom**: `target != null` -> `target.foreach`; `newTarget != null` -> `newTarget.foreach`
**Note**: `result` and `active` fields are `var` (public) in Scala vs package-private in Java. This is acceptable -- the Java package-privacy is loosened to Scala's default visibility.
**TODOs**: None
**Issues**: None

---

### FinishableAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/FinishableAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/FinishableAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 1 method: `finish`.
**Renames**: Java interface -> Scala trait
**Convention changes**: split packages
**TODOs**: None
**Issues**: None. Exact 1:1 port.

---

### FloatAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/FloatAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/FloatAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 8 methods: `begin`, `update`, `getValue`, `setValue`, `getStart`, `setStart`, `getEnd`, `setEnd`. 4 constructors: no-arg, (start, end), (start, end, duration), (start, end, duration, interpolation).
**Renames**: `end` -> `_end`
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: Java `super(duration)` / `super(duration, interpolation)` -> `this(start, _end)` + `setDuration` + `setInterpolation` (secondary constructors must chain to primary)
**TODOs**: None
**Issues**: None

---

### IntAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/IntAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/IntAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 8 methods: `begin`, `update`, `getValue`, `setValue`, `getStart`, `setStart`, `getEnd`, `setEnd`. 4 constructors matching FloatAction pattern.
**Renames**: `end` -> `_end`
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: `(int)` cast -> `.toInt`; same constructor chaining pattern as FloatAction
**TODOs**: None
**Issues**: None

---

### LayoutAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/LayoutAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/LayoutAction.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: 3 methods present: `act`, `isEnabled`, `setLayoutEnabled`. `setTarget` override commented out.
**Convention changes**: split packages
**TODOs**: `setTarget()` and `act()` bodies are commented out pending Layout trait port. `act()` returns `true` unconditionally instead of calling `Layout.setLayoutEnabled`.
**Issues**: Both `setTarget` and `act` are non-functional -- the action is a no-op until the Layout trait is ported. This blocks proper layout-enable/disable actions.

---

### MoveByAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/MoveByAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/MoveByAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 methods: `updateRelative`, `setAmount`, `getAmountX`, `setAmountX`, `getAmountY`, `setAmountY`.
**Convention changes**: no return; split packages
**Idiom**: `target.moveBy(...)` -> `target.foreach(_.moveBy(...))`
**TODOs**: None
**Issues**: None

---

### MoveToAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/MoveToAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/MoveToAction.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: 10 methods present: `begin`, `update`, `setPosition` (x2), `getX`, `setX`, `getY`, `setY`, `getAlignment`, `setAlignment`, `reset`. **Missing 3 methods from Java source**: `setStartPosition(float x, float y)`, `getStartX()`, `getStartY()`.
**Renames**: `int alignment` -> `Align` (opaque type); `Align.bottomLeft` constant
**Convention changes**: no return; split packages
**Idiom**: `target.getX(alignment)` -> `target.foreach(t => startX = t.getX(alignment))`
**TODOs**: None
**Issues**: Missing `setStartPosition`, `getStartX`, `getStartY`. These are used for testing/debugging to read or override the starting position captured in `begin()`. Low impact for typical usage but breaks API parity.

---

### ParallelAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/ParallelAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/ParallelAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 11 methods: `act`, `restart`, `reset`, `addAction`, `setActor`, `getActions`, `toString`. 6 constructors: no-arg through 5-arg.
**Renames**: `Array<Action>` -> `DynamicArray[Action]`; `actions` field is `val` (public)
**Convention changes**: null -> Nullable[A]; no return (boundary/break); split packages
**Idiom**: `setPool(null)` -> `setPool(Nullable.empty)`; `actor != null` -> `actor.isDefined`; `getActor() != null` -> `getActor.isDefined`; for-loop -> while; early return in loop -> `scala.util.boundary`/`break`; `if (actor != null) action.setActor(actor)` -> `actor.foreach(a => action.setActor(Nullable(a)))`
**TODOs**: None
**Issues**: None

---

### RelativeTemporalAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RelativeTemporalAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RelativeTemporalAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods: `begin`, `update`, `updateRelative`.
**Convention changes**: split packages
**TODOs**: None
**Issues**: None. Exact 1:1 port.

---

### RemoveAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RemoveAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RemoveAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 methods: `act`, `getAction`, `setAction`, `reset`.
**Renames**: `action` field -> `actionToRemove`
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: `target.removeAction(action)` -> `target.foreach(_.removeAction(actionToRemove))`
**TODOs**: None
**Issues**: None

---

### RemoveActorAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RemoveActorAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RemoveActorAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 2 methods: `act`, `restart`.
**Convention changes**: no return; split packages
**Idiom**: `target.remove()` -> `target.foreach(_.remove())`
**TODOs**: None
**Issues**: None

---

### RemoveListenerAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RemoveListenerAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RemoveListenerAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods: `act`, `getListener`, `setListener`, `getCapture`, `setCapture`, `reset`.
**Convention changes**: null -> Nullable[A]; split packages
**Idiom**: target null-access -> `target.foreach + listener.foreach`
**TODOs**: None
**Issues**: None

---

### RepeatAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RepeatAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RepeatAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 methods: `delegate`, `finish`, `restart`, `setCount`, `getCount`. Companion object with `FOREVER` constant.
**Renames**: `static FOREVER` -> companion object val; `implements FinishableAction` -> `with FinishableAction`
**Convention changes**: no return; split packages
**Idiom**: Multiple early returns -> `action.fold(true) { a => nested if/else chain }`. Logic is functionally equivalent.
**TODOs**: None
**Issues**: None

---

### RotateByAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RotateByAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RotateByAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods: `updateRelative`, `getAmount`, `setAmount`.
**Convention changes**: no return; split packages
**Idiom**: `target.rotateBy(...)` -> `target.foreach(_.rotateBy(...))`
**TODOs**: None
**Issues**: None

---

### RotateToAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RotateToAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RotateToAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 methods: `begin`, `update`, `getRotation`, `setRotation`, `isUseShortestDirection`, `setUseShortestDirection`. 2 constructors: no-arg, (useShortestDirection).
**Renames**: `end` -> `_end`
**Convention changes**: no return; split packages
**Idiom**: `target.getRotation()` -> `target.foreach(t => start = t.getRotation)`. MathUtils.lerpAngleDeg usage matches Java.
**TODOs**: None
**Issues**: None

---

### RunnableAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/RunnableAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/RunnableAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods: `act`, `run`, `restart`, `reset`, `getRunnable`, `setRunnable`.
**Convention changes**: null -> Nullable[A]; no return; split packages
**Idiom**: `setPool(null)` -> `setPool(Nullable.empty)`; `runnable.run()` -> `runnable.foreach(_.run())`; raw `Pool` -> `Nullable[Pool[?]]`
**TODOs**: None
**Issues**: None

---

### ScaleByAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/ScaleByAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/ScaleByAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods: `updateRelative`, `setAmount` (x2), `getAmountX`, `setAmountX`, `getAmountY`, `setAmountY`.
**Convention changes**: no return; split packages
**Idiom**: `target.scaleBy(...)` -> `target.foreach(_.scaleBy(...))`
**TODOs**: None
**Issues**: None

---

### ScaleToAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/ScaleToAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/ScaleToAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 methods: `begin`, `update`, `setScale` (x2), `getX`, `setX`, `getY`, `setY`.
**Convention changes**: no return; split packages
**Idiom**: `target.getScaleX()` -> `target.foreach(t => ...)`, tuple destructure `val (x, y) = ...` for interpolation
**TODOs**: None
**Issues**: None

---

### SequenceAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/SequenceAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/SequenceAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 2 methods: `act`, `restart`. 6 constructors: no-arg through 5-arg.
**Convention changes**: null -> Nullable[A]; no return; split packages
**Idiom**: `setPool(null)` -> `setPool(Nullable.empty)`; `actor == null` -> `actor.isEmpty`; multiple early returns -> if/else chain
**TODOs**: None
**Issues**: None

---

### SizeByAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/SizeByAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/SizeByAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 methods: `updateRelative`, `setAmount`, `getAmountWidth`, `setAmountWidth`, `getAmountHeight`, `setAmountHeight`.
**Convention changes**: no return; split packages
**Idiom**: `target.sizeBy(...)` -> `target.foreach(_.sizeBy(...))`
**TODOs**: None
**Issues**: None

---

### SizeToAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/SizeToAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/SizeToAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 methods: `begin`, `update`, `setSize`, `getWidth`, `setWidth`, `getHeight`, `setHeight`.
**Convention changes**: no return; split packages
**Idiom**: `target.getWidth()` -> `target.foreach(t => ...)`, tuple destructure for interpolation
**TODOs**: None
**Issues**: None

---

### TemporalAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/TemporalAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/TemporalAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 15 methods: `act`, `begin`, `end`, `update`, `finish`, `restart`, `reset`, `getTime`, `setTime`, `getDuration`, `setDuration`, `getInterpolation`, `setInterpolation`, `isReverse`, `setReverse`, `isComplete`. 3 constructors: no-arg, (duration), (duration, interpolation).
**Renames**: `implements FinishableAction` -> `with FinishableAction`
**Convention changes**: null -> Nullable[A]; no return; split packages
**Idiom**: `setPool(null)` -> `setPool(Nullable.empty)`; early return `if (complete) return true` -> `if (complete) true else { ... }`; `interpolation != null` -> `interpolation.foreach`; ternary `complete ? 1 : time / duration` -> `if (complete) 1f else time / duration`; `reverse ? 1 - percent : percent` -> `if (reverse) 1 - percent else percent`
**TODOs**: None
**Issues**: None

---

### TimeScaleAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/TimeScaleAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/TimeScaleAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods: `delegate`, `getScale`, `setScale`.
**Convention changes**: no return; split packages
**Idiom**: `if (action == null) return true; return action.act(delta * scale)` -> `action.fold(true)(_.act(delta * scale))`
**TODOs**: None
**Issues**: None

---

### TouchableAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/TouchableAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/TouchableAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods: `act`, `getTouchable`, `setTouchable`.
**Convention changes**: split packages
**Idiom**: `target.setTouchable(touchable)` -> `target.foreach(_.setTouchable(touchable))`; `Touchable touchable` field uses `scala.compiletime.uninitialized` (initializes to null for enum ref type, acceptable since always set before use via pool reset cycle)
**TODOs**: None
**Issues**: None

---

### VisibleAction.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/scenes/scene2d/actions/VisibleAction.scala` |
| Java source(s) | `com/badlogic/gdx/scenes/scene2d/actions/VisibleAction.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods: `act`, `isVisible`, `setVisible`.
**Convention changes**: split packages
**Idiom**: `target.setVisible(visible)` -> `target.foreach(_.setVisible(visible))`
**TODOs**: None
**Issues**: None
