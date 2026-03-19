Find code quality issues in the SGE codebase.

Argument: `$ARGUMENTS` — one of: `return`, `null`, `null_cast`, `java_syntax`, `todo`, `all`

## Procedure

1. Run the appropriate `sge-dev` command:

   ```
   sge-dev quality scan --$ARGUMENTS
   ```

   For summary counts only (no line-by-line output):
   ```
   sge-dev quality scan --$ARGUMENTS --summary
   ```

   Categories:
   - **`return`**: Actual `return` statements (excluding doc tags and comments)
   - **`null`**: `== null` / `!= null` checks (excluding comments and strings)
   - **`null_cast`**: `null.asInstanceOf` occurrences
   - **`java_syntax`**: Remaining Java keywords (`public`, `static`, `void`, etc.)
   - **`todo`**: `TODO`, `FIXME`, `HACK`, `XXX` markers
   - **`all`**: All categories except `java_syntax`

2. For each category, the scan reports:
   - Number of affected files
   - Total occurrences
   - Top 10 files by occurrence count (in full mode)

3. Cross-reference against the issues database using `sge-dev db issues list --category <cat>`
   and note any changes since last documented counts.

## Important

**Do NOT use `rg`, `grep`, or any shell commands directly.** All searches must go
through `sge-dev quality` or the dedicated Grep/Glob tools.
