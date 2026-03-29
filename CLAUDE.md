# CLAUDE.md

SGE (Scala Game Engine) is a cross-platform Scala 3 port of LibGDX targeting
JVM, Scala.js (browser), Scala Native, and Android. 539 of 605 core files
converted, 0 not started, 66 skipped (stdlib replacements), 0 deferred.
152 backend files: 124 done, 14 deferred (iOS), 14 skip.

## Build Rules

- Scala **3.8.2**, compiler flags: `-deprecation -feature -no-indent -rewrite -Werror`
- **Linter flags**: `-Wimplausible-patterns -Wrecurse-with-default -Wenum-comment-discard -Wunused:imports,privates,locals,patvars,nowarn`
- **Braces required** (`-no-indent`): `{}` for all `trait`, `class`, `enum`, method defs
- **Split packages**: `package sge` / `package graphics` / `package g2d` (never flat)
- **No `return`**: use `scala.util.boundary`/`break`
- **No `null`**: use `Nullable[A]` opaque type. **Never use `orNull`** except at Java interop boundaries (requires `@nowarn` + comment)
- **No comment removal**: preserve all original comments
- **No `scala.Enumeration`**: use Scala 3 `enum`, preferably `extends java.lang.Enum`
- **Case classes must be `final`**: all `case class` declarations require `final`
- **No Java-style getters/setters**: no-logic `getX()`/`setX(v)` → public `var x`; with-logic → `def x: T` + `def x_=(v: T): Unit`
- **`(using Sge)` propagation**: pass `Sge` context wherever needed (replaces LibGDX global `Gdx.*`). Add `(using Sge)` to **class constructors** so it's available in all methods. Never leave TODOs for missing Sge. Sge is effectively a per-application singleton passed explicitly instead of via globals.
- **Fix bugs, don't work around them**: when a test reveals a pre-existing bug in the codebase, fix the bug in the source code — never patch the test to avoid it
- **All 4 platforms are baseline**: JVM, JS, Native, Android — changes must be non-regressing on all
- Use `sge-dev` commands or `sbt --client` — never bare `sbt`
- **sbt hangs on build.sbt errors**: When `build.sbt` has an error, sbt reload becomes unresponsive. Do NOT mistake this for long compilation — **kill the process immediately** (`sge-dev proc kill-sbt`) to see the error output, then fix `build.sbt` and retry.

## Project Structure

| Directory | Purpose |
|-----------|---------|
| `sge/` | Core library (projectMatrix: JVM/JS/Native) |
| `sge-jvm-platform/` | JVM platform modules: `api/`, `jdk/`, `android/` (merged into sge JAR) |
| `sge-extension/freetype/` | FreeType font extension (JVM/JS/Native) |
| `sge-extension/physics/` | 2D physics via Rapier2D (JVM/JS/Native) |
| `sge-extension/tools/` | TexturePacker CLI (JVM-only) |
| `sge-build/` | sbt plugin (SgePlugin, AndroidBuild, packaging) |
| `sge-deps/native-components/` | Rust native library (GLFW, miniaudio, FreeType FFI) |
| `sge-test/` | Tests: integration, regression, android-smoke, browser |
| `demos/` | 10 feature demos (separate sub-build) |
| `scalafix-rules/` | Custom Scalafix lint rules |
| `original-src/` | Reference sources (not compiled). **Never fetch from GitHub.** |
| `original-src/libgdx/` | Local LibGDX reference source |
| `scripts/` | `sge-dev` CLI toolkit (Scala CLI, no sbt) |
| `docs/` | Architecture, conversion guides |

## CLI Toolkit: `sge-dev`

**Use `sge-dev` commands for all development tasks.** The PreToolUse hook validates
all Bash commands — if denied, use the suggested alternative.

**It's not `./sge-dev`, your hooks add it to `$PATH`, and it's defined in `scripts/bin/sge-dev`.**

