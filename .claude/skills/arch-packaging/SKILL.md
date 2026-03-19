---
description: Load the distribution packaging architecture for SGE games including JVM, browser, native, and Android targets
---

Distribution packaging architecture for SGE games.

## Key Files
- `sge-build/src/main/scala/sge/sbt/SgePackaging.scala` — Core packaging logic
- `sge-build/src/main/scala/sge/sbt/SgePlugins.scala` — JvmReleases, BrowserReleases, NativeReleases, AndroidReleases
- `demos/build.sbt` — Demo packaging configuration

## Packaging Modes
1. **Simple** (`sgePackage`): bin/ + lib/ + native/ with launcher scripts, requires system JDK
2. **Distribution** (`sgePackageAll`): Self-contained per-platform archives with jlinked JRE + Roast launcher
3. **Browser** (`sgePackageBrowser`): Scala.js fullLinkJS + index.html + assets
4. **Native** (`sgePackageNative`): Scala Native executable + shared libs
5. **Android** (`androidSign`): APK via D8 + aapt2 + apksigner

## Cross-Platform JVM Distribution
- `sgeCrossNativeLibDir` setting: points to `native-components/target/cross/`
- Per-platform native libs from `cross/<classifier>/` (e.g. `cross/macos-aarch64/`)
- ANGLE libs (libEGL, libGLESv2) bundled alongside Rust libs
- macOS .app bundles with ad-hoc codesigning

## Release Workflow
```
just release-prep                    # Cross-compile Rust + download ANGLE
cd demos && sbt --client releaseAll  # Build all demos (JVM/JS/Native/Android)
sbt --client collectReleases         # Collect into demos/target/releases/
```

## Verification Recipes
```
just package-verify-native pong      # Test native archive launches
just package-verify-jvm pong         # Test JVM archive launches
just package-verify-jvm-intel pong   # Test x86_64 under Rosetta
just android-test-collected pong     # Test collected APK on emulator
```

Load the SgePackaging source for implementation details:
$READ sge-build/src/main/scala/sge/sbt/SgePackaging.scala
