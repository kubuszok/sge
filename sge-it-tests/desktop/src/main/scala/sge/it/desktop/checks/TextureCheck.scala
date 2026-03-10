// SGE — Desktop integration test: Texture subsystem check

package sge.it.desktop.checks

import sge.Sge
import sge.graphics.{ Pixmap, Texture, TextureHandle }
import sge.it.desktop.CheckResult

/** Uploads a Pixmap to a Texture and verifies the GL handle is valid. */
object TextureCheck {

  def run()(using Sge): CheckResult =
    try {
      val pixmap = Pixmap(4, 4, Pixmap.Format.RGBA8888)
      try {
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1f)
        pixmap.fill()

        val texture = Texture(pixmap)
        try {
          val handle = texture.getTextureObjectHandle()
          if (handle != TextureHandle.none) {
            CheckResult("texture", passed = true, s"Texture created from Pixmap, GL handle=${handle.toInt}")
          } else {
            CheckResult("texture", passed = false, s"Invalid texture handle: ${handle.toInt}")
          }
        } finally
          texture.close()
      } finally
        pixmap.close()
    } catch {
      case e: Exception =>
        CheckResult("texture", passed = false, s"Exception: ${e.getMessage}")
    }
}
