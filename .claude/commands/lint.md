Check and manage compiler linter flags for the SGE project.

Argument: `$ARGUMENTS` — one of: `status`, `flags`, `unused`, `null`

## Procedure

Based on the argument:

### `status`
Show which linter flags are currently enabled and their suppression status.

1. Read `build.sbt` with the Read tool and extract the `scalacOptions` section.
2. Show enabled flags, their purpose, and `-Wconf` suppressions.
3. Summarize: which flags produce errors, which are downgraded to info.

### `flags`
Show all available Scala 3.8 linter flags and their status.

Display this table:

| Flag | Purpose | Status |
|------|---------|--------|
| `-Werror` | Fatal warnings | **Enabled** |
| `-deprecation` | Deprecation warnings | **Enabled** (info) |
| `-feature` | Feature warnings | **Enabled** |
| `-Wimplausible-patterns` | Implausible pattern matches | **Enabled** |
| `-Wrecurse-with-default` | Self-recursion with defaults | **Enabled** |
| `-Wenum-comment-discard` | Enum comment ambiguity | **Enabled** |
| `-Wunused:imports` | Unused imports | **Enabled** (info) |
| `-Wunused:privates` | Unused private members | **Enabled** (info) |
| `-Wunused:locals` | Unused local definitions | **Enabled** (info) |
| `-Wunused:patvars` | Unused pattern variables | **Enabled** (info) |
| `-Wunused:nowarn` | Unnecessary @nowarn | **Enabled** (info) |
| `-Wshadow:all` | Variable shadowing | Not enabled (too noisy — constructor params shadow superclass fields) |
| `-Wvalue-discard` | Discarded non-Unit values | Not enabled (noisy with imperative code) |
| `-Wnonunit-statement` | Non-Unit block statements | Not enabled (noisy with imperative code) |
| `-Wtostring-interpolated` | toString in interpolation | Not enabled (evaluate first) |
| `-Winfer-union` | Union type inference | Not enabled (evaluate first) |
| `-Wsafe-init` | Safe initialization | Not enabled (experimental, false positives) |
| `-Yexplicit-nulls` | Null safety via type system | Not enabled (breaks Java interop) |

### `unused`
Run unused symbol analysis.

```
re-scale build compile
```

Then use the Grep tool to search the output for `[E198]` patterns.
Count and categorize: unused imports, unused privates, unused locals, unused patvars.

### `null`
Run null check analysis using the unified shortcut scanner — its `null-cast`
pattern category catches `null.asInstanceOf[T]` and the `Nullable.empty.getOrElse(null)`
anti-patterns.

```
re-scale enforce shortcuts
```

Filter the output for `null-cast` hits to see violations by file. For
legitimate Java interop boundaries, add a skip-policy entry:
```
re-scale enforce skip-policy add <path> shortcuts --reason "Java interop boundary"
```

## Promoting Warnings to Errors

When a category of issues has been fully fixed (e.g., all unused imports removed),
the corresponding `-Wconf` suppression can be removed from `build.sbt` to promote
those warnings back to errors. This prevents regressions.

## Important

**Do NOT use shell commands directly.** Use `re-scale` commands or `sbt --client` only.
