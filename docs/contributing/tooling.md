# Tooling Guide

## MCP Servers

### metals-mcp (snapshot — not yet released)

Official Metals MCP server from [scalameta/metals](https://github.com/scalameta/metals).
Currently only available as a snapshot build. Install and run via the Justfile:

```bash
just metals-install   # cs bootstrap from Sonatype snapshots
just metals           # starts the MCP server on port 7845
```

To update the snapshot version, check the latest `publish` job at
<https://github.com/scalameta/metals/actions> and update `metals_mcp_version` in the Justfile.

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

- Scala 3.8.1
- Compiler flags: `-deprecation`, `-feature`, `-no-indent`, `-rewrite`, `-Werror`
- Prefer MCP tools or `just compile` / `just fmt` over running sbt directly

If you must run SBT directly (discouraged), **always use `sbt --client`** to avoid the overhead
of starting and shutting down a new JVM each time:

```bash
sbt --client compile
sbt --client scalafmt
```

The `--client` flag connects to an already-running SBT server (or starts one if needed), making
subsequent commands much faster. **Never use bare `sbt`** — always use `sbt --client`.

## Justfile

Reusable task recipes. Run `just --list` to see all available commands.

```bash
just compile            # sbt --client compile
just fmt                # sbt --client scalafmt
just compile-fmt        # compile + format + compile
just sge-status         # migration status summary
just sge-next-batch PKG # pick next not_started files
just sge-quality all    # run all quality scans
just libgdx-list PKG    # list Java files in a libgdx package
just libgdx-find PAT    # find files in libgdx submodule
just libgdx-compare PATH # side-by-side libgdx vs sge
```

## Scalafmt

Code formatting: `just fmt` (or `sbt --client scalafmt`). Config in `.scalafmt.conf`.
