/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/CascadeShadowMapAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: Alias,CascadeShadowMapAttribute,Type,cascadeShadowMap,compare,copy
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package attributes

import sge.gltf.scene3d.scene.CascadeShadowMap
import sge.graphics.g3d.Attribute

class CascadeShadowMapAttribute(
  val cascadeShadowMap: CascadeShadowMap
) extends Attribute(CascadeShadowMapAttribute.Type) {

  override def compare(that: Attribute): Int =
    (`type` - that.`type`).toInt

  override def copy(): Attribute =
    CascadeShadowMapAttribute(cascadeShadowMap)
}

object CascadeShadowMapAttribute {

  val Alias: String = "CSM"
  val Type:  Long   = Attribute.register(Alias)
}
