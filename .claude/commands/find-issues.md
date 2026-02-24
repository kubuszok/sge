Find code quality issues in the SGE codebase.

Argument: `$ARGUMENTS` — one of: `return`, `null`, `java_syntax`, `todo`, `all`

## Procedure

1. Based on the argument, search `core/src/main/scala/sge/` for:

   - **`return`**: Find files containing the `return` keyword. These need `boundary`/`break`
     rewriting per `docs/contributing/control-flow-guide.md`.

   - **`null`**: Find files with `== null`, `!= null`, or `null.asInstanceOf`. These need
     `Nullable[A]` patterns per `docs/contributing/nullable-guide.md`.

   - **`java_syntax`**: Find files with remaining Java keywords: `public`, `private`,
     `protected`, `static`, `void`, `boolean`, `final`, `implements`.

   - **`todo`**: Find files with `TODO`, `FIXME`, `HACK`, or `XXX` comments.

   - **`all`**: Run all four searches.

2. For each category, report:
   - Number of affected files
   - Total occurrences
   - Top 10 files by occurrence count

3. Cross-reference against `docs/progress/quality-issues.md` and note any changes
   since last documented counts.
