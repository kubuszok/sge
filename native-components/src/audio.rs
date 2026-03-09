// Audio bridge — C ABI functions implemented in vendor/sge_audio_bridge.c
//
// The actual implementation lives in C (sge_audio_bridge.c) which wraps miniaudio's
// Engine/Sound/Device APIs. The C file is compiled by build.rs via the cc crate and
// linked with --whole-archive / -force_load so all 37 sge_audio_* symbols are
// exported from the final cdylib/staticlib without needing Rust references.
//
// See vendor/sge_audio_bridge.c for the implementation.
