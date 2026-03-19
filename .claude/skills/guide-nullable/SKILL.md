---
description: Load the Nullable[A] opaque type guide for null-safe patterns in SGE code
---

Load the Nullable[A] opaque type guide for SGE.

$READ docs/contributing/nullable-guide.md

Key patterns:
- `Nullable.empty` instead of `null`
- `Nullable(value)` to wrap
- `isDefined`/`isEmpty` for checks (no `.nonEmpty`)
- `getOrElse`, `fold`, `foreach` for safe access
- `orNull` only at Java interop boundaries with `@nowarn` + comment
