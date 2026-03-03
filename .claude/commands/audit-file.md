Audit the SGE Scala file at `$ARGUMENTS` against its original LibGDX Java source.

## Procedure

1. **Read the SGE file**: Open `core/src/main/scala/$ARGUMENTS` with the Read tool.
   Extract the current header comment, note any existing migration notes.

2. **Find the LibGDX source**: Determine the original Java file path. The mapping is:
   `sge/<path>.scala` → `com/badlogic/gdx/<path>.java`.
   Check `docs/contributing/type-mappings.md` for renamed/merged files.
   Open the source from `./libgdx/gdx/src/` with the Read tool.

3. **Compare public API**:
   - List all public methods, constants, enums, and inner types in the Java source
   - Verify each exists in the Scala file (accounting for renames from type-mappings)
   - Note any missing methods, changed signatures, or extra methods
   - For files > 1000 lines, use Grep to compare method signatures rather than
     reading the entire body; only read full methods when a discrepancy is found

4. **Check conventions**:
   - **No `return`**: verify `boundary`/`break` is used instead
   - **No `null`**: verify `Nullable[A]` is used (check for raw `null` outside interop)
   - **Split packages**: verify package declaration uses split form
   - **Braces**: verify `{}` on all trait/class/enum/method defs
   - **Comments**: verify original comments are preserved
   - **No `scala.Enumeration`**: must use Scala 3 `enum`, preferably `extends java.lang.Enum`
   - **Case classes must be `final`**: check all `case class` declarations have `final`
   - **No Java-style getters/setters**: if `getX()`/`setX(v)` have no extra logic,
     replace with a public `var x`. If they have logic, use `def x: T` + `def x_=(v: T): Unit`

5. **Document renames and convention changes**:
   - Renames: class/method/field name changes from Java to Scala
   - Merged with: if file combines multiple Java sources
   - Convention: structural changes (opaque types, companion objects, sealed traits, etc.)
   - TODOs: count any remaining TODO/FIXME/HACK comments

6. **Add/update migration notes in the file header**:
   Edit the existing header comment to include a `Migration notes:` block after
   the copyright line. Format:
   ```
    * Migration notes:
    *   Renames: OldName -> NewName, oldMethod -> newMethod
    *   Merged with: OtherFile.java (if applicable)
    *   Convention: static class -> companion object; enum -> sealed trait
    *   Idiom: boundary/break (N return), Nullable (N null), split packages
    *   TODOs: N — brief summary (if any)
    *   Audited: YYYY-MM-DD
   ```
   Omit lines that don't apply (no renames = omit Renames line, etc.).

7. **Determine audit status**:
   - `pass`: All public API present, conventions followed, no issues
   - `minor_issues`: Cosmetic issues (could merge overloads, naming style)
   - `major_issues`: Missing methods, incorrect signatures, logic errors

8. **Return structured result**: Output an `AUDIT_RESULT` block:
   ```
   AUDIT_RESULT:
   file: <scala path>
   java_source: <java path(s)>
   status: pass|minor_issues|major_issues
   tested: Yes — <test file> | No
   completeness: <summary>
   renames: <comma-separated or "none">
   conventions: <summary>
   todos: <count and summary or "none">
   issues: <bullet list or "none">
   ```

## Important

**Do NOT use shell commands directly.** Use the Read tool for file reading,
Grep/Glob tools for code search, and `just compile` only if you need to verify
a header edit compiles.
