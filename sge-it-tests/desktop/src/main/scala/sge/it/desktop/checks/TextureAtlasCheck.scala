// SGE — Desktop integration test: TextureAtlas loading
//
// Loads a minimal .atlas fixture file that references test.png,
// verifies region parsing and lookup by name.

package sge.it.desktop.checks

import sge.Sge
import sge.graphics.g2d.TextureAtlas
import sge.it.desktop.CheckResult

/** Verifies TextureAtlas loading, region parsing, and findRegion lookup. */
object TextureAtlasCheck {

  def run()(using Sge): CheckResult =
    try {
      val files     = Sge().files
      val atlasFile = files.internal("test.atlas")

      if (!atlasFile.exists()) {
        return CheckResult("atlas", passed = false, "test.atlas not found in resources")
      }

      val atlas      = new TextureAtlas(atlasFile)
      val allRegions = atlas.regions
      val r1         = atlas.findRegion("region1")
      val r2         = atlas.findRegion("region2")
      atlas.close()

      if (allRegions.length < 2) {
        CheckResult("atlas", passed = false, s"Expected >= 2 regions, got ${allRegions.length}")
      } else if (r1.isEmpty || r2.isEmpty) {
        CheckResult("atlas", passed = false, s"findRegion failed: r1=${r1.isDefined}, r2=${r2.isDefined}")
      } else {
        CheckResult("atlas", passed = true, s"Atlas OK: ${allRegions.length} regions, r1=${r1.get.regionWidth}x${r1.get.regionHeight}")
      }
    } catch {
      case e: Exception =>
        CheckResult("atlas", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
