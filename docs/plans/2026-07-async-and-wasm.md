# Plan 2026-07: Async story (`Loading[A]`) + Scala.js Wasm/JSPI posture

Status: decision + implementation plan (Topic 7 of `FABLE5_PLANNING_ROADMAP.md`).
Written 2026-07-01 against branch `more-improvements-2`. No code in this document has
been implemented; every step below is for an Opus-tier implementer session.

---

## 1. Context a fresh model needs

### 1.1 Why this plan exists

SGE's asynchronous surface today is `scala.concurrent.Future` + `ExecutionContext`,
advanced poll-style by the game loop. Users see raw `Future` in two places (extension
loading, HTTP listeners are callback-based) and drive asset loading by calling
`AssetManager.update()` every frame. There is no single handle type a game can hold for
"a value that will exist later", and the declared `gears` dependency suggests a second
async model that in fact plays no role in production code. This plan:

1. defines **`Loading[A]`** — a platform-neutral handle over the existing poll-pump, so
   game code never touches `Future`/`ExecutionContext` (or gears) directly;
2. fixes a **JS fidelity gap** in the pump (upstream GWT executes async-loader work
   synchronously; SGE JS queues it, which makes `finishLoading()` spin forever on JS);
3. sets the **gears posture**: drop the unused dependency; any future gears layer is an
   optional separate artifact, never a core-semantics dependency;
4. records the **Wasm/JSPI research annex** with mechanical go/no-go criteria a future
   model can re-evaluate without redoing the research.

### 1.2 What "done" means

- `sge.async.Loading[A]` exists, is tested on JVM + JS + Native, and wraps both async
  categories (asset loading, HTTP) through the public API only.
- The JS pump passes the same observable contract as upstream GWT (issue A below).
- `gears` is absent from `build.sbt`, `project/Versions.scala`, `THIRD-PARTY-LICENSES`,
  and the stale claim in `docs/improvements/dependencies.md` §B3 is corrected.
- The annex criteria in §6 are written into this doc (no code work).
- All issues below are filed in `.rescale/data/issues.tsv` via `re-scale db issues add`
  and each has passed its verification commands.

### 1.3 What must NOT change

- **Port covenants**: `sge/src/main/scala/sge/assets/AssetManager.scala` and
  `AssetLoadingTask.scala` are covenanted full-ports (see their headers). Their public
  method sets and bodies stay untouched — `Loading` integrates from the OUTSIDE via
  public API (`load`, `AssetLoaderParameters.loadedCallback`, `errorListener`).
  Running `re-scale enforce verify --all` must stay green without re-baselining these
  two files.
- **Existing async surfaces remain**: `AssetManager.update()/finishLoading()`,
  `SgeHttpClient.send(request, listener)`, `SgeExtension.load(): Future[Unit]` keep
  their signatures. `Loading` is additive sugar over them, not a replacement.
- **All 4 platforms are baseline** (JVM, JS, Native, Android). Nothing here may make
  the JS target depend on the Wasm backend or JSPI.
- **`(using Sge)` convention**: context is passed explicitly; no new globals.

### 1.4 Branch state assumptions (`more-improvements-2`)

