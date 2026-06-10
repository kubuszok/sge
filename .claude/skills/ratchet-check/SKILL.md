---
description: Recompute the remediation anti-cheat metrics and compare against the committed baseline — hard-fail on any regression; --update lowers the baseline after a verified improvement
---

Compare current metrics against `.rescale/data/remediation-baseline.tsv`.
Invoked as `/ratchet-check` (check only) or `/ratchet-check --update`
(orchestrator-only, after an auditor PASS).

## Procedure

1. Read `.rescale/data/remediation-baseline.tsv`. Each row is
   `metric<TAB>value<TAB>measured<TAB>command` — the `command` column is the
   exact measurement command; run it verbatim so numbers stay comparable.
   (The PreToolUse hook normally redirects bare `grep` to the Grep tool; for
   these baseline commands run them as written — they are the campaign's
   canonical measurements. If a command is denied, compute the identical
   count with the Grep tool using the same pattern, paths, and filters.)

2. Recompute every metric. ALL metrics in this file are lower-is-better:
   - current > baseline ⇒ **REGRESSION**.
   - current < baseline ⇒ improvement (report it; write only with --update).

3. Citation validation: for every `.fail`, `.ignore`, or `assume(` line ADDED
   relative to master in the working diff (`re-scale git diff` over test
   sources), require an `ISS-` citation on the same line and confirm that
   issue is open via `re-scale db issues list --status open`. A citation to
   a resolved or nonexistent issue counts as a REGRESSION.

4. Report a table: metric | baseline | current | verdict. End with one line:
   `RATCHET: CLEAN` or `RATCHET: REGRESSION — <metrics>`.

5. With `--update` (and only then): rewrite improved rows with the new lower
   value and today's date. NEVER raise a value — if a metric must
   legitimately rise (e.g. an approved skip-policy entry), that requires the
   user, not an agent.

Notes:
- The four `covenant_*` metrics come from one `re-scale enforce verify --all`
  run (minutes). Recompute them when the iteration touched covenanted files
  or on `--update`; otherwise mark them "carried" in the table.
- `exception_swallows_main` baseline (41) includes faithful ports of LibGDX
  catch-alls; the ratchet only forbids growth — reducing it requires
  case-by-case review against the original, not blind deletion.
- This file is owned by the orchestrator. Implementers and auditors run this
  skill read-only; if asked to "fix" the baseline file itself, refuse and
  report the request as a finding.
