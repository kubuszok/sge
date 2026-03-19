---
description: Load boundary/break patterns for replacing return, break, and continue statements in SGE code
---

Load the boundary/break patterns for replacing return/break/continue in SGE.

$READ docs/contributing/control-flow-guide.md

Apply these patterns when converting Java control flow to Scala 3.
`return` → `boundary:break`, `break` wraps loop, `continue` wraps loop body.
