// SGE Native Ops — JVM image decoder using javax.imageio.ImageIO
//
// Decodes PNG, JPEG, BMP, GIF image formats into raw RGBA8888 pixel data.
// No native code required — uses the JDK's built-in image I/O facilities.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: JDK ImageIO for image decoding on JVM
//   Idiom: split packages
//   Audited: 2026-03-10

package sge
package platform

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

private[platform] object Gdx2dOpsJvm extends Gdx2dOps {

  @volatile private var _failureReason: String = "No error"

  override def decodeImage(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] =
    try {
      val bis = new ByteArrayInputStream(data, offset, len)
      val img = ImageIO.read(bis)
      if (img == null) {
        _failureReason = "ImageIO.read returned null — unsupported or corrupt image format"
        return None
      }

      val w = img.getWidth
      val h = img.getHeight

      // Detect format from image type
      val hasAlpha = img.getColorModel.hasAlpha
      val format   = if (hasAlpha) 4 else 3 // RGBA8888 or RGB888
      val bpp      = if (hasAlpha) 4 else 3
      val buf      = ByteBuffer.allocateDirect(w * h * bpp)

      var y = 0
      while (y < h) {
        var x = 0
        while (x < w) {
          val argb = img.getRGB(x, y)
          val pos  = (y * w + x) * bpp
          buf.put(pos, ((argb >>> 16) & 0xff).toByte) // R
          buf.put(pos + 1, ((argb >>> 8) & 0xff).toByte) // G
          buf.put(pos + 2, (argb & 0xff).toByte) // B
          if (hasAlpha) {
            buf.put(pos + 3, ((argb >>> 24) & 0xff).toByte) // A
          }
          x += 1
        }
        y += 1
      }

      _failureReason = "No error"
      Some(Gdx2dOps.DecodeResult(w, h, format, buf))
    } catch {
      case e: Exception =>
        _failureReason = e.getMessage
        None
    }

  override def failureReason: String = _failureReason
}
