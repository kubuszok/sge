# Parallel-agent territory protocol

Policy for running multiple implementation agents concurrently without
conflicts (maintainer directive 2026-07-16). The failure mode this prevents:
spawn several agents → overlapping edits → merge conflicts → "discard the
worktree and redo" → hours of work and quota wasted. Discarding a worktree
wholesale is treated as an orchestration failure, not a cleanup step.

## 1. Territory = the unit of assignment

Every dispatched agent receives an explicit **OWNED PATHS** list in its
prompt — a closed set of directories it may edit, typically one module or
package and its satellites:

```
sge-extension/ai/**                      (the module)
sge/src/test/**/ai/**                    (its tests, all platform rows)
docs/**/ai*                              (its docs, when in scope)
```

Rules:

- **Wave planning selects work whose path sets are pairwise disjoint.** The
  issues DB carries `file_path` per issue; the orchestrator groups issues by
  module and never assigns two live agents overlapping trees. One agent per
  module is the ideal case.
- An agent's prompt states both the owned paths AND the prohibition: *"You
  may create/edit files ONLY under these paths. Any other edit requires
  permission first (see §3). Unauthorized out-of-territory edits will be
  dropped in review."*
- Tests live inside the territory. A fix whose test must live in another
  module's tree is a planning error — re-scope before dispatch.

## 2. Global files are never shared

These are the empirical conflict magnets; they have special handling:

| Class | Examples | Rule |
|---|---|---|
| Build wiring | `build.sbt`, `project/**`, `sge-build/**` | Orchestrator-owned, or an **exclusive global lock**: at most ONE agent per wave may hold it, stated in its prompt |
| CI workflows | `.github/workflows/**` | Same as build wiring |
| Databases / config | `.rescale/**` (issues, audit, baseline, skip-policy) | Orchestrator only, always |
| Cross-cutting API | core traits consumed by many modules (`sge/src/main/scala/sge/*.scala`, `platform/*Ops`) | Never parallel with dependents: sequence as its own wave stage, dependents dispatch AFTER it merges (or against an agreed interface contract written into both prompts) |

## 3. Permission escalation (out-of-territory edits)

When an agent discovers it must touch a file outside its territory:

1. It **stops that thread of work** (continues other in-territory work if any)
   and sends the coordinator a message (`SendMessage` to `main`) containing:
   the file path, why the edit is needed, and a 2-3 line summary of the
   intended change.
2. The **coordinator checks the wave ledger** (§4): Is the path owned by
   another live agent? Does any queued work touch it? Is it a global file?
   - **Grant** — path is unowned and inert: reply granting the edit and add
     the path to the agent's ledger entry.
   - **Deny + take over** — global file or minor cross-cut: the agent
     delivers its in-territory work and REPORTS the needed edit; the
     coordinator applies it while stacking the train (this is the default
     for build.sbt/ci.yml one-liners).
   - **Deny + re-sequence** — the edit collides with another live agent: the
     requesting agent finishes what it can; the colliding piece becomes a
     follow-up dispatched after the other agent's work merges.
3. **Default-deny**: no grant = no edit. An agent that cannot proceed without
   the denied edit reports the blockage as a finding instead of working
   around it.

## 4. The wave ledger

The orchestrator keeps a per-wave ledger (scratchpad file, mirrored into the
campaign memory when a wave outlives a session):

```
wave 2026-07-17-1
  agent A (fix/ISS-757-font-scale): sge/src/main/scala/sge/scenes/scene2d/** + tests | grants: —
  agent B (fix/ISS-760-music-oncomplete): sge/src/main/scaladesktop/sge/audio/** + platform AudioOps (GRANTED sge/src/main/scala/sge/platform/AudioOps.scala — unowned) | grants: platform/AudioOps.scala
  global lock (build.sbt): UNHELD — orchestrator applies deltas at stack time
```

Before granting anything, the coordinator may also check ground truth:
`git -C .claude/worktrees/agent-*/ status --porcelain` shows what each live
agent has actually touched, catching drift from the declared territory.

## 5. Stacking and salvage

- Agents commit small, on their own worktree branch; the **orchestrator**
  advances refs and stacks the train (cherry-pick in ledger order; agents
  holding grants on shared paths stack LAST).
- On an unexpected conflict at stack time: **salvage, never discard** —
  cherry-pick the disjoint commits, re-dispatch only the conflicting slice
  with the merged state as its new base. Small commits exist precisely to
  make this cheap.
- Every agent hands off at a scalafmt fixpoint (files `git add`ed before
  `re-scale build fmt` — project.git=true skips untracked files).

## 6. Dispatch checklist (orchestrator)

0. **Cluster by root cause first** (maintainer directive 2026-07-16): issues
   sharing a systemic cause (a missing convention, a design flaw) are ONE
   territory fixing the design once — not N patch territories. Only genuine
   oversights/gaps/bugs get direct per-issue fixes. A theme with 3+ related
   issues is a design-fix candidate.
1. Group candidate issues by module/path; verify pairwise disjointness.
2. Sequence cross-cutting API work first (its own stage), dependents after.
3. Write the ledger; put OWNED PATHS + escalation instructions (§3) into
   every prompt, including the SendMessage-to-main mechanics.
4. Decide the global-lock holder (usually: nobody — orchestrator owns).
5. On completion: check each worktree's actually-touched files against its
   territory before stacking; out-of-territory edits without a ledger grant
   are dropped and bounced back.
6. **Saturate within memory**: if free memory allows another worktree sbt
   server (~1-3GB each; check `re-scale proc list --kind sbt` + OS memory),
   dispatch additional non-colliding territories rather than idling.
7. **Wave close-out**: sweep worktree sbt servers
   (`re-scale proc list --kind sbt` → targeted `re-scale proc kill`) — agents
   are told to kill their own, but verify; orphaned servers cost gigabytes.
