---
description: Load the distribution packaging architecture for SGE games including JVM, browser, native, and Android targets
---

Distribution packaging architecture for SGE games.

## Key Files
- `sge-build/src/main/scala/sge/sbt/SgePackaging.scala` — Core packaging logic
- `sge-build/src/main/scala/sge/sbt/SgePlugins.scala` — SgeDesktopJvmPlatform, SgeBrowserPlatform, SgeDesktopNativePlatform, SgeAndroidPlatform
- `demos/build.sbt` — Demo packaging configuration

## Packaging Modes
1. **Simple** (`sgePackage`): bin/ + lib/ + native/ with launcher scripts, requires system JDK
2. **Distribution** (`sgePackageAll`): Self-contained per-platform archives with jlinked JRE + Roast launcher
3. **Browser** (`sgePackageBrowser`): Scala.js fullLinkJS + index.html + assets
4. **Native** (`sgePackageNative`): Scala Native executable + shared libs
5. **Android** (`androidSign`): APK via D8 + aapt2 + apksigner

## Cross-Platform JVM Distribution
- `sgeCrossNativeLibDir` setting: points to `sge-deps/native-components/target/cross/` (CI staging area)
- Native libs built externally in sge-native-components, distributed as provider JARs
- CI extracts provider JARs to the staging dir; per-platform libs in `cross/<classifier>/`
- ANGLE libs (libEGL, libGLESv2) bundled alongside Rust libs
- Static curl libs (from stunnel/static-curl) for self-contained Scala Native releases
- macOS .app bundles with ad-hoc codesigning

## Release Workflow
```
sge-dev native release-prep                      # Cross-compile Rust + download ANGLE + curl
sge-dev build release --publish-first             # Publish SGE locally + build all releases
sge-dev build collect                             # Collect into demos/target/releases/
```

Or step by step:
```
sge-dev build publish-local --all                 # Publish SGE to local Maven
sge-dev build release                             # Build all demo releases (JVM/JS/Native/Android)
sge-dev build release --demo pong                 # Build a single demo release
sge-dev build collect                             # Collect releases
```

## Verification Commands
```
sge-dev build verify-native pong                  # Test native archive launches
sge-dev build verify-jvm pong                     # Test JVM archive launches
sge-dev build verify-jvm-intel pong               # Test x86_64 under Rosetta
sge-dev build verify-browser pong                 # Check browser archive structure
sge-dev build verify-releases --demo pong         # Run all verify-* for a demo
sge-dev build verify-releases                     # Verify all demos
sge-dev test android test pong                    # Test collected APK on emulator
```

Load the SgePackaging source for implementation details:
$READ sge-build/src/main/scala/sge/sbt/SgePackaging.scala
