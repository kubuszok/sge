# Platform FFI Gotchas

Runtime pitfalls and workarounds for SGE's native FFI layer across JVM (Panama FFM),
Scala Native (@extern), and macOS/Linux/Windows platforms.

**Last updated:** 2026-03-06

---

## Table of Contents

1. [macOS: GLFW requires the main thread](#1-macos-glfw-requires-the-main-thread)
2. [macOS: Homebrew library paths](#2-macos-homebrew-library-paths)
3. [JVM: Panama FFM requires --enable-native-access](#3-jvm-panama-ffm-requires---enable-native-access)
4. [JVM: SymbolLookup.libraryLookup vs System.load](#4-jvm-symbollookuplibrarlylookup-vs-systemload)
5. [JVM: MemorySegment.ofAddress creates zero-length segments](#5-jvm-memorysegmentofaddress-creates-zero-length-segments)
6. [Scala Native: CFuncPtr cannot close over local state](#6-scala-native-cfuncptr-cannot-close-over-local-state)
7. [Scala Native: CLong vs Long](#7-scala-native-clong-vs-long)
8. [Scala Native: fromCString requires explicit charset](#8-scala-native-fromcstring-requires-explicit-charset)
9. [Scala Native: toCString uses `using` syntax](#9-scala-native-tocstring-uses-using-syntax)
10. [Scala Native: Zone.alloc type constraints](#10-scala-native-zonealloc-type-constraints)
11. [Scala Native: stdlib.malloc overload resolution](#11-scala-native-stdlibmalloc-overload-resolution)
12. [Android: Panama FFM is not available](#12-android-panama-ffm-is-not-available)
13. [GLFW null platform for headless testing](#13-glfw-null-platform-for-headless-testing)
14. [LibGDX macOS workaround (reference)](#14-libgdx-macos-workaround-reference)

---

## 1. macOS: GLFW requires the main thread

**Problem:** On macOS, GLFW uses Cocoa/NSApplication internally. Apple requires all
Cocoa calls to happen on the main thread. Calling `glfwInit()` from a non-main thread
causes a **SIGTRAP** (signal 5) or **SIGABRT** that kills the JVM process. This crash
is not catchable by try/catch — the JVM is terminated by the OS.

**Symptoms:**
- Forked JVM exits with code 133 (128 + 5 = SIGTRAP) or 134 (128 + 6 = SIGABRT)
- `Total 0, Failed 0, Errors 0, Passed 0` — no tests run at all
- No `hs_err_pid*.log` crash dump (macOS SIGTRAP doesn't always produce one)

**Solutions:**

| Approach | Use case | How |
|----------|----------|-----|
| `GLFW_PLATFORM_NULL` | Tests/CI | `glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_NULL)` before `glfwInit()` |
| `-XstartOnFirstThread` | Desktop app | JVM flag that makes the main thread the AWT/Cocoa thread |
| Child process restart | Desktop app | Detect macOS + missing flag, respawn with `-XstartOnFirstThread` |
| LWJGL `glfw_async` | LWJGL only | Patched GLFW that dispatches Cocoa calls via GCD (not available with Panama) |

**For SGE tests:** We use `GLFW_PLATFORM_NULL` (see [gotcha #13](#13-glfw-null-platform-for-headless-testing)).

**For SGE desktop app:** `DesktopApplication` should either require `-XstartOnFirstThread`
on macOS or auto-restart the JVM with it. Example auto-restart pattern:

```scala
object DesktopLauncher {
  def main(args: Array[String]): Unit = {
    if (isMacOS && !isOnMainThread) {
      // Restart JVM with -XstartOnFirstThread
      val java = ProcessHandle.current().info().command().orElse("java")
      val cp = System.getProperty("java.class.path")
      val cmd = java :: "-XstartOnFirstThread" :: "-cp" :: cp ::
        getClass.getName.stripSuffix("$") :: args.toList
      val exitCode = new ProcessBuilder(cmd.asJava).inheritIO().start().waitFor()
      System.exit(exitCode)
    }
    // ... normal init ...
  }

  private def isMacOS: Boolean =
    System.getProperty("os.name", "").toLowerCase.contains("mac")

  private def isOnMainThread: Boolean =
    Thread.currentThread().getName == "main" &&
      System.getProperty("javafx.macosx.embedded") == null
}
```

---

## 2. macOS: Homebrew library paths

**Problem:** Homebrew on Apple Silicon installs libraries to `/opt/homebrew/Cellar/<name>/<version>/lib/`
but does NOT always create symlinks in `/opt/homebrew/lib/`. Additionally, versioned dylibs
(e.g., `libglfw.3.4.dylib`) may exist without an unversioned symlink (`libglfw.dylib`).

`System.mapLibraryName("glfw")` returns `libglfw.dylib`, which won't be found.

**Solution:** Search multiple paths and accept versioned dylib names:

1. Check `java.library.path` directories for exact match (`libglfw.dylib`)
2. Check `java.library.path` directories for versioned variants (`libglfw.*.dylib`)
3. Check Homebrew Cellar: `/opt/homebrew/Cellar/<name>/*/lib/`

This is implemented in `WindowingOpsJvm.findLibrary()` and `brewCellarLibDirs()`.

**build.sbt also needs Homebrew paths** in `java.library.path` for forked test JVMs:

```scala
Test / javaOptions ++= {
  val brewLib = if (sys.props("os.name").toLowerCase.contains("mac")) {
    val arm = "/opt/homebrew/lib"
    val x86 = "/usr/local/lib"
    s"${java.io.File.pathSeparator}$arm${java.io.File.pathSeparator}$x86"
  } else ""
  Seq(s"-Djava.library.path=$rustLib$brewLib")
}
```

---

## 3. JVM: Panama FFM requires --enable-native-access

**Problem:** On JDK 22+, Panama FFM restricted methods (`SymbolLookup.libraryLookup`,
`Linker.nativeLinker`, `Arena.global`) print warnings and may be blocked in future releases
unless `--enable-native-access` is set.

**Solution:** Add to JVM options:

```
--enable-native-access=ALL-UNNAMED
```

For sbt forked tests:

```scala
Test / javaOptions += "--enable-native-access=ALL-UNNAMED"
```

For the app launcher (in a future `run.sh` or `build.sbt`):

```scala
javaOptions += "--enable-native-access=ALL-UNNAMED"
```

---

## 4. JVM: SymbolLookup.libraryLookup vs System.load

**Problem:** `SymbolLookup.libraryLookup(path, arena)` loads a native library directly
via Panama. On macOS, this can cause **SIGTRAP** crashes if the library has framework
dependencies (Cocoa, IOKit, CoreVideo) that aren't properly resolved.

**Solution:** Use `System.load(absolutePath)` to load the library through the JVM's
standard native library mechanism (which handles macOS framework dependencies correctly),
then use `SymbolLookup.loaderLookup()` to resolve symbols:

```scala
// Correct: System.load handles macOS framework deps
System.load(found.toAbsolutePath.toString)
val lookup = SymbolLookup.loaderLookup()

// Risky on macOS: direct library load may crash
val lookup = SymbolLookup.libraryLookup(found, Arena.global())
```

`SymbolLookup.loaderLookup()` searches all libraries loaded by the current class loader,
including those loaded via `System.load()`.

---

## 5. JVM: MemorySegment.ofAddress creates zero-length segments

**Problem:** `MemorySegment.ofAddress(long)` creates a native segment at the given address
with **zero length**. This is fine for passing pointers to native functions (Panama only
uses the address), but you cannot read from or write to this segment without reinterpreting
it with a proper size.

**Pattern:** For opaque handles (window, cursor, monitor), zero-length is fine:

```scala
private def ptr(handle: Long): MemorySegment =
  MemorySegment.ofAddress(handle)

private def ptrVal(seg: MemorySegment): Long =
  seg.address()
```

For readable data, use `reinterpret(byteSize)`:

```scala
val seg = MemorySegment.ofAddress(addr).reinterpret(4 * count)
val value = seg.getAtIndex(JAVA_INT, 0)
```

---

## 6. Scala Native: CFuncPtr cannot close over local state

**Problem:** Scala Native's `CFuncPtr` (function pointers for C callbacks) cannot capture
local variables. The compiler errors with:
```
Closing over local state in a function that is passed to extern method is not allowed
```

This is a fundamental limitation: C function pointers are just addresses, they have no
closure environment.

**Solution:** Use a global callback registry pattern:

```scala
// Global registries keyed by window handle
private val cbFramebufferSize =
  mutable.HashMap.empty[Long, (Long, Int, Int) => Unit]

// Static function pointer — no closures, dispatches through registry
private val fnFramebufferSize =
  CFuncPtr3.fromScalaFunction[Ptr[Byte], CInt, CInt, Unit] { (win, w, h) =>
    val handle = longFromPtr(win)
    cbFramebufferSize.get(handle).foreach(_(handle, w, h))
  }

// Register callback
def setFramebufferSizeCallback(
  windowHandle: Long,
  callback: (Long, Int, Int) => Unit
): Unit = {
  if (callback != null) {
    cbFramebufferSize(windowHandle) = callback
    GlfwC.glfwSetFramebufferSizeCallback(ptrFromLong(windowHandle), fnFramebufferSize)
  } else {
    cbFramebufferSize.remove(windowHandle)
    GlfwC.glfwSetFramebufferSizeCallback(ptrFromLong(windowHandle), null)
  }
}
```

SGE uses 12 such registries for GLFW callbacks in `WindowingOpsNative.scala`.

---

## 7. Scala Native: CLong vs Long

**Problem:** `CLong` in Scala Native maps to C's `long`, which is **platform-dependent**:
- 64-bit on Linux/macOS (LP64)
- 32-bit on Windows (LLP64)

Scala's `Long` always maps to C's `long long` (64-bit everywhere).

**Rule:** When the C API uses `int64_t` or fixed-width 64-bit types, use `Long` in
`@extern` declarations, not `CLong`. Use `CLong` only when the C API literally uses `long`.

```scala
@extern object AudioC {
  // C API: int64_t sge_audio_engine_init(void)
  def sge_audio_engine_init(): Long = extern  // Long = int64_t

  // C API: long some_function(long x)
  def some_function(x: CLong): CLong = extern  // CLong = platform long
}
```

---

## 8. Scala Native: fromCString requires explicit charset

**Problem:** In Scala Native 0.5.10+, `fromCString(ptr)` without a charset parameter
fails to compile or uses an implicit that may not be in scope.

**Solution:** Always pass the charset explicitly:

```scala
import java.nio.charset.StandardCharsets

val name = fromCString(cstr, StandardCharsets.UTF_8)
```

---

## 9. Scala Native: toCString uses `using` syntax

**Problem:** In Scala 3 with Scala Native 0.5.x, `toCString` takes the `Zone` as a
context parameter (using), not an implicit parameter.

**Solution:**

```scala
Zone {
  // Correct (Scala 3 / SN 0.5.x)
  val cstr = toCString(scalaString)(using summon[Zone])
  // or simply:
  val cstr = toCString(scalaString)  // Zone is in scope as a given
}
```

---

## 10. Scala Native: Zone.alloc type constraints

**Problem:** `Zone.alloc()` accepts `Int`, `CSize`, or `ULong` as the count parameter,
but NOT Scala `Long`. Passing a `Long` causes a type mismatch.

**Solution:** Use `Int` for small allocations (most cases):

```scala
Zone {
  val buf = alloc[CInt](19)        // OK: Int
  val buf = alloc[CInt](19.toULong) // OK: ULong
  val buf = alloc[CInt](19L)       // ERROR: Long not accepted
}
```

---

## 11. Scala Native: stdlib.malloc overload resolution

**Problem:** `stdlib.malloc()` (from `scala.scalanative.libc.stdlib`) accepts `Int`,
`Long`, or `CSize`. Using `ULong` or other unsigned types may fail overload resolution.

**Solution:** Pass `Int` for small allocations:

```scala
import scala.scalanative.libc.stdlib

val ptr = stdlib.malloc(32)  // OK: Int for 32 bytes
```

---

## 12. Android: Panama FFM is not available

**Problem:** Android's ART runtime does not support `java.lang.foreign` (Panama FFM).
The Panama API is JDK 22+ and is not part of the Android SDK.

**Solution:** SGE retains JNI bridges for Android. The Rust native library
(`native-components/`) has feature flags:
- Default: C ABI exports (for Panama and Scala Native)
- `android` feature: JNI exports via the `jni` crate

Platform-specific code in `core/src/main/scalajvm/` uses Panama FFM.
Future Android backend will use a separate `scalaandroid/` source directory with JNI.

**Future option:** [PanamaPort](https://github.com/nicovova/PanamaPort) (vova7878) is
a backport of `java.lang.foreign` for Android. This could eventually replace JNI on
Android, but it's not production-ready yet.

---

## 13. GLFW null platform for headless testing

**Problem:** GLFW integration tests need to run in CI/headless environments and in
sbt forked JVMs where the main thread isn't the Cocoa thread (see [gotcha #1](#1-macos-glfw-requires-the-main-thread)).

**Solution:** GLFW 3.4+ supports `GLFW_PLATFORM_NULL` — a headless platform backend
with no display, no GPU, and no main-thread requirement.

```scala
windowing.setInitHint(WindowingOps.GLFW_PLATFORM, WindowingOps.GLFW_PLATFORM_NULL)
windowing.init()  // succeeds without a display

// Windows can be created but need GLFW_NO_API (no GL context in null platform)
windowing.setWindowHint(WindowingOps.GLFW_CLIENT_API, WindowingOps.GLFW_NO_API)
val handle = windowing.createWindow(320, 240, "Test")  // succeeds
```

**What works in null platform:**
- Window creation/destruction, resize, position
- Window hints and attributes
- Event polling
- Callbacks (framebuffer size, key, mouse, etc.)
- `glfwGetTime()`, `glfwSetWindowTitle()`, etc.

**What does NOT work:**
- GL/GLES context creation (use `GLFW_NO_API`)
- Monitor enumeration (returns empty/dummy)
- Cursor rendering
- Swap buffers

**Test pattern:** See `DesktopFfiIntegrationTest.scala` for the full example.

---

## 14. LibGDX macOS workaround (reference)

For reference, LibGDX/LWJGL3 solves the macOS main-thread issue differently:

**LWJGL's `glfw_async`:** LWJGL bundles a patched GLFW library (`glfw_async`) that
intercepts Cocoa calls and dispatches them to the main thread via Grand Central Dispatch (GCD).
LibGDX activates this via:

```java
// In Lwjgl3ApplicationConfiguration.java
public static void useGlfwAsync() {
    if (SharedLibraryLoader.os == Os.MacOsX) {
        Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
    }
}
```

This approach is **not available to SGE** because we use Panama FFM with the system GLFW
library, not LWJGL's bundled/patched version.

**What SGE uses instead:**
- Tests: `GLFW_PLATFORM_NULL` (headless, no Cocoa)
- Desktop app: `-XstartOnFirstThread` JVM flag or child-process restart

---

## Pointer/Handle Conversion Patterns

### JVM (Panama FFM)

```scala
// Long → MemorySegment (for passing to native functions)
private def ptr(handle: Long): MemorySegment =
  MemorySegment.ofAddress(handle)

// MemorySegment → Long (for returning from native functions)
private def ptrVal(seg: MemorySegment): Long =
  seg.address()
```

### Scala Native

```scala
import scala.scalanative.runtime.{Intrinsics, fromRawPtr, toRawPtr}

// Long → Ptr[Byte]
private inline def ptrFromLong(h: Long): Ptr[Byte] =
  if (h == 0L) null else fromRawPtr[Byte](Intrinsics.castLongToRawPtr(h))

// Ptr[Byte] → Long
private inline def longFromPtr(p: Ptr[Byte]): Long =
  if (p == null) 0L else Intrinsics.castRawPtrToLong(toRawPtr(p))
```

---

## Quick Reference Table

| Gotcha | Platform | Severity | Fix |
|--------|----------|----------|-----|
| GLFW main thread | macOS | **Fatal** (SIGTRAP) | `GLFW_PLATFORM_NULL` or `-XstartOnFirstThread` |
| Homebrew lib paths | macOS | Link error | Search Cellar + versioned dylibs |
| `--enable-native-access` | JVM 22+ | Warning → future block | Add JVM flag |
| `libraryLookup` crash | macOS JVM | SIGTRAP | Use `System.load` + `loaderLookup` |
| CFuncPtr closures | Scala Native | Compile error | Global callback registry |
| CLong vs Long | Scala Native | Wrong values on Windows | Use `Long` for `int64_t` |
| fromCString charset | Scala Native 0.5.10 | Compile error | Pass `StandardCharsets.UTF_8` |
| toCString syntax | Scala 3 + SN 0.5 | Compile error | `toCString(str)(using zone)` |
| Zone.alloc(Long) | Scala Native | Type error | Use `Int` or `CSize` |
| malloc overload | Scala Native | Type error | Pass `Int` directly |
| Panama on Android | Android | Not available | Retain JNI bridge |
| Null platform windows | GLFW 3.4 | createWindow fails | Set `GLFW_CLIENT_API = GLFW_NO_API` |
