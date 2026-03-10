/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package graphics

import sge.graphics.g2d.Gdx2DPixmap
import sge.graphics.g2d.Gdx2DPixmap.*

class Gdx2DPixmapTest extends munit.FunSuite {

  test("newPixmap creates blank RGBA8888 buffer") {
    val pm = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGBA8888)
    assertEquals(pm.width, 4)
    assertEquals(pm.height, 4)
    assertEquals(pm.format, GDX2D_FORMAT_RGBA8888)
    // All pixels should be 0 initially
    assertEquals(pm.getPixel(0, 0), 0)
    assertEquals(pm.getPixel(3, 3), 0)
    pm.close()
  }

  test("newPixmap creates blank RGB888 buffer") {
    val pm = new Gdx2DPixmap(8, 8, GDX2D_FORMAT_RGB888)
    assertEquals(pm.width, 8)
    assertEquals(pm.height, 8)
    assertEquals(pm.format, GDX2D_FORMAT_RGB888)
    pm.close()
  }

  test("setPixel and getPixel roundtrip RGBA8888") {
    val pm = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    val red = 0xff0000ff // RGBA: red with full alpha
    pm.setPixel(1, 2, red)
    assertEquals(pm.getPixel(1, 2), red)
    // Adjacent pixel unchanged
    assertEquals(pm.getPixel(0, 0), 0)
    pm.close()
  }

  test("setPixel and getPixel roundtrip RGB888") {
    val pm = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGB888)
    pm.setBlend(GDX2D_BLEND_NONE)
    val green = 0x00ff00ff // RGBA: green with full alpha
    pm.setPixel(0, 0, green)
    // RGB888 stores no alpha, so getPixel returns with alpha=0xff
    assertEquals(pm.getPixel(0, 0), 0x00ff00ff)
    pm.close()
  }

  test("clear fills all pixels") {
    val pm    = new Gdx2DPixmap(3, 3, GDX2D_FORMAT_RGBA8888)
    val white = 0xffffffff.toInt
    pm.clear(white)
    assertEquals(pm.getPixel(0, 0), white)
    assertEquals(pm.getPixel(1, 1), white)
    assertEquals(pm.getPixel(2, 2), white)
    pm.close()
  }

  test("out of bounds getPixel returns 0") {
    val pm = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGBA8888)
    assertEquals(pm.getPixel(-1, 0), 0)
    assertEquals(pm.getPixel(0, -1), 0)
    assertEquals(pm.getPixel(4, 0), 0)
    assertEquals(pm.getPixel(0, 4), 0)
    pm.close()
  }

  test("out of bounds setPixel is silently ignored") {
    val pm = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    // Should not throw
    pm.setPixel(-1, 0, 0xffffffff.toInt)
    pm.setPixel(0, -1, 0xffffffff.toInt)
    pm.setPixel(4, 0, 0xffffffff.toInt)
    pm.setPixel(0, 4, 0xffffffff.toInt)
    // All pixels still zero
    assertEquals(pm.getPixel(0, 0), 0)
    pm.close()
  }

  test("drawLine horizontal") {
    val pm = new Gdx2DPixmap(6, 2, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    val red = 0xff0000ff
    pm.drawLine(1, 0, 4, 0, red)
    // Pixels 1-4 on row 0 should be red
    assertEquals(pm.getPixel(0, 0), 0)
    assertEquals(pm.getPixel(1, 0), red)
    assertEquals(pm.getPixel(2, 0), red)
    assertEquals(pm.getPixel(3, 0), red)
    assertEquals(pm.getPixel(4, 0), red)
    assertEquals(pm.getPixel(5, 0), 0)
    pm.close()
  }

  test("drawLine vertical") {
    val pm = new Gdx2DPixmap(2, 6, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    val blue = 0x0000ffff
    pm.drawLine(0, 1, 0, 4, blue)
    assertEquals(pm.getPixel(0, 0), 0)
    assertEquals(pm.getPixel(0, 1), blue)
    assertEquals(pm.getPixel(0, 4), blue)
    assertEquals(pm.getPixel(0, 5), 0)
    pm.close()
  }

  test("fillRect fills rectangular region") {
    val pm = new Gdx2DPixmap(8, 8, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    val green = 0x00ff00ff
    pm.fillRect(2, 2, 3, 3, green)
    // Inside
    assertEquals(pm.getPixel(2, 2), green)
    assertEquals(pm.getPixel(4, 4), green)
    assertEquals(pm.getPixel(3, 3), green)
    // Outside
    assertEquals(pm.getPixel(1, 1), 0)
    assertEquals(pm.getPixel(5, 5), 0)
    pm.close()
  }

  test("drawRect draws outline only") {
    val pm = new Gdx2DPixmap(8, 8, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    val white = 0xffffffff.toInt
    pm.drawRect(1, 1, 4, 4, white)
    // Corners should be filled
    assertEquals(pm.getPixel(1, 1), white)
    assertEquals(pm.getPixel(4, 1), white)
    assertEquals(pm.getPixel(1, 4), white)
    assertEquals(pm.getPixel(4, 4), white)
    // Interior should be empty
    assertEquals(pm.getPixel(2, 2), 0)
    assertEquals(pm.getPixel(3, 3), 0)
    pm.close()
  }

  test("drawPixmap same-size blit copies pixels") {
    val src = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGBA8888)
    src.setBlend(GDX2D_BLEND_NONE)
    val red = 0xff0000ff
    src.clear(red)

    val dst = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGBA8888)
    dst.setBlend(GDX2D_BLEND_NONE)
    dst.drawPixmap(src, 0, 0, 0, 0, 4, 4)
    assertEquals(dst.getPixel(0, 0), red)
    assertEquals(dst.getPixel(3, 3), red)
    src.close()
    dst.close()
  }

  test("drawPixmap partial blit") {
    val src = new Gdx2DPixmap(4, 4, GDX2D_FORMAT_RGBA8888)
    src.setBlend(GDX2D_BLEND_NONE)
    src.clear(0x00ff00ff) // green

    val dst = new Gdx2DPixmap(8, 8, GDX2D_FORMAT_RGBA8888)
    dst.setBlend(GDX2D_BLEND_NONE)
    dst.drawPixmap(src, 0, 0, 2, 2, 4, 4)
    // (2,2) should be green
    assertEquals(dst.getPixel(2, 2), 0x00ff00ff)
    // (0,0) should still be empty
    assertEquals(dst.getPixel(0, 0), 0)
    src.close()
    dst.close()
  }

  test("blend SRC_OVER alpha blending") {
    val pm = new Gdx2DPixmap(2, 2, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    // Set background to opaque blue
    pm.setPixel(0, 0, 0x0000ffff)
    // Enable blending
    pm.setBlend(GDX2D_BLEND_SRC_OVER)
    // Draw semi-transparent red on top
    pm.setPixel(0, 0, 0xff000080.toInt) // red with ~50% alpha
    val result = pm.getPixel(0, 0)
    // Should be a blend — not pure red, not pure blue
    val r = (result >>> 24) & 0xff
    val b = (result >>> 8) & 0xff
    assert(r > 0, s"expected some red component, got $r")
    assert(b > 0, s"expected some blue component, got $b")
    pm.close()
  }

  test("GL format/type constants correct") {
    assertEquals(Gdx2DPixmap.toGlFormat(GDX2D_FORMAT_RGBA8888), GL20.GL_RGBA)
    assertEquals(Gdx2DPixmap.toGlFormat(GDX2D_FORMAT_RGB888), GL20.GL_RGB)
    assertEquals(Gdx2DPixmap.toGlFormat(GDX2D_FORMAT_ALPHA), GL20.GL_ALPHA)
    assertEquals(Gdx2DPixmap.toGlType(GDX2D_FORMAT_RGBA8888), GL20.GL_UNSIGNED_BYTE)
    assertEquals(Gdx2DPixmap.toGlType(GDX2D_FORMAT_RGB565), GL20.GL_UNSIGNED_SHORT_5_6_5)
  }

  test("format conversion via drawPixmap") {
    val rgba = new Gdx2DPixmap(2, 2, GDX2D_FORMAT_RGBA8888)
    rgba.setBlend(GDX2D_BLEND_NONE)
    rgba.clear(0xff0000ff) // red, full alpha

    val rgb = new Gdx2DPixmap(2, 2, GDX2D_FORMAT_RGB888)
    rgb.setBlend(GDX2D_BLEND_NONE)
    rgb.drawPixmap(rgba, 0, 0, 0, 0, 2, 2)
    // RGB888 getPixel returns RGBA8888 with alpha=0xff
    assertEquals(rgb.getPixel(0, 0), 0xff0000ff)

    rgba.close()
    rgb.close()
  }

  test("ALPHA format roundtrip") {
    val pm = new Gdx2DPixmap(2, 2, GDX2D_FORMAT_ALPHA)
    pm.setBlend(GDX2D_BLEND_NONE)
    // Set pixel with RGBA color — only alpha channel stored
    pm.setPixel(0, 0, 0x123456ab.toInt)
    val result = pm.getPixel(0, 0)
    // Alpha format: getPixel returns 0xffffffAA where AA is the stored alpha
    val alpha = result & 0xff
    assertEquals(alpha, 0xab)
    pm.close()
  }

  test("fillCircle writes pixels inside radius") {
    val pm = new Gdx2DPixmap(20, 20, GDX2D_FORMAT_RGBA8888)
    pm.setBlend(GDX2D_BLEND_NONE)
    val white = 0xffffffff.toInt
    pm.fillCircle(10, 10, 5, white)
    // Center should be filled
    assertEquals(pm.getPixel(10, 10), white)
    // Far corner should be empty
    assertEquals(pm.getPixel(0, 0), 0)
    pm.close()
  }

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
