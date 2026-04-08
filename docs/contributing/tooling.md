# Tooling Guide

## MCP Servers

### metals-mcp

Official Metals MCP server from [scalameta/metals](https://github.com/scalameta/metals).
Install via Coursier:

```bash
cs install metals-mcp
```

The lifecycle wrapper that the legacy tool shipped (`metals start/stop/status`)
is not yet ported into `re-scale` core (deferred — see
`re-scale/docs/cross-flavor-diff.md` for the rationale). For now, run the
binary directly or wire it into your editor.

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
- Prefer MCP tools or `re-scale build compile` / `re-scale build fmt` over running sbt directly

If you must run SBT directly (discouraged), **always use `sbt --client`** to avoid the overhead
of starting and shutting down a new JVM each time:

```bash
sbt --client compile
sbt --client scalafmt
```

The `--client` flag connects to an already-running SBT server (or starts one if needed), making
subsequent commands much faster. **Never use bare `sbt`** — always use `sbt --client`.

## `re-scale` CLI

Cross-project Scala porting toolkit. See CLAUDE.md for the full command
reference. Key examples:

```bash
# Build
re-scale build compile                # Compile SGE core (JVM)
re-scale build compile --all          # Compile all platforms (JVM/JS/Native)
re-scale build compile --errors-only  # Show only errors
re-scale build fmt                    # Run scalafmt
re-scale build compile-fmt            # Compile + format + compile
re-scale build publish-local --all    # Publish to local Maven (all platforms)

# Test
re-scale test unit                    # JVM unit tests
re-scale test unit --all              # All platforms (JVM/JS/Native)
re-scale test unit --only <Suite>     # Single test suite
re-scale test verify                  # Cross-platform compile gate

# Enforcement (covenant + shortcut detection)
re-scale enforce shortcuts            # Scan all source for shortcut/stub markers
re-scale enforce shortcuts --covenanted   # Only files with a Covenant: full-port header
re-scale enforce verify --all         # Re-verify every covenanted file
re-scale enforce verify --file <path> # Verify a single file
re-scale enforce stale-stubs          # Two-pass scan for "not yet ported" comments
re-scale enforce skip-policy add <path> shortcuts --reason "Java interop"

# Cross-language compare
re-scale enforce compare --port <scala> --source <java> [--strict]

# Database queries
re-scale db audit stats               # Audit progress
re-scale db audit list --package <pkg>
re-scale db migration stats           # Migration progress
re-scale db migration list --package <pkg>
re-scale db issues list --status open

# SGE-specific runners (defined in .rescale/runners.yaml)
re-scale runner list                  # Catalogue of available runners
re-scale runner desktop-it            # Desktop integration tests
re-scale runner browser-it            # Browser integration tests
re-scale runner android-it            # Android emulator tests
re-scale runner release-build         # Build all 11 demo releases
re-scale runner android-build-all     # Build all 11 demo APKs
```

## Scalafmt

Code formatting: `re-scale build fmt`. Config in `.scalafmt.conf`.
