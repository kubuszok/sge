# Audit: sge.input

Audited: 5/5 files | Pass: 5 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### NativeInputConfiguration.scala — pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/NativeInputConfiguration.scala` |
| Java source(s) | `com/badlogic/gdx/input/NativeInputConfiguration.java` |
| Status | pass |
| Tested | No |

Fixed: added `maskInput` field + `isMaskInput`/`setMaskInput`; added `NativeInputCloseCallback` inner trait + `closeCallback` field; renamed `showPasswordButton` back to `showUnmaskButton`; fixed `validate()` logic to match Java.

---

### TextInputWrapper.scala — pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/TextInputWrapper.scala` |
| Java source(s) | `com/badlogic/gdx/input/TextInputWrapper.java` |
| Status | pass |
| Tested | No |

Fixed: replaced divergent `setText`/`setPosition`/`shouldClose` API with Java's `writeResults(String, Int, Int)`. Added Javadoc comments.

---

### RemoteSender.scala — pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/RemoteSender.scala` |
| Java source(s) | `com/badlogic/gdx/input/RemoteSender.java` |
| Status | pass |
| Tested | No |

Fixed: split package; multitouch check uses `Sge().input.isPeripheralAvailable`; width/height uses `Sge().graphics.getWidth/Height`; `println` replaced with `Sge().application.log`; anonymous using.

---

### RemoteInput.scala — pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/RemoteInput.scala` |
| Java source(s) | `com/badlogic/gdx/input/RemoteInput.java` |
| Status | pass |
| Tested | No |

Fixed: touch coordinate scaling uses `Sge().graphics.getWidth/Height` instead of hardcoded 800x600; anonymous using. Remaining TODO: `postRunnable` not yet wired (blocked by Application trait API).

---

### GestureDetector.scala — pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/input/GestureDetector.scala` |
| Java source(s) | `com/badlogic/gdx/input/GestureDetector.java` |
| Status | pass |
| Tested | No |

Fixed: `tapCountIntervalNanos`/`maxFlingDelayNanos` changed from `val` to `var`; `setTapCountInterval`/`setMaxFlingDelay` bodies restored; `setMaxFlingDelay` signature fixed to `Long` (nanos); anonymous using.