- Browser assets are served **synchronously** from build-time-embedded resources:
  `sge/src/main/scalajs/sge/files/BrowserAssetLoader.scala` is backed by
  `multiarch.resources.PlatformResources` ("No fetch/manifest/preload: assets are
  embedded at compile time and served synchronously", header lines 7–10). This is
  load-bearing for issue A: on JS, the I/O half of `loadAsync` is synchronous already.
- sbt-scalajs is at 1.22 (see `project/plugins.sbt:14` comment), i.e. the Scala.js
  version where the Wasm backend became stable — relevant only to the annex.
- If any file:line reference below is off after a rebase, re-locate by the quoted
  identifier, not the number; if the identifier is gone, STOP and file an issue noting
  the drift instead of improvising.

---

## 2. Ground truth (verified 2026-07-01, this checkout)

### 2.1 SGE's actual async surface

| Piece | Location | Behavior |
|---|---|---|
| Per-manager executor | `sge/sge/src/main/scala/sge/assets/AssetManager.scala:64-69` | `ownedExecutor = concurrency.createExecutor()`; owned, shut down in `close()` (line 800) |
| Poll pump | `AssetManager.scala:453-483` | `update()` advances one task; `update(millis)` busy-loops with `yieldThread()` and on `ApplicationType.WebGL` delegates to a single `update()` (line 476) |
| Blocking waits | `AssetManager.scala:493-534` | `finishLoading()`/`finishLoadingAsset` loop `update()` + `concurrency.yieldThread()` |
| Off-thread work | `sge/sge/src/main/scala/sge/assets/AssetLoadingTask.scala:120-165` | `Future(apply())` on the owned executor for `getDependencies`/`loadAsync`; `loadSync` (GL upload) runs on the caller (main) thread; completion observed by polling `future.isCompleted` |
| Executor platform impls | `sge/sge/src/main/scala/sge/platform/ConcurrencyOps.scala`; `scaladesktop/sge/platform/ConcurrencyOpsDesktop.scala` (JVM+Native: fresh single-thread daemon executor per call, `yieldThread = Thread.yield`); `scalajs/sge/platform/ConcurrencyOpsJs.scala` (`ExecutionContext.global`, `yieldThread` no-op) | |
| HTTP | `sge/sge/src/main/scala/sge/net/SgeHttpClient.scala` (`Future` + listener callbacks, `ExecutionContext.global` at line 41); backends: JVM `DefaultFutureBackend` (java.net.http), JS `DefaultFutureBackend` (Fetch), Native `DefaultSyncBackend` (curl) wrapped in `Future` (`scalanative/sge/net/HttpBackendFactoryImpl.scala:25-28`) | |
| Extensions | `sge/sge/src/main/scala/sge/SgeExtension.scala:27` | `load(): Future[Unit]`, sequential fold in `loadAll` |
| Timers | `sge/sge/src/main/{scalajvm,scalanative}/sge/utils/TimerPlatformOps.scala` (daemon thread + `wait`/`notifyAll`), `scalajs/.../TimerPlatformOps.scala` (`setTimeout` reschedule) | No gears anywhere |

So async in SGE = exactly two categories — asset pipeline and HTTP — plus per-frame
timers, all pumped or callback-driven. Nothing blocks on JS today except the hazard in
§2.3.

### 2.2 gears is declared but unused

- `build.sbt:215`: `libraryDependencies += "ch.epfl.lamp" %% "gears" % Versions.gears`
  — inside `MatrixAction.ForPlatforms(VirtualAxis.jvm)`, i.e. **JVM axis only**.
- `project/Versions.scala:15`: `val gears = "0.3.1"`.
- `grep -rn gears` over `sge*/src` finds **zero** production or test usages. The only
  other mentions: `THIRD-PARTY-LICENSES:240` and `docs/improvements/dependencies.md`
  §B3, whose "Status: Done — … JVM: Gears `Async.blocking` + `AsyncOperations.sleep`"
  is **wrong**: the actual JVM `TimerPlatformOps` is a covenanted daemon-thread
  `wait`/`notifyAll` port with no gears import (verified by reading the file).

### 2.3 JS fidelity gap in the pump

Upstream GWT's `AsyncExecutor` emulation **runs the submitted task immediately**
(`original-src/libgdx/backends/gdx-backends-gwt/src/com/badlogic/gdx/backends/gwt/emu/com/badlogic/gdx/utils/async/AsyncExecutor.java:38-46`
— `submit` calls `task.call()` inline). That is why `finishLoading()` terminates on
GWT. SGE's JS executor is `ExecutionContext.global`
(`ConcurrencyOpsJs.scala:22`), which **queues** the task on the event loop; combined
with `yieldThread()` being a no-op on JS, `finishLoading()` on JS is an infinite spin
whenever an `AsynchronousAssetLoader` is involved: the queued `Future(apply())` can
never run while the loop occupies the only thread. No JS test currently exercises
`finishLoading` (all `finishLoading` tests live under `sge/src/test/scalajvm/`), which
is why this has gone unobserved. Issue A fixes it by restoring the upstream-GWT
synchronous-submit semantics.

### 2.4 Upstream census: what libgdx actually uses `AsyncExecutor` for

Commands (run in this checkout; reproduce to re-verify):

```
grep -rln "AsyncExecutor" original-src/libgdx --include="*.java"
grep -rn  "new AsyncExecutor" original-src/libgdx --include="*.java" | grep -v gwt/emu
```

Results — the complete reference set:

| File | Role |
|---|---|
| `gdx/src/com/badlogic/gdx/utils/async/{AsyncExecutor,AsyncResult,AsyncTask}.java` | the utility itself |
| `gdx/src/com/badlogic/gdx/assets/AssetManager.java:123` | **the only construction site in gdx core**: `executor = new AsyncExecutor(1, "AssetManager")` |
| `gdx/src/com/badlogic/gdx/assets/AssetLoadingTask.java:114,126` | `executor.submit(this)` for deps/async halves |
| `backends/gdx-backends-gwt/.../emu/.../utils/async/{AsyncExecutor,AsyncResult,AsyncTask}.java` + `emu/.../assets/AssetLoadingTask.java` | GWT emulation (synchronous submit) |

**Zero** references in `extensions/` or `tests/` of this checkout. Networking does NOT
use it: `gdx/src/com/badlogic/gdx/net/NetJavaImpl.java:138-144` builds its own
`ThreadPoolExecutor`. And `AssetManager.update(int)` special-cases WebGL exactly as SGE
does (`AssetManager.java:436-437`). Conclusion: upstream async = {asset pipeline via
one single-thread `AsyncExecutor`, HTTP via a private pool} + `AsyncExecutor` as a
public helper for user code. SGE's scope claim ("async = asset loading + HTTP") is
confirmed; SGE's user-facing equivalent of the public helper becomes `Loading` +
(internally) `ExecutionContext`.

---

## 3. Decision 1 — `Loading[A]`: a platform-neutral handle over the poll-pump

### 3.1 Design rules

1. The **poll-pump stays the portable core**. `Loading` never introduces blocking into
   core semantics; it is an observation/composition layer.
2. Completion callbacks run **on the main thread**, marshalled via
   `Sge().application.postRunnable` (`sge/sge/src/main/scala/sge/Application.scala:116`)
   — on JS the event loop is the main thread, so this is uniform on all platforms.
3. `Sge` is captured **at construction** (house `(using Sge)` constructor convention,
   see `CLAUDE.md`), so consuming methods (`onComplete`, `map`…) need no context.
4. House types: `lowlevel.Nullable` for absence, `scala.util.Try` for outcomes,
   `SgeError` for API misuse.
5. Blocking-style `await()` exists but is **advance-driven**: it pumps the producer,
   it does not park a thread on a condition variable. On WebGL it is legal only when
   the whole chain is synchronously advanceable (assets: yes, after issue A; HTTP: no —
   throws).

### 3.2 Exact API (new files; `Origin: SGE-original` headers, no covenant source ref)

File `sge/src/main/scala/sge/async/Loading.scala` (split packages: `package sge` /
`package async`; the package is named `async`, not `concurrent`, to avoid shadowing
`scala.concurrent` — always import `scala.concurrent.Future` explicitly inside):

```scala
package sge
package async

import scala.util.{ Failure, Success, Try }
import lowlevel.Nullable

/** Handle to a value produced by work that the game loop advances (poll-pump) or that
  * completes from a platform callback. Callbacks fire exactly once, on the main thread.
  */
sealed trait Loading[+A] {
  /** True once an outcome (success or failure) is available. Callable from any thread. */
  def isCompleted: Boolean
  /** Poll the outcome; `Nullable.empty` while in flight. */
  def outcome: Nullable[Try[A]]
  /** Poll the success value; empty while in flight; throws the failure if failed. */
  def value: Nullable[A]
  /** Register `cb`, invoked exactly once on the main thread (immediately-posted if
    * already completed). Multiple callbacks allowed; invocation order = registration order. */
  def onComplete[U](cb: Try[A] => U): Unit
  def map[B](f: A => B): Loading[B]
  def flatMap[B](f: A => Loading[B]): Loading[B]
  def zip[B](that: Loading[B]): Loading[(A, B)]
  /** True when `await()` is legal on single-threaded platforms: every pending stage of
    * this handle's chain can be driven to completion by its `advance` hook alone. */
  def advanceable: Boolean
  /** Drive to completion and return the value (or throw the failure).
    * JVM/Native: loops `advance(); yieldThread()` until completed.
    * WebGL (`Sge().application.applicationType == Application.ApplicationType.WebGL`):
    * legal only if `advanceable`; otherwise throws `SgeError.InvalidInput`
    * ("await() on a handle that needs the event loop; use onComplete or poll value"). */
  def await(): A
}

object Loading {
  def successful[A](a: A)(using Sge): Loading[A]
  def failed(e: Throwable)(using Sge): Loading[Nothing]

  /** ExecutionContext that posts runnables to the main thread via
    * `Sge().application.postRunnable`. The bridge for anything Future-based. */
  def mainThreadExecutionContext(using Sge): scala.concurrent.ExecutionContext

  /** Wrap a Future. `advanceable = false` (completion comes from the event loop or a
    * pool thread); callbacks marshalled through [[mainThreadExecutionContext]]. */
  def fromFuture[A](f: scala.concurrent.Future[A])(using Sge): Loading[A]

  /** Producer-side handle. `advance` is called by `await()` to push the producer
    * forward (e.g. `() => { manager.update(); () }`); `advanceable` declares whether
    * `advance` alone reaches completion on a single thread. */
  def promise[A](advance: () => Unit = () => (), advanceable: Boolean = false)(using Sge): Promise[A]

  final class Promise[A] private[Loading] (...) {
    def loading: Loading[A]
    def trySuccess(a: A): Boolean   // false if already completed
    def tryFailure(e: Throwable): Boolean
  }
}
```

Combinator semantics (implementer must follow exactly):

- `map`/`flatMap`/`zip` results inherit `advance` hooks: `map` keeps the source's;
  `flatMap`'s composite advances the currently-pending stage; `zip` advances both
  sides (call both hooks). `advanceable` of a composite = conjunction of its parts
  (for `flatMap`, of the source and — once known — the produced handle).
- Exceptions thrown by `f` in `map`/`flatMap` become a `Failure` of the result.
- All internal state transitions go through one `synchronized` block on the handle
  (JVM/Native thread-safety; free on JS); callbacks are never invoked while holding it.
- `onComplete` after completion posts the callback via `postRunnable` rather than
  invoking inline — one delivery discipline everywhere.

### 3.3 Wrapping the AssetManager (no covenant edits)

File `sge/src/main/scala/sge/async/AssetHandles.scala`. The covenanted
`AssetManager.scala` is not modified; everything goes through public API:

```scala
package sge
package async

/** Loading-handle façade over one AssetManager. Create at most one per manager. */
final class AssetHandles(manager: assets.AssetManager)(using Sge) {
  /** Queue `fileName` (via `manager.load`) and return a handle completed by the pump. */
  def load[T](fileName: String)(using scala.reflect.ClassTag[T]): Loading[T]
  def load[T](fileName: String, parameter: assets.AssetLoaderParameters[T])(using scala.reflect.ClassTag[T]): Loading[T]
}
```

Mechanics (deterministic, from public surface only):

1. **Success**: wrap/compose the descriptor's `loadedCallback`
   (`AssetLoaderParameters.loadedCallback`) — if the caller already set one, invoke
   theirs first, then `promise.trySuccess(manager.apply[T](fileName, tpe))`. The
   callback fires inside `update()` on the main thread (`AssetManager.scala:672-677`),
   so no extra marshalling is needed, but route through the promise anyway for the
   single delivery discipline.
2. **Failure**: on construction, `AssetHandles` reads `manager.errorListener` and
   installs a multiplexer listener that (a) completes a registered matching handle
   (keyed by `assetDesc.fileName`) with `tryFailure`, then (b) delegates to the
   previously-installed listener if one existed, else rethrows (preserving the
   default-throw contract of `handleTaskError`, `AssetManager.scala:734-738`).
   Document on the class: replacing `errorListener` after constructing `AssetHandles`
   disconnects failure routing — callers who need their own listener set it BEFORE
   constructing the façade (it will be chained).
3. **advance hook**: `() => { val _ = manager.update() }`; `advanceable = true`
   (valid on JS only after issue A lands — hence the issue ordering).
4. Already-loaded assets (`manager.isLoaded`) short-circuit: `load` still calls
   `manager.load` (ref-count increment happens in `nextTask`), and the loadedCallback
   path completes the handle on the next `update()`.

### 3.4 Wrapping HTTP (sttp stays fully hidden)

File `sge/src/main/scala/sge/async/HttpHandles.scala`:

```scala
package sge
package async

extension (client: net.SgeHttpClient) {
  /** Send and return a handle. Completion marshalled to the main thread.
    * Cancellation (`client.cancel(request)`) fails the handle with
    * `SgeError.InvalidInput("HTTP request cancelled: <url>")`. `advanceable = false`. */
  def sendLoading(request: net.SgeHttpRequest)(using Sge): Loading[net.SgeHttpResponse]
}
```

Implemented as a `Net.HttpResponseListener` passed to the existing
`client.send(request, Nullable(listener))`: `handleHttpResponse → trySuccess`,
`failed → tryFailure`, `cancelled → tryFailure`. Listener methods may arrive on a
backend thread (JVM `java.net.http` pool; Native `ExecutionContext.global`) — the
promise's completion path already posts callbacks via `postRunnable`, which covers it.
Per-platform truth this inherits (no new code): JVM = java.net.http async, JS = Fetch
(event loop; never blocks), Native = curl executed on a global-EC thread
(`scalanative/sge/net/HttpBackendFactoryImpl.scala:25-28`). `await()` on an HTTP handle
is legal on JVM/Native (loops `yieldThread` until the backend thread completes it) and
throws on WebGL, per §3.2 — Fetch cannot be driven synchronously, and that is stated
in the scaladoc rather than worked around.

### 3.5 Extensions and `(using Sge)` relationship

`SgeExtension.load(): Future[Unit]` keeps its signature (public API stability). Games
that want handles use `Loading.fromFuture(SgeExtension.loadAll(...)(using sge, Loading.mainThreadExecutionContext))`.
No wrapper API is added for extensions in this plan — one obvious bridge
(`fromFuture`) beats a second bespoke façade.

---

## 4. Decision 2 — gears posture: optional layer, never core; DROP the dependency now

**Decision: remove the dependency.** Justification:

1. **Unused**: zero references in production and test sources (§2.2). A declared-but-
   unused dependency misleads readers about core semantics and drags a resolution.
2. **It cannot be core for a 4-platform-baseline engine**: gears 0.3.1 requires JVM ≥ 21
   virtual threads (Android/ART has no virtual-thread story SGE can rely on) and its
   JS support (since 0.3.0, 2026-04) exists **only on the Scala.js Wasm backend via
   JSPI** — which §6 shows is absent from stable Safari and default-configuration
   Firefox as of 2026-07. Plain-JS output — SGE's shipping browser target — is
   unsupported by gears.
3. **The poll-pump already covers the entire async surface** (§2.1, §2.4); gears would
   add a second model without removing the first.

Posture going forward: if a JVM/Native structured-concurrency convenience is ever
wanted, it ships as a **separate optional artifact** (working name `sge-gears`,
JVM+Native axes only) adapting `Loading` to gears — sketch:
`extension [A](l: Loading[A]) def awaitAsync(using gears.async.Async): A` (suspend on
`onComplete`). That artifact is explicitly OUT OF SCOPE for this plan; no issue is
filed for it; this paragraph is its only deliverable, so a future decision can start
from the recorded sketch instead of re-deriving it.

Build change (issue E): delete `build.sbt:215`
(`libraryDependencies += "ch.epfl.lamp" %% "gears" % Versions.gears,`), delete
`project/Versions.scala:15` (`val gears = "0.3.1"`), delete the
`gears — Apache-2.0` line at `THIRD-PARTY-LICENSES:240`, and rewrite
`docs/improvements/dependencies.md` §B3's status paragraph to match reality (JVM timer
= daemon thread + `wait`/`notifyAll` faithful port; gears dependency removed 2026-07,
see this plan).

