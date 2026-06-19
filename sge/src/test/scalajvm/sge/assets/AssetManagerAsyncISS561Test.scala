/*
 * SGE — AssetManager ASYNC load-path unit tests (ISS-561, batch F)
 *
 * AssetManagerUnitTest.scala already covers the SYNCHRONOUS loader path
 * (SynchronousAssetLoader): load + finishLoading + get, ref-counting,
 * unload/dispose, progress reaching 1.0, queue removal, etc.
 *
 * This suite covers the ASYNC path it misses: an AsynchronousAssetLoader
 * driven by update()/finishLoading() through AssetLoadingTask. On JVM the
 * async part (loadAsync) genuinely runs on a background worker thread
 * (ConcurrencyOpsDesktop), and update() polls the future until it completes,
 * then runs loadSync on the calling thread. We pin EXACT phase order,
 * EXACT progress fractions, and EXACT ref-count/dispose behaviour so that
 * representative production mutations would fail.
 */
package sge
package assets

import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters.*

import munit.FunSuite
import sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver }
import sge.files.{ FileHandle, FileType }
import lowlevel.util.DynamicArray

class AssetManagerAsyncISS561Test extends FunSuite {

  // ─── Test infrastructure ─────────────────────────────────────────────

  /** A trivial "asset" type for testing. Records dispose for ref-count tests. */
  final case class AsyncAsset(name: String, loadAsyncTag: String) extends AutoCloseable {
    @volatile var closed: Boolean = false
    override def close(): Unit    = closed = true
  }

  /** Resolver that returns a FileHandle wrapping the filename (no real I/O). */
  final private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  /** An asynchronous loader for AsyncAsset that RECORDS every phase it runs.
    *
    * loadAsync (the "background"/worker-thread phase) stores an intermediate value; loadSync (the "GL-thread" phase) builds the final asset from it. If loadSync ever runs without loadAsync having run
    * first, the asset's loadAsyncTag is "" and the produced name is wrong — caught by assertions.
    */
  final private class RecordingAsyncLoader(
    resolver:   FileHandleResolver,
    val phases: CopyOnWriteArrayList[String],
    // Maps a PARENT fileName -> its single distinct CHILD dependency. Only the
    // exact parent gets a dependency; every other asset (the child included)
    // gets NONE. This keeps the dependency graph strictly acyclic — no asset
    // ever declares itself (or anything else) as its own dependency, so a
    // faithful AssetManager with no in-flight-task cycle guard cannot loop.
    val dependencyOf: Map[String, String] = Map.empty
  ) extends AsynchronousAssetLoader[AsyncAsset, AssetLoaderParameters[AsyncAsset]](resolver) {

    // value produced by loadAsync, consumed by loadSync (per current task)
    @volatile private var asyncValue: String = ""

    override def getDependencies(
      fileName:  String,
      file:      FileHandle,
      parameter: AssetLoaderParameters[AsyncAsset]
    ): DynamicArray[AssetDescriptor[?]] = {
      phases.add(s"getDependencies:$fileName")
      dependencyOf.get(fileName) match {
        case Some(child) =>
          val arr = DynamicArray[AssetDescriptor[?]]()
          arr.add(AssetDescriptor(child, classOf[AsyncAsset], lowlevel.Nullable.empty))
          arr
        case None =>
          // null = no dependencies (matches AssetManagerUnitTest convention)
          null.asInstanceOf[DynamicArray[AssetDescriptor[?]]]
      }
    }

    override def loadAsync(
      manager:   AssetManager,
      fileName:  String,
      file:      FileHandle,
      parameter: AssetLoaderParameters[AsyncAsset]
    ): Unit = {
      phases.add(s"loadAsync:$fileName")
      asyncValue = s"async($fileName)"
    }

    override def loadSync(
      manager:   AssetManager,
      fileName:  String,
      file:      FileHandle,
      parameter: AssetLoaderParameters[AsyncAsset]
    ): AsyncAsset = {
      phases.add(s"loadSync:$fileName")
      AsyncAsset(fileName, asyncValue)
    }
  }

  private def makeContext(): Sge = SgeTestFixture.testSge()

