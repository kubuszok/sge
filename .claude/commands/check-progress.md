Show migration progress for the SGE project, optionally filtered by package.

## Procedure

1. Run the status recipe:

   Overall summary:
   ```
   just sge-status
   ```

   Filtered by package (e.g., `graphics/g2d`):
   ```
   just sge-status <package>
   ```

2. If `$ARGUMENTS` is provided, use it as the package filter:
   ```
   just sge-status $ARGUMENTS
   ```

3. Summarize:
   - Total files per status (`not_started`, `partial`, `ai_converted`, `verified`, `idiomatized`, `skipped`)
   - Percentage complete (count files that are not `not_started` or `skipped`, divided by total non-skipped)
   - List any files with notes indicating issues

4. If no argument provided, show a high-level summary grouped by top-level package.

## Important

**Do NOT use shell commands directly.** Use `just sge-status` or Read the TSV file
with the Read tool.
