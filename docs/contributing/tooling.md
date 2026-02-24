# Tooling Guide

## MCP Servers

MCP (Model Context Protocol) servers are configured in `.vscode/mcp.json` (not committed to git —
configure locally).

### sge-metals

Use for compilation and diagnostics. **Never call `sbt` directly** — always use this MCP server.

Capabilities:
- Compile the project
- Get errors and warnings
- Navigate to definitions

### context7

Use for looking up documentation of external libraries (LWJGL, scala-js-dom, etc).

## Source Reference

The original LibGDX source is available locally as a git submodule:

```
./libgdx/                         # Git submodule (read-only reference)
./libgdx/gdx/src/com/badlogic/gdx/   # Core library source
```

**NEVER fetch LibGDX source from GitHub.** Always read from the local submodule.

Path mapping: `com/badlogic/gdx/<path>.java` → `core/src/main/scala/sge/<path>.scala`

## SBT

Build tool configuration is in `build.sbt`. Key settings:

- Scala 3.7.1
- Compiler flags: `-deprecation`, `-feature`, `-no-indent`, `-rewrite`, `-Xfatal-warnings`
- Use `sge-metals` MCP instead of running `sbt` commands directly

If you must run SBT directly (discouraged), **always use `sbt --client`** to avoid the overhead
of starting and shutting down a new JVM each time:

```bash
sbt --client compile
sbt --client scalafmt
```

The `--client` flag connects to an already-running SBT server (or starts one if needed), making
subsequent commands much faster. **Never use bare `sbt`** — always use `sbt --client`.

## Scalafmt

Code formatting: `sbt --client scalafmt` (or via MCP). Config in `.scalafmt.conf`.
