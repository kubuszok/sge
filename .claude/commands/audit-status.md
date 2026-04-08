Show audit progress for the SGE project.

Argument: `$ARGUMENTS` — optional package name (e.g., `math`, `graphics/g2d`).
If omitted, show overall progress.

## Procedure

### If no argument (overall progress):

1. **Query audit database**: Run `re-scale db audit stats` for overall counts.

2. **Read progress file**: Open `memory/audit-progress.md` with the Read tool.
   Display the tier status table and completed packages list.

3. **Summary**: Display:
   - Packages audited / total
   - Files audited / total
   - Pass / Minor / Major breakdown
   - Next tier to process

### If package argument given:

1. **Query audit database**: Run `re-scale db audit list --package $ARGUMENTS`.

2. **If results found**: Display the per-file audit statuses.

3. **If no results**: Report that the package has not been audited yet. List the
   Scala files in that package using Glob so the user knows what would be covered.

## Important

**Do NOT use shell commands directly.** Use the Read tool for file reading and
Grep/Glob tools for code search.
