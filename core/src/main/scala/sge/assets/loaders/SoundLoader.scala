package sge
package assets
package loaders

import sge.files.FileHandle
import sge.audio.Sound
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer

/** {@link AssetLoader} for {@link Sound} instances. The Sound instance is loaded synchronously.
  * @author
  *   mzechner (original implementation)
  */
class SoundLoader(resolver: FileHandleResolver)(using sge: Sge) extends AsynchronousAssetLoader[Sound, SoundLoader.SoundParameter](resolver) {

  private var sound: Nullable[Sound] = Nullable.empty

  /** Returns the {@link Sound} instance currently loaded by this {@link SoundLoader} .
    *
    * @return
    *   the currently loaded {@link Sound} , otherwise {@code null} if no {@link Sound} has been loaded yet.
    */
  protected def getLoadedSound: Nullable[Sound] = sound

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: SoundLoader.SoundParameter): Unit =
    sound = Nullable(sge.audio.newSound(file))

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: SoundLoader.SoundParameter): Sound = {
    val result = sound
    sound = Nullable.empty
    result.orNull
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: SoundLoader.SoundParameter): ArrayBuffer[AssetDescriptor[?]] =
    ArrayBuffer.empty
}

object SoundLoader {
  class SoundParameter extends AssetLoaderParameters[Sound]
}
