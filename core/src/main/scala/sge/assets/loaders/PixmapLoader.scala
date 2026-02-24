/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/PixmapLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer

/** {@link AssetLoader} for {@link Pixmap} instances. The Pixmap is loaded asynchronously.
  * @author
  *   mzechner (original implementation)
  */
class PixmapLoader(resolver: FileHandleResolver) extends AsynchronousAssetLoader[Pixmap, PixmapLoader.PixmapParameter](resolver) {

  private var pixmap: Nullable[Pixmap] = Nullable.empty

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: PixmapLoader.PixmapParameter): Unit = {
    pixmap = Nullable.empty
    pixmap = Nullable(new Pixmap(file))
  }

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: PixmapLoader.PixmapParameter): Pixmap = {
    val result = pixmap
    pixmap = Nullable.empty
    result.orNull
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: PixmapLoader.PixmapParameter): ArrayBuffer[AssetDescriptor[?]] =
    ArrayBuffer.empty
}

object PixmapLoader {
  class PixmapParameter extends AssetLoaderParameters[Pixmap]
}
