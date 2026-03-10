// SGE Native Ops — Gdx2D image decoding API
//
// Platform implementations:
//   JVM:    Gdx2dOpsJvm    (delegates to javax.imageio.ImageIO)
//   JS:     Gdx2dOpsJs     (stub — browser image decode is async)
//   Native: Gdx2dOpsNative (stub — needs stb_image via Rust FFI)

/*
 * Migration notes:
 *   SGE-original platform abstraction trait, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-10
 */
package sge
package platform

import java.nio.ByteBuffer

/** Image decoding for pixmap creation from encoded data (PNG, JPEG, BMP, etc).
  *
  * Drawing primitives are implemented in pure Scala in [[sge.graphics.g2d.Gdx2dDraw]] and don't require native code. Only image format decoding needs platform-specific implementations.
  */
private[sge] trait Gdx2dOps {

  /** Decodes encoded image data (PNG, JPEG, BMP, etc) into raw pixel data.
    *
    * @param data
    *   the encoded image bytes
    * @param offset
    *   byte offset into the data array
    * @param len
    *   number of bytes to read
    * @return
    *   decoded pixel data, or None if decoding failed
    */
  def decodeImage(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult]

  /** Returns a human-readable reason for the last decode failure. */
  def failureReason: String
}

private[sge] object Gdx2dOps {

  /** Result of decoding an image.
    *
    * @param width
    *   image width in pixels
    * @param height
    *   image height in pixels
    * @param format
    *   pixel format (GDX2D_FORMAT_* constant, 1-6)
    * @param pixels
    *   direct ByteBuffer containing the raw pixel data
    */
  final case class DecodeResult(width: Int, height: Int, format: Int, pixels: ByteBuffer)
}
