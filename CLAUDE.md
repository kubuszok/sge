# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

SGE is a Scala 3 port of LibGDX, a cross-platform game development framework.
~248 of ~605 core Java files have been AI-converted so far. The conversion follows
a systematic approach tracked in `docs/progress/migration-status.tsv`.

## Build System

This project uses SBT (Scala Build Tool). **Do NOT call `sbt` directly** — use
`sge-metals` MCP instead. If you must use SBT directly, **always use `sbt --client`**
to connect to the running SBT server and avoid JVM startup overhead.

- Scala version: 3.7.1
- Compiler flags: `-deprecation`, `-feature`, `-no-indent`, `-rewrite`, `-Xfatal-warnings`
- Format code: `sbt --client scalafmt`

## Source Reference

The original LibGDX source is available as a local git submodule:

```
./libgdx/gdx/src/com/badlogic/gdx/   # Original Java source (read-only reference)
```

**NEVER fetch LibGDX source from GitHub.** Always read from `./libgdx/`.

Path mapping: `com/badlogic/gdx/<path>.java` → `core/src/main/scala/sge/<path>.scala`

## Project Structure

```
core/src/main/scala/sge/           # SGE library code
├── *.scala                        # Core engine classes
├── assets/, audio/, files/        # Subsystem packages
├── graphics/                      # Graphics (g2d/, glutils/, profiling/)
├── input/, math/, net/            # Input, math (collision/), networking
├── scenes/scene2d/                # 2D scene graph (partial)
└── utils/                         # Utilities (compression/, viewport/)
libgdx/                            # Git submodule — original LibGDX source
docs/                              # Project documentation
├── contributing/                  # Conversion rules, code style, tooling
├── progress/                      # Migration tracking (TSV), quality issues
├── architecture/                  # Platform targets, backend analysis
└── improvements/                  # Design improvements over LibGDX
```

## Tooling

| Tool | Purpose |
|------|---------|
| `sge-metals` MCP | Compile, get errors/warnings. **Use instead of raw sbt.** |
| `context7` MCP | Look up external library documentation |
| `./libgdx/` | Local reference source. **Never fetch from GitHub.** |

MCP servers are configured in `.vscode/mcp.json` (local, not committed).
See `docs/contributing/tooling.md` for details.

## Architecture

1. **Core Engine Access**: `Sge` object provides all subsystems (graphics, audio, files, input, net)
2. **Scala-First Design**: Opaque types, `given`/`using` context, Scala stdlib collections
3. **Package Mapping**: `com.badlogic.gdx.*` → `sge.*` (see `docs/contributing/type-mappings.md`)

## Code Conventions (Key Rules)

- **License header**: Every ported file must have a header with original source path, authors,
  and Apache 2.0 license. See `docs/contributing/code-style.md` for the template.
- **Braces required**: `{}` for all `trait`, `class`, `enum`, method definitions (`-no-indent`)
- **Split packages**: `package sge` / `package graphics` / `package g2d` (not flat)
- **No `return`**: Use `scala.util.boundary`/`break` (see `docs/contributing/control-flow-guide.md`)
- **No `null`**: Use `Nullable[A]` opaque type (see `docs/contributing/nullable-guide.md`)
- **No comment removal**: Preserve all original comments during conversion
- **Uninitialized vars**: Use `scala.compiletime.uninitialized`
- **Comparators**: Use `given Ordering[T]`
- **Disposable**: Use `AutoCloseable`/`close()` instead

## Java to Scala Conversion

Full procedure with 19 type/API adjustments: `docs/contributing/conversion-rules.md`

Quick reference for common replacements:

| Java | Scala |
|------|-------|
| `Gdx` | `Sge` (implicit) |
| `GdxRuntimeException` | `SgeError` |
| `Array<T>` | `ArrayBuffer[T]` |
| `ObjectMap<K,V>` | `mutable.Map[K,V]` |
| `Disposable`/`dispose()` | `AutoCloseable`/`close()` |
| `@Null` | `Nullable[A]` |
| `return value` | `boundary { break(value) }` |
| `Comparator<T>` | `given Ordering[T]` |

## Documentation Index

| Path | Content |
|------|---------|
| `docs/contributing/` | Conversion rules, nullable guide, control flow, type mappings, code style, tooling |
| `docs/progress/migration-status.tsv` | Per-file migration status (605 files) |
| `docs/progress/quality-issues.md` | Systemic code issues (return, null, Java syntax, TODOs) |
| `docs/architecture/` | Platform targets, backend analysis, build structure |
| `docs/improvements/` | Type safety, API design improvements over LibGDX |
| `CHANGES.md` | Human-readable narrative of porting decisions |