  // ─── 0. ISS-684 anchor: close() must NOT kill other managers' loads ──

  /** ISS-684 (deterministic red anchor).
    *
    * AssetManager.close() calls `concurrency.shutdown()` on a PROCESS-WIDE SHARED executor singleton (PlatformOps.concurrency = ConcurrencyOpsDesktop, a single `object` with ONE executorService).
    * LibGDX uses a PER-INSTANCE executor (AssetManager.java:123/703). After ANY AssetManager is closed, every OTHER AssetManager's async loads are submitted to a now-shut-down executor and throw
    * RejectedExecutionException.
    *
    * This test closes manager A, then drives an async load on a fresh manager B built from the SAME Sge. On a correct (per-instance executor) implementation B loads fine. On current code A.close()
    * killed the shared executor, so B's loadAsync submission throws RejectedExecutionException (surfaced via SgeError) — making this the clean proof-of-red.
    */
  test("ISS-684: closing one AssetManager must NOT break async loading on another (shared executor must not be shut down)") {
    given Sge    = makeContext()
    val resolver = StubResolver()

    // Manager A: async-load an asset, finish it, then CLOSE it.
    val phasesA  = CopyOnWriteArrayList[String]()
    val managerA = AssetManager(resolver, defaultLoaders = false)
    managerA.setLoader(classOf[AsyncAsset], new RecordingAsyncLoader(resolver, phasesA))
    managerA.load("a-first.async", classOf[AsyncAsset])
    managerA.finishLoading()
    assert(managerA.isLoaded("a-first.async"), "manager A must load its asset before close()")
    managerA.close() // <-- on current code this shuts down the SHARED executor

    // Manager B: built from the SAME Sge, must still be able to async-load.
    val phasesB  = CopyOnWriteArrayList[String]()
    val managerB = AssetManager(resolver, defaultLoaders = false)
    managerB.setLoader(classOf[AsyncAsset], new RecordingAsyncLoader(resolver, phasesB))
    managerB.load("b-second.async", classOf[AsyncAsset])

    // Driving the async load must NOT raise RejectedExecutionException / SgeError.
    // (finishLoading internally drains update(); loadAsync is submitted to the
    // executor, which on current code is already terminated.)
    managerB.finishLoading()

    assert(managerB.isLoaded("b-second.async"), "manager B's async asset must load after manager A was closed")
    val asset = managerB.apply[AsyncAsset]("b-second.async", classOf[AsyncAsset])
    assertEquals(asset.name, "b-second.async")
    assertEquals(asset.loadAsyncTag, "async(b-second.async)", "loadAsync must have run on a live executor for manager B")
    // Prove loadAsync genuinely ran for B (not skipped/short-circuited).
    assertEquals(
      phasesB.asScala.count(_ == "loadAsync:b-second.async"),
      1,
      "manager B's loadAsync must run exactly once on a live executor"
    )

    managerB.close()
  }

  // ─── 1. load() queues; asset not available until updated ─────────────

  test("ISS561: async load queues but asset is NOT finished/available until update() drives it") {
    given Sge    = makeContext()
    val resolver = StubResolver()
    val phases   = CopyOnWriteArrayList[String]()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[AsyncAsset], new RecordingAsyncLoader(resolver, phases))

    manager.load("a.async", classOf[AsyncAsset])
    // queued, but not driven: must not be finished, not loaded, get is empty
    assert(!manager.isFinished, "queued async asset must leave manager not finished")
    assertEquals(manager.isLoaded("a.async"), false)
    assert(manager.get[AsyncAsset]("a.async", classOf[AsyncAsset]).isEmpty, "get must be empty before update")
    assert(manager.contains("a.async"), "contains must be true for a queued asset")
    // and no loader phase has run yet (load() only queues)
    assertEquals(phases.size, 0, "no loader phase may run from load() alone")

