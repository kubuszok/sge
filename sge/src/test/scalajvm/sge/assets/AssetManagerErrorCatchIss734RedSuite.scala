/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test — ISS-734 umbrella clause c5 (core minors #2), wave 2026-07-17-F,
 * territory X. Reproducer-authored: MUST NOT be modified by the fixer; it
 * encodes the ORIGINAL LibGDX semantics
 * (com/badlogic/gdx/assets/AssetManager.java, original-src/libgdx), not the
 * port's.
 *
 * DEFECT. AssetManager.update() (AssetManager.scala lines 463-467) catches only
 * `NonFatal(t)`:
 *
 *   catch { case NonFatal(t) => handleTaskError(t); loadQueue.size == 0 }
 *
 * The original AssetManager.java lines 425-428 catches `Throwable`:
 *
 *   } catch (Throwable t) { handleTaskError(t); return loadQueue.size == 0; }
 *
 * scala.util.control.NonFatal deliberately does NOT match Error subtypes
 * (VirtualMachineError, LinkageError, ...). So when a loader throws an Error
 * subtype (e.g. a LinkageError from a bad native/class link), the port lets it
 * escape update() UNCAUGHT: handleTaskError is never called and the registered
 * AssetErrorListener never fires. The original routes every Throwable through
 * handleTaskError -> the listener.
 *
 * (Orchestrator pre-adjudicated fix: catch Throwable faithfully, but rethrow
 * scala.util.boundary.Break before the catch-all, since NonFatal also swallows
 * Break — a second latent defect. This test only pins the Error-escape half.)
 */
package sge
package assets

import munit.FunSuite
import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
import sge.files.{ FileHandle, FileType }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

class AssetManagerErrorCatchIss734RedSuite extends FunSuite {

  /** A trivial asset type. */
  final case class TestAsset(name: String)

  /** An Error subtype (NOT NonFatal — LinkageError is explicitly excluded by scala.util.control.NonFatal). */
  final private class LoaderBoomError extends LinkageError("boom-loader-error")

  private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle = FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  /** Synchronous loader whose load() throws an Error subtype. */
  private class BoomLoader(resolver: FileHandleResolver) extends SynchronousAssetLoader[TestAsset, AssetLoaderParameters[TestAsset]](resolver) {
    override def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: AssetLoaderParameters[TestAsset]): TestAsset =
      throw new LoaderBoomError
    override def getDependencies(fileName: String, file: FileHandle, parameter: AssetLoaderParameters[TestAsset]): DynamicArray[AssetDescriptor[?]] =
      null.asInstanceOf[DynamicArray[AssetDescriptor[?]]]
  }

  test(
    "ISS-734 c5: an Error thrown by a loader must reach handleTaskError / the AssetErrorListener (AssetManager.java:425 catches Throwable)"
  ) {
    given Sge    = SgeTestFixture.testSge()
    val resolver = StubResolver()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[TestAsset], new BoomLoader(resolver))

    var received: Nullable[Throwable] = Nullable.empty
    manager.errorListener = Nullable(new AssetErrorListener {
      def error(asset: AssetDescriptor[?], throwable: Throwable): Unit = received = Nullable(throwable)
    })

    manager.load("boom.asset", classOf[TestAsset])

    // Faithful (catch Throwable): update() catches the LinkageError, calls
    //   handleTaskError -> listener.error(desc, t). No throw escapes.
    // Port (catch NonFatal): the LinkageError is not NonFatal -> escapes update()
    //   uncaught; the listener never fires.
    val escaped: Nullable[Throwable] =
      try {
        val _ = manager.update()
        Nullable.empty
      } catch {
        case t: Throwable => Nullable(t)
      }

    assert(
      received.isDefined,
      s"a loader Error must be routed to the AssetErrorListener via handleTaskError (AssetManager.java:425 catches Throwable); " +
        s"the port's NonFatal catch (AssetManager.scala:464) lets it escape instead. escaped=${escaped}"
    )
    assert(
      received.exists(_.isInstanceOf[LoaderBoomError]),
      "the listener must receive the original loader Error (a LinkageError subtype)"
    )
    assert(
      escaped.isEmpty,
      s"the loader Error must not escape update() uncaught; it did: ${escaped}"
    )

    manager.close()
  }
}
