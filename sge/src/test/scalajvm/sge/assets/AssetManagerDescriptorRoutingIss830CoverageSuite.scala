/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * COVERAGE test (GREEN) — ISS-830 (test-coverage), wave 2026-07-17-F,
 * territory X. This pins the descriptor-overload type-routing of
 * AssetManager.get, which previously had NO test.
 *
 * VERDICT: current behavior is CORRECT — this is a plain coverage test, not a
 * red. The original com/badlogic/gdx/assets/AssetManager.java lines 180-181:
 *
 *   public synchronized <T> T get (AssetDescriptor<T> assetDescriptor) {
 *     return get(assetDescriptor.fileName, assetDescriptor.type, true);
 *   }
 *
 * routes strictly by the DESCRIPTOR'S declared type (`assets.get(type)`). An
 * asset loaded under type A is therefore NOT returned by a descriptor that
 * declares a different type B — B has no entry for that filename.
 *
 * The port (AssetManager.scala lines 168-170) does the same via
 * `lookupByClass[T](fileName, assetDescriptor.type)` (AssetManager.scala:106-110,
 * `assets.get(tpe)`), returning Nullable.empty for a mismatched type. Same
 * routing; the port returns an empty Nullable where the original throws
 * (the port's get(...) is the Nullable-returning, non-throwing overload).
 */
package sge
package assets

import munit.FunSuite
import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
import sge.files.{ FileHandle, FileType }
import lowlevel.util.DynamicArray

class AssetManagerDescriptorRoutingIss830CoverageSuite extends FunSuite {

  final case class TestAsset(name: String)
  final case class OtherAsset(name: String)

  private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle = FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  private class TestAssetLoader(resolver: FileHandleResolver) extends SynchronousAssetLoader[TestAsset, AssetLoaderParameters[TestAsset]](resolver) {
    override def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: AssetLoaderParameters[TestAsset]): TestAsset                        = TestAsset(fileName)
    override def getDependencies(fileName: String, file: FileHandle, parameter: AssetLoaderParameters[TestAsset]):                  DynamicArray[AssetDescriptor[?]] =
      null.asInstanceOf[DynamicArray[AssetDescriptor[?]]]
  }

  test("ISS-830 (coverage): get(AssetDescriptor) routes by the descriptor's declared type (AssetManager.java:180-181)") {
    given Sge    = SgeTestFixture.testSge()
    val resolver = StubResolver()
    val manager  = AssetManager(resolver, defaultLoaders = false)
    manager.setLoader(classOf[TestAsset], new TestAssetLoader(resolver))

    manager.load("foo.asset", classOf[TestAsset])
    manager.finishLoading()
    assert(manager.isLoaded("foo.asset"), "precondition: foo.asset loaded under TestAsset")

    // Correct type: descriptor routes to assets.get(TestAsset) -> found.
    val right = manager.get(new AssetDescriptor[TestAsset]("foo.asset", classOf[TestAsset]))
    assert(right.isDefined, "get(descriptor) with the matching type must return the asset")
    assertEquals(right.map(_.name).getOrElse(""), "foo.asset")

    // Wrong type: same filename, different declared type. Routes to
    // assets.get(OtherAsset), which has no entry -> empty. This is the
    // route-by-descriptor-type contract (AssetManager.java:181), NOT a
    // filename-only lookup.
    val wrong = manager.get(new AssetDescriptor[OtherAsset]("foo.asset", classOf[OtherAsset]))
    assert(
      wrong.isEmpty,
      "get(descriptor) with a mismatched declared type must NOT return the asset stored under a different type " +
        "(route-by-descriptor-type, AssetManager.java:180-181)"
    )

    manager.close()
  }
}
