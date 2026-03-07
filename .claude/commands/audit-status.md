Show audit progress for the SGE project.

Argument: `$ARGUMENTS` — optional package name (e.g., `math`, `graphics/g2d`).
If omitted, show overall progress.

## Procedure

### If no argument (overall progress):

1. **Read progress file**: Open `memory/audit-progress.md` with the Read tool.
   Display the tier status table and completed packages list.

2. **Count audit docs**: Use Glob to find all `docs/audit/*.md` files (excluding README.md).
   Count them and list packages with their pass/minor/major counts.

3. **Count migration notes**: Use Grep to search for `Audited:` in Scala file headers
   under `sge/src/main/scala/sge/`. Report how many files have migration notes
   vs total files.

4. **Summary**: Display:
   - Packages audited / total
   - Files audited / total
   - Pass / Minor / Major breakdown
   - Next tier to process

### If package argument given:

1. **Find audit doc**: Look for `docs/audit/<slug>.md` where slug uses hyphens.

2. **If found**: Read and display the audit doc summary (top table and per-file statuses).

3. **If not found**: Report that the package has not been audited yet. List the
   Scala files in that package using Glob so the user knows what would be covered.

## Important

**Do NOT use shell commands directly.** Use the Read tool for file reading and
Grep/Glob tools for code search.
