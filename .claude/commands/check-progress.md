Show migration progress for the SGE project, optionally filtered by package.

## Procedure

1. Run the status command:

   Overall summary:
   ```
   re-scale db migration stats
   ```

   Filtered by package (e.g., `graphics/g2d`):
   ```
   re-scale db migration list --package <package>
   ```

2. If `$ARGUMENTS` is provided, use it as the package filter:
   ```
   re-scale db migration list --package $ARGUMENTS
   ```

3. Summarize:
   - Total files per status (`not_started`, `partial`, `ai_converted`, `verified`, `idiomatized`, `skipped`)
   - Percentage complete (count files that are not `not_started` or `skipped`, divided by total non-skipped)
   - List any files with notes indicating issues

4. If no argument provided, show a high-level summary grouped by top-level package.

## Important

**Do NOT use shell commands directly.** Use `re-scale db migration` commands or Read the TSV file
at `.rescale/data/migration.tsv` with the Read tool.
