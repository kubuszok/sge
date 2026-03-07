/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original test: com/badlogic/gdx/graphics/g3d/utils/AnimationDescTest.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import scala.language.implicitConversions
import sge.graphics.g3d.model.Animation
import sge.graphics.g3d.utils.AnimationController.AnimationDesc
import sge.utils.{ Nullable, Seconds }

class AnimationDescTest extends munit.FunSuite {

  private val epsilon: Float = 1e-6f

  private def makeAnim(): AnimationDesc = {
    val anim = new AnimationDesc()
    anim.animation = new Animation()
    anim.duration = Seconds(1f)
    anim.listener = Nullable.empty
    anim.loopCount = 1
    anim.offset = Seconds.zero
    anim.speed = 1f
    anim.time = Seconds.zero
    anim
  }

  test("update nominal") {
    val anim = makeAnim()
    assertEqualsFloat(anim.update(Seconds(.75f)).toFloat, -1f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.75f)).toFloat, .5f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.75f)).toFloat, .75f, epsilon)
  }

  test("update just end") {
    val anim = makeAnim()
    assertEqualsFloat(anim.update(Seconds(.5f)).toFloat, -1f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.5f)).toFloat, 0f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.5f)).toFloat, .5f, epsilon)
  }

  test("update big delta") {
    val anim = makeAnim()
    assertEqualsFloat(anim.update(Seconds(5.2f)).toFloat, 4.2f, epsilon)
    assertEqualsFloat(anim.update(Seconds(7.3f)).toFloat, 7.3f, epsilon)
  }

  test("update zero delta") {
    val anim = makeAnim()
    assertEqualsFloat(anim.update(Seconds(0f)).toFloat, -1f, epsilon)
    assertEqualsFloat(anim.time.toFloat, 0f, epsilon)
  }

  test("update reverse nominal") {
    val anim = makeAnim()
    anim.speed = -1
    anim.time = anim.duration

    assertEqualsFloat(anim.update(Seconds(.75f)).toFloat, -1f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.75f)).toFloat, .5f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.75f)).toFloat, .75f, epsilon)
  }

  test("update reverse just end") {
    val anim = makeAnim()
    anim.speed = -1
    anim.time = anim.duration

    assertEqualsFloat(anim.update(Seconds(.5f)).toFloat, -1f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.5f)).toFloat, 0f, epsilon)
    assertEqualsFloat(anim.update(Seconds(.5f)).toFloat, .5f, epsilon)
  }

  test("update reverse big delta") {
    val anim = makeAnim()
    anim.speed = -1
    anim.time = anim.duration

    assertEqualsFloat(anim.update(Seconds(5.2f)).toFloat, 4.2f, epsilon)
    assertEqualsFloat(anim.update(Seconds(7.3f)).toFloat, 7.3f, epsilon)
  }

  test("update reverse zero delta") {
    val anim = makeAnim()
    anim.speed = -1
    anim.time = anim.duration

    assertEqualsFloat(anim.update(Seconds(0f)).toFloat, -1f, epsilon)
    assertEqualsFloat(anim.time.toFloat, anim.duration.toFloat, epsilon)
  }
}
