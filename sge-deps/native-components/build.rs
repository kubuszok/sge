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

    // Determine the final output directory (where libsge_native_ops will be placed).
    // Derive from OUT_DIR to handle both `cargo build` and `cargo build --target <triple>`
    // correctly. When --target is explicit (even if it matches host), cargo uses
    // target/<triple>/<profile>/, but TARGET == HOST so we can't distinguish by comparing them.
    // OUT_DIR is always correct: .../target[/<triple>]/<profile>/build/<pkg>/out
    let target = std::env::var("TARGET").unwrap_or_default();
    let host = std::env::var("HOST").unwrap_or_default();
    let release_dir = {
        let out = std::path::Path::new(&out_dir);
        // OUT_DIR = .../target[/<triple>]/<profile>/build/<pkg>/out
        // Go up 3 levels: out -> <pkg> -> build -> <profile>
        out.parent().unwrap().parent().unwrap().parent().unwrap()
            .to_str().unwrap().to_string()
    };

    let skip_c_libs = std::env::var("SGE_SKIP_C_LIBS").unwrap_or_default() == "1";
    let is_android = target_os == "android";
    let is_windows = target_os == "windows";
    let is_cross = target != host && !target.is_empty();

    // On Windows cross-compilation, merge C libs into sge_native_ops.dll instead of
    // creating separate DLLs. This avoids complex lld-link invocations with Windows SDK
    // import libs. The JVM NativeLibLoader falls back to sge_native_ops when companion
    // libs aren't found as separate files.
    let merge_into_cdylib = is_windows && is_cross;

    if !skip_c_libs && !is_android {
        if merge_into_cdylib {
            // Compile C code and let cargo link it into sge_native_ops.dll.
            // cargo_metadata(true) = default, tells cargo to link this archive.
            build_audio_bridge_merged(&target_os);
            build_glfw_merged(&target_os, &target_env);
        } else {
            // Create separate shared libraries (macOS, Linux, native Windows builds)
            build_audio_bridge_shared(&out_dir, &release_dir, &target_os);
            build_glfw_shared(&out_dir, &release_dir, &target_os, &target_env);
        }
    } else if !skip_c_libs && is_android {
        // Android only needs audio bridge (no GLFW)
        build_audio_bridge_shared(&out_dir, &release_dir, &target_os);
    }
    link_system_libs(&target_os);

    // Copy static archives to release dir for Scala Native static linking.
    if !skip_c_libs && !merge_into_cdylib {
        copy_static_archive(&out_dir, &release_dir, "sge_audio_bridge", "sge_audio");
        if !is_android {
            copy_static_archive(&out_dir, &release_dir, "glfw3", "glfw3");
        }
    }

    // Audio bridge is loaded separately now — no need to link into libsge_native_ops
    println!("cargo:rerun-if-changed=vendor/sge_audio_bridge.c");
    println!("cargo:rerun-if-changed=vendor/miniaudio/miniaudio.c");
    println!("cargo:rerun-if-changed=vendor/miniaudio/miniaudio.h");
    println!("cargo:rerun-if-changed=vendor/glfw/src");
    println!("cargo:rerun-if-changed=vendor/glfw/include");
    println!("cargo:rerun-if-changed=vendor/glfw_platform_stubs.c");
}

/// Compile audio bridge and link it into the main sge_native_ops cdylib (Windows cross-compilation).
/// Uses cargo_metadata(true) so cargo links the C archive into the Rust cdylib.
fn build_audio_bridge_merged(target_os: &str) {
    let mut build = cc::Build::new();
    build
        .file("vendor/sge_audio_bridge.c")
        .include("vendor/miniaudio")
        .include("vendor")
        .define("MA_NO_GENERATION", None)
        .warnings(false)
        .pic(true);
    build.compile("sge_audio_bridge");
    // cargo_metadata is true by default — cargo will link the archive into sge_native_ops
    // Link Windows audio system libraries
    if target_os == "windows" {
        println!("cargo:rustc-link-lib=ole32");
        println!("cargo:rustc-link-lib=user32");
        println!("cargo:rustc-link-lib=advapi32");
    }
}

