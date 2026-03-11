/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package graphics

import sge.graphics.g2d.Gdx2DPixmap
import sge.graphics.g2d.Gdx2DPixmap.*

/** JVM-only Gdx2DPixmap tests that require javax.imageio / java.awt. */
class Gdx2DPixmapJvmTest extends munit.FunSuite {

  test("image decode on JVM loads PNG") {
    // Generate a valid 2x2 red PNG via ImageIO
    val img = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB)
    for {
      y <- 0 until 2
      x <- 0 until 2
    } img.setRGB(x, y, 0xffff0000) // opaque red
    val baos = new java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(img, "png", baos)
    val pngBytes = baos.toByteArray()

    val pm = new Gdx2DPixmap(pngBytes, 0, pngBytes.length, 0)
    assertEquals(pm.width, 2)
    assertEquals(pm.height, 2)
    // Should be RGB888 (format=3) since PNG has no alpha channel
    assertEquals(pm.format, GDX2D_FORMAT_RGB888)
    // Red pixel
    val pixel = pm.getPixel(0, 0)
    val r     = (pixel >>> 24) & 0xff
    val g     = (pixel >>> 16) & 0xff
    val b     = (pixel >>> 8) & 0xff
    assertEquals(r, 0xff)
    assertEquals(g, 0)
    assertEquals(b, 0)
    pm.close()
  }
}
