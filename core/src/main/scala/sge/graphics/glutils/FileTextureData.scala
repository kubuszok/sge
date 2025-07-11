package sge
package graphics
package glutils

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.graphics.Pixmap.Format
import sge.graphics.PixmapIO
import sge.graphics.TextureData
import sge.utils.SgeError
import sge.utils.Nullable
import scala.compiletime.uninitialized

import scala.language.implicitConversions

class FileTextureData(
  val file:            FileHandle,
  preloadedPixmap:     Nullable[Pixmap] = Nullable.empty,
  formatArg:           Nullable[Format] = Nullable.empty,
  val useMipMapsParam: Boolean = false
) extends TextureData {

  private var width:           Int              = 0
  private var height:          Int              = 0
  private var format:          Nullable[Format] = formatArg
  private var pixmap:          Nullable[Pixmap] = preloadedPixmap
  private var isPreparedState: Boolean          = false

  // Initialize from constructor parameters
  if (pixmap.isDefined) {
    width = pixmap.orNull.getWidth()
    height = pixmap.orNull.getHeight()
    if (format.isEmpty) this.format = pixmap.orNull.getFormat()
  }

  override def isPrepared: Boolean = isPreparedState

  override def prepare(): Unit = {
    if (isPreparedState) throw SgeError.InvalidInput("Already prepared")
    if (pixmap.isEmpty) {
      if (file.extension().equals("cim"))
        pixmap = PixmapIO.readCIM(file)
      else
        pixmap = new Pixmap(file)
      width = pixmap.orNull.getWidth()
      height = pixmap.orNull.getHeight()
      if (format.isEmpty) format = pixmap.orNull.getFormat()
    }
    isPreparedState = true
  }

  override def consumePixmap(): Pixmap = {
    if (!isPreparedState) throw SgeError.InvalidInput("Call prepare() before calling getPixmap()")
    isPreparedState = false
    val pixmapToReturn = this.pixmap.orNull
    this.pixmap = Nullable.empty
    pixmapToReturn
  }

  override def disposePixmap: Boolean = true

  override def getWidth: Int = width

  override def getHeight: Int = height

  override def getFormat: Format = format.orNull

  override def useMipMaps: Boolean = useMipMapsParam

  override def isManaged: Boolean = true

  def getFileHandle(): FileHandle = file

  override def getType(): TextureData.TextureDataType = TextureData.TextureDataType.Pixmap

  override def consumeCustomData(target: Int): Unit =
    throw SgeError.InvalidInput("This TextureData implementation does not upload data itself")

  override def toString(): String = file.toString()
}
