// Build script for sge-native-ops: compiles vendored C libraries and bridges.
//
// Vendored libraries:
//   - miniaudio (public domain / MIT-0) — single-file audio library
//   - GLFW 3.4 (zlib/libpng license) — windowing, input, context creation
//
// Strategy: Rust's cdylib target only exports #[no_mangle] Rust symbols, so C
// libraries that need their own exported symbols are built as SEPARATE shared
// libraries placed alongside libsge_native_ops in the output directory.
//
// Output (all in native-components/target/release/):
//   - libsge_native_ops.{dylib,so,dll}  — Rust code (buffer ops, ETC1, transforms)
//   - libsge_audio.{dylib,so,dll}       — miniaudio + audio bridge (37 sge_audio_* functions)
//   - libglfw.{dylib,so,dll}            — GLFW windowing library (built from source)
//
// This means downstream has zero external native dependencies — everything is
// compiled from source and placed in a single directory.

fn main() {
    let out_dir = std::env::var("OUT_DIR").unwrap();
    let target_os = std::env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
    let target_env = std::env::var("CARGO_CFG_TARGET_ENV").unwrap_or_default();

    // Determine the final output directory (where libsge_native_ops will be placed)
    let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
    let profile = std::env::var("PROFILE").unwrap(); // "debug" or "release"
    let release_dir = format!("{}/target/{}", manifest_dir, profile);

    build_audio_bridge_shared(&out_dir, &release_dir, &target_os);
    build_glfw_shared(&out_dir, &release_dir, &target_os, &target_env);
    link_system_libs(&target_os);

    // Audio bridge is loaded separately now — no need to link into libsge_native_ops
    println!("cargo:rerun-if-changed=vendor/sge_audio_bridge.c");
    println!("cargo:rerun-if-changed=vendor/miniaudio/miniaudio.c");
    println!("cargo:rerun-if-changed=vendor/miniaudio/miniaudio.h");
    println!("cargo:rerun-if-changed=vendor/glfw/src");
    println!("cargo:rerun-if-changed=vendor/glfw/include");
}

/// Compile miniaudio + audio bridge as a static archive, then link it into a
/// separate shared library (libsge_audio) placed in the release directory.
fn build_audio_bridge_shared(out_dir: &str, release_dir: &str, target_os: &str) {
    cc::Build::new()
        .file("vendor/sge_audio_bridge.c")
        .include("vendor/miniaudio")
        .include("vendor")
        .define("MA_NO_GENERATION", None)
        .warnings(false)
        .cargo_metadata(false)
        .pic(true) // position-independent code for shared library
        .compile("sge_audio_bridge");

    // Create shared library from the static archive
    let archive = format!("{}/libsge_audio_bridge.a", out_dir);
    let (dylib_name, link_args) = match target_os {
        "macos" | "ios" => (
            "libsge_audio.dylib",
            vec![
                "-dynamiclib".into(),
                "-Wl,-all_load".into(),
                archive.clone(),
                "-framework".into(), "AudioToolbox".into(),
                "-framework".into(), "CoreAudio".into(),
                "-framework".into(), "CoreFoundation".into(),
                "-install_name".into(), "@rpath/libsge_audio.dylib".into(),
            ],
        ),
        "windows" => (
            "sge_audio.dll",
            vec![
                "-shared".into(),
                "-Wl,--whole-archive".into(),
                archive.clone(),
                "-Wl,--no-whole-archive".into(),
            ],
        ),
        _ => (
            "libsge_audio.so",
            vec![
                "-shared".into(),
                "-Wl,--whole-archive".into(),
                archive.clone(),
                "-Wl,--no-whole-archive".into(),
                "-lpthread".into(),
                "-lm".into(),
            ],
        ),
    };

    let output = format!("{}/{}", release_dir, dylib_name);
    let cc_tool = cc::Build::new().get_compiler();
    let status = std::process::Command::new(cc_tool.path())
        .args(&link_args)
        .arg("-o")
        .arg(&output)
        .status()
        .unwrap_or_else(|e| panic!("Failed to link {}: {}", dylib_name, e));
    assert!(status.success(), "Failed to link {}", dylib_name);
    eprintln!("cargo:warning=Built {}", output);
}

