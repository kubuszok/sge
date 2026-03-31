// SGE Native Ops — Rust implementation of ETC1 codec + buffer operations + extensions
//
// Modules:
//   etc1       — ETC1 texture compression/decompression (port of etc1_utils.cpp)
//   buffer_ops — Memory copy, vertex transforms, vertex find/compare, memory management
//   jni_bridge — JNI exports for Android (behind "android" feature flag)
//   freetype   — FreeType font rasterization bindings (behind "freetype_support" feature flag)
//   audio      — Audio C ABI stubs (unconditional — no external dependency)
//   physics    — 2D physics via Rapier2D (behind "physics" feature flag)
//   gdx2d      — Image decoding via `image` crate (behind "image_decode" feature flag)
//
// C ABI functions are exported from etc1 and buffer_ops for:
//   - Desktop JVM via Panama FFM (java.lang.foreign)
//   - Scala Native via @extern
// JNI functions are exported from jni_bridge for Android (ART doesn't support Panama).

pub mod buffer_ops;
pub mod etc1;

#[cfg(feature = "android")]
pub mod jni_bridge;

#[cfg(feature = "freetype_support")]
pub mod freetype;

pub mod audio;

#[cfg(feature = "physics")]
pub mod physics;

#[cfg(feature = "image_decode")]
pub mod gdx2d;

// Note: Controller/joystick FFI is handled directly by the sge-controllers
// extension via GLFW calls (Scala Native @link("glfw3") / JVM Panama).
// No Rust wrapper needed since GLFW is already linked by sge-core.
