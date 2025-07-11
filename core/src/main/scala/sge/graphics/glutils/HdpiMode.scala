package sge
package graphics
package glutils

import sge.graphics.GL20

enum HdpiMode {

  /** mouse coordinates, {@link Graphics#getWidth()} and {@link Graphics#getHeight()} will return logical coordinates according to the system defined HDPI scaling. Rendering will be performed to a
    * backbuffer at raw resolution. Use {@link HdpiUtils} when calling {@link GL20#glScissor} or {@link GL20#glViewport} which expect raw coordinates.
    */
  case Logical

  /** Mouse coordinates, {@link Graphics#getWidth()} and {@link Graphics#getHeight()} will return raw pixel coordinates irrespective of the system defined HDPI scaling.
    */
  case Pixels
}
