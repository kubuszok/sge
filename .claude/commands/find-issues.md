Find code quality issues in the SGE codebase.

Argument: `$ARGUMENTS` — one of: `return`, `null`, `null_cast`, `java_syntax`, `todo`, `all`

## Procedure

1. Run the appropriate `just` recipe:

   ```
   just sge-quality $ARGUMENTS
   ```

   For summary counts only (no line-by-line output):
   ```
   just sge-quality $ARGUMENTS summary
   ```

   Categories:
   - **`return`**: Actual `return` statements (excluding doc tags and comments)
   - **`null`**: `== null` / `!= null` checks (excluding comments and strings)
   - **`null_cast`**: `null.asInstanceOf` occurrences
   - **`java_syntax`**: Remaining Java keywords (`public`, `static`, `void`, etc.)
   - **`todo`**: `TODO`, `FIXME`, `HACK`, `XXX` markers
   - **`all`**: All categories except `java_syntax`

2. For each category, the recipe reports:
   - Number of affected files
   - Total occurrences
   - Top 10 files by occurrence count (in full mode)

3. Cross-reference against `docs/progress/quality-issues.md` (read with the Read tool)
   and note any changes since last documented counts.

## Important

**Do NOT use `rg`, `grep`, or any shell commands directly.** All searches must go
through `just sge-quality` or the dedicated Grep/Glob tools.
