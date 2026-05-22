import sbt.*
import sbt.Keys.*

object TestAssets {

  /** Generate minimal regression test assets (PNG + text + manifest). */
  val regressionAssets: Def.Initialize[Task[Seq[File]]] = Def.task {
    val dir = (Compile / resourceManaged).value / "regression"
    IO.createDirectory(dir)

    val pngFile = dir / "test-texture.png"
    if (!pngFile.exists()) {
      val img = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB)
      val g   = img.createGraphics()
      g.setColor(java.awt.Color.RED)
      g.fillRect(0, 0, 4, 4)
      g.dispose()
      javax.imageio.ImageIO.write(img, "PNG", pngFile)
    }

    val txtFile = dir / "test-data.txt"
    IO.write(txtFile, "SGE_REGRESSION_TEST_DATA")

    val manifestFile = (Compile / resourceManaged).value / "assets.txt"
    IO.write(manifestFile, "regression/test-texture.png\nregression/test-data.txt\n")

    Seq(pngFile, txtFile, manifestFile)
  }
}
