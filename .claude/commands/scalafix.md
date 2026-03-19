Run a Scalafix rule on the SGE codebase.

Argument: `$ARGUMENTS` — a rule name, optionally followed by a file path or glob.

## Available Rules

### Local rules (in `sge/src/scalafix/scala/fix/`)

- **`NullToNullable`** — Lint-only: reports `== null` and `!= null` comparisons with
  suggested Nullable replacements (isEmpty, isDefined, fold, foreach, getOrElse).

### Built-in Scalafix rules

- **`RemoveUnused`** — Removes unused imports, privates, locals, and pattern variables.
  Requires `-Wunused` compiler flags (already enabled).
- **`DisableSyntax`** — Lints for banned syntax. Configure in `.scalafix.conf`:
  ```
  DisableSyntax { noReturns = true, noNulls = true }
  ```
- **`OrganizeImports`** — Sorts and groups imports.

## Procedure

1. Parse `$ARGUMENTS`:
   - If a single word: treat as rule name, run on all files.
   - If two words: first is rule name, second is file path/glob.

2. Run the rule:

   All files:
   ```
   sge-dev quality scalafix <RuleName>
   ```

   Specific file:
   ```
   sge-dev quality scalafix <RuleName> --file <path>
   ```

3. Report results:
   - For lint rules (NullToNullable, DisableSyntax): show violations found, grouped by file.
   - For rewrite rules (RemoveUnused, OrganizeImports): report files modified.

4. If the user wants to fix violations found by a lint rule, help them apply the correct
   pattern from the nullable guide (`docs/contributing/nullable-guide.md`) or control flow
   guide (`docs/contributing/control-flow-guide.md`).

## Important Notes

- **Never use `orNull`** for unwrapping Nullable values. `orNull` is `@deprecated` and
  should only be used at Java interop boundaries with an explicit `@nowarn` + comment.
- Use `fold`, `foreach`, `getOrElse`, `map`, `isDefined`, or `isEmpty` instead.
- The NullToNullable rule is diagnostic-only — it cannot auto-fix because the correct
  replacement depends on context. Each null check must be manually reviewed.
- **Do NOT use shell commands directly.** Use `sge-dev` commands or `sbt --client` only.
