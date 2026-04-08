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
- Use `re-scale` commands or `sbt --client` — never bare `sbt` (avoids the JVM startup tax on every invocation)
- **sbt server stuck?** kill with `re-scale proc kill --kind sbt --dir .`, fix the cause, retry

## Project Structure

| Directory | Purpose |
|-----------|---------|
| `sge/` | Core library (projectMatrix: JVM/JS/Native) |
| `sge-jvm-platform/` | JVM platform modules: `api/`, `jdk/`, `android/` (merged into sge JAR) |
| `sge-extension/freetype/` | FreeType font extension (JVM/JS/Native) |
| `sge-extension/physics/` | 2D physics via Rapier2D (JVM/JS/Native) |
| `sge-extension/tools/` | TexturePacker CLI (JVM-only) |
| `sge-build/` | sbt plugin (SgePlugin, AndroidBuild, packaging) |
| `sge-test/` | Tests: integration, regression, android-smoke, browser |
| `demos/` | 11 feature demos (separate sub-build) |
| `original-src/` | Reference sources (not compiled). **Never fetch from GitHub.** |
| `original-src/libgdx/` | Local LibGDX reference source |
| `.rescale/` | Per-project re-scale config + data |
| `.rescale/data/` | TSV databases (migration, issues, audit) |
| `.rescale/claude-hooks.yaml` | (optional) per-project hook overrides |
| `.rescale/doctor.yaml` | (optional) dev-environment bootstrap steps |
| `.rescale/runners.yaml` | (optional) test-runner adapters |
| `docs/` | Architecture, conversion guides |

## CLI Toolkit: `re-scale`

**Use `re-scale` commands for all development tasks.** The PreToolUse hook
delegates to `re-scale hook`, which validates every Bash command — if
denied, use the suggested alternative.

Source repo: <https://github.com/kubuszok/re-scale>. Install via
`scripts/install.sh` from a clone of that repo (builds the Scala
Native binary + wrapper and copies them into `$HOME/bin/`).

| Command | Purpose |
|---------|---------|
| `re-scale build compile [--module M] [--jvm/--js/--native/--all] [--errors-only]` | Compile via `sbt --client` |
| `re-scale build compile-fmt` | Run scalafmt then compile |
| `re-scale build fmt` | Run `scalafmtAll` |
| `re-scale build publish-local [--module M] [--jvm/--js/--native/--all]` | Publish to local Maven |
| `re-scale build kill-sbt` | Shut down the sbt server |
| `re-scale test unit [--module M] [--jvm/--js/--native/--all] [--only SUITE]` | Run unit tests |
| `re-scale test verify` | Compile every module on every platform (JVM × JS × Native) |
| `re-scale enforce shortcuts [--src DIRS] [--file F] [--covenanted]` | Scan for shortcut/stub markers |
| `re-scale enforce stale-stubs [--src DIRS]` | Two-pass scan for stale "not yet ported" comments |
| `re-scale enforce verify --file <path> \| --all` | Re-verify covenanted file(s) |
| `re-scale enforce skip-policy [list \| add <path> <tool>]` | Manage the skip-policy allow list |
| `re-scale enforce compare --port <scala> --source <java> [--strict]` | Cross-language method-set + body comparison |
| `re-scale git status/diff/log/blame/branch/tags` | Git read-only |
| `re-scale git stage/commit/push` | Git write |
| `re-scale git gh pr list/view/diff/checks` | GitHub PR operations |
| `re-scale git gh issue list/view` | GitHub issues |
| `re-scale git gh run list/view/log` | GitHub CI runs |
| `re-scale git gh api <endpoint>` | GitHub API |
| `re-scale db migration list/get/set/stats` | Migration database |
| `re-scale db issues list/add/resolve/stats` | Issues database |
| `re-scale db audit list/get/set/stats` | Audit database |
| `re-scale db merge --target <tsv> --source <tsv> [--strategy ...]` | Cross-branch TSV reconciliation |
| `re-scale proc list [--kind sbt\|java\|metals] [--dir DIR]` | List sbt/java/metals processes with cwd |
| `re-scale proc kill --pid N \| --kind ... --dir DIR` | Targeted process termination |
| `re-scale doctor [--ci]` | Run `.rescale/doctor.yaml` bootstrap steps |
| `re-scale runner <name> [--mode MODE] [args...]` | Dispatch a runner from `.rescale/runners.yaml` |

