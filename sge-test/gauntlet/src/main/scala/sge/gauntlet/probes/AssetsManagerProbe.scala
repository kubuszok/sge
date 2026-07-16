/*
 * SGE Gauntlet — assets: AssetManager async load/get/unload cycle.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.assets.{ AssetDescriptor, AssetLoaderParameters, AssetManager }
import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
import sge.files.FileHandle
import lowlevel.Nullable
import lowlevel.util.DynamicArray

import scala.collection.mutable.ListBuffer

/** Drives the AssetManager machinery (queueing, async update loop, ref-counted unload) with a texture-less text fixture, so it runs headless. */
object AssetsManagerProbe extends FeatureProbe {

  /** Minimal text asset for the custom loader. */
  final case class TextFixture(content: String)

  private final class TextFixtureLoader(resolver: FileHandleResolver)
      extends SynchronousAssetLoader[TextFixture, AssetLoaderParameters[TextFixture]](resolver) {

    override def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: AssetLoaderParameters[TextFixture]): TextFixture =
      TextFixture(file.readString())

    override def getDependencies(fileName: String, file: FileHandle, parameter: AssetLoaderParameters[TextFixture]): DynamicArray[AssetDescriptor[?]] =
      DynamicArray[AssetDescriptor[?]]()
  }

  override def id: String = "assets/manager-async"

  override def area: String = "assets"

  override def requiresGpu: Boolean = false

  override def frames: Int = 60

  private val checks = ListBuffer.empty[Check]

  private var manager: Option[AssetManager] = None
  private var path                          = ""
  private var loadedAtFrame                 = -1

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    loadedAtFrame = -1
    given Sge = ctx.sgeCtx
    val fixture = ctx.tempDir.child("fixture.txt")
    fixture.writeString("gauntlet-fixture-content", false)
    path = fixture.path
    val resolver = new FileHandleResolver.Absolute()
    val m        = new AssetManager(resolver, true)
    m.setLoader[TextFixture](new TextFixtureLoader(resolver))
    m.load[TextFixture](path)
    manager = Some(m)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    manager.foreach { m =>
      // Drive the async loading machinery one update per frame, like a game would.
      if (loadedAtFrame < 0 && m.update()) {
        loadedAtFrame = frame
        ctx.log(s"asset loaded at frame $frame")
      }
    }

  override def verify(ctx: ProbeContext): List[Check] = {
    manager.foreach { m =>
      checks += Check.cond("update-completed", loadedAtFrame > 0, "manager.update() true within 60 frames", s"loadedAtFrame=$loadedAtFrame")
      checks += Check.eq("is-loaded", true, m.isLoaded(path))
      val content = m.get[TextFixture](path).fold("<missing>")(_.content)
      checks += Check.eq("content", "gauntlet-fixture-content", content)
      // second load of the same asset bumps the ref count; one unload keeps it loaded
      m.load[TextFixture](path)
      m.finishLoading()
      m.unload(path)
      checks += Check.eq("refcount-still-loaded", true, m.isLoaded(path))
      m.unload(path)
      checks += Check.eq("unloaded", false, m.isLoaded(path))
      m.close()
    }
    manager = None
    checks.toList
  }
}
