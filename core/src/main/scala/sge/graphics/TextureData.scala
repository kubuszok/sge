/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/TextureData.java
 * Original authors: mzechner, Vincent Bousquet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.utils.Nullable
import scala.language.implicitConversions

import sge.graphics.{ Pixmap, PixmapIO }
import sge.graphics.Pixmap.Format
import sge.graphics.g2d.Gdx2DPixmap
import sge.graphics.glutils.{ ETC1TextureData, FileTextureData, KTXTextureData }
import sge.files.FileHandle

/** Used by a {@link Texture} to load the pixel data. A TextureData can either return a {@link Pixmap} or upload the pixel data itself. It signals it's type via {@link #getType()} to the Texture
  * that's using it. The Texture will then either invoke {@link #consumePixmap()} or {@link #consumeCustomData(int)} . These are the first methods to be called by Texture. After that the Texture will
  * invoke the other methods to find out about the size of the image data, the format, whether mipmaps should be generated and whether the TextureData is able to manage the pixel data if the OpenGL ES
  * context is lost. </p>
  *
  * In case the TextureData implementation has the type {@link TextureDataType#Custom} , the implementation has to generate the mipmaps itself if necessary. See {@link MipMapGenerator} . </p>
  *
  * Before a call to either {@link #consumePixmap()} or {@link #consumeCustomData(int)} , Texture will bind the OpenGL ES texture. </p>
  *
  * Look at {@link FileTextureData} and {@link ETC1TextureData} for example implementations of this interface.
  * @author
  *   mzechner
  */
trait TextureData {
  import TextureData.TextureDataType

  /** @return the {@link TextureDataType} */
  def getType(): TextureDataType

  /** @return whether the TextureData is prepared or not. */
  def isPrepared: Boolean

  /** Prepares the TextureData for a call to {@link #consumePixmap()} or {@link #consumeCustomData(int)} . This method can be called from a non OpenGL thread and should thus not interact with OpenGL.
    */
  def prepare(): Unit

  /** Returns the {@link Pixmap} for upload by Texture. A call to {@link #prepare()} must precede a call to this method. Any internal data structures created in {@link #prepare()} should be disposed
    * of here.
    *
    * @return
    *   the pixmap.
    */
  def consumePixmap(): Pixmap

  /** @return whether the caller of {@link #consumePixmap()} should dispose the Pixmap returned by {@link #consumePixmap()} */
  def disposePixmap: Boolean

  /** Uploads the pixel data to the OpenGL ES texture. The caller must bind an OpenGL ES texture. A call to {@link #prepare()} must preceed a call to this method. Any internal data structures created
    * in {@link #prepare()} should be disposed of here.
    */
  def consumeCustomData(target: Int): Unit

  /** @return the width of the pixel data */
  def getWidth: Int

  /** @return the height of the pixel data */
  def getHeight: Int

  /** @return the {@link Format} of the pixel data */
  def getFormat: Format

  /** @return whether to generate mipmaps or not. */
  def useMipMaps: Boolean

  /** @return whether this implementation can cope with a EGL context loss. */
  def isManaged: Boolean
}

object TextureData {

  /** The type of this {@link TextureData} .
    * @author
    *   mzechner
    */
  enum TextureDataType {
    case Pixmap, Custom
  }

  /** Provides static method to instantiate the right implementation (Pixmap, ETC1, KTX).
    * @author
    *   Vincent Bousquet
    */
  object Factory {

    def loadFromFile(file: FileHandle, useMipMaps: Boolean)(using sge: Sge): TextureData =
      loadFromFile(file, Nullable.empty, useMipMaps)

    def loadFromFile(file: FileHandle, format: Nullable[Format], useMipMaps: Boolean)(using sge: Sge): TextureData =
      if (file.name().endsWith(".cim")) new FileTextureData(file, PixmapIO.readCIM(file), format, useMipMaps)
      else if (file.name().endsWith(".etc1")) new ETC1TextureData(file, useMipMaps)
      else if (file.name().endsWith(".ktx") || file.name().endsWith(".zktx")) new KTXTextureData(file, useMipMaps)
      else new FileTextureData(file, new Pixmap(100, 100, format.getOrElse(Format.RGBA8888)), format, useMipMaps)
  }
}