/// Compile GLFW and link it into the main sge_native_ops cdylib (Windows cross-compilation).
fn build_glfw_merged(target_os: &str, target_env: &str) {
    let glfw_src = "vendor/glfw/src";
    let glfw_include = "vendor/glfw/include";
    let mut build = cc::Build::new();
    build
        .include(glfw_include)
        .include(glfw_src)
        .warnings(false)
        .pic(true);

    // Common sources
    for f in &[
        "context.c", "init.c", "input.c", "monitor.c", "platform.c",
        "vulkan.c", "window.c", "egl_context.c", "osmesa_context.c",
        "null_init.c", "null_monitor.c", "null_window.c", "null_joystick.c",
    ] {
        build.file(format!("{}/{}", glfw_src, f));
    }

    if target_os == "windows" {
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
    }

    build.file("vendor/glfw_platform_stubs.c");
    build.compile("glfw3");

    // Link Windows system libraries
    if target_os == "windows" {
        println!("cargo:rustc-link-lib=gdi32");
        println!("cargo:rustc-link-lib=user32");
        println!("cargo:rustc-link-lib=shell32");
    }
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
        "windows" => {
            // When cross-compiling with cargo-xwin, cc returns clang-cl which uses
            // MSVC-style flags. Use lld-link directly to create the DLL, passing
            // the Windows SDK/CRT lib paths from cargo-xwin's cache.
            let target = std::env::var("TARGET").unwrap_or_default();
            let host = std::env::var("HOST").unwrap_or_default();
            let is_cross = target != host && !target.is_empty();
            if is_cross {
                let dll_output = format!("{}/sge_audio.dll", release_dir);
                let arch = if target.contains("aarch64") { "arm64" } else { "x86_64" };
                // Find xwin cache dir (cargo-xwin downloads Windows SDK here)
                let home = std::env::var("HOME").unwrap_or_default();
                let xwin_cache = format!("{}/.cache/cargo-xwin/xwin", home);
                // Fallback to Library/Caches on macOS
                let xwin_dir = if std::path::Path::new(&xwin_cache).exists() {
                    xwin_cache
                } else {
                    format!("{}/Library/Caches/cargo-xwin/xwin", home)
                };
                let machine = if target.contains("aarch64") { "/machine:arm64x" } else { "/machine:x64" };
                let status = lld_link_command()
                    .arg("/dll")
                    .arg("/force:multiple")
                    .arg(machine)
                    .arg(&format!("/out:{}", dll_output))
                    .arg("/wholearchive")
                    .arg(&archive)
                    .arg(&format!("/libpath:{}/crt/lib/{}", xwin_dir, arch))
                    .arg(&format!("/libpath:{}/sdk/lib/um/{}", xwin_dir, arch))
                    .arg(&format!("/libpath:{}/sdk/lib/ucrt/{}", xwin_dir, arch))
                    .args(&["kernel32.lib", "ucrt.lib", "vcruntime.lib", "ole32.lib",
                            "user32.lib", "advapi32.lib"])
                    .status()
                    .unwrap_or_else(|e| panic!("Failed to link sge_audio.dll: {}", e));
                assert!(status.success(), "Failed to link sge_audio.dll");
                eprintln!("cargo:warning=Built {}", dll_output);
                return;
            }
            (
                "sge_audio.dll",
                vec![
                    "-shared".into(),
                    "-Wl,--whole-archive".into(),
                    archive.clone(),
                    "-Wl,--no-whole-archive".into(),
                ],
            )
        },
        "android" => (
            "libsge_audio.so",
            vec![
                "-shared".into(),
                "-Wl,--whole-archive".into(),
                archive.clone(),
                "-Wl,--no-whole-archive".into(),
                "-lm".into(),
                "-llog".into(),
                "-lOpenSLES".into(),
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
            // When cross-compiling for Linux (e.g. from macOS via zigbuild), X11
            // headers aren't in the default sysroot. Use vendored headers if available.
            let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
            let x11_include = format!("{}/vendor/x11-include", manifest_dir);
            if std::path::Path::new(&x11_include).join("X11/Xlib.h").exists() {
                build.include(&x11_include);
            }
            for f in &[
                "x11_init.c", "x11_monitor.c", "x11_window.c",
                "xkb_unicode.c", "glx_context.c",
                "posix_module.c", "posix_time.c", "posix_thread.c",
                "posix_poll.c", "linux_joystick.c",
            ] {
                build.file(format!("{}/{}", glfw_src, f));
            }
            // When cross-compiling, system libs (-lX11 etc.) aren't available.
            // GLFW dlopen's X11 at runtime, so we can skip them and allow
            // undefined symbols in the shared library.
            let target = std::env::var("TARGET").unwrap_or_default();
            let host = std::env::var("HOST").unwrap_or_default();
            let is_cross = target != host && !target.is_empty();
            if is_cross {
                vec!["-Wl,--allow-shlib-undefined".into(), "-lpthread".into(), "-lm".into(), "-ldl".into()]
            } else {
                vec!["-lX11".into(), "-lpthread".into(), "-lm".into(), "-ldl".into()]
            }
        }
        _ => {
            eprintln!("cargo:warning=GLFW: unsupported target OS '{}', using null backend only", target_os);
            vec![]
        }
    };

    // Platform stubs for native window handle functions (glfwGetCocoaWindow,
    // glfwGetX11Window, glfwGetWin32Window). Scala Native requires ALL @extern
    // symbols to resolve, even if guarded by runtime checks. The stubs use the
    // same _GLFW_COCOA/_GLFW_X11/_GLFW_WIN32 defines to only provide stubs for
    // functions that don't exist on the current platform.
    build.file("vendor/glfw_platform_stubs.c");

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

    // When cross-compiling for Windows, use lld-link directly (cc returns clang-cl
    // which uses MSVC-style flags incompatible with our Unix-style link commands).
    let target = std::env::var("TARGET").unwrap_or_default();
    let host = std::env::var("HOST").unwrap_or_default();
    let is_cross = target != host && !target.is_empty();
    if target_os == "windows" && is_cross {
        let arch = if target.contains("aarch64") { "arm64" } else { "x86_64" };
        let home = std::env::var("HOME").unwrap_or_default();
        let xwin_cache = format!("{}/.cache/cargo-xwin/xwin", home);
        let xwin_dir = if std::path::Path::new(&xwin_cache).exists() {
            xwin_cache
        } else {
            format!("{}/Library/Caches/cargo-xwin/xwin", home)
        };
        // Convert -lfoo flags to foo.lib
        let link_libs: Vec<String> = framework_args.iter()
            .filter(|a| a.starts_with("-l"))
            .map(|a| format!("{}.lib", &a[2..]))
            .collect();
        let machine = if target.contains("aarch64") { "/machine:arm64x" } else { "/machine:x64" };
        let status = lld_link_command()
            .arg("/dll")
            .arg("/force:multiple")
            .arg(machine)
            .arg(&format!("/out:{}", output))
            .arg("/wholearchive")
            .arg(&archive)
            .arg(&format!("/libpath:{}/crt/lib/{}", xwin_dir, arch))
            .arg(&format!("/libpath:{}/sdk/lib/um/{}", xwin_dir, arch))
            .arg(&format!("/libpath:{}/sdk/lib/ucrt/{}", xwin_dir, arch))
            .args(&["kernel32.lib", "ucrt.lib", "vcruntime.lib"])
            .args(&link_libs)
            .status()
            .unwrap_or_else(|e| panic!("Failed to link {}: {}", dylib_name, e));
        assert!(status.success(), "Failed to link {}", dylib_name);
        eprintln!("cargo:warning=Built {}", output);
        return;
    }

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

/// Create an lld-link Command for Windows DLL cross-linking.
/// Prefers Homebrew's lld (newer, supports arm64 import libs) over rustup's rust-lld.
fn lld_link_command() -> std::process::Command {
    // Homebrew lld (brew install lld)
    let brew_lld = "/opt/homebrew/opt/lld/bin/lld";
    if std::path::Path::new(brew_lld).exists() {
        let mut cmd = std::process::Command::new(brew_lld);
        cmd.arg("-flavor").arg("link");
        return cmd;
    }
    // Try lld-link on PATH
    std::process::Command::new("lld-link")
}

/// Copy a static archive from the cc::Build output directory to the Cargo
/// release directory, renaming it so that `-l<to_name>` resolves correctly
/// for Scala Native static linking.
fn copy_static_archive(out_dir: &str, release_dir: &str, from_name: &str, to_name: &str) {
    let src = format!("{}/lib{}.a", out_dir, from_name);
    let dst = format!("{}/lib{}.a", release_dir, to_name);
    match std::fs::copy(&src, &dst) {
        Ok(_) => eprintln!("cargo:warning=Copied static archive: {}", dst),
        Err(e) => eprintln!("cargo:warning=Failed to copy {} -> {}: {}", src, dst, e),
    }
}
