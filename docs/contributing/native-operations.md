# Native Operations (sge.platform)

SGE uses cross-platform native operations for performance-critical work
(buffer copies, vertex transforms, ETC1 texture codec, audio, windowing).
All native ops are in `sge.platform` with `private[sge]` visibility.

| Platform | Backend | Implementation |
|----------|---------|---------------|
| **JVM** | Rust via Panama FFM | `BufferOpsPanama.scala` / `ETC1OpsPanama.scala` → `sge_native_ops` library |
| **Scala.js** | Pure Scala | `BufferOpsJs.scala` / `ETC1OpsJs.scala` |
| **Scala Native** | Rust via C ABI | `@link("sge_native_ops") @extern` bindings |
| **Android** | Rust via PanamaPort | `PanamaPortProvider` → `com.v7878.foreign` (Panama FFM backport for ART) |

## Package Structure

```
sge/
  src/main/scala/sge/platform/           ← Shared traits (all platforms)
    BufferOps.scala                          private[sge] trait
    ETC1Ops.scala                            private[sge] trait
    PlatformOps.scala                        platform service aggregator
  src/main/scalajvm/sge/platform/        ← JVM (Panama FFM downcall handles)
  src/main/scalajs/sge/platform/         ← JS (pure Scala fallbacks)
  src/main/scalanative/sge/platform/     ← Native (@extern C ABI)
  src/main/scaladesktop/sge/platform/    ← Desktop-shared (JVM + Native, not JS)
```

## JVM Platform Modules

JDK-version and Android-specific code is isolated into three companion modules:

| Module | JDK | Purpose |
|--------|-----|---------|
| `sge-jvm-platform-api` | 17 | `PanamaProvider` trait + Android ops interfaces |
| `sge-jvm-platform-jdk` | 22+ | `JdkPanama` — `java.lang.foreign` implementation |
| `sge-jvm-platform-android` | 17 | `PanamaPortProvider` — `com.v7878.foreign` implementation (Panama FFM backport) |

All three are merged into the sge JVM JAR via `packageBin/mappings` (not `dependsOn`
— avoids circular dependency). Runtime detection in `Panama.scala` uses
`Class.forName` + reflection to select the provider.

Both desktop and Android use the same `PanamaProvider` abstraction — only the
underlying FFM implementation differs (`java.lang.foreign` vs `com.v7878.foreign`).

## Rust Libraries (sge-native-providers)

The Rust workspace (`native-components/`) provides compute implementations as
three separate libraries:

### Core (`core/` → `libsge_native_ops`)
- **`etc1.rs`** — ETC1 texture compression codec, C ABI exports (`etc1_*`, `sge_pkm_*`)
- **`buffer_ops.rs`** — Memory copy, vertex transforms, vertex find, C ABI exports (`sge_*`)
- **`audio.rs`** — Audio C ABI stubs (miniaudio bridge built by `build.rs`)
- **`gdx2d.rs`** — Image decoding via `image` crate, C ABI exports (`sge_image_*`)

### FreeType (`freetype/` → `libsge_freetype`)
- **`lib.rs`** — FreeType font rasterization bindings, C ABI exports (`sge_ft_*`)

### Physics (`physics/` → `libsge_physics`)
- **`lib.rs`** — 2D physics via Rapier2D, C ABI exports (`sge_phys_*`)

Build and test (in the external sge-native-providers repo):
```bash
cargo build --release             # All 3 crates
cargo test                        # All 3 crates
```

Inside this repo, native libs are consumed via the published provider
JARs (`com.kubuszok:panama-sge-*-provider`), not built locally.

### C ABI Naming Convention

C functions exposed for Panama FFM and Scala Native use `sge_` or `etc1_` prefixes:

```rust
#[no_mangle]
pub extern "C" fn sge_transform_v4m4(
    data: *mut f32, stride: c_int, count: c_int,
    matrix: *const f32, offset: c_int,
) { ... }
```

### Panama FFM Usage (JVM + Android)

Both desktop JVM and Android use `PanamaProvider` to call C ABI functions directly:

```scala
private val hTransformV4M4: MethodHandle = linker.downcallHandle(
  lookup("sge_transform_v4m4"),
  FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
)
```

Desktop uses `java.lang.foreign` (JDK 22+), Android uses `com.v7878.foreign`
(PanamaPort backport). No JNI anywhere — all platforms use the same C ABI exports.

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
4. Implement JVM Panama bridge (`BufferOpsJvm.scala` / `ETC1OpsJvm.scala`)
5. Implement pure Scala fallback (`BufferOpsJs.scala` / `ETC1OpsJs.scala`)
6. Implement Scala Native binding (`BufferOpsNative.scala` / `ETC1OpsNative.scala`)
7. Add tests
8. Run `re-scale test unit` to verify JVM, `re-scale test unit --js` for JS, `re-scale test unit --native` for Native

## Dependencies

| Dependency | Version | Scope |
|-----------|---------|-------|
| `libc` (Rust) | 0.2 | Always (memory management C ABI) |
| `munit` (Scala) | 1.2.3 | Test only |