/// Compile GLFW from vendored source as a static archive, then link it into a
/// separate shared library (libglfw) placed in the release directory.
fn build_glfw_shared(out_dir: &str, release_dir: &str, target_os: &str, target_env: &str) {
    let glfw_src = "vendor/glfw/src";
    let glfw_include = "vendor/glfw/include";

    let mut build = cc::Build::new();
    build
        .include(glfw_include)
        .include(glfw_src)
        .warnings(false)
        .cargo_metadata(false)
        .pic(true);

    // Common sources (all platforms)
    for f in &[
        "context.c", "init.c", "input.c", "monitor.c", "platform.c",
        "vulkan.c", "window.c", "egl_context.c", "osmesa_context.c",
        "null_init.c", "null_monitor.c", "null_window.c", "null_joystick.c",
    ] {
        build.file(format!("{}/{}", glfw_src, f));
    }

    let framework_args: Vec<String> = match target_os {
        "macos" | "ios" => {
            build.define("_GLFW_COCOA", None);
            for f in &[
                "cocoa_init.m", "cocoa_joystick.m", "cocoa_monitor.m",
                "cocoa_window.m", "nsgl_context.m",
            ] {
                build.file(format!("{}/{}", glfw_src, f));
            }
            for f in &["cocoa_time.c", "posix_module.c", "posix_thread.c"] {
                build.file(format!("{}/{}", glfw_src, f));
            }
            vec![
                "-framework".into(), "Cocoa".into(),
                "-framework".into(), "IOKit".into(),
                "-framework".into(), "CoreFoundation".into(),
                "-framework".into(), "CoreVideo".into(),
                "-install_name".into(), "@rpath/libglfw.dylib".into(),
            ]
        }
        "windows" => {
            build.define("_GLFW_WIN32", None);
            build.define("UNICODE", None);
            build.define("_UNICODE", None);
            if target_env == "gnu" {
                build.define("WINVER", Some("0x0501"));
            }
            for f in &[
                "win32_init.c", "win32_joystick.c", "win32_monitor.c",
                "win32_window.c", "wgl_context.c",
                "win32_module.c", "win32_time.c", "win32_thread.c",
            ] {
                build.file(format!("{}/{}", glfw_src, f));
            }
            vec!["-lgdi32".into(), "-luser32".into(), "-lshell32".into()]
        }
        "linux" | "freebsd" | "dragonfly" | "netbsd" | "openbsd" => {
            build.define("_GLFW_X11", None);
            build.define("_DEFAULT_SOURCE", None);
            for f in &[
                "x11_init.c", "x11_monitor.c", "x11_window.c",
                "xkb_unicode.c", "glx_context.c",
                "posix_module.c", "posix_time.c", "posix_thread.c",
                "posix_poll.c", "linux_joystick.c",
            ] {
                build.file(format!("{}/{}", glfw_src, f));
            }
            vec!["-lX11".into(), "-lpthread".into(), "-lm".into(), "-ldl".into()]
        }
        _ => {
            eprintln!("cargo:warning=GLFW: unsupported target OS '{}', using null backend only", target_os);
            vec![]
        }
    };

    build.compile("glfw3");

    // Create shared library from the static archive
    let archive = format!("{}/libglfw3.a", out_dir);
    let dylib_name = match target_os {
        "macos" | "ios" => "libglfw.dylib",
        "windows" => "glfw.dll",
        _ => "libglfw.so",
    };

    let shared_flag = match target_os {
        "macos" | "ios" => "-dynamiclib",
        _ => "-shared",
    };

    let whole_archive_flag = match target_os {
        "macos" | "ios" => "-Wl,-all_load",
        _ => "-Wl,--whole-archive",
    };

    let output = format!("{}/{}", release_dir, dylib_name);
    let cc_tool = cc::Build::new().get_compiler();
    let mut cmd = std::process::Command::new(cc_tool.path());
    cmd.arg(shared_flag)
       .arg(whole_archive_flag)
       .arg(&archive);

    // On Linux, close the whole-archive group
    if target_os != "macos" && target_os != "ios" {
        cmd.arg("-Wl,--no-whole-archive");
    }

    cmd.args(&framework_args)
       .arg("-o")
       .arg(&output);

    let status = cmd.status()
        .unwrap_or_else(|e| panic!("Failed to link {}: {}", dylib_name, e));
    assert!(status.success(), "Failed to link {}", dylib_name);
    eprintln!("cargo:warning=Built {}", output);
}

/// Link system libraries needed by libsge_native_ops itself (Rust code only).
fn link_system_libs(target_os: &str) {
    // libsge_native_ops itself only needs libc (provided by the Rust toolchain).
    // The C libraries (audio, GLFW) are separate shared libraries and handle
    // their own system library dependencies.
    let _ = target_os;
}