Use `re-scale db` for all migration/issues/audit queries — never read TSVs by hand.

### SGE-specific workflow notes

The legacy `sge-dev` tool had several SGE-specific subcommands that
re-scale doesn't ship in its core. They're expected to live in
`.rescale/doctor.yaml` (for setup steps) and `.rescale/runners.yaml`
(for test harnesses). Until the YAML configs are written, these
workflows are not invokable from this repo:

| Legacy command | Migration target |
|----------------|------------------|
| `sge-dev setup [--ci]` | `re-scale doctor [--ci]` reading `.rescale/doctor.yaml` (Rust targets, NDK, Zig, JDK, cargo-zigbuild, ...) |
| `sge-dev metals install/start/stop/status` | `re-scale runner metals --mode start` reading `.rescale/runners.yaml`, OR keep using `cs install metals-mcp` directly |
| `sge-dev test integration --android` | `re-scale runner android-test` reading `.rescale/runners.yaml` |
| `sge-dev test browser` | `re-scale runner playwright` reading `.rescale/runners.yaml` |
| `sge-dev build extensions` | `re-scale build compile --module sge-extension-*` (each extension is its own sbt module) |
| `sge-dev build release/collect/verify-*` | `re-scale runner release-*` reading `.rescale/runners.yaml` |
| `sge-dev compare file/package/find/status/next-batch` | LibGDX-specific compare workflows have no direct equivalent — use `re-scale enforce compare` for the strict-mode method-by-method gap analysis |

The TSV data files survived the migration unchanged at
`.rescale/data/{audit,issues,migration}.tsv` so nothing has been
lost. See <https://github.com/kubuszok/re-scale/blob/master/docs/cross-flavor-diff.md>
for the rationale.

## Bash Restrictions

**The PreToolUse hook validates ALL Bash commands.** Only `re-scale`,
`sbt --client`, `git`, `cargo`, `npm`, `npx`, and `scala-cli` are allowed
directly. All other commands are denied or redirected to dedicated tools:

- **Denied**: `python`/`python3`, `kill`/`pkill`, `rm -rf`, `sbt` (without `--client`)
- **Redirected to tools**: `grep`→Grep, `find`/`ls`→Glob, `cat`/`head`/`tail`→Read, `sed`/`awk`→Edit
- **Use `re-scale`** for builds, tests, git, process management, enforcement, and database queries
- **Use dedicated tools** (`Grep`, `Glob`, `Read`, `Edit`) for code search and file operations
- **Path normalization**: `/opt/homebrew/bin/rg` is treated the same as `rg` — full paths don't bypass rules

Per-project rule overrides live at `.rescale/claude-hooks.yaml`.

## Tooling

| Tool | Purpose |
|------|---------|
| `re-scale` | CLI toolkit — builds, tests, git, enforce, databases, process management, doctor, runner |
| `metals-mcp` | Compile, search, inspect, format (install via `cs install metals-mcp`) |
| `context7` MCP | External library docs (LWJGL, scala-js-dom, etc) |
| `./original-src/libgdx/` | Local reference source. **Never fetch from GitHub.** |

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
- **Database**: `re-scale db audit stats`, `re-scale db audit list --package <pkg>`
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
| Demo compilation | ubuntu-latest — all 11 demos × JVM + JS + Native |

**CI-specific mechanisms:**
- Demos always consume published sge-build plugin (`sbt publishLocal` in sge-build/ required after plugin changes)
- `SGE_SKIP_NATIVE_VALIDATION=true` — skip native lib validation when only Android/subset libs present
- `matrix.native` flag — controls which verify-release steps run per platform (native link, static curl)
- ANGLE shared libs downloaded in build-native (workflow now lives in sge-native-components repo)
- Native lib stubs (libobjc on Linux, companion .lib on Windows) are embedded in provider JARs
- Windows curl uses MSVC-built static libs from kubuszok/curl-natives (real HTTP, not stubs)

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
| `.rescale/data/` | TSV databases (migration, issues, audit) |