---

## 5. Issue decomposition (one implementer session each)

File each into `.rescale/data/issues.tsv` with `re-scale db issues add` (run
`re-scale db issues add --help` first; if the flag set differs from expectations, use
the help text — do not hand-edit the TSV). Category `improvement` unless stated;
severity as given. Reference this document in every description.

Order: **A → B → C → D**; **E** is independent (may run first or in parallel).
All work happens on top of `more-improvements-2` (or its merge result); if that branch
is gone, STOP and confirm the merge landed on the default branch before proceeding.

### Issue A — JS pump fidelity: synchronous owned executor (severity: major, category: fidelity)

- **Change**: `sge/src/main/scalajs/sge/platform/ConcurrencyOpsJs.scala` — replace the
  `ExecutionContext.global` owned executor with one whose `execute(r)` invokes
  `r.run()` **inline**, mirroring upstream GWT `AsyncExecutor.submit`
  (`original-src/libgdx/backends/gdx-backends-gwt/.../utils/async/AsyncExecutor.java:38-46`).
  Exceptions from `r.run()` must propagate to `reportFailure` semantics identical to a
  synchronous EC (wrap: catch non-fatal, complete the Future as failed — note
  `Future(body)` already catches, so plain inline `run()` suffices). Scope: ONLY the
  `OwnedExecutor` returned by `createExecutor()`; do not touch `ExecutionContext.global`
  users elsewhere (e.g. `SgeExtension`, HTTP).