    manager.finishLoading()
    manager.close()
  }

  // ─── 2. update() drives loadAsync THEN loadSync, producing the asset ──

  test("ISS561: update() runs loadAsync BEFORE loadSync and get() returns the exact produced asset") {
    given Sge    = makeContext()
    val resolver = StubResolver()
    val phases   = CopyOnWriteArrayList[String]()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[AsyncAsset], new RecordingAsyncLoader(resolver, phases))

    manager.load("hero.async", classOf[AsyncAsset])

    // Drive update() until finished. update() must NOT load everything at once:
    // for an async loader it takes multiple update() calls (submit future,
    // poll future, run loadSync) — at least 2.
    var iterations = 0
    var done       = false
    while (!done && iterations < 1000) {
      done = manager.update()
      iterations += 1
    }
    assert(done, s"async load did not finish in $iterations iterations")
    assert(
      iterations >= 2,
      s"async load must take >= 2 update() calls (was $iterations); one-shot load means loadAsync was skipped or run on the calling thread"
    )

    assert(manager.isFinished, "manager must be finished after draining the queue")
    assert(manager.isLoaded("hero.async"))

    val recorded = phases.asScala.toList
    // EXACT phase order: getDependencies, then loadAsync, then loadSync.
    assertEquals(
      recorded,
      List("getDependencies:hero.async", "loadAsync:hero.async", "loadSync:hero.async"),
      s"phase order wrong: $recorded"
    )
    // loadAsync strictly before loadSync
    assert(
      recorded.indexOf("loadAsync:hero.async") < recorded.indexOf("loadSync:hero.async"),
      "loadAsync must run strictly before loadSync"
    )

    // get() returns the EXACT asset loadSync produced, carrying loadAsync's output.
    val asset = manager.apply[AsyncAsset]("hero.async", classOf[AsyncAsset])
    assertEquals(asset.name, "hero.async")
    assertEquals(asset.loadAsyncTag, "async(hero.async)", "loadSync must consume the value loadAsync produced")

    manager.close()
  }

  // ─── 3. progress goes 0 -> 0.5 -> 1.0 across two async assets ────────

  test("ISS561: getProgress advances 0 -> 0.5 -> 1.0 across two async assets") {
    given Sge    = makeContext()
    val resolver = StubResolver()
    val phases   = CopyOnWriteArrayList[String]()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[AsyncAsset], new RecordingAsyncLoader(resolver, phases))

    manager.load("first.async", classOf[AsyncAsset])
    manager.load("second.async", classOf[AsyncAsset])

    // Before any update: toLoad=2, loaded=0, no peak fraction -> EXACTLY 0.
    assertEqualsFloat(manager.progress, 0.0f, 0.0f)

    // Drive ONLY the first asset to completion (queue is FIFO: first.async loads first).
    manager.finishLoadingAsset[AsyncAsset]("first.async")
    assert(manager.isLoaded("first.async"))
    assertEquals(manager.isLoaded("second.async"), false, "second asset must still be pending")
    // loaded=1, toLoad=2, no in-flight task fraction -> EXACTLY 0.5.
    assertEqualsFloat(manager.progress, 0.5f, 0.0f)

    // Finish the rest.
    manager.finishLoading()
    assert(manager.isLoaded("second.async"))
    // loaded=2, toLoad=2 -> EXACTLY 1.0.
    assertEqualsFloat(manager.progress, 1.0f, 0.0f)
    assertEquals(manager.loadedAssets, 2)

    manager.close()
  }

  // ─── 4. ref-counting + dispose with the async path ───────────────────

  test("ISS561: loading the same async asset twice loads it ONCE; dispose only at ref-count 0") {
    given Sge    = makeContext()
    val resolver = StubResolver()
    val phases   = CopyOnWriteArrayList[String]()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[AsyncAsset], new RecordingAsyncLoader(resolver, phases))

    manager.load("shared.async", classOf[AsyncAsset])
    manager.load("shared.async", classOf[AsyncAsset])
    manager.finishLoading()

    // loadAsync ran exactly once despite two load() calls (second bumps ref count).
    val loadAsyncCount = phases.asScala.count(_ == "loadAsync:shared.async")
    assertEquals(loadAsyncCount, 1, "the actual async load must run exactly once for a shared asset")
    assertEquals(manager.referenceCount("shared.async"), 2)

    val asset = manager.apply[AsyncAsset]("shared.async", classOf[AsyncAsset])
    assertEquals(asset.closed, false)

    // First unload decrements to 1: still loaded, NOT disposed.
    manager.unload("shared.async")
    assertEquals(manager.referenceCount("shared.async"), 1)
    assert(manager.isLoaded("shared.async"), "asset must survive first unload")
    assertEquals(asset.closed, false, "dispose must NOT run while ref count > 0")

    // Second unload drops to 0: removed AND disposed.
    manager.unload("shared.async")
    assertEquals(manager.loadedAssets, 0)
    assertEquals(asset.closed, true, "dispose must run when ref count reaches 0")

    manager.close()
  }

  // ─── 5. dependency declared via getDependencies loads first (async) ──

  test("ISS561: an async asset with a dependency loads the dependency FIRST") {
    given Sge    = makeContext()
    val resolver = StubResolver()
    val phases   = CopyOnWriteArrayList[String]()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    // STRICTLY ACYCLIC dependency graph: the PARENT "parent.async" declares a
    // single DISTINCT CHILD "child.async". The child (and any other asset)
    // declares NO further dependencies. No asset depends on itself, so a
    // faithful AssetManager (LibGDX has no in-flight-task cycle guard,
    // AssetManager.java:509) cannot loop.
    manager.setLoader(
      classOf[AsyncAsset],
      new RecordingAsyncLoader(resolver, phases, dependencyOf = Map("parent.async" -> "child.async"))
    )

    manager.load("parent.async", classOf[AsyncAsset])
    manager.finishLoading()

    assert(manager.isLoaded("parent.async"))
    assert(manager.isLoaded("child.async"), "dependency must be loaded")

    val recorded = phases.asScala.toList
    // The child (dependency) must declare NO dependency of its own: exactly one
    // getDependencies call for the child, and it never pulls anything further.
    assertEquals(
      recorded.count(_ == "getDependencies:child.async"),
      1,
      s"child must resolve dependencies exactly once and declare none: $recorded"
    )
    // No asset other than the parent may declare a dependency: the only
    // child-bearing getDependencies is the parent's. (Guards the fixture from
    // re-introducing a cycle.)
    assert(
      !recorded.exists(p => p.startsWith("getDependencies:") && p != "getDependencies:parent.async" && p != "getDependencies:child.async"),
      s"only parent.async and child.async may resolve dependencies: $recorded"
    )

    // The child's loadSync must complete BEFORE the parent's loadSync:
    // the parent cannot finish until its dependency has loaded.
    val childDone  = recorded.indexOf("loadSync:child.async")
    val parentDone = recorded.indexOf("loadSync:parent.async")
    assert(childDone >= 0, s"dependency was never loaded via loadSync: $recorded")
    assert(parentDone >= 0, s"parent was never loaded via loadSync: $recorded")
    assert(childDone < parentDone, s"dependency must finish loading before the parent: $recorded")

    // The parent's loadAsync/loadSync ran exactly once, carrying loadAsync's output.
    val parentAsset = manager.apply[AsyncAsset]("parent.async", classOf[AsyncAsset])
    assertEquals(parentAsset.name, "parent.async")
    assertEquals(parentAsset.loadAsyncTag, "async(parent.async)", "parent loadSync must consume parent loadAsync's value")

    // Parent declares child as a dependency.
    val deps = manager.dependencies("parent.async")
    assert(deps.isDefined, "parent must record its dependency")

    manager.close()
  }

  // ─── 6. unload while queued (async) cancels it cleanly ───────────────

  test("ISS561: unloading a queued async asset removes it without loading") {
    given Sge    = makeContext()
    val resolver = StubResolver()
    val phases   = CopyOnWriteArrayList[String]()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[AsyncAsset], new RecordingAsyncLoader(resolver, phases))

    manager.load("doomed.async", classOf[AsyncAsset])
    manager.unload("doomed.async") // still in queue, never driven
    manager.finishLoading()

    assertEquals(manager.loadedAssets, 0)
    assertEquals(manager.isLoaded("doomed.async"), false)
    // It was removed from the queue before any task ran: no loadSync produced it.
    assertEquals(phases.asScala.count(_.startsWith("loadSync")), 0, "a queued-then-unloaded asset must never run loadSync")

    manager.close()
  }
}
