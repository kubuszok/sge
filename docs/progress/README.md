# Migration Progress Tracking

## Files

- **migration-status.tsv** — Per-file status of every LibGDX core Java file
- **quality-issues.md** — Known systemic code quality issues across the codebase

## Status Definitions

| Status | Meaning |
|--------|---------|
| `not_started` | No SGE equivalent exists yet |
| `partial` | Conversion started but incomplete |
| `ai_converted` | AI-generated conversion exists, not yet verified |
| `verified` | Human-verified against LibGDX source (see [verification checklist](../contributing/verification-checklist.md)) |
| `idiomatized` | Fully idiomatic Scala 3 (opaque types, inline, etc) |
| `skipped` | Intentionally not ported (Scala stdlib replacement, not needed, etc) |

## How to Update

1. Open `migration-status.tsv` in any TSV-capable editor
2. Find the row for the file you worked on
3. Update the `status` column
4. Add notes in the `notes` column (especially for issues found)
5. Update the date in the header comment

## TSV Format

Tab-separated, 4 columns:
```
libgdx_path	sge_path	status	notes
```

Lines starting with `#` are comments.
