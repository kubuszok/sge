// SGE Native Ops — ETC1 Scala Native bindings (delegates to Rust via C ABI)
//
// Binds to the sge_native_ops Rust library using @link/@extern annotations.
// Array data is passed to C functions via .at(offset) to obtain Ptr[Byte].

package sge
package platform

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@link("sge_native_ops")
@extern
private object ETC1C {
  def etc1_get_encoded_data_size(width: CUnsignedInt, height: CUnsignedInt):                CUnsignedInt = extern
  def etc1_pkm_format_header(header: Ptr[Byte], width: CUnsignedInt, height: CUnsignedInt): Unit         =
    extern
  def etc1_pkm_get_width(header:  Ptr[Byte]): CUnsignedInt = extern
  def etc1_pkm_get_height(header: Ptr[Byte]): CUnsignedInt = extern
  def etc1_pkm_is_valid(header:   Ptr[Byte]): CInt         = extern
  def etc1_decode_image(
    pIn:       Ptr[Byte],
    pOut:      Ptr[Byte],
    width:     CUnsignedInt,
    height:    CUnsignedInt,
    pixelSize: CUnsignedInt,
    stride:    CUnsignedInt
  ): CInt = extern
  def etc1_encode_image(
    pIn:       Ptr[Byte],
    width:     CUnsignedInt,
    height:    CUnsignedInt,
    pixelSize: CUnsignedInt,
    stride:    CUnsignedInt,
    pOut:      Ptr[Byte]
  ): CInt = extern
}

private[platform] object ETC1OpsNative extends ETC1Ops {

  override def getCompressedDataSize(width: Int, height: Int): Int =
    ETC1C.etc1_get_encoded_data_size(width.toUInt, height.toUInt).toInt

  override def formatHeader(header: Array[Byte], offset: Int, width: Int, height: Int): Unit =
    ETC1C.etc1_pkm_format_header(header.at(offset), width.toUInt, height.toUInt)

  override def getWidthPKM(header: Array[Byte], offset: Int): Int =
    ETC1C.etc1_pkm_get_width(header.at(offset)).toInt

  override def getHeightPKM(header: Array[Byte], offset: Int): Int =
    ETC1C.etc1_pkm_get_height(header.at(offset)).toInt

  override def isValidPKM(header: Array[Byte], offset: Int): Boolean =
    ETC1C.etc1_pkm_is_valid(header.at(offset)) != 0

  override def decodeImage(
    compressedData:   Array[Byte],
    compressedOffset: Int,
    decodedData:      Array[Byte],
    decodedOffset:    Int,
    width:            Int,
    height:           Int,
    pixelSize:        Int
  ): Unit = {
    val stride = width * pixelSize
    ETC1C.etc1_decode_image(
      compressedData.at(compressedOffset),
      decodedData.at(decodedOffset),
      width.toUInt,
      height.toUInt,
      pixelSize.toUInt,
      stride.toUInt
    )
  }

  override def encodeImage(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] = {
    val compressedSize = getCompressedDataSize(width, height)
    val out            = new Array[Byte](compressedSize)
    val stride         = width * pixelSize
    ETC1C.etc1_encode_image(
      imageData.at(offset),
      width.toUInt,
      height.toUInt,
      pixelSize.toUInt,
      stride.toUInt,
      out.at(0)
    )
    out
  }

  override def encodeImagePKM(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] = {
    val compressedSize = getCompressedDataSize(width, height)
    val result         = new Array[Byte](PKM_HEADER_SIZE + compressedSize)
    // Write PKM header into the first 16 bytes
    ETC1C.etc1_pkm_format_header(result.at(0), width.toUInt, height.toUInt)
    // Encode image data into the remaining bytes after the header
    val stride = width * pixelSize
    ETC1C.etc1_encode_image(
      imageData.at(offset),
      width.toUInt,
      height.toUInt,
      pixelSize.toUInt,
      stride.toUInt,
      result.at(PKM_HEADER_SIZE)
    )
    result
  }
}
