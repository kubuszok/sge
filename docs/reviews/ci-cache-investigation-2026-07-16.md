# CI warm-up → fan-out investigation (2026-07-16)

Maintainer idea: warm-up jobs compile all IR (JVM/JS/Native `Test/compile`),
hand the compiled state to downstream jobs, flatten the `needs` chain so
unit/IT/demo/release jobs fan out in parallel after one cheap compile gate.
Verified empirically by a worktree agent (local Apple-Silicon, sbt 2.0.2;
all CI numbers from run 29488410745, the 23m28s PR-tier run).

## Verdict

**PARTIAL — the goal (fail-fast compile gate + parallel fan-out) is sound and
worth doing, but the winning mechanism is NOT workspace hand-off; it is
per-OS reuse of the sbt 2.0 disk cache (`~/.cache/sbt/v2`) via
`actions/cache`, plus flattening the `needs` chain.** Shipping `target/` is
unsound by design — sbt 2 materializes outputs as ~2 550 absolute symlinks
into the content-addressed store, so a tarred `target/` dangles on the
consumer. And the long-pole jobs' dominant cost (per-module
`nativeLink`/`fastLinkJS` link→optimize→run cycles) is NOT CAS-cached, which
caps savings on `test-native` at ~20%.

## Key measurements

| Experiment | Result |
|---|---|
| Cold `Test/compile` (sge JVM/JS/Native rows) | 33 s / 27 s / 32 s local (≈3.4× slower on ubuntu runners: 112 s sge JVM compile on CI) |
| Same path, target+cache intact, fresh server | 10 s / 7 s / ~1 s — zinc fully up-to-date |
| Fresh clone, DIFFERENT absolute path, cache-only seeded | full hits observed (cache IS path-portable), but nondeterministic partial misses in 2 of 3 trials (see risks) |
| `publishLocal-jvm-3` warm re-run (new session/version) | 55 s → 22-27 s (only version-stamped generated sources recompile) |
| `nativeLink` / `fastLinkJS` with seeded cache | compile hits, link/optimize/clang ALWAYS re-run (not cacheable) |
| sbt v2 disk cache size (3-row Test/compile) | 111 MB / 165 files (artifact-friendly); +full JVM publishLocal → 288 MB; est. all-rows 400-600 MB raw, ~150-250 MB zstd |
| `target/` same state | 106 MB / 22 400 files incl. absolute CAS symlinks — do not ship |
| CI today | `needs` edges transfer NOTHING (zero artifacts in the workflow — pure ordering); NOTHING preserves `~/.cache/sbt/v2` (`setup-java cache: sbt` caches dependency jars only). Every job pays full compile despite sbt 2 caching being on. |
| Publish-from-source tax in downstream jobs | browser-IT 254 s / demo-compile-all 347 s / demo-smoke 208 s / android-IT 205 s / verify-release 190-280 s per leg |

## Recommended plan

**Phase 1 — zero-topology-change:** add `actions/cache` for the sbt v2 cache
dir (`~/.cache/sbt/v2` linux; `~/Library/Caches/sbt/v2` macOS;
`%LOCALAPPDATA%\sbt\v2` windows — verify) to every sbt job, keyed
`sbt2cache-${{ runner.os }}-${{ runner.arch }}-${{ github.sha }}` with
prefix restore-keys; save on master pushes only. PR jobs restore master's
warm cache and compile only the delta. Estimated: test-jvm −2 min, test-js
−1 min, test-native −1.2 min, each publishLocal-heavy job −2-4 min. No
artifact plumbing; every job keeps minting its own version via
`writeDemoVersion; publishLocal-*`, now cheaply (sidesteps the
session-minted timestamp version coupling that an Ivy-local hand-off would
create).

**Phase 2 — flatten the graph:** new `compile-gate` job (restore cache →
`Test/compile` all three rows → save cache); repoint
`test-browser-it`, `demo-smoke-js`, `demo-compile-all`, `test-desktop-it`,
`test-native-ffi-it`, `test-native-runtime` to `needs: [compile-gate]`;
`verify-release` to `needs: [plan, compile-gate]`. The matrix test jobs stay
ungated (they ARE the merge gate). No macOS warm-up job — macOS runner
scarcity (ISS-740) means an extra queued macOS job would lengthen the chain.
**Expected PR-tier wall: 23.5 min → ~14-15 min (−35-40%).** Master full-tier
stays dominated by the Rosetta native leg (~50 min); only a proven
cross-arch cache hit could shave its compile portion.

**Rejected:** tar of `target/` (CAS symlinks, 22k files); artifact hand-off
of `~/.ivy2/local` (couples consumers to one minted version + `.sge-version`
files; warm re-publish is nearly as fast with zero plumbing).

## Risks / spike questions (one scratch workflow on a branch answers 1-3)

1. Real cache hit-rates on GH runners (env/attrs differences) — 2-job
   scratch workflow: A compiles+saves, B restores+compiles, grep B's log for
   `compiling`.
2. Cross-OS / cross-arch hits (linux→macos/windows; aarch64→x64 Rosetta) —
   sbt 2's path-virtualized Bazel-compat design should allow it; unverified.
3. Nondeterministic full-row misses (2 of 3 local trials): suspects are the
   multiarch `GeneratedEmbeddedResources` generator, `scalafmtOnCompile` on
   matrix-row source views, timestamp-version-stamped generated sources
   (proven for freetype/physics recompiles), and the ~40 `Def.uncached`
   products-as-classpath wirings (build.sbt:261/277/282) leaking into sge's
   Test scope. Fixing these raises hit-rate from ~70% to ~100% of compile.
4. Cache eviction under the 10 GB repo cap (~150-250 MB × 6 os/arch ×
   commits): save master-only, prefix restore.
5. Windows cache path + tar performance — measure in the spike.
6. Meta-build (project/ incl. sge-build sources) never cache-hits in a fresh
   checkout (~10 s/job) — minor.

Longer-term: sbt 2 natively speaks the Bazel gRPC remote-cache protocol
(bazel-remote / BuildBuddy) — a persistent shared cache would replace
`actions/cache` keying entirely; evaluate after Phase 1 proves hit-rates.
