Convert the LibGDX Java file at `$ARGUMENTS` to idiomatic Scala 3 for SGE.

## Procedure

1. **Read the source**: Open `./original-src/libgdx/gdx/src/$ARGUMENTS` with the Read tool to
   understand the original Java code.
   **NEVER fetch from GitHub** — always use the local submodule.
   Extract `@author` tags from the Javadoc for the license header.

2. **Add license header**: The very first thing in the Scala file (before `package`) must be
   the license header per `docs/contributing/code-style.md`. Include the original source path
   (`$ARGUMENTS`) and authors from `@author` tags (or "See AUTHORS file" if none).

3. **Check existing conversion**: Look for an existing Scala file at the mapped path under
   `sge/src/main/scala/sge/`. The path mapping is: `com/badlogic/gdx/<path>.java` → `sge/<path>.scala`.
   Check `docs/contributing/type-mappings.md` for renamed/merged files.

4. **Plan the conversion** following `docs/contributing/conversion-rules.md`:
   - Apply all 19 type/API adjustments (a–s)
   - Follow code style rules from `docs/contributing/code-style.md`
   - Use `Nullable[A]` patterns from `docs/contributing/nullable-guide.md`
   - Use `boundary`/`break` patterns from `docs/contributing/control-flow-guide.md`

5. **Execute the conversion**:
   - Use `re-scale build compile` to compile and check errors/warnings
   - If intermediate steps need compilation, comment out code rather than removing it

6. **Verify compilation**: Use `re-scale build compile` to confirm zero errors and zero warnings.

7. **Update tracking**: Run `re-scale db migration set <source_path> --status ai_converted`
   to update the file's status.

8. **Write initial audit entry**: Add a `Migration notes:` block to the file's header
   comment documenting renames, convention changes, and idiom compliance. Follow the
   format described in `/audit-file`. Set `Audited:` to today's date.

## Important

**Do NOT use shell commands directly.** Use `re-scale build compile` for compilation,
`re-scale db migration` for tracking, the Read tool for file reading, and the
Grep/Glob tools for code search.
