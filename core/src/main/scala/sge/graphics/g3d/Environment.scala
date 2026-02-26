/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/Environment.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d

import scala.collection.mutable.ArrayBuffer
import sge.graphics.g3d.attributes.DirectionalLightsAttribute
import sge.graphics.g3d.attributes.PointLightsAttribute
import sge.graphics.g3d.attributes.SpotLightsAttribute
import sge.graphics.g3d.environment.BaseLight
import sge.graphics.g3d.environment.DirectionalLight
import sge.graphics.g3d.environment.PointLight
import sge.graphics.g3d.environment.ShadowMap
import sge.graphics.g3d.environment.SpotLight
import sge.utils.Nullable
import sge.utils.SgeError

class Environment extends Attributes {

  /** Shadow map used to render shadows */
  var shadowMap: Nullable[ShadowMap] = Nullable.empty

  def add(lights: BaseLight[?]*): Environment = {
    for (light <- lights)
      add(light)
    this
  }

  def add(lights: ArrayBuffer[BaseLight[?]]): Environment = {
    for (light <- lights)
      add(light)
    this
  }

  def add(light: BaseLight[?]): Environment = {
    light match {
      case l: DirectionalLight => add(l)
      case l: PointLight       => add(l)
      case l: SpotLight        => add(l)
      case _ => throw SgeError.GraphicsError("Unknown light type")
    }
    this
  }

  def add(light: DirectionalLight): Environment = {
    val dirLights = get(DirectionalLightsAttribute.Type).map(_.asInstanceOf[DirectionalLightsAttribute])
    dirLights.fold {
      val newDirLights = new DirectionalLightsAttribute()
      set(newDirLights)
      newDirLights.lights += light
    } { dl =>
      dl.lights += light
    }
    this
  }

  def add(light: PointLight): Environment = {
    val pointLights = get(PointLightsAttribute.Type).map(_.asInstanceOf[PointLightsAttribute])
    pointLights.fold {
      val newPointLights = new PointLightsAttribute()
      set(newPointLights)
      newPointLights.lights += light
    } { pl =>
      pl.lights += light
    }
    this
  }

  def add(light: SpotLight): Environment = {
    val spotLights = get(SpotLightsAttribute.Type).map(_.asInstanceOf[SpotLightsAttribute])
    spotLights.fold {
      val newSpotLights = new SpotLightsAttribute()
      set(newSpotLights)
      newSpotLights.lights += light
    } { sl =>
      sl.lights += light
    }
    this
  }

  def removeLight(lights: BaseLight[?]*): Environment = {
    for (light <- lights)
      removeLight(light)
    this
  }

  def removeLights(lights: ArrayBuffer[BaseLight[?]]): Environment = {
    for (light <- lights)
      removeLight(light)
    this
  }

  def removeLight(light: BaseLight[?]): Environment = {
    light match {
      case l: DirectionalLight => removeLight(l)
      case l: PointLight       => removeLight(l)
      case l: SpotLight        => removeLight(l)
      case _ => throw SgeError.GraphicsError("Unknown light type")
    }
    this
  }

  def removeLight(light: DirectionalLight): Environment = {
    if (has(DirectionalLightsAttribute.Type)) {
      get(DirectionalLightsAttribute.Type).foreach { attr =>
        val dirLights = attr.asInstanceOf[DirectionalLightsAttribute]
        dirLights.lights -= light
        if (dirLights.lights.isEmpty) remove(DirectionalLightsAttribute.Type)
      }
    }
    this
  }

  def removeLight(light: PointLight): Environment = {
    if (has(PointLightsAttribute.Type)) {
      get(PointLightsAttribute.Type).foreach { attr =>
        val pointLights = attr.asInstanceOf[PointLightsAttribute]
        pointLights.lights -= light
        if (pointLights.lights.isEmpty) remove(PointLightsAttribute.Type)
      }
    }
    this
  }

  def removeLight(light: SpotLight): Environment = {
    if (has(SpotLightsAttribute.Type)) {
      get(SpotLightsAttribute.Type).foreach { attr =>
        val spotLights = attr.asInstanceOf[SpotLightsAttribute]
        spotLights.lights -= light
        if (spotLights.lights.isEmpty) remove(SpotLightsAttribute.Type)
      }
    }
    this
  }
}
