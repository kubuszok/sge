# Audit: sge (root)

Audited: 17/17 files | Pass: 12 | Minor: 1 | Major: 4
Last updated: 2026-03-03

---

### Application.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Application.scala` |
| Java source(s) | `com/badlogic/gdx/Application.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present. `ApplicationType` enum with all 6 values. LOG constants match.
**Convention changes**: Java interface -> Scala trait; `ApplicationType` uses Scala 3 `enum`
**Issues**: None

---

### ApplicationListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/ApplicationListener.scala` |
| Java source(s) | `com/badlogic/gdx/ApplicationListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods present.
**Issues**: None

---

### ApplicationLogger.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/ApplicationLogger.scala` |
| Java source(s) | `com/badlogic/gdx/ApplicationLogger.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods present.
**Issues**: None

---

### Audio.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Audio.scala` |
| Java source(s) | `com/badlogic/gdx/Audio.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods present. `switchOutputDevice` uses `Nullable[String]`.
**Convention changes**: Deliberately omits `extends AutoCloseable` (backends manage lifecycle)
**Issues**: None

---

### Files.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Files.scala` |
| Java source(s) | `com/badlogic/gdx/Files.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 10 methods present. `FileType` enum extracted to `sge.files.FileType`.
**Issues**: None

---

### Input.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Input.scala` |
| Java source(s) | `com/badlogic/gdx/Input.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All major methods present. Keys object (137 constants) matches. Buttons object (5 values) matches.
**Issues**:
- `major`: **4 enums use `scala.Enumeration`** (Peripheral, OnscreenKeyboardType, VibrationType, Orientation) — must be Scala 3 `enum`
- `major`: Missing `closeTextInputField(boolean, NativeInputCloseCallback)` 2-arg overload
- `major`: Missing `isTextInputFieldOpened()` default method
- `major`: `KeyboardHeightObserver` missing `onKeyboardShow` and `onKeyboardHide` methods
- `minor`: `Keys.toString` returns raw `null` — should return `Nullable[String]`

---

### InputProcessor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/InputProcessor.scala` |
| Java source(s) | `com/badlogic/gdx/InputProcessor.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 9 methods with default `= false` implementations (eliminates need for `InputAdapter`).
**Issues**: None

---

### LifecycleListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/LifecycleListener.scala` |
| Java source(s) | `com/badlogic/gdx/LifecycleListener.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods present.
**Issues**: None

---

### Preferences.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Preferences.scala` |
| Java source(s) | `com/badlogic/gdx/Preferences.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 19 methods present.
**Convention changes**: `put` uses `Map[String, Boolean | Int | Long | Float | String]` (Scala 3 union types — improvement)
**Issues**: None

---

### Screen.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Screen.scala` |
| Java source(s) | `com/badlogic/gdx/Screen.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 methods present. `dispose()` -> `close()`.
**Issues**: None

---

### Version.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Version.scala` |
| Java source(s) | `com/badlogic/gdx/Version.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods present.
**Convention changes**: Java class -> Scala object; static initializer -> lazy val
**Issues**:
- `minor`: VERSION is "1.13.5" but Java source is "1.14.1" — version string outdated

---

### Graphics.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Graphics.scala` |
| Java source(s) | `com/badlogic/gdx/Graphics.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All 37 trait methods present. `GraphicsType` enum (7 values), `DisplayMode`, `Monitor`, `BufferFormat` all match.
**Convention changes**: Added convenience properties `gl`, `gl20`, `gl30`, `gl31`, `gl32`
**Issues**:
- `major`: `type GLVersion = AnyRef` is a placeholder — should reference real `sge.graphics.glutils.GLVersion`

---

### InputMultiplexer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/InputMultiplexer.scala` |
| Java source(s) | `com/badlogic/gdx/InputMultiplexer.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present.
**Convention changes**: `SnapshotArray` -> `DynamicArray` with copy pattern
**Issues**: None (`removeProcessor(Int)` returns Unit instead of InputProcessor — minor)

---

### Net.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Net.scala` |
| Java source(s) | `com/badlogic/gdx/Net.java` |
| Status | pass |
| Tested | No |

**Completeness**: All trait methods, `HttpMethod` enum, `HttpRequest`/`HttpResponse`/`HttpResponseListener` present.
**Convention changes**: `HttpMethods` string constants -> `HttpMethod` enum (improvement)
**Issues**: None

---

### Sge.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Sge.scala` |
| Java source(s) | `com/badlogic/gdx/Gdx.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 core fields mapped (app, graphics, audio, files, input, net).
**Convention changes**: Mutable `public static` fields -> immutable `case class` with `using` context parameter (excellent redesign)
**Issues**: None

---

### InputEventQueue.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/InputEventQueue.scala` |
| Java source(s) | `com/badlogic/gdx/InputEventQueue.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods and 8 event type constants present.
**Convention changes**: `IntArray` -> `DynamicArray[Int]`; `boundary`/`break` for returns; `Nullable` for processor
**Issues**: None

---

### Game.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/Game.scala` |
| Java source(s) | `com/badlogic/gdx/Game.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All methods present.
**Issues**:
- `major`: `render()` uses hardcoded `0.016f` instead of `Sge().graphics.getDeltaTime()`
- `major`: `setScreen()` uses hardcoded `800x600` instead of `Sge().graphics.getWidth()/getHeight()`
- `minor`: Needs `(using Sge)` context parameter to access runtime environment
