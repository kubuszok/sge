---
description: Load the 14 FFI pitfalls reference for cross-platform native code debugging
---

Load the 14 FFI pitfalls reference for cross-platform native code.

$READ docs/architecture/platform-ffi-gotchas.md

Critical issues: macOS main-thread (GLFW), Homebrew lib paths, Panama flags,
memory segment lifetime, CFuncPtr closures, type mismatches, Android constraints.
Load this when touching platform-specific native code or debugging FFI issues.
