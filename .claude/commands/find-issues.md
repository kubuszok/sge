Find code quality issues in the SGE codebase.

Argument: `$ARGUMENTS` — optional source path filter (e.g. `sge`, `sge-extension`,
or a single file). If omitted, scans all source roots in `.rescale/scan-targets.txt`.

## Procedure

1. **Run the unified shortcut scanner** via `re-scale enforce shortcuts`:

   Scan everything in `.rescale/scan-targets.txt`:
   ```
   re-scale enforce shortcuts
   ```

   Scan a specific path:
   ```
   re-scale enforce shortcuts --src $ARGUMENTS
   ```

   Scan only files that have an existing `Covenant: full-port` header
   (these are the ones the verify gate will fail on if they regress):
   ```
   re-scale enforce shortcuts --covenanted
   ```

   Scan a single file:
   ```
   re-scale enforce shortcuts --file <path>
   ```

2. **Pattern categories produced by the scanner**:

   - **Code stubs**: `TODO`, `FIXME`, `HACK`, `XXX`, `???` (Scala unimplemented),
     `throw new UnsupportedOperationException`, `throw new NotImplementedError`
   - **Java interop workarounds**: `null.asInstanceOf[T]`, `Nullable.empty.getOrElse(null)`,
     `this(null, ...)`
   - **Pre-`scala.util.boundary` patterns**: `var done|continue|finished|stop|...`
     used as a flag-break variable
   - **Comment markers**: `stub`, `simplified`, `minimal`, `placeholder`, `pending`,
     `shim`, `best effort`, `approximation`, `deferred`, `Phase N`, `not yet`,
     `for now`, `aspirational`

3. **Two-pass stale-stub scan** for "not yet ported" comments where the symbol
   has since been added elsewhere:
   ```
   re-scale enforce stale-stubs
   ```

4. **Cross-reference against the issues database**:
   ```
   re-scale db issues list --status open
   re-scale db issues stats
   ```

5. **Skip-policy management** — for hits that are legitimate (Java interop
   boundaries, intentional `UnsupportedOperationException` like
   `ai/steer/limiters/NullLimiter`, platform stubs that are inert by design):
   ```
   re-scale enforce skip-policy list
   re-scale enforce skip-policy add <path> shortcuts --reason "Java interop boundary"
   ```

## Important

**Do NOT use `rg`, `grep`, or any shell commands directly.** All searches must go
through `re-scale enforce shortcuts` / `re-scale enforce stale-stubs` or the
dedicated Grep/Glob tools.
