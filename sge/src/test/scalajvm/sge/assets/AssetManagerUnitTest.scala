/*
 * SGE — AssetManager unit tests
 *
 * Tests loading lifecycle, loader registration, reference counting,
 * and error handling with mock loaders and resolver.
 */
package sge
package assets

import munit.FunSuite
import sge.utils.GdxRuntimeException
import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
import sge.files.{ FileHandle, FileType }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

class AssetManagerUnitTest extends FunSuite {

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── Test infrastructure ─────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  /** A trivial "asset" type for testing. */
  final case class TestAsset(name: String) extends AutoCloseable {
    var closed:           Boolean = false
    override def close(): Unit    = closed = true
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  /** Resolver that returns a FileHandle wrapping the filename (no real I/O). */
  private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  /** Synchronous loader for TestAsset. */
  private class TestAssetLoader(resolver: FileHandleResolver) extends SynchronousAssetLoader[TestAsset, AssetLoaderParameters[TestAsset]](resolver) {

    import munit.FunSuite
    import sge.utils.GdxRuntimeException
    import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
    import sge.files.{ FileHandle, FileType }
    import lowlevel.Nullable
    import lowlevel.util.DynamicArray
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException

    override def load(
      assetManager: AssetManager,
      fileName:     String,
      file:         FileHandle,
      parameter:    AssetLoaderParameters[TestAsset]
    ): TestAsset = TestAsset(fileName)

    import munit.FunSuite
    import sge.utils.GdxRuntimeException
    import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
    import sge.files.{ FileHandle, FileType }
    import lowlevel.Nullable
    import lowlevel.util.DynamicArray
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException
    import sge.utils.GdxRuntimeException

    override def getDependencies(
      fileName:  String,
      file:      FileHandle,
      parameter: AssetLoaderParameters[TestAsset]
    ): DynamicArray[AssetDescriptor[?]] = null.asInstanceOf[DynamicArray[AssetDescriptor[?]]] // null = no dependencies
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  private def makeContext(): Sge = SgeTestFixture.testSge()

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  private def makeManager()(using Sge): AssetManager = {
    val resolver = StubResolver()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[TestAsset], new TestAssetLoader(resolver))
    manager
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── Basic lifecycle ─────────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("newly created manager has no loaded assets") {
    given Sge   = makeContext()
    val manager = makeManager()
    assertEquals(manager.loadedAssets, 0)
    assert(manager.isFinished)
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("load and finishLoading makes asset available") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("test.asset", classOf[TestAsset])
    manager.finishLoading()
    assert(manager.isLoaded("test.asset"))
    assertEquals(manager.loadedAssets, 1)
    val asset = manager[TestAsset]("test.asset", classOf[TestAsset])
    assertEquals(asset.name, "test.asset")
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("update returns true when all assets loaded") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("a.asset", classOf[TestAsset])
    // Keep updating until done
    var done       = false
    var iterations = 0
    while (!done && iterations < 100) {
      done = manager.update()
      iterations += 1
    }
    assert(done, s"Expected loading to finish, took $iterations iterations")
    assert(manager.isLoaded("a.asset"))
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("update with no queued assets returns true immediately") {
    given Sge   = makeContext()
    val manager = makeManager()
    assert(manager.update())
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── contains / isLoaded ─────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("contains returns true for queued asset") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("queued.asset", classOf[TestAsset])
    assert(manager.contains("queued.asset"))
    manager.finishLoading()
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("isLoaded returns false for queued but not yet loaded") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("pending.asset", classOf[TestAsset])
    // Don't call update yet
    assertEquals(manager.isLoaded("pending.asset"), false)
    manager.finishLoading()
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── Unload ──────────────────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("unload removes asset and calls close") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("disposable.asset", classOf[TestAsset])
    manager.finishLoading()
    val asset = manager[TestAsset]("disposable.asset", classOf[TestAsset])
    manager.unload("disposable.asset")
    assert(asset.closed, "Expected asset.close() to be called")
    assertEquals(manager.loadedAssets, 0)
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("unload queued asset removes it from queue") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("will-remove.asset", classOf[TestAsset])
    manager.unload("will-remove.asset")
    manager.finishLoading()
    assertEquals(manager.loadedAssets, 0)
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── Reference counting ──────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("loading same asset twice increments ref count") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("shared.asset", classOf[TestAsset])
    manager.load("shared.asset", classOf[TestAsset])
    manager.finishLoading()
    assertEquals(manager.referenceCount("shared.asset"), 2)
    // First unload decrements
    manager.unload("shared.asset")
    assertEquals(manager.referenceCount("shared.asset"), 1)
    assert(manager.isLoaded("shared.asset"), "Asset should still be loaded after first unload")
    // Second unload removes
    manager.unload("shared.asset")
    assertEquals(manager.loadedAssets, 0)
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── Multiple assets ─────────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("load multiple assets") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("alpha.asset", classOf[TestAsset])
    manager.load("beta.asset", classOf[TestAsset])
    manager.load("gamma.asset", classOf[TestAsset])
    manager.finishLoading()
    assertEquals(manager.loadedAssets, 3)
    assert(manager.isLoaded("alpha.asset"))
    assert(manager.isLoaded("beta.asset"))
    assert(manager.isLoaded("gamma.asset"))
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── Loader registration ─────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("loading unknown type throws") {
    given Sge   = makeContext()
    val manager = makeManager()
    interceptMessage[GdxRuntimeException]("No loader for type: String") {
      manager.load("nope.txt", classOf[String])
    }
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── clear / close ───────────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("clear unloads all assets") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("a.asset", classOf[TestAsset])
    manager.load("b.asset", classOf[TestAsset])
    manager.finishLoading()
    assertEquals(manager.loadedAssets, 2)
    manager.clear()
    assertEquals(manager.loadedAssets, 0)
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── progress ────────────────────────────────────────────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("progress reaches 1.0 when done") {
    given Sge   = makeContext()
    val manager = makeManager()
    manager.load("prog.asset", classOf[TestAsset])
    manager.finishLoading()
    assertEqualsFloat(manager.progress, 1.0f, 0.01f)
    manager.close()
  }

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  // ─── get returns Nullable.empty for missing assets ───────────────────

  import munit.FunSuite
  import sge.utils.GdxRuntimeException
  import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
  import sge.files.{ FileHandle, FileType }
  import lowlevel.Nullable
  import lowlevel.util.DynamicArray
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException
  import sge.utils.GdxRuntimeException

  test("get throws for unloaded asset") {
    given Sge   = makeContext()
    val manager = makeManager()
    intercept[GdxRuntimeException] {
      manager.get[TestAsset]("missing.asset", classOf[TestAsset])
    }
    manager.close()
  }
}
