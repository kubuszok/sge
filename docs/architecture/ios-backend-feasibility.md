# iOS Backend Feasibility

Date: 2026-03-13 (consolidated from ios-strategy.md + backend-ios-analysis.md)
Status: Deferred (blocked on Scala Native iOS support)

## 1. Scala Native iOS Support Status

**Current state: Unofficial but functional with caveats.**

Scala Native does not officially list iOS as a supported platform. The documentation
covers macOS, Linux, FreeBSD, OpenBSD, NetBSD, and Windows only. However, community
members have successfully built Scala Native code for iOS.

### Key references

- **Issue [scala-native#4334](https://github.com/scala-native/scala-native/issues/4334)**
  (May 2025, open): Developer (yurique) successfully compiled Scala Native as a static
  library for iOS and linked it into an Xcode project. The approach works but has
  **GC-related segfaults** — the Immix GC causes `EXC_BAD_ACCESS` crashes after repeated
  calls. Workarounds include using `GC.none` or Boehm GC with specific settings.

- **Issue [scala-native#2875](https://github.com/scala-native/scala-native/issues/2875)**
  (Oct 2022, open): Reports linking errors when targeting `x86_64-apple-ios15.5-simulator`
  due to preprocessor guards in Scala Native's C runtime not recognizing iOS (e.g.,
  `time_nano.c` and `process_monitor.cpp`).

- **PR [scala-native#4051](https://github.com/scala-native/scala-native/pull/4051)**
  (Sep 2024, merged): Improved `LinktimeInfo.isMac` detection for alternative LLVM target
  triple formats, but does not address iOS specifically.

### Working reference project

[yurique/XCodeNativeSandbox](https://github.com/yurique/XCodeNativeSandbox) demonstrates
the full workflow:

1. Scala Native compiles to a **static library** (`.a` file) using `buildTarget = libraryStatic`
2. Target triples: `arm64-apple-ios` (device) and `arm64-apple-ios-simulator` (simulator)
3. `@exported` functions provide C-compatible entry points
4. A shell script assembles device + simulator `.a` files into an `.xcframework`
5. The Xcode project links against the `.xcframework` and calls Scala Native via C FFI
6. GC settings: Immix (default, but has issues), Boehm (works with tuning), none (leaks)
7. LTO: none; Mode: releaseFast; Multithreading: disabled

## 2. Apple Developer Account Requirements

| Scenario | Cost | Limitations |
|----------|------|-------------|
| **iOS Simulator** | Free | None — just install Xcode |
| **Physical device** | Free (Personal Team) | 3 devices, profiles expire every 7 days, 10 app IDs |
| **App Store / TestFlight** | $99/year | Full distribution capabilities |

For development and testing, the free account is sufficient.

## 3. iOS Simulator on ARM Mac

- Simulator runs **natively** on Apple Silicon (no emulation overhead)
- **OpenGL ES in simulator: degrading/broken.** iOS 17.5+ simulators crash on `glDrawElements`
  calls. Apple is actively removing OpenGL ES support; Metal is the only path forward.
- Older simulator runtimes (iOS 15.2) still work but are not a long-term solution.
- **ANGLE on iOS**: translates OpenGL ES calls to Metal backend. MetalANGLE's work was
  merged into upstream ANGLE. Building ANGLE for iOS requires Chromium checkout.

## 4. Technical Approach Options

### Option A: Scala Native static library + Xcode project (PROVEN)

This is the approach demonstrated by XCodeNativeSandbox:

1. Scala Native compiles game engine to `.a` static library
2. Set target triple to `arm64-apple-ios` / `arm64-apple-ios-simulator`
3. `@exported` functions provide C-compatible entry points
4. Assemble `.xcframework` from device + simulator `.a` files
5. Xcode project links against the framework
6. Swift/ObjC app code handles UIKit lifecycle, calls into Scala Native via C FFI

### Option B: GLFW + ANGLE (desktop approach) — NOT viable for iOS

GLFW does **not** support iOS. It only supports Windows, macOS, Linux/Wayland/X11.
There is no planned iOS support.

### Option C: SDL3 for windowing + ANGLE for GL — VIABLE

SDL3 has first-class iOS support:
- Handles UIKit integration (window creation, event loop, lifecycle)
- Supports both OpenGL ES and Metal rendering
- Provides xcframework distribution
- Requires Xcode 12.2+ and iOS 14.2 SDK
- Single-window only on iOS
- Could replace GLFW on the iOS platform while keeping ANGLE for OpenGL ES translation

### Option D: GLFM (GLFW-inspired, mobile-focused)

[GLFM](https://github.com/brackeen/glfm) is an alternative inspired by GLFW that
supports iOS, Android, and WebGL. Smaller and simpler than SDL3.

### How LibGDX's iOS backend works (RoboVM)

- [RoboVM/MobiVM](https://github.com/MobiVM/robovm) is an AOT compiler for JVM bytecode targeting iOS
- Pipeline: Java bytecode → Soot (Jimple IR) → optimization → LLVM IR → native ARM binary
- Uses "Bro" bridge for calling Objective-C/C from Java
- IOSGraphics uses `EAGLContext` + `GLKView` + `GLKViewController` for OpenGL ES rendering
- IOSGLES20/30 are JNI-like native method wrappers around OpenGL ES C calls
- IOSApplication extends `UIApplicationDelegateAdapter` for lifecycle
- Audio uses ObjectAL (Objective-C library)
- Has `IS_METALANGLE` flag suggesting MetalANGLE integration was planned

Key insight: RoboVM solves the same problem Scala Native solves (AOT to native ARM),
but with a full JVM class library reimplementation. Scala Native compiles Scala directly
to LLVM IR without the JVM bytecode intermediate step.

## 5. Component Mapping: Desktop Native → iOS

| Component | Desktop Native | iOS | Shared? |
|-----------|---------------|-----|---------|
| **Windowing** | GLFW | SDL3 (UIKit support) | No |
| **GL rendering** | ANGLE (GL ES → desktop GL) | ANGLE (GL ES → Metal) | Yes |
| **Audio** | miniaudio | miniaudio | Yes |
| **GL bindings** | AngleGL20/30 via @extern | AngleGL20/30 via @extern | Yes |
| **Music/Sound** | miniaudio wrappers | miniaudio wrappers | Yes |
| **Files** | java.io.File (POSIX) | NSBundle / Documents dir | Partial |
| **Input** | GLFW keyboard + mouse | SDL3 touch + accelerometer | No |

Only ~8 files are truly iOS-specific: IOSApplication, IOSGraphics, IOSInput, IOSFiles,
IOSFileHandle, IOSNet, IOSPreferences, IOSDevice/IOSScreenBounds.

## 6. Testing Strategy

### Phase 1 — macOS first (no iOS needed)

Test ANGLE GL + miniaudio + core rendering on macOS Scala Native. This validates all
shared code without touching iOS at all. Already partially working (Pong Native links
and runs on macOS).

### Phase 2 — iOS simulator

Compile as static library targeting `arm64-apple-ios-simulator`. Test in simulator with
Boehm GC. Free — no Apple Developer account needed.

### Phase 3 — Thin iOS layer

Implement SDL3 for UIKit lifecycle + touch input. Only ~8 new files needed.

### Phase 4 — Physical device (optional)

Free provisioning profile (Personal Team). Limited to 3 devices, 7-day expiry.

## 7. Blockers

| Blocker | Severity | Status | Investigation |
|---------|----------|--------|---------------|
| **GC segfaults on iOS** | High | Open (scala-native#4334) | Try Boehm GC with tuning; monitor upstream |
| **C runtime preprocessor guards** | Medium | Open (scala-native#2875) | `time_nano.c` / `process_monitor.cpp` don't recognize iOS |
| **ANGLE iOS build complexity** | Medium | Solvable | Must build from Chromium checkout |
| **GLFW not available on iOS** | Resolved | N/A | SDL3 or GLFM replaces it |
| **OpenGL ES deprecation** | Resolved | N/A | ANGLE translates to Metal |
| **No JIT / no reflection** | Non-issue | N/A | Scala Native is already AOT; SGE eliminated reflection |
| **Paid Apple account** | Non-issue | N/A | Free for simulator + limited device testing |

## 8. Rust Native Components on iOS

The Rust `native-components/` library (GLFW, miniaudio, buffer ops) already cross-compiles
to multiple targets. Adding iOS targets should be straightforward:

- `aarch64-apple-ios` (device)
- `aarch64-apple-ios-sim` (simulator on Apple Silicon)
- GLFW would be excluded (not supported on iOS)
- miniaudio supports iOS natively
- Buffer ops are pure C/Rust — platform-independent

## 9. MetalANGLE is Dead — Use Mainline ANGLE

MetalANGLE's author (kakashidinho) joined Google and merged all Metal backend
improvements into mainline ANGLE as of June 2021. Mainline ANGLE now has
complete ES 3.1 on Metal, surpassing MetalANGLE's ~90% ES 3.0 coverage.
iOS is explicitly supported (iOS 12+, Metal backend only).

## 10. Objective-C Interop — Bypassed by SDL3

The biggest apparent blocker (no ObjC interop in Scala Native) is completely
sidestepped by SDL3 for game engines: SDL3 handles all UIKit setup internally,
manages app lifecycle, provides touch input/accelerometer/haptics, and exposes
a 100% pure C API — perfect for Scala Native `@extern`. For general iOS apps,
ObjC interop would require years of work; for games using SDL3, zero ObjC is needed.

## 11. LibGDX iOS Backend Reference

The LibGDX iOS backend (`gdx-backend-robovm/`) contains 38 files (~3,950 LOC):
21 root (IOSApplication, IOSGraphics, IOSInput, IOSGLES20/30, IOSHaptics, etc.),
13 ObjectAL audio bindings, 4 deprecated UIKit custom bindings. Uses RoboVM/MobiVM
to compile JVM bytecode to native ARM. Key dependencies: GLKit (deprecated by Apple),
EAGLContext for GL ES, ObjectAL for audio, UIKit for lifecycle/input.

SGE's approach shares ANGLE + miniaudio with desktop Native, requiring only ~8
iOS-specific files (windowing via SDL3 instead of GLFW, touch input, file paths).

## 12. Deferred iOS Files (14 total, from migration-status.tsv)

| File | Notes |
|------|-------|
| IOSApplication | SDL3 lifecycle, replaces UIApplicationDelegateAdapter |
| IOSGraphics | SDL3 window + ANGLE context setup |
| IOSGLES20 | Share AngleGL20 with desktop |
| IOSGLES30 | Share AngleGL30 with desktop |
| IOSInput | SDL3 touch events + accelerometer |
| IOSAudio | Share miniaudio with desktop |
| IOSMusic | Share DesktopMusic with desktop |
| IOSSound | Share DesktopSound with desktop |
| IOSFiles | NSBundle + Documents directory paths |
| IOSFileHandle | iOS-specific file resolution |
| IOSNet | UIApplication.openURL for browser launch |
| IOSPreferences | NSUserDefaults or file-based |
| IOSHaptics | UIImpactFeedbackGenerator |
| IOSDevice/IOSScreenBounds | UIScreen metrics |