- **Red test first** (commit the failing test before the fix; campaign proof-of-red):
  new `sge/src/test/scalajs/sge/assets/AssetManagerJsPumpTest.scala` with a small
  `AsynchronousAssetLoader` test fixture (model it on the loader in
  `sge/src/test/scalajvm/sge/assets/AssetManagerAsyncISS561Test.scala`, but JS-local —
  the JVM fixtures are not on the JS test classpath). Test body: `manager.load(...)`,
  then call `manager.update()` in a plain `while` loop bounded at 1000 iterations
  **without returning to the event loop**, assert `manager.isLoaded(...)`. With the
  queued executor this deterministically fails (the queued task can never run); with
  the inline executor it passes within a few iterations. Do NOT write the test as
  `finishLoading()` — that variant hangs instead of failing (single-threaded JS cannot
  time out a busy loop). Add a second test that `finishLoading()` returns, enabled
  only after the fix is in (same commit as the fix).
- **Done criteria**: both tests green on JS; JVM + Native suites unchanged.
- **Verify**: `re-scale test unit --module sge --js`, then
  `re-scale test unit --module sge --all`, then `re-scale build compile --all`.
- **Failure branches**: if any existing JS test breaks because it relied on deferred
  executor semantics, list the test(s) in the issue and check whether they exercise
  `createExecutor()` (only `AssetManager` does today) — if something else acquired the
  owned executor since this plan was written, re-scope the inline behavior to a new
  `ConcurrencyOps.createInlineExecutor()` used by nothing but AssetManager's JS path…
  which requires touching the covenanted `AssetManager.scala` — in that case STOP and
  file a follow-up issue instead of editing the covenant. If reentrancy errors appear
  (the inline task runs inside `update()` while holding the manager monitor), that is
  expected and safe on the single JS thread (monitors are free/reentrant there);
  document it in the test comment rather than "fixing" it.

