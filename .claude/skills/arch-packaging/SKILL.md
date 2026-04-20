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
- Native libs built externally in sge-native-providers, distributed as provider JARs
- CI extracts provider JARs to the staging dir; per-platform libs in `cross/<classifier>/`
- ANGLE libs (libEGL, libGLESv2) bundled alongside Rust libs
- Static curl libs (from stunnel/static-curl) for self-contained Scala Native releases
- macOS .app bundles with ad-hoc codesigning

## Release Workflow

Native libs are built externally in
[sge-native-providers](https://github.com/kubuszok/sge-native-providers) and
distributed as provider JARs from Maven. The release flow consumes those
provider JARs and builds the per-platform demo archives via sbt
command aliases defined in `demos/build.sbt`.

```
re-scale build publish-local --all                # Publish SGE to local Maven (JVM + JS + Native)
re-scale runner release-build                     # sbt --client releaseAll (in demos/)
re-scale runner release-collect                   # sbt --client collectReleases — demos/target/releases/
re-scale runner android-build-all                 # sbt --client androidAll — all 11 demo APKs
```

Per-demo (single demo) is wired via sbt command aliases inside `demos/`,
e.g. `cd demos && sbt --client releasePong` for the JVM/Browser/Native trio
of the Pong demo, or `sbt --client androidPong` for the APK alone.

## Verification

Manual archive launch / smoke verification was previously done by legacy
shell wrappers that are no longer present. There are no equivalent sbt tasks
yet — verify by running the integration test runners on the host platform:

```
re-scale runner desktop-it                        # GLFW + ANGLE + miniaudio integration test
re-scale runner browser-it                        # Playwright + headless Chromium
re-scale runner android-it                        # Android emulator smoke test
re-scale runner native-ffi-it                     # Scala Native C ABI wiring
```

Load the SgePackaging source for implementation details:
$READ sge-build/src/main/scala/sge/sbt/SgePackaging.scala
