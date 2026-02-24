Show migration progress for the SGE project, optionally filtered by package.

## Procedure

1. Read `docs/progress/migration-status.tsv`

2. If `$ARGUMENTS` is provided, filter rows where `libgdx_path` contains that string
   (e.g., `graphics/g2d` shows only the g2d package).

3. Summarize:
   - Total files per status (`not_started`, `partial`, `ai_converted`, `verified`, `idiomatized`, `skipped`)
   - Percentage complete (count files that are not `not_started` or `skipped`, divided by total non-skipped)
   - List any files with notes indicating issues

4. If no argument provided, show a high-level summary grouped by top-level package.
