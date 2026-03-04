Re-audit and fix all issues in the SGE package `$ARGUMENTS`.

Argument: `$ARGUMENTS` — a package path like `math`, `graphics/g2d`, `assets/loaders`

## Procedure

### Phase 1: Re-audit

1. **Run `/audit-package $ARGUMENTS`** with the latest audit criteria.
   This will re-read every file, compare against Java sources, and identify all issues
   including new criteria (SGE TODOs, test coverage, stubs, missing fields, var→val errors,
   copyright header).

2. **Collect the full TODO/issue list** from the audit results.
   Categorise by severity (major first, then minor).

### Phase 2: Fix all issues

3. **Fix each issue**, working through the list in priority order:

   - **Stubs / incomplete implementations**: Implement the missing logic by reading the
     Java source and converting properly. Follow all rules in `docs/contributing/conversion-rules.md`.
   - **Missing methods / fields**: Port from the Java source.
   - **Incorrect `var`→`val`**: Change back to `var` where the field is mutated.
   - **Missing `val` fields (API removed)**: Re-add the field to the class body.
   - **SGE-specific TODOs**: Resolve each one — implement the feature, fix the bug,
     or remove the TODO if it's no longer relevant.
   - **Convention violations**: Fix return→boundary/break, null→Nullable, copyright header,
     missing braces, flat packages, etc.
   - **Missing tests**:
     - If LibGDX has a test: port it to `core/src/test/scala/sge/` using munit.
     - If no test exists anywhere: create a basic test covering the public API.
   - **Copyright header**: Fix `Copyright` → `copyright`, `2024` → `2025`.

4. **Compile after each batch of fixes**: Run `just compile-errors` to catch regressions.
   If errors appear, fix them before proceeding.

5. **Run tests**: If tests were added or modified, run `just test` to verify they pass.

### Phase 3: Re-audit

6. **Run `/audit-package $ARGUMENTS` again** to verify all issues are resolved.
   New issues may have been accidentally introduced during fixes — catch them now.

7. **If new issues remain**, fix them and re-audit once more (max 3 iterations).

### Phase 4: Commit

8. **Compile-verify**: `just compile-errors` and `just compile-warnings`.

9. **Commit**: `just commit-all 'Correct sge.<pkg>: fix N issues (M major, m minor)'`

10. **Update progress**: Update `memory/audit-progress.md` if statuses changed.

## Important

- **Do NOT use shell commands directly.** Use `just` recipes and dedicated tools only.
- **Do NOT remove comments** from the original source — only add/update migration notes.
- Follow all conversion rules in `docs/contributing/conversion-rules.md`.
- When porting tests, use munit (`extends munit.FunSuite`).
- Use `SgeTestFixture` for tests that need a `Sge` context.
