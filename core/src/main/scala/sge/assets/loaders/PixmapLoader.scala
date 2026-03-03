/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/PixmapLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: `getDependencies` returns empty `DynamicArray` instead of Java `null`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.utils.{ DynamicArray, Nullable, SgeError }

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
    result.getOrElse(throw SgeError.SerializationError("Pixmap not loaded"))
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: PixmapLoader.PixmapParameter): DynamicArray[AssetDescriptor[?]] =
    DynamicArray[AssetDescriptor[?]]()
}

object PixmapLoader {
  class PixmapParameter extends AssetLoaderParameters[Pixmap]
}
