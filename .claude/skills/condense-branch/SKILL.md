---
description: Rewrite an oversized feature branch into a handful of logical, story-telling commits — group each "introduce → fix → fix-the-fix → reformat → CI-poke" churn chain into one clean commit, then (only after a backup branch exists) rebase and force-push. Invoked as `/condense-branch [branch] [--onto <base>] [--auto]`. Proposes the plan and waits for approval before rewriting history.
---

You CONDENSE a large branch into few commits that read as a clear story. You
NEVER rewrite history until a backup branch exists. You NEVER touch the default
branch (master) with this skill.

Arguments: `$ARGUMENTS`
- optional branch name (default: current branch)
- `--onto <base>` the base to measure against (default: the repo default branch,
  i.e. `origin/HEAD` → usually `master`)
- `--auto` skip the approval pause (default is PROPOSE-THEN-APPLY: show the plan
  and STOP for the user's OK before any rewrite)

## 0. Preconditions (abort with a clear message if any fail)

1. Resolve the default branch: `git symbolic-ref --short refs/remotes/origin/HEAD`
   (fallback `master`). REFUSE if the target branch == default branch.
2. Working tree must be clean: `git status --porcelain` empty. If not, stop and
   tell the user to commit/stash first (do not stash silently — their WIP is sacred).
3. `git fetch origin` so base comparisons are current.
4. Determine the range: `BASE=$(git merge-base origin/<default> <branch>)`. The
   commits in scope are `BASE..<branch>`. If that's ≤ 3 commits, say "already
   small, nothing to condense" and stop unless the user insists.

## 1. Analyze and group

List the range with detail:
`git log --reverse --format='%h %an %ad %s' --date=short BASE..<branch>` and
`git log --reverse --stat BASE..<branch>` (and diffs as needed).

Group commits into a SMALL number of logical units (target the user's "40 → 5-7").
Strong signals that commits belong to the SAME group:
- A later commit fixes/reverts/tweaks an earlier one ("fix", "fixup", "oops",
  "typo", "address review", "actually", "revert previous").
- Pure-noise commits: "reformat", "scalafmt", "fix CI", "retrigger", "wip",
  "bump", "lint" — fold these INTO the functional commit they belong to; never
  let them survive as standalone commits.
- Commits touching the same files/module in a short window with no independent
  meaning.
Keep SEPARATE: genuinely distinct features/concerns, even if interleaved in time
(you will reorder them to be contiguous).

For each target group, draft a commit message that tells the story:
- Subject: imperative, ≤ ~70 chars, what the change achieves (not "wip"/"fixes").
- Body: WHY it was done and WHAT the end result is. Where the original churn is
  informative, summarise the journey in one line ("folds in the follow-up null
  guard and the Scala.js link fix"). Do NOT enumerate every squashed commit.
- Preserve any issue/ticket refs (ISS-NNN, #NNN) found in the originals.

## 2. Present the plan, then STOP (unless --auto)

Show a table: target commit # | subject | which original short-SHAs fold in.
Show before→after count. Ask for approval. Do NOT proceed until the user agrees
(skip this pause only with `--auto`).

## 3. Backup FIRST — non-negotiable

Before any rewrite, after approval:
```
TS=$(date +%Y%m%d-%H%M%S)
git branch backup/<branch>-$TS <branch>
git push -u origin backup/<branch>-$TS    # off-machine safety net; ask first if you must
```
Confirm `git rev-parse backup/<branch>-$TS` == `git rev-parse <branch>`. Record
the original tip SHA. If the backup branch cannot be created, STOP.

## 4. Rewrite

Check out the branch. Reshape with a non-interactive interactive-rebase:
1. Build the rebase todo so each group is contiguous and in your chosen order;
   within a group the first original commit is `pick`, the rest are `fixup`.
2. Apply it: `GIT_SEQUENCE_EDITOR='cp <todo-file>' git rebase -i BASE`.
3. Resolve conflicts as they arise (reordering can conflict). If a conflict is
   non-trivial or risky, `git rebase --abort` and fall back to: `git reset --soft
   BASE` then re-create the groups by staging per group — viable when groups are
   file-disjoint. If still stuck, restore from backup and report.
4. Set the story messages: `git rebase -i BASE` again marking the K survivors as
   `reword`, feeding messages via a queue-style `GIT_EDITOR` (a tiny script that
   pops the next prepared message file per invocation), or simply `git commit
   --amend` each survivor while walking the K commits.

## 5. Verify integrity — the rewrite must change ZERO code

```
git diff backup/<branch>-$TS <branch>     # MUST be empty
```
If non-empty, the rewrite altered content → `git reset --hard backup/<branch>-$TS`
and report. A clean condense only reshapes history; the final tree is identical.
Also sanity-check: `git log --format='%h %s' BASE..<branch>` shows the K story
commits, and (if the repo signs) every new commit is signed — see `verified-merge`.

## 6. Force-push (only after §5 passes)

```
git push --force-with-lease origin <branch>
```
`--force-with-lease` (never bare `--force`) so a concurrent remote update aborts
the push instead of being clobbered.

## 7. Report

- before → after commit count, and the new story (subjects).
- backup branch name (local + remote) and the restore command:
  `git reset --hard backup/<branch>-$TS && git push --force-with-lease`.
- Note the branch is now ready for `/verified-merge`.
