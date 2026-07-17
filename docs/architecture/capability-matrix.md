# Platform capability matrix

Which platform backends support which host capabilities, and how a *missing*
capability is signaled. Written for ISS-815 (clause 1) and kept accurate to the
**code** — every row below is backed by a source citation, and there are no
aspirational entries.

## Legend

| Cell | Meaning |
|------|---------|
| **Supported** | The capability works on this backend. |
| **Unsupported** | The capability is unavailable; the call **throws `sge.utils.SgeError.Unsupported`** (never a raw `java.lang` exception, never `InvalidInput`). |
| **No-op (documented)** | The call returns normally and does nothing, by design; documented at the call site so portable game code can invoke it unconditionally. |
| **—** | Not part of the SGE API surface on any backend (see notes). |

The four backends are: **Desktop-JVM** (LWJGL/Panama + GLFW), **Desktop-Native**
(Scala Native + GLFW), **Browser-JS** (Scala.js/DOM), **Android** (Android SDK).
Desktop-JVM and Desktop-Native share the `scaladesktop` source set; per-platform
splits live in `scalajvm` / `scalanative`.

## The `SgeError.Unsupported` convention (ISS-771)

Every capability-not-available site signals through the single
`sge.utils.SgeError.Unsupported` enum case — a member of SGE's own error
hierarchy — never a raw JDK `java.lang` runtime exception (which sits outside the
hierarchy) and never `SgeError.InvalidInput` (which means *bad user input*, a
semantically distinct condition). The `message` names the unavailable capability.
See `sge/src/main/scala/sge/utils/SgeError.scala`.

`Unsupported` (throws) is distinct from **No-op (documented)** (returns silently):
a No-op is chosen only where the original LibGDX backend is itself a no-op and
portable code is expected to call it on every platform unconditionally.

## Matrix

| Capability | Desktop-JVM | Desktop-Native | Browser-JS | Android |
|------------|-------------|----------------|------------|---------|
| Text input (`Input.getTextInput`) | Supported (Swing `JOptionPane`) | **Unsupported** | Supported (`window.prompt`) | Supported (native dialog) |
| Application exit (`Application.exit`) | Supported (stops the loop) | Supported (stops the loop) | **No-op (documented)** | Supported (`Activity.finish`) |
| Audio recorder (`Audio.newAudioRecorder`) | Supported (`DesktopAudioRecorder`) | **Unsupported** | **Unsupported** | Supported (`AndroidAudioRecorderAdapter`) |
| Audio device (`Audio.newAudioDevice`) | Supported | Supported | **Unsupported** | Supported |
| External files (`Files.external`) | Supported | Supported | **Unsupported** | Supported |
| Absolute files (`Files.absolute`) | Supported | Supported | **Unsupported** | Supported |
| Local files (`Files.local`) | Supported | Supported | **Unsupported** | Supported |
| `FileHandle.file` (→ `java.io.File`) | Supported | Supported | **Unsupported** | Supported |
| Server sockets (`Net.newServerSocket`) | Supported | Supported | **Unsupported** | Supported |
| Client sockets (`Net.newClientSocket`) | Supported | Supported | **Unsupported** | Supported |
| Clipboard (`Application.clipboard`) | Supported (GLFW) | Supported (GLFW) | Supported (Navigator Clipboard API) | Supported (`ClipboardManager`) |
| Cursors (`Cursor` / system cursors) | Supported (GLFW) | Supported (GLFW) | Supported (CSS cursors) | — (touch; no cursor API) |
| Gamepad / controllers | — | — | — | — |

Notes:

- **Gamepad / controllers**: LibGDX's controller support lives in the separate
  `gdx-controllers` extension, which is **not ported** to SGE. There is no
  gamepad API on any backend, so this is `—` rather than Supported/Unsupported.
  (The many `*Controller*` types in `sge.graphics.g3d` are animation/camera
  controllers, unrelated to input devices.)
- **Cursors on Android**: Android is touch-first and exposes no cursor API in
  SGE; marked `—`. Desktop and Browser cursors are real.

## Clause decisions recorded for ISS-815

- **Clause 2 — `BrowserApplication.exit` is a documented No-op.** Faithful to the
  GWT backend (`GwtApplication.exit()` is empty): a web page cannot terminate its
  own process, and closing the tab is the user's affordance. It must **not**
  throw, because portable shutdown code calls `app.exit()` unconditionally — so
  it is a No-op, not `Unsupported`.
  See `sge/src/main/scalajs/sge/BrowserApplication.scala` (`exit`).
- **Clause 3 — `getTextInput` is platform-split.** The shared `scaladesktop`
  `DefaultDesktopInput.getTextInput` previously reached Swing through
  `java.lang` reflection (`Class.forName` / `Method.invoke` / `Field.get`) so a
  single source could compile for Scala Native. Those reflection symbols are
  **unreachable on Scala Native** and break the native link (dead-code
  elimination only hid them while no test referenced the path). The capability is
  now split into per-platform helpers:
  - JVM: `sge/src/main/scalajvm/sge/DesktopTextInputPlatform.scala` — direct
    Swing, no reflection.
  - Native: `sge/src/main/scalanative/sge/DesktopTextInputPlatform.scala` —
    signals `SgeError.Unsupported`, references **no** reflection symbols.

  A native test (`sge/src/test/scalanative/sge/DesktopTextInputNativeUnsupportedIss815Suite.scala`)
  links the native binary against this path and asserts `SgeError.Unsupported`,
  proving the reflection is gone rather than merely DCE-hidden.

## Every `SgeError.Unsupported` site (as of this document)

| Site | Backend | Capability |
|------|---------|-----------|
| `scalanative/sge/DesktopTextInputPlatform.scala` (`getTextInput`) | Desktop-Native | text input dialog (ISS-815 clause 3) |
| `scaladesktop/sge/audio/MiniaudioEngine.scala` default `recorderFactory`; wired unset by `scalanative/sge/DesktopApplicationFactory.scala` | Desktop-Native | audio recorder (ISS-785) |
| `scalajs/sge/audio/DefaultBrowserAudio.scala:59` | Browser-JS | audio device |
| `scalajs/sge/audio/DefaultBrowserAudio.scala:62` | Browser-JS | audio recorder |
| `scalajs/sge/BrowserNet.scala:32`, `:35` | Browser-JS | server sockets |
| `scalajs/sge/BrowserNet.scala:38` | Browser-JS | client sockets |
| `scalajs/sge/files/BrowserFiles.scala:36` | Browser-JS | unsupported `FileType` |
| `scalajs/sge/files/BrowserFiles.scala:46` | Browser-JS | external files |
| `scalajs/sge/files/BrowserFiles.scala:49` | Browser-JS | absolute files |
| `scalajs/sge/files/BrowserFiles.scala:52` | Browser-JS | local files |
| `scalajs/sge/files/BrowserFileHandle.scala:74` | Browser-JS | `FileHandle.file` (`java.io.File`) |

All rows above throw `sge.utils.SgeError.Unsupported` per ISS-771. Paths are
relative to `sge/src/main/`.