| Command | Purpose |
|---------|---------|
| `sge-dev setup [--ci]` | Idempotent dev environment setup (all tools + targets) |
| `sge-dev build compile [--jvm/--js/--native/--all]` | Compile |
| `sge-dev build compile --errors-only` | Compile showing only errors |
| `sge-dev build compile --warnings` | Compile showing warnings + errors |
| `sge-dev build compile-fmt` | Compile, format, compile again |
| `sge-dev build fmt` | Scalafmt |
| `sge-dev build publish-local [--jvm/--js/--native/--all]` | Publish to local Maven |
| `sge-dev build extensions [--tools/--freetype/--physics/--all]` | Compile extensions |
| `sge-dev build texture-pack [args]` | Run TexturePacker CLI |
| `sge-dev build kill-sbt` | Kill sbt server gracefully |
| `sge-dev build release [--demo <name>] [--publish-first]` | Build demo release archives |
| `sge-dev build collect` | Collect releases into demos/target/releases/ |
| `sge-dev build verify-native <demo>` | Verify native release archive |
| `sge-dev build verify-jvm <demo>` | Verify JVM release archive |
| `sge-dev build verify-browser <demo>` | Verify browser release archive |
| `sge-dev build verify-releases [--demo <name>]` | Run all verify-* for a demo |
| `sge-dev test unit [--jvm/--js/--native/--all] [--only SUITE]` | Unit tests |
| `sge-dev test integration [--desktop/--browser/--native-ffi/--android/--all]` | Integration tests |
| `sge-dev test regression [--jvm/--js/--native/--android/--all]` | Regression tests |
| `sge-dev test browser` | Playwright browser IT |
| `sge-dev test extensions [--tools/--freetype/--physics/--all]` | Extension module tests |
| `sge-dev test android {setup,start,stop,ensure,test,demo,...}` | Android emulator + testing |
| `sge-dev test verify` | Full 4-platform verification gate |
| `sge-dev quality scan [--return/--null/--todo/--all] [--summary]` | Quality scans |
| `sge-dev quality grep <pattern> [--count/--files-only]` | Code search |
| `sge-dev quality scalafix <rule> [--file PATH]` | Run Scalafix rule |
| `sge-dev quality lint-null` | Check null patterns (NullToNullable) |
| `sge-dev git status/diff/log/blame/branch/tags` | Git read-only |
| `sge-dev git diff-stat/diff-count/diff-staged` | Git diff variants |
| `sge-dev git log-full [-n N]` | Detailed log with stats |
| `sge-dev git stage/commit/push` | Git write |
| `sge-dev git gh pr list/view/diff/checks/comments` | GitHub PR operations |
| `sge-dev git gh issue list/view` | GitHub issues |
| `sge-dev git gh run list/view/log` | GitHub CI runs |
| `sge-dev git gh api <endpoint>` | GitHub API |
| `sge-dev native build [--static/--android/--freetype/--physics]` | Build Rust native lib |
| `sge-dev native cross <target>` | Cross-compile for specific target |
| `sge-dev native cross-all` | Build all 6 desktop targets |
| `sge-dev native cross-android [target]` | Build for Android NDK |
| `sge-dev native collect` | Collect cross-compiled artifacts |
| `sge-dev native test` | Run Rust tests |
| `sge-dev native angle {setup,download,cross-collect,check}` | ANGLE library management |
| `sge-dev native curl {setup,download,cross-collect,check}` | Static curl library management |
| `sge-dev native release-prep` | Full release preparation |
| `sge-dev native setup-toolchain` | Install cross-compilation tools |
| `sge-dev compare file/package/find/status/next-batch` | LibGDX/SGE comparison |
| `sge-dev metals install/start/stop/status` | Metals LSP server |
| `sge-dev proc list` | List project processes |
| `sge-dev proc kill` | Kill all project processes |
| `sge-dev proc kill-sbt` | Kill sbt server |
| `sge-dev db migration stats/list/get/set/sync` | Migration database |
| `sge-dev db issues stats/list/add/resolve/import` | Issues database |
| `sge-dev db audit stats/list/get/set/import` | Audit database |

Use `sge-dev db` for all migration/issues/audit queries — never grep markdown files.

## Bash Restrictions

**The PreToolUse hook validates ALL Bash commands.** Only `sge-dev`, `sbt --client`,
`git`, `cargo`, `npm`, `npx`, and `scala-cli` are allowed directly. All other commands
are denied or redirected to dedicated tools:

- **Denied**: `python`/`python3`, `kill`/`pkill`, `rm -rf`, `sbt` (without `--client`)
- **Redirected to tools**: `grep`→Grep, `find`/`ls`→Glob, `cat`/`head`/`tail`→Read, `sed`/`awk`→Edit
- **Use `sge-dev`** for builds, tests, git, quality scans, process management, and database queries
- **Use dedicated tools** (`Grep`, `Glob`, `Read`, `Edit`) for code search and file operations
- **Path normalization**: `/opt/homebrew/bin/rg` is treated the same as `rg` — full paths don't bypass rules

## Tooling

| Tool | Purpose |
|------|---------|
| `sge-dev` | CLI toolkit — builds, tests, git, quality, databases, process management |
| `metals-mcp` | Compile, search, inspect, format (snapshot — see `sge-dev metals install`) |
| `context7` MCP | External library docs (LWJGL, scala-js-dom, etc) |
| `./original-src/libgdx/` | Local reference source. **Never fetch from GitHub.** |
| `scalafix-rules/` | Custom Scalafix lint rules. Run: `sge-dev quality scalafix <Rule>` |

