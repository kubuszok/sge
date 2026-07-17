/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/animation/AnimationsPlayer.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 97
 * Covenant-baseline-methods: AnimationsPlayer,addAnimation,addAnimations,c,clearAnimations,controllers,dt,i,loopAll,n,playAll,removeAnimation,stopAll,update
 * Covenant-source-reference: net/mgsx/gltf/scene3d/animation/AnimationsPlayer.java
 * Covenant-verified: 2026-06-12
 */
package sge
package gltf
package scene3d
package animation

import sge.gltf.scene3d.scene.Scene
import sge.graphics.g3d.model.Animation
import sge.graphics.g3d.utils.AnimationController
import sge.graphics.g3d.utils.AnimationController.AnimationDesc
import lowlevel.util.DynamicArray
import sge.utils.Seconds

class AnimationsPlayer(private val scene: Scene) {

  private val controllers: DynamicArray[AnimationController] = DynamicArray[AnimationController]()

  def addAnimations(animations: DynamicArray[AnimationDesc]): Unit = {
    var i = 0
    while (i < animations.size) {
      addAnimation(animations(i))
      i += 1
    }
  }

  def addAnimation(animation: AnimationDesc): Unit = {
    val c = AnimationControllerHack(scene.modelInstance)
    c.calculateTransforms = false
    c.setAnimationDesc(animation)
    controllers.add(c)
  }

  def removeAnimation(animation: Animation): Unit = {
    var i = controllers.size - 1
    while (i >= 0) {
      // Idiom: `_.animation.contains(animation)` matches by REFERENCE equality, faithful to the
      // original `current.animation == animation` (AnimationsPlayer.java:33). Animation (and its
      // sge.graphics.g3d.model.Animation port) declares no `equals` override, so `contains` on the
      // Nullable[Animation] falls back to identity — the same instance the caller added is removed,
      // not a structurally-equal one. Do not give Animation a value-based equals without revisiting
      // this call site.
      if (controllers(i).current.exists(_.animation.contains(animation))) {
        controllers.removeIndex(i)
      }
      i -= 1
    }
  }

  def clearAnimations(): Unit = {
    controllers.clear()
    if (scene.animationController != null) { // @nowarn — nullable field
      scene.animationController.setAnimation(null) // @nowarn — Java interop, null means stop
    }
  }

  def playAll(): Unit = playAll(false)

  def loopAll(): Unit = playAll(true)

  def playAll(loop: Boolean): Unit = {
    clearAnimations()
    var i = 0
    val n = scene.modelInstance.animations.size
    while (i < n) {
      val c = AnimationControllerHack(scene.modelInstance)
      c.calculateTransforms = false
      c.setAnimation(scene.modelInstance.animations(i), if (loop) -1 else 1)
      controllers.add(c)
      i += 1
    }
  }

  def stopAll(): Unit = clearAnimations()

  def update(delta: Float): Unit = {
    val dt = Seconds(delta)
    if (controllers.size > 0) {
      var i = 0
      while (i < controllers.size) {
        controllers(i).update(dt)
        i += 1
      }
      scene.modelInstance.calculateTransforms()
    } else {
      if (scene.animationController != null) { // @nowarn — nullable field
        scene.animationController.update(dt)
      }
    }
  }
}
