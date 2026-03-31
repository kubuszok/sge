/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/SceneModel.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package scene

import sge.graphics.Camera
import sge.graphics.g3d.Model
import sge.graphics.g3d.environment.BaseLight
import sge.graphics.g3d.model.Node
import sge.utils.{ Nullable, ObjectMap }

class SceneModel extends AutoCloseable {

  var name:    Nullable[String]              = Nullable.empty
  var model:   Model                         = scala.compiletime.uninitialized
  val cameras: ObjectMap[Node, Camera]       = ObjectMap[Node, Camera]()
  val lights:  ObjectMap[Node, BaseLight[?]] = ObjectMap[Node, BaseLight[?]]()

  override def close(): Unit =
    model.close()
}
