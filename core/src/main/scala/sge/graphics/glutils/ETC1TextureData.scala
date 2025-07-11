package sge
package graphics
package glutils

import sge.files.FileHandle
import sge.graphics.GL20
import sge.graphics.Pixmap
import sge.graphics.Pixmap.Format
import sge.graphics.TextureData
import sge.graphics.glutils.ETC1.ETC1Data
import sge.utils.SgeError
import scala.compiletime.uninitialized

class ETC1TextureData(
  val file:            FileHandle,
  val useMipMapsValue: Boolean
) extends TextureData {

  private var data:         ETC1Data = uninitialized
  private var width:        Int      = 0
  private var height:       Int      = 0
  private var preparedFlag: Boolean  = false

  def this(file: FileHandle) = {
    this(file, false)
  }

  def this(encodedImage: ETC1Data, useMipMaps: Boolean) = {
    this(file = null, useMipMapsValue = useMipMaps)
    this.data = encodedImage
  }

  override def getType(): TextureData.TextureDataType =
    TextureData.TextureDataType.Custom

  override def isPrepared: Boolean =
    preparedFlag

  override def prepare(): Unit = {
    if (preparedFlag) throw SgeError.GraphicsError("Already prepared")
    if (file == null && data == null) throw SgeError.GraphicsError("Can only load once from ETC1Data")
    if (file != null) {
      data = new ETC1Data(file)
    }
    width = data.width
    height = data.height
    preparedFlag = true
  }

  override def consumeCustomData(target: Int): Unit = {
    if (!preparedFlag) throw SgeError.GraphicsError("Call prepare() before calling consumeCompressedData()")

    // TODO: Add graphics support check
    // if (!sge.graphics.supportsExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
    //   val pixmap = ETC1.decodeImage(data, Format.RGB565)
    //   sge.graphics.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
    //     pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
    //   if (useMipMapsValue) MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth(), pixmap.getHeight())
    //   pixmap.close()
    //   useMipMapsValue = false
    // } else {
    //   sge.graphics.gl.glCompressedTexImage2D(target, 0, ETC1.ETC1_RGB8_OES, width, height, 0,
    //     data.compressedData.capacity() - data.dataOffset, data.compressedData)
    //   if (useMipMapsValue) sge.graphics.gl20.glGenerateMipmap(GL20.GL_TEXTURE_2D)
    // }
    data.close()
    data = null
    preparedFlag = false
  }

  override def consumePixmap(): Pixmap =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def disposePixmap: Boolean =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def getWidth: Int =
    width

  override def getHeight: Int =
    height

  override def getFormat: Format =
    Format.RGB565

  override def useMipMaps: Boolean =
    useMipMapsValue

  override def isManaged: Boolean =
    true
}
