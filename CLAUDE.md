# CLAUDE.md

SGE is a Scala 3 port of LibGDX. 539 of 605 core files converted, 0 not started,
66 skipped (stdlib replacements), 0 deferred.

## Build Rules

- Scala **3.8.2**, compiler flags: `-deprecation -feature -no-indent -rewrite -Werror`
- **Linter flags**: `-Wimplausible-patterns -Wrecurse-with-default -Wenum-comment-discard -Wunused:imports,privates,locals,patvars,nowarn` (info-level via `-Wconf:id=E198:info`)
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
- Use `sbt --client` or `just compile` / `just fmt` — never bare `sbt`
- **sbt hangs on build.sbt errors**: When `build.sbt` has an error, sbt reload becomes unresponsive. Do NOT mistake this for long compilation — **kill the process immediately** to see the error output, then fix `build.sbt` and retry.

## Bash Restrictions

**Only `just` recipes and `sbt --client` are allowed in Bash.** All other commands
(`rg`, `grep`, `head`, `tail`, `sort`, `find`, `ls`, `wc`, `awk`, `sed`, `perl`,
`echo`, `for`, `while`, `xargs`, `python`) are denied. Use:

- **Dedicated tools** (`Grep`, `Glob`, `Read`) for code search and file reading
- **`just` recipes** for builds, git, GitHub CLI, quality scans, and search
- **`sbt --client`** for compilation, tests, and Scalafix

Key `just` recipes:

| Recipe | Purpose |
|--------|---------|
| `just compile` / `just test` | Build and test |
| `just compile-errors` / `just compile-warnings` | Filtered build output |
| `just git-status` / `just diff-stat` / `just git-log` | Git read-only |
| `just stage <files>` / `just commit '<msg>'` | Git staging + commit |
| `just commit-all '<msg>'` | Stage all + commit |
| `just gh-pr-list` / `just gh-issue-list` | GitHub CLI read-only |
| `just sge-quality <type> [summary]` | Quality scans (return/null/todo/all) |
| `just sge-grep '<pattern>'` / `just sge-count '<pattern>'` | Code search |

## Tooling

| Tool | Purpose |
|------|---------|
| `metals-mcp` | Compile, search, inspect, format (snapshot — see `just metals-install`) |
| `context7` MCP | External library docs (LWJGL, scala-js-dom, etc) |
| `Justfile` | Task recipes — run `just --list` for full list |
| `./libgdx/` | Local reference source. **Never fetch from GitHub.** |
| `scalafix-rules/` | Custom Scalafix lint rules (separate module). Run: `just scalafix <Rule>` |

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
Each audited file gets a `Migration notes:` block in its header comment, and each
package gets a summary doc in `docs/audit/`.

- **Skills**: `/audit-file <path>`, `/audit-package <pkg>`, `/audit-status [pkg]`
- **Statuses**: `pass`, `minor_issues`, `major_issues`, `not_ported`
- **In-file notes**: `Renames`, `Merged with`, `Convention`, `Idiom`, `TODOs`, `Audited` date
- **Package docs**: `docs/audit/<slug>.md` (slug = path with `-` separators)
- **Progress tracking**: `memory/audit-progress.md`

## Documentation

| Path | Content |
|------|---------|
| `docs/contributing/` | All conversion guides, code style, tooling |
| `docs/progress/migration-status.tsv` | Per-file status (605 files) |
| `docs/progress/quality-issues.md` | Systemic issues (return, null, Java syntax, TODOs) |
| `docs/audit/` | Per-package audit docs and index |
| `docs/architecture/` | Platform targets, backend analysis |
| `docs/improvements/` | Type safety, API design improvements |
