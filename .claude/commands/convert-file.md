Convert the LibGDX Java file at `$ARGUMENTS` to idiomatic Scala 3 for SGE.

## Procedure

1. **Read the source**: Open `./libgdx/gdx/src/$ARGUMENTS` to understand the original Java code.
   **NEVER fetch from GitHub** — always use the local submodule.
   Extract `@author` tags from the Javadoc for the license header.

2. **Add license header**: The very first thing in the Scala file (before `package`) must be
   the license header per `docs/contributing/code-style.md`. Include the original source path
   (`$ARGUMENTS`) and authors from `@author` tags (or "See AUTHORS file" if none).

3. **Check existing conversion**: Look for an existing Scala file at the mapped path under
   `core/src/main/scala/sge/`. The path mapping is: `com/badlogic/gdx/<path>.java` → `sge/<path>.scala`.
   Check `docs/contributing/type-mappings.md` for renamed/merged files.

4. **Plan the conversion** following `docs/contributing/conversion-rules.md`:
   - Apply all 19 type/API adjustments (a–s)
   - Follow code style rules from `docs/contributing/code-style.md`
   - Use `Nullable[A]` patterns from `docs/contributing/nullable-guide.md`
   - Use `boundary`/`break` patterns from `docs/contributing/control-flow-guide.md`

5. **Execute the conversion**:
   - Use `sge-metals` MCP to compile and check errors/warnings
   - Never call `sbt` directly
   - If intermediate steps need compilation, comment out code rather than removing it

6. **Verify compilation**: Use `sge-metals` MCP to confirm zero errors and zero warnings.

7. **Update tracking**: Update the file's row in `docs/progress/migration-status.tsv`
   to reflect the new status.
