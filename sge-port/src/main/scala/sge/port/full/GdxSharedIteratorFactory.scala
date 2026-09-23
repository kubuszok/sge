package sge.port.full

import balticporter.tir.{ ConfigView, Phase, TransformFactory }

/** A library-specific rule reaching the CONFIG front door via `META-INF/services`, so a `.conf` can name it (`{ transform = "gdx-shared-iterator" }`) without the engine reflectively instantiating a
  * class name from data. Lives beside [[GdxSharedIteratorRule]] in `corpus`, not the engine.
  */
final class GdxSharedIteratorFactory extends TransformFactory {

  /** same string the phase reports under; this rule has no policy to configure, so no reason to spell it twice.
    */
  def name: String = "gdx-shared-iterator"

  def fromConfig(config: ConfigView): Phase = new GdxSharedIteratorRule
}
