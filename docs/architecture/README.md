# Architecture

Documentation for SGE's architecture, platform targets, and build structure.

## Current Architecture

| Document | Purpose |
|----------|---------|
| [platform-targets.md](platform-targets.md) | Platform target matrix, ANGLE/Panama rationale, per-platform details |
| [sge-core-interfaces.md](sge-core-interfaces.md) | Backend trait inventory — 9 traits, 150 abstract methods, Sge context |
| [build-structure.md](build-structure.md) | SBT multi-project layout and module structure |
| [cross-platform-settings.md](cross-platform-settings.md) | Settings reference — plugin versions, build.sbt patterns, gotchas |
| [platform-ffi-gotchas.md](platform-ffi-gotchas.md) | FFI gotchas — Panama, Scala Native, macOS, GLFW pitfalls |
| [android-native-constraints.md](android-native-constraints.md) | Android constraints — API level, PanamaPort, NDK |
| [physics-limitations.md](physics-limitations.md) | Rapier2D vs Box2D — unsupported joints, callbacks, workarounds |

## Future / Deferred

| Document | Purpose |
|----------|---------|
| [ios-backend-feasibility.md](ios-backend-feasibility.md) | iOS via Scala Native + SDL3 + ANGLE (deferred, blocked on SN iOS) |
| [wasm-strategy.md](wasm-strategy.md) | Scala.js WASM backend — opt-in, zero code changes |

