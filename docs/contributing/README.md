# Contributing Guides

Reference documentation for the Java-to-Scala conversion process.

> Building a game *with* SGE (rather than working on the port)? See the
> user-facing [Getting Started guide](../getting-started.md). It uses
> [`sge-build/src/main/resources/sge-template-build.sbt`](../../sge-build/src/main/resources/sge-template-build.sbt)
> as the starting `build.sbt`.

| Guide | Purpose |
|-------|---------|
| [conversion-rules.md](conversion-rules.md) | Full Java→Scala procedure (19 type/API adjustments) |
| [nullable-guide.md](nullable-guide.md) | `Nullable[A]` opaque type patterns |
| [control-flow-guide.md](control-flow-guide.md) | `boundary`/`break` patterns replacing `return`/`break`/`continue` |
| [type-mappings.md](type-mappings.md) | LibGDX → SGE type/package mapping table |
| [code-style.md](code-style.md) | Brace rules, split packages, scalafmt, comment preservation |
| [verification-checklist.md](verification-checklist.md) | Checklist for marking a file as "verified" |
| [setup.md](setup.md) | Development setup — required and optional dependencies |
| [tooling.md](tooling.md) | MCP tools, SBT client, local reference usage |
| [native-operations.md](native-operations.md) | Native ops architecture — Rust FFI, Panama, platform modules |
