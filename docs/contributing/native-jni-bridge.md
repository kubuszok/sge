# Native Operations (sge.platform)

SGE uses a cross-platform `native-ops` module for performance-critical operations
(buffer copies, vertex transforms, ETC1 texture codec). The module compiles for
three platforms using `sbt-projectmatrix`:

| Platform | Backend | Implementation |
|----------|---------|---------------|
| **JVM** | Rust via JNI | `BufferOpsBridge.java` / `ETC1Bridge.java` → `sge_native_ops` library |
| **Scala.js** | Pure Scala | `BufferOpsJs.scala` / `ETC1OpsJs.scala` |
| **Scala Native** | Rust via C ABI | `@link("sge_native_ops") @extern` bindings |

## Package Structure

All native-ops code lives in `sge.platform` with `private[sge]` visibility,
so it is accessible from `core` but hidden from end users.

```
native-ops/
  src/main/scala/sge/platform/        ← Shared traits (all platforms)
    BufferOps.scala                       private[sge] trait
    ETC1Ops.scala                         private[sge] trait
  src/main/scalajvm/sge/platform/     ← JVM implementations
  src/main/javajvm/sge/platform/      ← JNI bridge declarations
  src/main/scalajs/sge/platform/      ← JS implementations
  src/main/scalanative/sge/platform/  ← Native bindings
  src/test/scala/sge/platform/        ← Shared tests (38 per platform)
```

## Rust Library (native-components)

The Rust library (`native-components/`) provides the compute implementations:

- **`etc1.rs`** — ETC1 texture compression codec (ported from C)
- **`buffer_ops.rs`** — Memory copy, vertex transforms (V×M), vertex find
- **`jni_bridge.rs`** — JNI exports (`#[no_mangle] pub extern "system" fn Java_sge_platform_*`)
- **`c_bridge.rs`** — C ABI exports (`#[no_mangle] pub extern "C" fn sge_*` / `etc1_*`)

Build and test:
```bash
just rust-build          # cargo build --release --features jvm
just rust-test           # cargo test
```

### JNI Naming Convention

JNI functions are named `Java_sge_platform_<ClassName>_<methodName>`:

```rust
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_transformV4M4<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JFloatArray<'local>,
    stride: jint,
    count: jint,
    matrix: JFloatArray<'local>,
    offset: jint,
) { ... }
```

### C ABI Naming Convention

C functions exposed for Scala Native use `sge_` or `etc1_` prefixes:

```rust
#[no_mangle]
pub extern "C" fn sge_transform_v4m4(
    data: *mut f32, stride: c_int, count: c_int,
    matrix: *const f32, offset: c_int,
) { ... }
```

## Core Integration

`core` depends on native-ops JVM:

```scala
val core = (project in file("core"))
  .dependsOn(nativeOps.jvm("3.8.2"))
```

### BufferUtils.scala

`sge.utils.BufferUtils` uses two imports:
- `sge.platform.PlatformOps` — for Array-based transforms and finds
- `sge.platform.BufferOpsBridge` — for memory management (malloc/free)

**Key offset conversion**: The old LibGDX JNI took byte offsets and divided by 4
internally. `PlatformOps.buffer` takes float offsets directly:

```scala
// Old (byte offsets):
transformV4M4Jni(data, strideInBytes, count, matrix.values, offset)

// New (float offsets):
PlatformOps.buffer.transformV4M4(data, strideInBytes / 4, count, matrix.values, offset / 4)
```

Buffer operations use pure NIO (no native calls needed for copies):

```scala
val bb = asByteBuffer(dst)
bb.position(positionInBytes(dst))
bb.asFloatBuffer().put(src, srcOffset, numFloats)
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
`BufferOpsBridge.newDisposableByteBuffer(size)`.

## Testing

| Recipe | Platforms | Expected |
|--------|-----------|----------|
| `just native-ops-test-jvm` | JVM only | 38 pass |
| `just native-ops-test-js` | JS only | 38 pass |
| `just native-ops-test-native` | Native only | 38 pass (CI; may fail locally due to arch mismatch) |
| `just native-ops-test` | Rust build + JVM + JS | 76 pass |

## Adding New Operations

1. Add method to shared trait (`BufferOps.scala` or `ETC1Ops.scala`)
2. Implement in Rust (`buffer_ops.rs` or `etc1.rs`)
3. Add C ABI export in `c_bridge.rs`
4. Add JNI export in `jni_bridge.rs`
5. Add Java `native` declaration in `BufferOpsBridge.java` or `ETC1Bridge.java`
6. Implement JVM wrapper (`BufferOpsJvm.scala` / `ETC1OpsJvm.scala`)
7. Implement pure Scala fallback (`BufferOpsJs.scala` / `ETC1OpsJs.scala`)
8. Implement Scala Native binding (`BufferOpsNative.scala` / `ETC1OpsNative.scala`)
9. Add tests in shared test suite (`BufferOpsSuite.scala` / `ETC1OpsSuite.scala`)
10. Run `just native-ops-test` to verify JVM + JS

## Dependencies

| Dependency | Version | Scope |
|-----------|---------|-------|
| `jni` (Rust) | 0.21 | Optional (jvm feature) |
| `libc` (Rust) | 0.2 | Optional (jvm feature) |
| `munit` (Scala) | 1.2.3 | Test only |
