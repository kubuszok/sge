/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 61
 * Covenant-baseline-methods: ActorUtils,camera,keepWithinStage
 * Covenant-source-reference: com/kotcrab/vis/ui/util/ActorUtils.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util

import sge.graphics.OrthographicCamera
import sge.scenes.scene2d.{ Actor, Stage }
import sge.utils.{ Align, Nullable }

/** [[Actor]] related utils.
  * @author
  *   Kotcrab
  */
object ActorUtils {

  /** Makes sure that actor will be fully visible in stage. If it's necessary actor position will be changed to fit it on screen.
    * @throws IllegalStateException
    *   if actor does not belong to any stage.
    */
  def keepWithinStage(actor: Actor): Unit = {
    if (actor.stage.isEmpty) {
      throw new IllegalStateException("keepWithinStage cannot be used on Actor that doesn't belong to any stage.")
    }
    keepWithinStage(actor.stage.get, actor)
  }

  /** Makes sure that actor will be fully visible in stage. If it's necessary actor position will be changed to fit it on screen. */
  def keepWithinStage(stage: Stage, actor: Actor): Unit = {
    val camera = stage.camera
    camera match {
      case ortho: OrthographicCamera =>
        val parentWidth  = stage.width
        val parentHeight = stage.height
        if (actor.getX(Align.right) - camera.position.x > parentWidth / 2 / ortho.zoom)
          actor.setPosition(camera.position.x + parentWidth / 2 / ortho.zoom, actor.getY(Align.right), Align.right)
        if (actor.getX(Align.left) - camera.position.x < -parentWidth / 2 / ortho.zoom)
          actor.setPosition(camera.position.x - parentWidth / 2 / ortho.zoom, actor.getY(Align.left), Align.left)
        if (actor.getY(Align.top) - camera.position.y > parentHeight / 2 / ortho.zoom)
          actor.setPosition(actor.getX(Align.top), camera.position.y + parentHeight / 2 / ortho.zoom, Align.top)
        if (actor.getY(Align.bottom) - camera.position.y < -parentHeight / 2 / ortho.zoom)
          actor.setPosition(actor.getX(Align.bottom), camera.position.y - parentHeight / 2 / ortho.zoom, Align.bottom)
      case _ =>
        actor.parent.foreach { p =>
          if (p eq stage.root) {
            val parentWidth  = stage.width
            val parentHeight = stage.height
            if (actor.x < 0) actor.setX(0)
            if (actor.right > parentWidth) actor.setX(parentWidth - actor.width)
            if (actor.y < 0) actor.setY(0)
            if (actor.top > parentHeight) actor.setY(parentHeight - actor.height)
          }
        }
    }
  }
}