### Issue B — `Loading[A]` core (severity: minor)

- **Change**: add `sge/src/main/scala/sge/async/Loading.scala` exactly per §3.2 (API
  surface is normative — any deviation goes back through plan review, not silent
  adjustment). House style: `-no-indent` braces, `Nullable`, no `null`, header comment
  with `Origin: SGE-original` migration notes.
- **Tests**: `sge/src/test/scala/sge/async/LoadingTest.scala` (platform-neutral, runs
  on all three) covering: successful/failed constructors; promise double-completion
  returns false; map/flatMap/zip success + failure + exception-in-f; callback
  main-thread delivery (use a recording fake `Application` whose `postRunnable`
  collects then drains — model on existing headless/test applications in
  `sge-test`/`sge/src/test`); `await()` with an advanceable promise; `await()` on
  WebGL app-type with `advanceable = false` throws `SgeError.InvalidInput`.
- **Done criteria**: suite green on JVM + JS + Native.
- **Verify**: `re-scale test unit --module sge --all --only sge.async.LoadingTest`
  (if `--only` needs a different suite-name form, check `re-scale test unit --help`);
  then `re-scale build compile --all`.
- **Failure branches**: if a fake `Application` is impractical to construct (too many
  abstract members), build a narrow test double implementing only
  `postRunnable` + `applicationType` and throwing `SgeError.InvalidInput` from the
  rest, and note that in the test header; if `Sge` cannot be instantiated from test
  code (`private[sge]` constructor — tests under `package sge` CAN call it), keep the
  test file inside `package sge.async` which satisfies `private[sge]`. If neither
  works, STOP and attach the compile error to the issue.

