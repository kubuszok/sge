# Native Operations (sge.platform)

SGE uses cross-platform native operations for performance-critical work
(buffer copies, vertex transforms, ETC1 texture codec). The module compiles for
three platforms using `sbt-projectmatrix`:

| Platform | Backend | Implementation |
|----------|---------|---------------|
| **JVM** | Rust via Panama FFM | `BufferOpsJvm.scala` / `ETC1OpsJvm.scala` → `sge_native_ops` library |
| **Scala.js** | Pure Scala | `BufferOpsJs.scala` / `ETC1OpsJs.scala` |
| **Scala Native** | Rust via C ABI | `@link("sge_native_ops") @extern` bindings |
| **Android** (future) | Rust via JNI | `jni_bridge.rs` behind `android` feature flag |

## Package Structure

All native-ops code lives in `sge.platform` with `private[sge]` visibility,
so it is accessible from `core` but hidden from end users.

```
core/
  src/main/scala/sge/platform/        ← Shared traits (all platforms)
    BufferOps.scala                       private[sge] trait
    ETC1Ops.scala                         private[sge] trait
  src/main/scalajvm/sge/platform/     ← JVM implementations (Panama FFM)
  src/main/scalajs/sge/platform/      ← JS implementations (pure Scala)
  src/main/scalanative/sge/platform/  ← Native bindings (@extern C ABI)
```

## Rust Library (native-components)

The Rust library (`native-components/`) provides the compute implementations:

- **`etc1.rs`** — ETC1 texture compression codec (ported from C), with C ABI exports (`etc1_*`)
- **`buffer_ops.rs`** — Memory copy, vertex transforms, vertex find, memory management, with C ABI exports (`sge_*`)
- **`jni_bridge.rs`** — JNI exports for Android only (`#[cfg(feature = "android")]`)

Build and test:
```bash
just rust-build          # cargo build --release (C ABI for Panama + Scala Native)
just rust-build-android  # cargo build --release --features android (adds JNI bridge)
just rust-test           # cargo test
```

### C ABI Naming Convention

C functions exposed for Panama FFM and Scala Native use `sge_` or `etc1_` prefixes:

```rust
#[no_mangle]
pub extern "C" fn sge_transform_v4m4(
    data: *mut f32, stride: c_int, count: c_int,
    matrix: *const f32, offset: c_int,
) { ... }
```

### Panama FFM Usage (JVM)

The JVM implementations use `java.lang.foreign` to call C ABI functions directly:

```scala
private val hTransformV4M4: MethodHandle = linker.downcallHandle(
  lookup("sge_transform_v4m4"),
  FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
)
```

This eliminates JNI, the `jni` Rust crate, and Java `native` method declarations
for desktop JVM. Requires JDK 22+ (Panama FFM finalized).

### JNI Bridge (Android only)

Android's ART runtime does not support Panama FFM. The JNI bridge (`jni_bridge.rs`)
is retained behind the `android` Cargo feature flag:

```rust
#[cfg(feature = "android")]
pub mod jni_bridge;
```

## Core Integration

### BufferUtils.scala

`sge.utils.BufferUtils` uses two imports:
- `sge.platform.PlatformOps` — for Array-based transforms and finds
- Memory management via `PlatformOps.buffer` (newDisposableByteBuffer, freeMemory)

**Key offset conversion**: The old LibGDX JNI took byte offsets and divided by 4
internally. `PlatformOps.buffer` takes float offsets directly:

```scala
// Old (byte offsets):
transformV4M4Jni(data, strideInBytes, count, matrix.values, offset)

// New (float offsets):
PlatformOps.buffer.transformV4M4(data, strideInBytes / 4, count, matrix.values, offset / 4)
```

### ETC1.scala

`sge.graphics.glutils.ETC1` adapts between ByteBuffer (public API) and
Array[Byte] (PlatformOps API):

```scala
val arr = extractBytes(header, offset, PKM_HEADER_SIZE)
etc1.formatHeader(arr, 0, width, height)
putBytes(header, offset, arr, PKM_HEADER_SIZE)
```

Encode operations return malloc-backed direct ByteBuffers via
`PlatformOps.buffer.newDisposableByteBuffer(size)`.

## Adding New Operations

1. Add method to shared trait (`BufferOps.scala` or `ETC1Ops.scala`)
2. Implement in Rust (`buffer_ops.rs` or `etc1.rs`)
3. Add C ABI export (`#[no_mangle] pub extern "C" fn sge_*`)
4. Add JNI export in `jni_bridge.rs` (for Android)
5. Implement JVM Panama bridge (`BufferOpsJvm.scala` / `ETC1OpsJvm.scala`)
6. Implement pure Scala fallback (`BufferOpsJs.scala` / `ETC1OpsJs.scala`)
7. Implement Scala Native binding (`BufferOpsNative.scala` / `ETC1OpsNative.scala`)
8. Add tests
9. Run `just test` to verify JVM, `just test-js` for JS, `just test-native` for Native

## Dependencies

| Dependency | Version | Scope |
|-----------|---------|-------|
| `libc` (Rust) | 0.2 | Always (memory management C ABI) |
| `jni` (Rust) | 0.21 | Optional (android feature) |
| `munit` (Scala) | 1.2.3 | Test only |
