Audit all SGE Scala files in the package `$ARGUMENTS` against their LibGDX Java sources.

Argument: `$ARGUMENTS` — a package path like `math`, `graphics/g2d`, `graphics/g3d/particles`,
or `/batch` to audit multiple packages in sequence.

## Batch mode

If `$ARGUMENTS` is `/batch`, prompt the user for a list of package paths (one per line or
comma-separated) and audit each package in sequence. Between packages, write partial audit
docs and commit progress so work isn't lost if the session is interrupted.

## Procedure

1. **Enumerate files**: Use Glob to find all `.scala` files under
   `sge/src/main/scala/sge/$ARGUMENTS/` (non-recursive — only direct children).
   For platform packages, also check `scalajvm/`, `scalajs/`, `scalanative/` paths.

2. **Check for existing audit doc**: Look for `docs/audit/<slug>.md` where the slug
   is the package path with `/` replaced by `-` (e.g., `graphics/g3d` -> `graphics-g3d`).

3. **Audit each file**: For each Scala file, run the `/audit-file` procedure:
   - Read the Scala file and its Java source
   - Compare public API (methods, constants, enums, inner types)
   - Check conventions (no return, no null, split packages, braces, comments, copyright)
   - Check for SGE-specific TODOs and stub implementations
   - Check for missing `val`/`var` fields and incorrect `var`→`val` conversions
   - Check for tests (in both SGE and LibGDX)
   - Add/update migration notes in the file header
   - Record the audit result

   For packages with 30+ files, process in batches of 15 and write partial
   audit docs between batches.

4. **Write the per-package audit doc** at `docs/audit/<slug>.md`:
   ```markdown
   # Audit: sge.<dotted.package>

   Audited: N/N files | Pass: P | Minor: M | Major: J
   Last updated: YYYY-MM-DD

   ---

   ### FileName.scala

   | Field | Value |
   |-------|-------|
   | SGE path | `sge/src/main/scala/sge/<path>` |
   | Java source(s) | `com/badlogic/gdx/<path>` |
   | Status | pass/minor_issues/major_issues |
   | Tested | Yes — `test path` / No — reason |

   **Completeness**: Summary of API coverage.
   **Renames**: List or "None"
   **Convention changes**: Structural changes
   **TODOs**: Count and summary or "None"
   **Issues**:
   - `minor`/`major`: Description of each issue
   ```

5. **Compile-verify**: Run `sge-dev build compile` to ensure header edits didn't break anything.

6. **Commit**: Run `sge-dev git commit-all 'Audit sge.<pkg>: N files (P pass, M minor, J major)'`

7. **Update progress**: Update `memory/audit-progress.md` with the completed package.

## Important

**Do NOT use shell commands directly.** Use `sge-dev build compile` for compilation,
`sge-dev git commit-all` for committing, the Read tool for file reading, and
Grep/Glob tools for code search.
