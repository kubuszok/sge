# Audit: sge.input

Audited: 5/5 files | Pass: 0 | Minor: 3 | Major: 2
Last updated: 2026-03-03

---

### NativeInputConfiguration.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/NativeInputConfiguration.scala` |
| Java source(s) | `com/badlogic/gdx/input/NativeInputConfiguration.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: Core getters/setters present. Missing: `maskInput` field (`isMaskInput`/`setMaskInput`), `NativeInputCloseCallback` interface (`closeCallback` field + getter/setter).
**Renames**: `showUnmaskButton` -> `showPasswordButton` (semantics differ)
**Convention changes**: `maxLength` from `int` with `-1` sentinel -> `Option[Int]`; `autoComplete` from `String[]` (null) -> `Nullable[Array[String]]`; split packages
**TODOs**: None
**Issues**:
- `major`: Missing `maskInput` boolean field and `isMaskInput`/`setMaskInput` methods
- `major`: Missing `NativeInputCloseCallback` inner interface and `closeCallback` field with getter/setter
- `minor`: `validate()` missing `showUnmaskButton`-vs-`maskInput` logic (uses Password type check instead)
- `minor`: `showPasswordButton` semantics differ from Java's `showUnmaskButton`

---

### TextInputWrapper.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/TextInputWrapper.scala` |
| Java source(s) | `com/badlogic/gdx/input/TextInputWrapper.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: `getText`, `getSelectionStart`, `getSelectionEnd` preserved. `writeResults(String, int, int)` replaced with different API (`setText` + `setPosition` + `shouldClose`).
**Renames**: None
**Convention changes**: Java interface -> Scala trait; split packages
**TODOs**: None
**Issues**:
- `major`: API divergence — Java's `writeResults(String, int, int)` replaced with three separate methods (`setText`, `setPosition`, `shouldClose`) with no Java equivalent
- `minor`: Missing all Javadoc comments from original source

---

### RemoteSender.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/RemoteSender.scala` |
| Java source(s) | `com/badlogic/gdx/input/RemoteSender.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods and constants present. Constructor, `sendUpdate`, all `InputProcessor` methods faithfully ported.
**Renames**: `Gdx` -> `Sge` (implicit context)
**Convention changes**: Java static constants -> companion object `final val`s; `Gdx` singleton -> implicit `Sge`; `boundary`/`break` for returns; `Nullable` for `out` field
**TODOs**: None
**Issues**:
- `minor`: Flat package (`package sge.input`) instead of split packages
- `minor`: Constructor hardcodes `multitouch = true` instead of querying `Sge.input`
- `minor`: `sendUpdate` hardcodes `800.0f`/`600.0f` instead of `sge.graphics.getWidth()`/`getHeight()`
- `minor`: Uses `println` instead of `sge.app.log`

---

### RemoteInput.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/RemoteInput.scala` |
| Java source(s) | `com/badlogic/gdx/input/RemoteInput.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods, inner classes, constants, and `Input` interface implementation present.
**Renames**: `Gdx` -> `Sge`, `GdxRuntimeException` -> `SgeError.NetworkError`, `RemoteInputListener` extracted as top-level trait
**Convention changes**: Constructor overloads -> default params; `listener` null -> `Option`; `processor` null -> `Nullable`; split packages
**TODOs**: 1 — `postRunnable` not yet wired (EventTrigger called directly)
**Issues**:
- `minor`: Touch coordinate scaling uses hardcoded 800/600 instead of dynamic screen dimensions
- `minor`: `EventTrigger.run()` called directly instead of via `app.postRunnable`

---

### GestureDetector.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/GestureDetector.scala` |
| Java source(s) | `com/badlogic/gdx/input/GestureDetector.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public/protected methods present including `GestureListener` trait, `GestureAdapter`, `VelocityTracker`.
**Renames**: `InputAdapter` -> `InputProcessor`, `dst` -> `distance`
**Convention changes**: Java static inner classes -> companion object members; `GestureListener` interface -> trait; `GestureAdapter` implements -> extends
**TODOs**: None
**Issues**:
- `minor`: `setTapCountInterval()` is a no-op (field is `val` not `var`)
- `minor`: `setMaxFlingDelay()` is a no-op (field is `val` not `var`)
- `minor`: `setMaxFlingDelay` signature changed from `long` (nanos) to `Float` (seconds) — breaking API change
- `minor`: `VelocityTracker.getSum()` omitted (unused in Java)
