# Type Safety Improvements

## TS-001: GL Constants as Opaque Types

- **LibGDX**: `com.badlogic.gdx.graphics.GL20`, `GL30`, `GL31`, `GL32`
- **SGE**: `sge.graphics.GL20`, `GL30`, `GL31`, `GL32`
- **Problem**: LibGDX defines 600+ OpenGL constants as bare `int` values. Any integer
  can be passed where a GL constant is expected, leading to runtime errors that are
  hard to diagnose (e.g., passing a texture format where a blend mode is expected).
- **Improvement**: Use Scala 3 opaque types to create zero-cost typed wrappers for GL
  constant categories (blend modes, texture formats, buffer targets, etc). This catches
  misuse at compile time with no runtime overhead.
- **Status**: proposed — detailed roadmap in [opaque-types.md](opaque-types.md)

## TS-002: Nullable Opaque Type

- **LibGDX**: `com.badlogic.gdx.utils.Null` (annotation)
- **SGE**: `sge.utils.Nullable[A]` (opaque type)
- **Problem**: LibGDX uses `@Null` annotation to document nullable parameters/returns,
  but this is purely informational. The compiler does not enforce null safety, leading
  to NullPointerExceptions at runtime.
- **Improvement**: `Nullable[A]` is an opaque type that forces callers to handle the
  null case explicitly via `fold`, `getOrElse`, `foreach`, etc. Zero runtime overhead
  (it's erased to the underlying type) but full compile-time safety.
- **Status**: implemented

## TS-003: AutoCloseable Instead of Disposable

- **LibGDX**: `com.badlogic.gdx.utils.Disposable` (custom interface)
- **SGE**: `java.lang.AutoCloseable` / `close()` method
- **Problem**: LibGDX defines its own `Disposable` interface with a `dispose()` method.
  This is incompatible with Java's try-with-resources and Scala's `Using` facility,
  requiring manual cleanup and leading to resource leaks.
- **Improvement**: Use the standard `AutoCloseable` interface with `close()`. This enables:
  - Scala `Using` blocks for automatic resource management
  - Java try-with-resources interop
  - IDE support for leak detection
- **Status**: implemented