## Skill Dispatch Rules

Load the relevant skill when working on specific areas — this avoids polluting context
with information that isn't needed for the current task.

| Context | Skill to load |
|---------|---------------|
| Converting a Java file to Scala | `/guide-conversion` |
| Code style, license headers, formatting | `/guide-code-style` |
| Replacing return/break/continue | `/guide-control-flow` |
| Nullable patterns, null safety | `/guide-nullable` |
| LibGDX→SGE class/package renames | `/guide-type-mappings` |
| Post-conversion verification | `/guide-verification` |
| Platform-specific code (JVM/JS/Native) | `/arch-platforms` |
| FFI issues, native code bugs | `/arch-ffi` |
| sbt build config, projectMatrix | `/arch-build` or `/arch-cross-platform` |
| Graphics architecture, ANGLE | `/arch-graphics` |
| Android-specific code | `/arch-android` |
| Rust native ops, C ABI bridge | `/arch-native-bridge` |
| Packaging, distribution, releases | `/arch-packaging` |
| Opaque types, API improvements | `/guide-improvements` |
| Auditing a file | `/audit-file <path>` |
| Auditing a package | `/audit-package <pkg>` |

## Conversion Guides

- [Conversion rules](docs/contributing/conversion-rules.md) — full 19-step procedure
- [Type mappings](docs/contributing/type-mappings.md) — package/class renames, skipped classes
- [Code style](docs/contributing/code-style.md) — license header template, formatting
- [Nullable guide](docs/contributing/nullable-guide.md) — `Nullable[A]` opaque type
- [Control flow guide](docs/contributing/control-flow-guide.md) — `boundary`/`break` patterns
- [Verification checklist](docs/contributing/verification-checklist.md) — post-conversion checks

## Source Reference

Path mapping: `com/badlogic/gdx/<path>.java` → `sge/src/main/scala/sge/<path>.scala`

## Audit System

Per-file audit trail comparing every SGE Scala file against its LibGDX Java source.
Each audited file gets a `Migration notes:` block in its header comment.

- **Skills**: `/audit-file <path>`, `/audit-package <pkg>`, `/audit-status [pkg]`
- **Database**: `sge-dev db audit stats`, `sge-dev db audit list --package <pkg>`
- **Statuses**: `pass`, `minor_issues`, `major_issues`, `not_ported`
- **In-file notes**: `Renames`, `Merged with`, `Convention`, `Idiom`, `TODOs`, `Audited` date
- **Progress tracking**: `memory/audit-progress.md`

## CI Pipeline

~20 jobs in `.github/workflows/ci.yml`. Key patterns:

| Job group | Platforms |
|-----------|-----------|
| JVM tests | linux-x86_64, linux-aarch64, macos-aarch64, windows-x86_64, windows-aarch64 |
| Scala Native tests | linux-x86_64, linux-aarch64, macos-aarch64, windows-x86_64 |
| Release verification | above + macos-x86_64 (Rosetta, JVM only) |
| Android tests | ubuntu-latest (x86_64 emulator) — smoke + Pong demo APK |
| Browser/JS | ubuntu-latest — Scala.js tests, Playwright smoke, browser packaging |
| Demo compilation | ubuntu-latest — all 10 demos × JVM + JS + Native |

**CI-specific mechanisms:**
- `SGE_USE_PLUGIN=true` — demos consume published sge-build plugin instead of source inclusion
- `SGE_SKIP_NATIVE_VALIDATION=true` — skip native lib validation when only Android/subset libs present
- `matrix.native` flag — controls which verify-release steps run per platform (native link, libobjc stub, static curl)
- ANGLE shared libs downloaded in `build-native` via `sge-dev native angle cross-collect`
- libobjc stub on Linux — no-op `objc_msgSend`/`sel_registerName` for macOS-only `@link("objc")`
- Windows Native uses compiled C stub `.lib` files for idn2/curl (no real HTTP)

**Known CI limitations (excluded from pass/fail):**
- Android: JSON_XML, FILEHANDLE_TYPES, TOUCH_DISPATCH, LIFECYCLE, CLIPBOARD
- macOS x86_64: no runner (macos-13 retired); release verified via Rosetta
- Windows aarch64: Scala Native unsupported (generates x64 code); JVM tests pass
- Scaladoc: disabled (`packageDoc/publishArtifact := false`); non-blocking probe monitors upstream fix

## Documentation

| Path | Content |
|------|---------|
| `docs/contributing/` | All conversion guides, code style, tooling |
| `docs/architecture/` | Platform targets, backend analysis |
| `docs/improvements/` | Type safety, API design improvements |
| `scripts/data/` | TSV databases (migration, issues, audit) |
