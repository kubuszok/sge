// SGE Native Ops — JVM image decoder
//
// Decodes PNG, JPEG, BMP, GIF image formats into raw RGBA8888 pixel data.
// Uses javax.imageio.ImageIO on desktop JVM and android.graphics.BitmapFactory
// on Android (detected at runtime via reflection).
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: JDK ImageIO for desktop, BitmapFactory for Android
//   Idiom: split packages
//   Audited: 2026-03-10

package sge
package platform

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

private[platform] object Gdx2dOpsJvm extends Gdx2dOps {

  @volatile private var _failureReason: String = "No error"

  // Runtime detection: javax.imageio.ImageIO is not available on Android.
  // On Android we use android.graphics.BitmapFactory via reflection.
  // See AndroidRuntime for why Class.forName is not sufficient.
  private val isAndroid: Boolean = AndroidRuntime.isAndroid

  override def decodeImage(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] =
    try
      if (isAndroid) decodeAndroid(data, offset, len)
      else decodeDesktop(data, offset, len)
    catch {
      case e: Exception =>
        _failureReason = e.getMessage
        None
    }

  override def failureReason: String = _failureReason

  // --- Desktop JVM (javax.imageio) ---

  private def decodeDesktop(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    val bis = new ByteArrayInputStream(data, offset, len)
    val img = javax.imageio.ImageIO.read(bis)
    if (img == null) {
      _failureReason = "ImageIO.read returned null — unsupported or corrupt image format"
      return None
    }

    val w = img.getWidth
    val h = img.getHeight

    // Detect format from image type
    val hasAlpha = img.getColorModel().hasAlpha
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
  }

  // --- Android (android.graphics.BitmapFactory via reflection) ---
  // Reflection avoids compile-time dependency on android.jar.

  private def decodeAndroid(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    // BitmapFactory.decodeByteArray(data, offset, len) -> Bitmap
    val bitmapFactoryClass = Class.forName("android.graphics.BitmapFactory")
    val decodeByteArray    = bitmapFactoryClass.getMethod(
      "decodeByteArray",
      classOf[Array[Byte]],
      classOf[Int],
      classOf[Int]
    )
    val bitmap = decodeByteArray.invoke(null, data, Int.box(offset), Int.box(len))
    if (bitmap == null) {
      _failureReason = "BitmapFactory.decodeByteArray returned null — unsupported or corrupt image format"
      return None
    }

    val bitmapClass = bitmap.getClass
    val getWidth    = bitmapClass.getMethod("getWidth")
    val getHeight   = bitmapClass.getMethod("getHeight")
    val getPixels   = bitmapClass.getMethod("getPixels", classOf[Array[Int]], classOf[Int], classOf[Int], classOf[Int], classOf[Int], classOf[Int], classOf[Int])
    val hasAlphaM   = bitmapClass.getMethod("hasAlpha")
    val recycle     = bitmapClass.getMethod("recycle")

    val w        = getWidth.invoke(bitmap).asInstanceOf[Int]
    val h        = getHeight.invoke(bitmap).asInstanceOf[Int]
    val hasAlpha = hasAlphaM.invoke(bitmap).asInstanceOf[Boolean]

    // Get pixels as ARGB_8888 int array
    val pixels = new Array[Int](w * h)
    getPixels.invoke(bitmap, pixels, Int.box(0), Int.box(w), Int.box(0), Int.box(0), Int.box(w), Int.box(h))
    recycle.invoke(bitmap)

    // Convert ARGB → RGBA (or RGB if no alpha)
    val format = if (hasAlpha) 4 else 3 // RGBA8888 or RGB888
    val bpp    = if (hasAlpha) 4 else 3
    val buf    = ByteBuffer.allocateDirect(w * h * bpp)

    var i = 0
    while (i < pixels.length) {
      val argb = pixels(i)
      val pos  = i * bpp
      buf.put(pos, ((argb >>> 16) & 0xff).toByte) // R
      buf.put(pos + 1, ((argb >>> 8) & 0xff).toByte) // G
      buf.put(pos + 2, (argb & 0xff).toByte) // B
      if (hasAlpha) {
        buf.put(pos + 3, ((argb >>> 24) & 0xff).toByte) // A
      }
      i += 1
    }

    _failureReason = "No error"
    Some(Gdx2dOps.DecodeResult(w, h, format, buf))
  }
}