### Issue C — Asset handles façade (severity: minor; depends on A + B)

- **Change**: add `sge/src/main/scala/sge/async/AssetHandles.scala` per §3.3.
  `AssetManager.scala` and `AssetLoadingTask.scala` byte-identical afterwards
  (enforced by covenant verify).
- **Tests**: JVM: mirror the load/finish/error scenarios of
  `AssetManagerAsyncISS561Test` through handles (success completes with the loaded
  asset; failing loader fails the handle AND still honors a pre-installed user error
  listener; two handles + one shared dependency). JS: one end-to-end handle test
  driving `update()` in a loop (possible because of issue A).
- **Done criteria**: tests green on JVM + JS; `re-scale enforce verify --all` green
  with NO re-baseline of the two covenanted asset files.
- **Verify**: `re-scale test unit --module sge --jvm --only sge.async.AssetHandlesTest`,
  same `--js`; `re-scale enforce verify --all`; `re-scale build compile --all`.
- **Failure branches**: if failure routing cannot key reliably on
  `assetDesc.fileName` (dependency failure surfaces under the dependency's name —
  `handleTaskError` clears the whole task stack, `AssetManager.scala:712-739`), then:
  fail EVERY registered in-flight handle of that manager when the multiplexer fires
  (conservative, documented) and record the refinement as a follow-up issue; do not
  attempt to reconstruct the dependency graph from outside. If `errorListener`
  chaining conflicts with a test that installs its own listener after façade
  construction, that test is exercising the documented misuse — assert the documented
  behavior instead of changing the design.

### Issue D — HTTP handles (severity: minor; depends on B)

- **Change**: add `sge/src/main/scala/sge/async/HttpHandles.scala` per §3.4.
- **Tests**: platform-neutral test with a fake `HttpBackendFactory` (the trait and
  `SgeHttpClient`'s `private[sge]` constructor are reachable from `package sge` test
  code — `SgeHttpClient.noop()` at `SgeHttpClient.scala:233` shows the pattern):
  success completes with response; backend failure fails the handle;
  `client.cancel(request)` fails the handle with the documented error; `await()` on a
  WebGL-typed fake application throws.
- **Done criteria**: suite green on JVM + JS + Native.
- **Verify**: `re-scale test unit --module sge --all --only sge.async.HttpHandlesTest`;
  `re-scale build compile --all`.
