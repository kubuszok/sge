# Bugs and Ambiguities

Discovered bugs, undocumented behavior, and ambiguities in the original LibGDX source
found during the porting process.

## BA-001: (Template)

- **LibGDX**: `com.badlogic.gdx.path.ClassName` (line X)
- **SGE**: `sge.path.ClassName` (line Y)
- **Problem**: Description of the bug or ambiguity
- **Impact**: How it affects users
- **Resolution**: What SGE does about it
- **Status**: documented | fixed | reported-upstream

---

Bugs discovered during porting are tracked in the issues database:

```bash
re-scale db issues list              # List all open issues
re-scale db issues add <desc>        # Add a new issue
re-scale db issues resolve <id>      # Mark an issue as resolved
re-scale db issues stats             # Summary statistics
```

The template above (BA-nnn) can be used for documenting particularly notable bugs
that warrant detailed write-ups beyond what fits in the issues db.
