# Tooling Guide

## MCP Servers

### metals-mcp

Official Metals MCP server from [scalameta/metals](https://github.com/scalameta/metals).
Install and run via `sge-dev`:

```bash
sge-dev metals install   # cs bootstrap from Sonatype snapshots
sge-dev metals start     # starts the MCP server on port 7845 (background)
sge-dev metals status    # check if running
sge-dev metals stop      # stop the server
```

MCP tools available: `compile-file`, `compile-full`, `glob-search`, `typed-glob-search`,
`inspect`, `get-docs`, `get-usages`, `find-dep`, `list-modules`, `format-file`, `test`.

### context7

Use for looking up documentation of external libraries (LWJGL, scala-js-dom, etc).

## Source Reference

The original LibGDX source is available locally as a git submodule:

```
./libgdx/                         # Git submodule (read-only reference)
./libgdx/gdx/src/com/badlogic/gdx/   # Core library source
```

**NEVER fetch LibGDX source from GitHub.** Always read from the local submodule.

Path mapping: `com/badlogic/gdx/<path>.java` → `sge/src/main/scala/sge/<path>.scala`

## SBT

Build tool configuration is in `build.sbt`. Key settings:

- Scala 3.8.2
- Compiler flags: `-deprecation`, `-feature`, `-no-indent`, `-rewrite`, `-Werror`
- Prefer MCP tools or `sge-dev build compile` / `sge-dev build fmt` over running sbt directly

If you must run SBT directly (discouraged), **always use `sbt --client`** to avoid the overhead
of starting and shutting down a new JVM each time:

```bash
sbt --client compile
sbt --client scalafmt
```

The `--client` flag connects to an already-running SBT server (or starts one if needed), making
subsequent commands much faster. **Never use bare `sbt`** — always use `sbt --client`.

## sge-dev CLI

Development toolkit for all build, test, quality, and release tasks. See CLAUDE.md for
the full command reference (70+ commands). Key examples:

```bash
# Build
sge-dev build compile                # Compile SGE core (JVM)
sge-dev build compile --all          # Compile all platforms (JVM/JS/Native)
sge-dev build compile --errors-only  # Show only errors
sge-dev build fmt                    # Run scalafmt
sge-dev build compile-fmt            # Compile + format + compile
sge-dev build publish-local --all    # Publish to local Maven (all platforms)

# Test
sge-dev test unit                    # JVM unit tests
sge-dev test unit --all              # All platforms (JVM/JS/Native)
sge-dev test unit --only <Suite>     # Single test suite
sge-dev test integration --all       # Integration tests
sge-dev test regression --all        # Regression tests

# Quality
sge-dev quality scan --all           # Run all quality scans
sge-dev quality scalafix <Rule>      # Run Scalafix rule

# Native
sge-dev native build                 # Build Rust native library
sge-dev native angle setup           # Download ANGLE libraries
sge-dev native curl setup            # Download static curl libraries
sge-dev native cross-all             # Cross-compile all 6 desktop targets

# Compare & audit
sge-dev compare file <path>          # Side-by-side libgdx vs sge
sge-dev db audit stats               # Audit progress
sge-dev db migration stats           # Migration progress
```

## Scalafmt

Code formatting: `sge-dev build fmt`. Config in `.scalafmt.conf`.