- **Failure branches**: if the fake backend cannot complete the returned `Future` on a
  chosen thread deterministically, use `scala.concurrent.Promise` completed explicitly
  by the test body — never `Thread.sleep` timing. If listener callbacks arrive before
  `send` returns (synchronous fake), the promise API already tolerates it
  (`trySuccess` before any `onComplete` registration is a supported ordering — that is
  what the §3.2 "post if already completed" rule exists for).

### Issue E — remove gears (severity: minor; independent)

- **Change**: the four deletions/corrections listed at the end of §4.
- **Done criteria**: `grep -rn gears build.sbt project/ THIRD-PARTY-LICENSES` → no
  matches; `docs/improvements/dependencies.md` §B3 status matches the shipped
  `TimerPlatformOps` implementations.
- **Verify**: `re-scale build compile --all`; `re-scale test verify` (compiles every
  module × platform). Expect zero diffs outside the four files.
- **Failure branches**: if compilation fails after removal, someone introduced a gears
  usage after 2026-07-01 — STOP, do not re-add the dependency silently; file an issue
  quoting the usage site and this section, and let the orchestrator decide (the §4
  posture says such usage belongs in an optional artifact, not core).

### Issue F — demo adoption + docs (severity: minor; depends on C, D)

- **Change**: convert ONE demo's loading screen to handles —
  `demos/asset-showcase/src/main/scala/demos/assets/AssetShowcaseGame.scala`
  (`renderLoading` at line ~132 keeps polling `manager.update()` for progress; add a
  `Loading` handle for the *completion* signal to show the intended pattern). Add a
  short "Async loading" section to `docs/getting-started.md` documenting: poll-pump is
  the model; `Loading` is the handle; `await()` platform rules; JS never blocks on HTTP.
- **Done criteria**: demo compiles on JVM + JS + Native; browser Playwright smoke for
  the demo passes (`re-scale runner list` → use the browser/demo runner listed there).
- **Verify**: the demo-compilation runner from `.rescale/runners.yaml` (exact name via
  `re-scale runner list`) + the browser smoke runner.
- **Failure branches**: demos consume the *published* sge (see `CLAUDE.md` CI notes) —
  run `re-scale build publish-local --module sge --all` first; if the demo still
  resolves a stale artifact, check `demos/` build's version pin and STOP if it pins a
  released version (note it in the issue; do not bump pins ad hoc).

### Gates the orchestrator re-runs independently

After each issue: `re-scale build compile --all`, the issue's test commands,
`re-scale enforce verify --all`, `re-scale enforce shortcuts --covenanted`, and the
campaign ratchet (`/sge:ratchet-check`). Before closing the plan:
`re-scale test verify`. Opus dry-run gate (roadmap Topic 8): a fresh Opus subagent
restates §5 and executes Issue E (the smallest) in a scratch worktree; every ambiguity
it hits is a defect in THIS document — fix here and re-gate.

---

## 6. Research annex — Scala.js WebAssembly backend + JSPI (as of 2026-07-01)

Forward-looking only: **nothing in §3–§5 depends on Wasm.** Sources verified
2026-07-01 by web research; each claim carries its source so a future model can
re-check the exact pages.

### 6.1 Current state

