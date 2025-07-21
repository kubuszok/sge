package sge
package utils
package viewport

import sge.graphics.Camera;
import sge.graphics.OrthographicCamera;

/** A ScalingViewport that uses {@link Scaling#stretch} so it does not keep the aspect ratio, the world is scaled to take the whole screen.
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class StretchViewport(worldWidth: Float, worldHeight: Float, camera: Camera)(using sge: Sge) extends ScalingViewport(Scaling.stretch, worldWidth, worldHeight, camera) {

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this(worldWidth: Float, worldHeight: Float)(using sge: Sge) = {
    this(worldWidth, worldHeight, new OrthographicCamera())
  }
}
