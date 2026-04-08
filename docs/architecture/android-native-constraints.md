# Android Native Library & Panama Constraints

**Date**: 2026-03-13

---

## Minimum Android API Level

**API 36 (Android 16)** is the minimum supported Android version for SGE.

This is dictated by the PanamaPort library (`io.github.vova7878.panama`), which provides
`java.lang.foreign` (Panama FFM) support on Android. All tested versions (v0.1.0, v0.1.1,
v0.1.2) reference `Build.VERSION.SDK_INT_FULL`, a field that only exists on API 36+. Running
on older APIs causes `NoSuchFieldError` at class-load time.

This is an **upstream constraint** in PanamaPort's `ArtVersion.computeIndex()` — not something
SGE can work around without forking or patching the library.

### Impact

- Emulators must target API 36+ (system image: `system-images;android-36;google_apis;arm64-v8a`)
- `targetSdkVersion` and `minSdkVersion` should be set to 36 in APK manifests
- NDK cross-compilation targets API 26 (the Rust/C toolchain minimum), but the APK runtime
  requires API 36 for PanamaPort class loading

### PanamaPort Dependency Chain

```
io.github.vova7878.panama:Core:v0.1.0
├── io.github.vova7878.panama:Unsafe:v0.1.0
├── io.github.vova7878.panama:VarHandles:v0.1.0
├── io.github.vova7878.panama:LLVM:v0.1.0
├── io.github.vova7878:DexFile:v1.2.0
├── io.github.vova7878:SunCleanerStub:v1.0.0
├── io.github.vova7878:SunUnsafeWrapper:v1.0.0
├── io.github.vova7878:R8Annotations:v1.0.0
└── io.github.vova7878:AndroidMisc:v1.0.0
```

All modules are packaged as AARs (except DexFile and AndroidMisc which are JARs).
`AndroidBuild.scala` and `AndroidDeps.scala` handle downloading, AAR extraction, and
DEXing these dependencies into the APK.

---

## Native Library Architecture

### Build Pipeline

1. **Rust cross-compilation** (in the external sge-native-components repo): builds
   `libsge_native_ops.so` for 3 Android architectures using NDK 27 clang as the linker
2. **C audio bridge** (`build.rs`): builds `libsge_audio.so` from vendored miniaudio,
   linked against `-llog -lOpenSLES -lm` (Android audio APIs)
3. **No GLFW on Android**: Android uses `GLSurfaceView` + system EGL, not GLFW
4. **JAR bundling** (`build.sbt`): native `.so` files are packaged into the sge JAR under
   `native/android-{aarch64,armv7,x86_64}/`
5. **APK extraction** (`AndroidBuild.scala`): at APK build time, `.so` files are extracted
   from dependency JARs and placed in the APK's `lib/{arm64-v8a,armeabi-v7a,x86_64}/`

### Android ABI Mapping

| Rust target                 | SGE classifier    | Android ABI      |
|-----------------------------|-------------------|------------------|
| `aarch64-linux-android`     | `android-aarch64` | `arm64-v8a`      |
| `armv7-linux-androideabi`   | `android-armv7`   | `armeabi-v7a`    |
| `x86_64-linux-android`      | `android-x86_64`  | `x86_64`         |

### Runtime Loading

`NativeLibLoader` resolves libraries in this order on Android:

1. **`java.library.path`** scan (standard, but Android doesn't include app lib dir here)
2. **`BaseDexClassLoader.findLibrary()`** via reflection — returns the full filesystem path
   to the `.so` extracted from the APK (e.g. `/data/app/.../lib/arm64-v8a/libsge_native_ops.so`)
3. **`System.loadLibrary()`** fallback — loads the lib but may not yield a path
4. **Classpath resource extraction** — not used on Android (APK handles extraction)

The path returned by step 2 is passed to PanamaPort's `SymbolLookup.libraryLookup()` for
FFM symbol resolution.

---

## NDK Cross-Compilation Setup

### Prerequisites

- Android NDK 27.2 (auto-installed by the sbt `androidSdkRoot` task on first invocation)
- Rust targets: `aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android`
- Installed via `rustup target add` (requires rustup, not Homebrew cargo)

### Configuration

The `.cargo/config.toml` in the
[sge-native-components](https://github.com/kubuszok/sge-native-components) repo
specifies NDK clang as the linker for each target. The cross-android build script
sets `CC_<target>`, `CXX_<target>`, and `AR_<target>` environment variables for
the `cc` crate used by `build.rs`.

### Android-Specific Build Differences

| Aspect           | Desktop                     | Android                        |
|------------------|-----------------------------|--------------------------------|
| Audio backend    | miniaudio (CoreAudio/ALSA)  | miniaudio (OpenSL ES)          |
| Audio link flags | `-lpthread -lm`             | `-lm -llog -lOpenSLES`        |
| Windowing        | GLFW (built from source)    | Not built (system GLSurfaceView) |
| pthread          | Separate `-lpthread`        | Built into bionic libc         |
| Cargo feature    | (default)                   | `--features android`           |

---

## Known Limitations

1. **PanamaPort requires API 36+**: cannot support older Android versions without upstream fix
2. **No ANGLE on Android**: uses system GL ES implementation (unlike desktop which bundles ANGLE)
3. **No hot-reload**: native libs are extracted at install time, not at runtime
4. **armv7 support**: included for completeness but increasingly rare on modern devices
