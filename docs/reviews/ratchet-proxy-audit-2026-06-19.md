# Ratchet proxy-metric audit — 2026-06-19

Triggered when resuming the remediation campaign (`/goal`): two ratchet rows
read above their 2026-06-10/06-15 baselines. A deep file-by-file audit was run
(user-authorized, "deep-audit first") to prove the drift is benign before
re-arming `/loop /goal`. Result: **every elevated hit is benign; no real
incomplete work was introduced.**

## Authoritative gate — CLEAN (exactly at baseline)

`re-scale enforce verify --all` (2026-06-19):

| metric | baseline | current |
|---|---|---|
| covenant_fail_total | 190 | 190 |
| covenant_shortcut_drift | 134 | 134 |
| covenant_missing_header | 56 | 56 |
| covenant_methods_removed | 0 | 0 |
| open_review_issues | 13 | 13 |

No covenanted file gained a shortcut or lost a method. This is the real
anti-cheat signal and it has not moved.

## Drifted proxy rows — audited benign

### shortcut_hits 241 → 449 (raw repo-wide scanner)

136 hits in main source (50 files), 313 in test source (47 files). Three
parallel Explore audits classified every main-source hit:

- **Platform stubs** — `throw UnsupportedOperationException` on ops genuinely
  unsupported on a platform/GL-version (browser file I/O & sockets, native
  buffer address, GL ES methods absent from ANGLE — cross-checked identical to
  LibGDX `Lwjgl3GL20`). Faithful divergence, not dropped functionality.
- **Interop null-casts** — JS/Java/C boundary sentinels, all carrying
  `@nowarn` / `scalafix:ok` + comment.
- **Prose false-positives** — the words *stub/minimal/placeholder/pending/
  simplified* in legitimate comments (GLFW "upcall stub arena", "Execute
  pending runnables", "placeholder hint text", Panama arena-lifecycle docs).
- **Inherited upstream TODOs** — 3 from gdx-vfx / gdx-gltf (documentation
  debt, full method bodies present).

313 test-source hits are campaign red-test fixtures and stub mock providers
(`???` on unused mock methods, "Minimal stub subsystems" prose). The campaign
adds test files by design.

All three audits returned **VERDICT: ALL BENIGN**. The skip-policy allow-list
already carries 338 approved shortcut exemptions for these established
patterns. The 449 floor cannot be lowered without deleting faithful platform
stubs; it is the true current floor.

### sge_original_covenant_refs 79 → 85

7 files added during the campaign carry `Covenant-source-reference:
SGE-original` because they have **no LibGDX Java equivalent** (verified: no
matching `.java` in `original-src/libgdx`): FrameHookHost, SgeExtension,
NoopGL20, SgeAndroidDriver, PhysicsExtension(3d), NativeProviderValidation —
from approved ISS-676/677/678 + tooling work. SGE-original is the correct
reference for them.

## Policy going forward

The hard-stop ratchet gate is the **covenant family + open_review_issues**
(all clean). `shortcut_hits` and `sge_original_covenant_refs` are coarse
informational proxies; their baselines are updated to the audited current
values (449, 85) with this audit as provenance. They do **not** mask future
real shortcuts: any shortcut added to a covenanted file independently trips
`covenant_shortcut_drift`, and `covenant_methods_removed` catches any dropped
method. A future raw rise still warrants a look, but is not a hard stop on its
own.
