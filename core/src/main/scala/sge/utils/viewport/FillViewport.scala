package sge
package utils
package viewport

import sge.graphics.Camera;
import sge.graphics.OrthographicCamera;

/** A ScalingViewport that uses {@link Scaling#fill} so it keeps the aspect ratio by scaling the world up to take the whole screen (some of the world may be off screen).
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class FillViewport(worldWidth: Float, worldHeight: Float, camera: Camera)(using sge: Sge) extends ScalingViewport(Scaling.fill, worldWidth, worldHeight, camera) {

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this(worldWidth: Float, worldHeight: Float)(using sge: Sge) = {
    this(worldWidth, worldHeight, new OrthographicCamera())
  }
}
