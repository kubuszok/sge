# CLAUDE.md

SGE is a Scala 3 port of LibGDX. 539 of 605 core files converted, 0 not started,
66 skipped (stdlib replacements), 0 deferred.

## Build Rules

- Scala **3.8.1**, compiler flags: `-deprecation -feature -no-indent -rewrite -Werror`
- **Braces required** (`-no-indent`): `{}` for all `trait`, `class`, `enum`, method defs
- **Split packages**: `package sge` / `package graphics` / `package g2d` (never flat)
- **No `return`**: use `scala.util.boundary`/`break`
- **No `null`**: use `Nullable[A]` opaque type
- **No comment removal**: preserve all original comments
- Use `sbt --client` or `just compile` / `just fmt` — never bare `sbt`

## Tooling

| Tool | Purpose |
|------|---------|
| `metals-mcp` | Compile, search, inspect, format (snapshot — see `just metals-install`) |
| `context7` MCP | External library docs (LWJGL, scala-js-dom, etc) |
| `Justfile` | Task recipes — run `just --list` |
| `./libgdx/` | Local reference source. **Never fetch from GitHub.** |

## Conversion Guides

- [Conversion rules](docs/contributing/conversion-rules.md) — full 19-step procedure
- [Type mappings](docs/contributing/type-mappings.md) — package/class renames, skipped classes
- [Code style](docs/contributing/code-style.md) — license header template, formatting
- [Nullable guide](docs/contributing/nullable-guide.md) — `Nullable[A]` opaque type
- [Control flow guide](docs/contributing/control-flow-guide.md) — `boundary`/`break` patterns
- [Verification checklist](docs/contributing/verification-checklist.md) — post-conversion checks

## Source Reference

Path mapping: `com/badlogic/gdx/<path>.java` → `core/src/main/scala/sge/<path>.scala`

## Documentation

| Path | Content |
|------|---------|
| `docs/contributing/` | All conversion guides, code style, tooling |
| `docs/progress/migration-status.tsv` | Per-file status (605 files) |
| `docs/progress/quality-issues.md` | Systemic issues (return, null, Java syntax, TODOs) |
| `docs/architecture/` | Platform targets, backend analysis |
| `docs/improvements/` | Type safety, API design improvements |