| Fact | Source |
|---|---|
| Scala.js Wasm backend introduced experimentally in 1.17.0 (2024-09-28) | scala-js.org 1.17.0 announcement |
| `js.async { js.await(...) }` (JSPI-backed blocking-style await inside an async block) since 1.19.0 (2025-04-21); "orphan" awaits via `scala.scalajs.js.wasm.JSPI.allowOrphanJSAwait`; Scala 3 syntax support since Scala 3.8.0 | scala-js.org 1.19.0 announcement; scala/scala3 3.8.0 release notes |
| **Scala.js 1.22.0 (2026-06-20): Wasm backend officially stable.** Config: `withESFeatures(_.withESVersion(ESVersion.ES2022).withUseWebAssembly(true))` + `ModuleKind.ESModule`; the old `withExperimentalUseWebAssembly` flag is deprecated. **JSPI is a separate opt-in**: `withWasmFeatures(_.withUseJSPI(true))` (scala-js PR #5370) | scala-js.org 1.22.0 announcement; scala-js.org/doc/project/webassembly.html |
| Wasm backend targets Wasm 3.0 (WasmGC + exnref); runs flag-free in Node 25, Chrome 137, Firefox 134, Safari 26. JSPI is NOT part of Wasm 3.0 | scala-js.org/doc/project/webassembly.html |
| Performance: ~30% lower run time (geomean, compute-heavy) vs JS backend; ~2× fullLink code size | same page |
| JSPI spec: W3C Wasm CG **Phase 4** | v8.dev/blog/jspi; scala-js webassembly doc |
| JSPI browsers: **Chrome/Edge 137+ default-on**; Firefox implemented (~139) but pref-gated (`javascript.options.wasm_js_promise_integration`) through at least Fx 155 — conflicting sources, treat as NOT default; Safari: Technology Preview 238 + announced for Safari 27 beta (WWDC26), absent from stable Safari 26; JSPI is an Interop 2026 focus area | caniuse.com/wf-wasm-jspi; MDN Fx152 notes; webkit.org STP 238 + WWDC26 posts; webkit standards-positions #422 |
| Node: `--experimental-wasm-jspi` on 23/24; default in Node 25 per the scala-js support table (inferred, moderate confidence) | scala-js webassembly doc |
| `scala.concurrent.Await` remains unsupported on Scala.js; no scala-js issue/PR proposes implementing it via JSPI (checked #186, #1996, #3127 lineage) | scala-js GitHub issue search |
| gears 0.3.0 (2026-04-24) added Scala.js support **only on the Wasm backend via JSPI**; 0.3.1 (2026-05-07) optimized it (one `js.async` per Future). Plain-JS output unsupported. Known V8 < 14.2.75 stack bug in nested async contexts (avoid Node 24/25 for gears-on-Wasm) | lampepfl/gears README + release notes |
| sge already builds with sbt-scalajs 1.22 | `project/plugins.sbt:14` comment |

### 6.2 Would Scala.js JSPI give SGE blocking-style await?

Yes, mechanically — `js.async { ... js.await(promise) ... }` suspends without blocking
the event loop, and gears rides exactly that — but **only** on the Wasm backend, with
JSPI enabled at link time, in engines that ship JSPI. It is not `scala.concurrent.Await`:
code must be inside a `js.async` block (or use orphan awaits), so `finishLoading()`
could not silently become blocking; SGE would have to route the render loop itself
through `js.async`, a structural change to `BrowserApplication`.

### 6.3 Go/no-go — mechanical re-evaluation procedure

Re-run this checklist (each item is a lookup, not a judgment call):

1. **Browser baseline**: does caniuse.com/wf-wasm-jspi show JSPI enabled by default in
   current stable Chrome, Firefox, AND Safari? (2026-07-01: Chrome yes; Firefox no —
   pref-gated; Safari no — 27 beta only.)
2. **Toolchain**: is sge's Scala.js ≥ 1.22 with the Wasm backend stable (already true)
   AND does one sge demo link and pass the Playwright browser smoke with
   `withUseWebAssembly(true)` + `withUseJSPI(true)`? (Scripted probe: flip the linker
   config on `demos/asset-showcase` JS, run the browser smoke runner.)
3. **Need**: is there a concrete requirement — (a) a game-facing API that requires
   true blocking-style await in the browser beyond what §3 provides, or (b) a measured
   ≥ 20% frame/compute win on a real sge demo under Wasm that survives the ~2× bundle
   size against the browser packaging budget?

**IF 1 AND 2 AND 3 hold, THEN** open a plan for: a Wasm packaging leg in
`sge-build`/browser packaging (alongside plain JS, never replacing it until JSPI is
Baseline *widely available*), and optionally the `sge-gears` artifact extended to the
JS-Wasm axis. **What SGE gains**: browser `finishLoading()`/`await()` with upstream
blocking semantics, one structured-concurrency model on all platforms, and the Wasm
perf headroom. **Until then: no action** — the §3 design already covers both async
categories without any of it; that is the point of the poll-pump-as-portable-core
decision. Earliest plausible flip of criterion 1: Safari 27 stable + a Firefox
default-on release (Interop 2026 pressure), so re-check no earlier than late 2026.

---

## 7. Summary of decisions

1. **Poll-pump stays the portable core**; `Loading[A]` (new `sge.async` package) is the
   single user-facing handle over assets and HTTP; `await()` is advance-driven and
   platform-honest (WebGL: legal only for advanceable chains; HTTP on JS: poll or
   `onComplete`, never block).
2. **JS executor becomes synchronous-submit** (upstream-GWT fidelity), which makes the
   pump and `finishLoading()` behave identically on JS and fixes a latent spin.
3. **gears dropped from the build** (unused, JVM-only-declared, JS support requires
   Wasm+JSPI); future gears support = optional `sge-gears` artifact, sketched in §4,
   out of scope.
4. **Wasm/JSPI: no-go today** (Safari stable and default Firefox lack JSPI); §6.3 is
   the mechanical re-evaluation trigger.
