// SGE Native Ops — Rust implementation of ETC1 codec + buffer operations
//
// Modules:
//   etc1       — ETC1 texture compression/decompression (port of etc1_utils.cpp)
//   buffer_ops — Memory copy, vertex transforms, vertex find/compare, memory management
//   jni_bridge — JNI exports for Android (behind "android" feature flag)
//
// C ABI functions are exported from etc1 and buffer_ops for:
//   - Desktop JVM via Panama FFM (java.lang.foreign)
//   - Scala Native via @extern
// JNI functions are exported from jni_bridge for Android (ART doesn't support Panama).

pub mod buffer_ops;
pub mod etc1;

#[cfg(feature = "android")]
pub mod jni_bridge;
