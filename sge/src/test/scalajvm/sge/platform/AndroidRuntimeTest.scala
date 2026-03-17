/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Verifies that AndroidRuntime.isAndroid is false on desktop JVM.
 * This catches the bug where android.jar on the classpath (for cross-compilation)
 * caused Class.forName("android.*") to succeed, making desktop code think it was
 * running on Android.
 */
package sge
package platform

import munit.FunSuite

class AndroidRuntimeTest extends FunSuite {

  test("isAndroid is false on desktop JVM") {
    assert(
      !AndroidRuntime.isAndroid,
      "AndroidRuntime.isAndroid should be false on desktop JVM — " +
        "if this fails, the detection likely uses Class.forName instead of VM name checks. " +
        s"java.vm.name=${System.getProperty("java.vm.name")}"
    )
  }

  test("image decoder uses ImageIO on desktop JVM") {
    // Decode a minimal 1x1 red PNG (generated inline)
    val img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    img.setRGB(0, 0, 0xffff0000) // opaque red
    val baos = new java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(img, "png", baos)
    val pngBytes = baos.toByteArray

    val result = Gdx2dOpsJvm.decodeImage(pngBytes, 0, pngBytes.length)
    assert(result.isDefined, s"decodeImage failed: ${Gdx2dOpsJvm.failureReason}")
    val decoded = result.get
    assertEquals(decoded.width, 1)
    assertEquals(decoded.height, 1)
  }
}
