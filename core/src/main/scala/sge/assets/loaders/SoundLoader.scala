/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/SoundLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODOs: test: SoundLoader loadAsync/loadSync (requires audio backend)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.audio.Sound
import sge.utils.{ DynamicArray, Nullable, SgeError }

/** {@link AssetLoader} for {@link Sound} instances. The Sound instance is loaded synchronously.
  * @author
  *   mzechner (original implementation)
  */
class SoundLoader(resolver: FileHandleResolver)(using Sge) extends AsynchronousAssetLoader[Sound, SoundLoader.SoundParameter](resolver) {

  private var sound: Nullable[Sound] = Nullable.empty

  /** Returns the {@link Sound} instance currently loaded by this {@link SoundLoader} .
    *
    * @return
    *   the currently loaded {@link Sound} , otherwise {@code null} if no {@link Sound} has been loaded yet.
    */
  protected def getLoadedSound: Nullable[Sound] = sound

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: SoundLoader.SoundParameter): Unit =
    sound = Nullable(Sge().audio.newSound(file))

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: SoundLoader.SoundParameter): Sound = {
    val result = sound
    sound = Nullable.empty
    result.getOrElse(throw SgeError.SerializationError("Sound not loaded"))
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: SoundLoader.SoundParameter): DynamicArray[AssetDescriptor[?]] =
    DynamicArray[AssetDescriptor[?]]()
}

object SoundLoader {
  class SoundParameter extends AssetLoaderParameters[Sound]
}
