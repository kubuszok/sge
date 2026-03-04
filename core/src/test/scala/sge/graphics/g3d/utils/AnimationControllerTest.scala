/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original test: com/badlogic/gdx/graphics/g3d/utils/AnimationControllerTest.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import sge.Sge
import sge.SgeTestFixture
import sge.graphics.g3d.model.{ Animation, NodeKeyframe }
import sge.graphics.g3d.utils.AnimationController.AnimationDesc
import sge.utils.{ DynamicArray, Nullable }

class AnimationControllerTest extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  test("getFirstKeyframeIndexAtTime nominal") {
    val keyFrames = DynamicArray[NodeKeyframe[String]]()

    keyFrames.add(new NodeKeyframe[String](0f, "1st"))
    keyFrames.add(new NodeKeyframe[String](3f, "2nd"))
    keyFrames.add(new NodeKeyframe[String](12f, "3rd"))
    keyFrames.add(new NodeKeyframe[String](13f, "4th"))

    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, -1f), 0)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 0f), 0)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 2f), 0)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 9f), 1)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 12.5f), 2)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 13f), 2)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 14f), 0)
  }

  test("getFirstKeyframeIndexAtTime single key") {
    val keyFrames = DynamicArray[NodeKeyframe[String]]()

    keyFrames.add(new NodeKeyframe[String](10f, "1st"))

    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 9f), 0)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 10f), 0)
    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 11f), 0)
  }

  test("getFirstKeyframeIndexAtTime empty") {
    val keyFrames = DynamicArray[NodeKeyframe[String]]()

    assertEquals(BaseAnimationController.getFirstKeyframeIndexAtTime(keyFrames, 3f), 0)
  }

  private def assertSameAnimation(expected: Animation, actual: Nullable[AnimationDesc]): Unit = {
    assert(actual.isDefined, "AnimationDesc should not be empty")
    actual.foreach { desc =>
      val actualId = desc.animation.fold("<empty>")(_.id)
      if (expected.id != actualId) {
        fail(s"expected: ${expected.id}, actual: ${actualId}")
      }
    }
  }

  test("end up action at duration time") {
    val loop = new Animation()
    loop.id = "loop"
    loop.duration = 1f

    val action = new Animation()
    action.id = "action"
    action.duration = 0.2f

    val modelInstance = new ModelInstance(new Model())
    modelInstance.animations.add(loop)
    modelInstance.animations.add(action)

    val animationController = new AnimationController(modelInstance)

    animationController.setAnimation("loop", -1)
    assertSameAnimation(loop, animationController.current)

    animationController.update(1)
    assertSameAnimation(loop, animationController.current)

    animationController.update(0.01f)
    assertSameAnimation(loop, animationController.current)

    animationController.action("action", 1, 1f, Nullable.empty, 0f)
    assertSameAnimation(action, animationController.current)

    animationController.update(0.2f)
    assertSameAnimation(loop, animationController.current)
  }

  test("end up action at duration time reverse") {
    val loop = new Animation()
    loop.id = "loop"
    loop.duration = 1f

    val action = new Animation()
    action.id = "action"
    action.duration = 0.2f

    val modelInstance = new ModelInstance(new Model())
    modelInstance.animations.add(loop)
    modelInstance.animations.add(action)

    val animationController = new AnimationController(modelInstance)

    animationController.setAnimation("loop", -1, -1f, Nullable.empty)
    assertSameAnimation(loop, animationController.current)

    animationController.update(1)
    assertSameAnimation(loop, animationController.current)

    animationController.update(0.01f)
    assertSameAnimation(loop, animationController.current)

    animationController.action("action", 1, -1f, Nullable.empty, 0f)
    assertSameAnimation(action, animationController.current)

    animationController.update(0.2f)
    assertSameAnimation(loop, animationController.current)
  }
}
