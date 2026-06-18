---
description: Merge a PR while keeping YOUR verified (signed) commits in GitHub's history. Works around GitHub rebase-merge re-signing every commit (losing your signature) and the "merge-locally-then-push closes the PR instead of marking it merged" problem. Merges on GitHub to get the merged status, then overwrites the base with your locally-signed equivalent via a guarded force-push — only after proving no content is lost and nothing else merged in between. Skips bot PRs. Invoked as `/verified-merge <pr-number|branch>`.
---

You merge a PR so that GitHub shows the resulting commits as **Verified with the
user's own signature**, not GitHub's web-flow re-signature. The mechanism: let
GitHub mark the PR merged, then replace the base tip with your locally-signed
commits — guarded so you can never lose work or clobber someone else's merge.

Arguments: `$ARGUMENTS` — a PR number or the PR's head branch.

## 0. Preconditions / bail-outs

1. Resolve PR + metadata: `gh pr view <pr> --json number,headRefName,baseRefName,author,state,mergeable`.
2. **Skip bots.** If `author.login` ends with `[bot]` or is one of
   `scala-steward`, `dependabot[bot]`, `renovate[bot]`, `github-actions[bot]`:
   do a plain `gh pr merge <pr> --rebase` (or `--auto`) and STOP. We don't
   preserve verification for bot PRs — that's by design.
3. **Signing must be configured**, else this skill is pointless. Check:
   `git config commit.gpgsign` is `true`, and a key exists
   (`git config user.signingkey` / `git config gpg.format`). If not, warn the
   user and STOP (offer to help set up signing) — don't produce unsigned commits.
4. Working tree clean (`git status --porcelain` empty).
5. This skill assumes the repo merges via **rebase** (sge/ssg:
   `rebaseMergeAllowed=true`, merge-commit & squash disabled). Confirm with
   `gh repo view --json rebaseMergeAllowed,mergeCommitAllowed,squashMergeAllowed`.

## 1. Pre-flight safety snapshot (the "lose nothing / nothing merged in between" guarantee)

```
git fetch origin
BASE=<baseRefName>                       # usually master
BASE_BEFORE=$(git rev-parse origin/$BASE)   # remote base tip BEFORE merging
MB=$(git merge-base origin/$BASE origin/<headRefName>)
```
Record the PR's commits: `git log --format='%h %s' $MB..origin/<headRefName>`.
Confirm your local `$BASE` is at or behind `BASE_BEFORE` and fast-forwardable
(no local divergence you'd lose).

## 2. Build YOUR signed version locally (before merging on GitHub)

Replay the PR commits onto the current base, signed by you — the same shape
GitHub's rebase-merge will produce, but with your signature:
```
git checkout -B vmerge-tmp origin/<headRefName>
git rebase --gpg-sign origin/$BASE        # or: -c commit.gpgsign=true rebase
LOCAL_RESULT=$(git rev-parse HEAD)
```
Resolve any conflict exactly as the merge would; if conflicts are non-trivial,
stop and surface them. Verify every commit `$BASE..LOCAL_RESULT` is signed:
`git log --format='%h %G?' origin/$BASE..LOCAL_RESULT` (expect `G`/`U`, not `N`).

## 3. Merge on GitHub to get the "merged" status

```
gh pr merge <pr> --rebase --delete-branch=false
git fetch origin
GH_RESULT=$(git rev-parse origin/$BASE)
```
Now `origin/$BASE` holds GitHub's re-signed commits and the PR is marked merged.

## 4. PARITY + NO-INTERLEAVE CHECKS — do not force-push until BOTH pass

1. **Content identical** (only signatures/SHAs differ):
   `git diff $GH_RESULT $LOCAL_RESULT` MUST be empty. If not → ABORT, leave
   GitHub's merge as-is, report the difference.
2. **Nothing else merged in between**: the commits GitHub added must be EXACTLY
   the PR's commits — same count, same patches — sitting directly on
   `BASE_BEFORE`. Check:
   - `git rev-list --count $BASE_BEFORE..$GH_RESULT` == number of PR commits, and
   - `git log --format=%s $BASE_BEFORE..$GH_RESULT` matches the PR subjects, and
   - `git merge-base --is-ancestor $BASE_BEFORE $GH_RESULT` is true.
   If any unexpected commit appears (someone merged another PR in the window) →
   ABORT. Force-pushing would erase their work. Tell the user; they can rebase
   `LOCAL_RESULT` onto the new tip and re-run.

## 5. Guarded force-push (swap GitHub's commits for yours)

```
git checkout $BASE
git reset --hard $LOCAL_RESULT
git push --force-with-lease=$BASE:$GH_RESULT origin $BASE
```
The explicit lease `=$BASE:$GH_RESULT` makes the push refuse unless remote
`$BASE` is still exactly `GH_RESULT` — i.e. nothing landed since §4. This is the
final guard against clobbering a concurrent merge. (Branch protection must permit
force-push to `$BASE`; if it rejects, that's expected — the user toggles it.)

## 6. Verify and clean up

- `gh pr view <pr> --json state` → `MERGED` (not CLOSED).
- Confirm verification: `gh api repos/{owner}/{repo}/commits/$(git rev-parse $BASE) --jq .commit.verification.verified` → `true`, and the GitHub UI shows the green **Verified** badge with your signature.
- Delete the temp branch (`git branch -D vmerge-tmp`) and the PR head branch if desired.
- Report: PR merged + commits verified under the user's signature, BASE_BEFORE →
  new tip, and confirmation that no other commits were touched.

## Notes
- Run `/condense-branch` first if the PR branch is large and messy — merge a
  clean story, verified.
- Everything before §5 is reversible. The only irreversible step is the
  force-push, and the lease + parity checks gate it.
