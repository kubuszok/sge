/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/MusicLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.audio.Music
import sge.utils.{ DynamicArray, Nullable, SgeError }

/** {@link AssetLoader} for {@link Music} instances. The Music instance is loaded synchronously.
  * @author
  *   mzechner (original implementation)
  */
class MusicLoader(resolver: FileHandleResolver)(using sge: Sge) extends AsynchronousAssetLoader[Music, MusicLoader.MusicParameter](resolver) {

  private var music: Nullable[Music] = Nullable.empty

  /** Returns the {@link Music} instance currently loaded by this {@link MusicLoader} .
    *
    * @return
    *   the currently loaded {@link Music} , otherwise {@code null} if no {@link Music} has been loaded yet.
    */
  protected def getLoadedMusic: Nullable[Music] = music

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: MusicLoader.MusicParameter): Unit =
    music = Nullable(sge.audio.newMusic(file))

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: MusicLoader.MusicParameter): Music = {
    val result = music
    music = Nullable.empty
    result.getOrElse(throw SgeError.SerializationError("Music not loaded"))
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: MusicLoader.MusicParameter): DynamicArray[AssetDescriptor[?]] =
    DynamicArray[AssetDescriptor[?]]()
}

object MusicLoader {
  class MusicParameter extends AssetLoaderParameters[Music]
}
