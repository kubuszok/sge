# iOS Strategy

**Date**: 2026-03-05
**Status**: Deferred (blocked on Scala Native iOS support)

## Goal

Run SGE games on iOS using Scala Native + ANGLE + SDL3, sharing the same
backend code as desktop Native.

## MetalANGLE is Dead -- Use Mainline ANGLE

MetalANGLE's author (kakashidinho / Le Hoang Quyen) joined Google and merged
all Metal backend improvements into mainline ANGLE as of June 2021. The
MetalANGLE repository is no longer actively maintained.

Mainline ANGLE Metal backend status:

| OpenGL ES | Status     |
|-----------|------------|
| ES 2.0    | Complete   |
| ES 3.0    | Complete   |
| ES 3.1    | Complete   |
| ES 3.2    | In progress |

iOS is explicitly supported (iOS 12+, Metal backend only).

## Scala Native iOS Blockers

### Definitely need fixing (in Scala Native C runtime)

| # | Blocker | Effort | Issue |
|---|---------|--------|-------|
| 1 | `sys/posix_sem.h` missing in `process_monitor.cpp` | Low (ifdef) | #2875 |
| 2 | `time_nano.c` macOS version guard excludes iOS | Low (ifdef) | — |
| 3 | `Discover.scala` hardcodes macOS include paths | Medium | #3507 |
| 4 | Other missing POSIX headers (audit needed) | Medium | — |
| 5 | Immix GC mmap too aggressive for iOS memory | Low (config) | #1312 (fixed) |

These are all ifdef guards and build system fixes -- estimated 2-4 weeks of
focused work by someone familiar with Scala Native internals.

### NOT blockers

| Concern | Why it's fine |
|---------|--------------|
| W^X / no JIT | Scala Native is AOT -- no JIT, no executable memory |
| GC memory ops | mprotect for read/write is allowed; only executable pages banned |
| Threading | pthreads fully supported on iOS |
| Bitcode | Apple dropped requirement in Xcode 14 (2022) |
| Framework linking | nativeLinkingOptions supports `-framework UIKit` etc. |
| App Store | ANGLE translates to Metal -- Apple sees Metal calls |

### Objective-C interop -- bypassed by SDL3

The biggest apparent blocker (no ObjC interop in Scala Native) is completely
sidestepped by SDL3 for game engines:

- SDL3 handles all UIKit setup internally (UIWindow, UIViewController, UIView)
- SDL3 manages app lifecycle (foreground/background/terminate)
- SDL3 provides touch input, accelerometer, haptics
- SDL3's API is 100% pure C -- perfect for Scala Native `@extern`
- Entry point is just `int main()` calling SDL3 functions

For general iOS apps, ObjC interop would require years of work (cf. Kotlin/Native).
For games using SDL3, zero ObjC code is needed.

## Target Architecture

```
Scala Native (arm64-apple-ios15.0)
       |
   @extern C FFI
       |
   +---+---+--------+
   |       |        |
  SDL3   ANGLE   miniaudio (or SDL3 audio)
   |       |
  UIKit  Metal    <-- handled internally by SDL3 and ANGLE
```

## Packaging

iOS apps must be .app bundles with Info.plist and code signature.
An Xcode project template + sbt build configuration would handle this.
SDL3's own iOS examples demonstrate the pattern.

## Fallback: MobiVM

If Scala Native iOS remains blocked, MobiVM (actively maintained fork of
RoboVM) can compile JVM bytecode to native ARM for iOS:

- Proven path (LibGDX ships with it)
- Built-in Objective-C interop
- Scala 3 JVM bytecode should work
- Downside: separate backend from Scala Native desktop, no code sharing

## Timeline

Will start iOS support when Scala Native fixes the runtime blockers.
No work planned until then.
