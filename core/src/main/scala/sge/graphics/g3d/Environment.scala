/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/Environment.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - shadowMap: Nullable[ShadowMap] (Java ShadowMap, nullable).
 *   - Java remove(BaseLight...) renamed to removeLight() to avoid clash with Attributes.remove(Long).
 *   - Java remove(Array<BaseLight>) renamed to removeLights().
 *   - add(lights: BaseLight[?]*) uses varargs like Java, DynamicArray overload also present.
 *   - removeLight uses .removeValue without identity flag (DynamicArray API differs from Array).
 *   - All light types (Directional, Point, Spot) handled via pattern match.
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d

import sge.graphics.g3d.attributes.DirectionalLightsAttribute
import sge.graphics.g3d.attributes.PointLightsAttribute
import sge.graphics.g3d.attributes.SpotLightsAttribute
import sge.graphics.g3d.environment.BaseLight
import sge.graphics.g3d.environment.DirectionalLight
import sge.graphics.g3d.environment.PointLight
import sge.graphics.g3d.environment.ShadowMap
import sge.graphics.g3d.environment.SpotLight
import sge.utils.{ DynamicArray, Nullable, SgeError }

class Environment extends Attributes {

  /** Shadow map used to render shadows */
  var shadowMap: Nullable[ShadowMap] = Nullable.empty

  def add(lights: BaseLight[?]*): Environment = {
    for (light <- lights)
      add(light)
    this
  }

  def add(lights: DynamicArray[BaseLight[?]]): Environment = {
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
      newDirLights.lights.add(light)
    } { dl =>
      dl.lights.add(light)
    }
    this
  }

  def add(light: PointLight): Environment = {
    val pointLights = get(PointLightsAttribute.Type).map(_.asInstanceOf[PointLightsAttribute])
    pointLights.fold {
      val newPointLights = new PointLightsAttribute()
      set(newPointLights)
      newPointLights.lights.add(light)
    } { pl =>
      pl.lights.add(light)
    }
    this
  }

  def add(light: SpotLight): Environment = {
    val spotLights = get(SpotLightsAttribute.Type).map(_.asInstanceOf[SpotLightsAttribute])
    spotLights.fold {
      val newSpotLights = new SpotLightsAttribute()
      set(newSpotLights)
      newSpotLights.lights.add(light)
    } { sl =>
      sl.lights.add(light)
    }
    this
  }

  def removeLight(lights: BaseLight[?]*): Environment = {
    for (light <- lights)
      removeLight(light)
    this
  }

  def removeLights(lights: DynamicArray[BaseLight[?]]): Environment = {
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
        dirLights.lights.removeValue(light)
        if (dirLights.lights.isEmpty) remove(DirectionalLightsAttribute.Type)
      }
    }
    this
  }

  def removeLight(light: PointLight): Environment = {
    if (has(PointLightsAttribute.Type)) {
      get(PointLightsAttribute.Type).foreach { attr =>
        val pointLights = attr.asInstanceOf[PointLightsAttribute]
        pointLights.lights.removeValue(light)
        if (pointLights.lights.isEmpty) remove(PointLightsAttribute.Type)
      }
    }
    this
  }

  def removeLight(light: SpotLight): Environment = {
    if (has(SpotLightsAttribute.Type)) {
      get(SpotLightsAttribute.Type).foreach { attr =>
        val spotLights = attr.asInstanceOf[SpotLightsAttribute]
        spotLights.lights.removeValue(light)
        if (spotLights.lights.isEmpty) remove(SpotLightsAttribute.Type)
      }
    }
    this
  }
}
