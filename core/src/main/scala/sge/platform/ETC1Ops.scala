// SGE Native Ops — ETC1 texture compression/decompression API
//
// Platform implementations:
//   JVM:    ETC1OpsJvm    (delegates to Rust via JNI)
//   JS:     ETC1OpsJs     (pure Scala fallback)
//   Native: ETC1OpsNative (delegates to Rust via C ABI)

/*
 * Migration notes:
 *   SGE-original platform abstraction trait, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package platform

/** ETC1 texture compression codec.
  *
  * Supports encoding/decoding ETC1 compressed textures and reading/writing PKM headers. Pixel data uses RGB888 (3 bytes per pixel) or RGB565 (2 bytes per pixel) formats.
  */
private[sge] trait ETC1Ops {

  /** ETC1 PKM header size in bytes. */
  val PKM_HEADER_SIZE: Int = 16

  /** Size of one ETC1 encoded block in bytes (encodes a 4x4 pixel block). */
  val ENCODED_BLOCK_SIZE: Int = 8

  /** Size of one decoded block in bytes (4x4 pixels × 3 bytes RGB). */
  val DECODED_BLOCK_SIZE: Int = 48

  /** Returns the number of bytes needed to store the compressed data for the given dimensions.
    *
    * @param width
    *   image width in pixels
    * @param height
    *   image height in pixels
    * @return
    *   compressed data size in bytes (not including PKM header)
    */
  def getCompressedDataSize(width: Int, height: Int): Int

  /** Writes a PKM header into the given byte array.
    *
    * @param header
    *   byte array to write the header into (must have at least offset + 16 bytes)
    * @param offset
    *   byte offset into the header array
    * @param width
    *   image width in pixels
    * @param height
    *   image height in pixels
    */
  def formatHeader(header: Array[Byte], offset: Int, width: Int, height: Int): Unit

  /** Reads the image width from a PKM header.
    *
    * @param header
    *   byte array containing the PKM header
    * @param offset
    *   byte offset to the PKM header
    * @return
    *   width in pixels
    */
  def getWidthPKM(header: Array[Byte], offset: Int): Int

  /** Reads the image height from a PKM header.
    *
    * @param header
    *   byte array containing the PKM header
    * @param offset
    *   byte offset to the PKM header
    * @return
    *   height in pixels
    */
  def getHeightPKM(header: Array[Byte], offset: Int): Int

  /** Checks if a PKM header is valid.
    *
    * @param header
    *   byte array containing the PKM header
    * @param offset
    *   byte offset to the PKM header
    * @return
    *   true if the PKM header is valid
    */
  def isValidPKM(header: Array[Byte], offset: Int): Boolean

  /** Decodes ETC1 compressed data to pixel data.
    *
    * @param compressedData
    *   the compressed ETC1 data
    * @param compressedOffset
    *   byte offset into the compressed data
    * @param decodedData
    *   output buffer for decoded pixel data (must be large enough: width × height × pixelSize)
    * @param decodedOffset
    *   byte offset into the decoded data buffer
    * @param width
    *   image width in pixels
    * @param height
    *   image height in pixels
    * @param pixelSize
    *   bytes per pixel: 2 (RGB565) or 3 (RGB888)
    */
  def decodeImage(
    compressedData:   Array[Byte],
    compressedOffset: Int,
    decodedData:      Array[Byte],
    decodedOffset:    Int,
    width:            Int,
    height:           Int,
    pixelSize:        Int
  ): Unit

  /** Encodes pixel data to ETC1 compressed format.
    *
    * @param imageData
    *   the source pixel data
    * @param offset
    *   byte offset into the image data
    * @param width
    *   image width in pixels
    * @param height
    *   image height in pixels
    * @param pixelSize
    *   bytes per pixel: 2 (RGB565) or 3 (RGB888)
    * @return
    *   new byte array containing the compressed data
    */
  def encodeImage(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte]

  /** Encodes pixel data to ETC1 compressed format with a PKM header prepended.
    *
    * @param imageData
    *   the source pixel data
    * @param offset
    *   byte offset into the image data
    * @param width
    *   image width in pixels
    * @param height
    *   image height in pixels
    * @param pixelSize
    *   bytes per pixel: 2 (RGB565) or 3 (RGB888)
    * @return
    *   new byte array containing the PKM header followed by compressed data
    */
  def encodeImagePKM(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte]
}
