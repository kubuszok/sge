// SGE Native Ops — Rust implementation of ETC1 codec + buffer operations
//
// Modules:
//   etc1       — ETC1 texture compression/decompression (port of etc1_utils.cpp)
//   buffer_ops — Memory copy, vertex transforms, vertex find/compare
//   jni_bridge — JNI exports for JVM (behind "jvm" feature flag)
//
// C ABI functions are exported from etc1 and buffer_ops for Scala Native @extern.
// JNI functions are exported from jni_bridge for JVM System.loadLibrary.

pub mod buffer_ops;
pub mod etc1;

#[cfg(feature = "jvm")]
pub mod jni_bridge;
