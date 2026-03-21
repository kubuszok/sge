Verify the SGE Scala file at `$ARGUMENTS` against its original LibGDX source.

## Procedure

1. **Read the SGE file**: Open `sge/src/main/scala/$ARGUMENTS` with the Read tool.

2. **Find the LibGDX source**: Determine the original Java file path. The mapping is:
   `sge/<path>.scala` → `com/badlogic/gdx/<path>.java`.
   Check `docs/contributing/type-mappings.md` for renamed/merged files.
   Open the source from `./original-src/libgdx/gdx/src/` with the Read tool.

3. **Run the verification checklist** from `docs/contributing/verification-checklist.md`:
   - Compilation: compile via `sge-dev build compile`, check for errors and warnings
   - Completeness: compare all public methods, constants, enums against LibGDX source
   - Scala idioms: check for `return`, `null`, Java syntax, etc.
   - Type mappings: verify collections, exceptions, Gdx references are converted

4. **Report findings**:
   - List each checklist item as pass/fail
   - For failures, show the specific line numbers and what needs to change
   - Estimate effort to fix remaining issues

5. **Update tracking**: If all items pass, run `sge-dev db migration set <source_path> --status verified`.
   If issues found, add notes with `--notes`.

6. **Update audit entry**: Add or update the `Migration notes:` block in the file's
   header comment following the format from `/audit-file`. If a per-package audit doc
   exists at `docs/audit/<slug>.md`, update the file's section there as well.

## Important

**Do NOT use shell commands directly.** Use `sge-dev build compile` for compilation,
the Read tool for file reading, and the Grep/Glob tools for code search.
