/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d

/** Red tests for ISS-552: Actor exposes position/size/scale/rotation state as PUBLIC `var`s.
  *
  * LibGDX (com/badlogic/gdx/scenes/scene2d/Actor.java) keeps x/y/width/height/scaleX/scaleY/rotation PRIVATE precisely because assigning them must fire layout-invalidation hooks. The setter methods
  * (setX/setY/setWidth/setHeight/setScaleX/setScaleY/setRotation, with a change-guard) DO fire:
  *   - setX/setY/setPosition/moveBy -> positionChanged()
  *   - setWidth/setHeight/setSize/setBounds -> sizeChanged()
  *   - setScaleX/setScaleY/setScale/scaleBy -> scaleChanged()
  *   - setRotation/rotateBy -> rotationChanged()
  *
  * But because the port made these PUBLIC `var`s, the natural Scala spelling `actor.x = 5f` SILENTLY bypasses the hook -> layout never invalidates -> stale/incorrect rendering.
  *
  * These tests mutate via the PUBLIC PROPERTY spelling (NOT the setX/setWidth methods) and assert the corresponding protected hook fired. On current code the assignment bypasses the hook, so the
  * first assertion (`a.x = 5f` -> positionChanged) FAILS — the required red.
  */
class ActorPropertyHookRedSuite extends munit.FunSuite {

  given sge: Sge = SgeTestFixture.testSge()

  /** Actor subclass instrumenting the four protected layout-invalidation hooks. */
  final private class CountingActor(using Sge) extends Actor() {
    var positionChangedCalls: Int = 0
    var sizeChangedCalls:     Int = 0
    var scaleChangedCalls:    Int = 0
    var rotationChangedCalls: Int = 0

    override protected def positionChanged(): Unit = positionChangedCalls += 1
    override protected def sizeChanged():     Unit = sizeChangedCalls += 1
    override protected def scaleChanged():    Unit = scaleChangedCalls += 1
    override protected def rotationChanged(): Unit = rotationChangedCalls += 1
  }

  // ---------------------------------------------------------------------------
  // (1) position: `a.x = 5f` / `a.y = 6f` must fire positionChanged()
  // ---------------------------------------------------------------------------

  test("ISS-552 assigning public var x/y fires positionChanged()") {
    val a = new CountingActor
    // Defaults are x=0,y=0; assign DIFFERENT values so the change-guard would fire.
    a.x = 5f
    assertEquals(a.x, 5f, "the field must actually take the new value")
    assertEquals(
      a.positionChangedCalls,
      1,
      "assigning public var `a.x = 5f` must fire positionChanged() (the field is PUBLIC, bypassing the hook -> RED)"
    )
    a.y = 6f
    assertEquals(a.y, 6f, "the field must actually take the new value")
    assertEquals(a.positionChangedCalls, 2, "assigning public var `a.y = 6f` must fire positionChanged()")
  }

  // ---------------------------------------------------------------------------
  // (2) size: `a.width = 10f` / `a.height = 11f` must fire sizeChanged()
  // ---------------------------------------------------------------------------

  test("ISS-552 assigning public var width/height fires sizeChanged()") {
    val a = new CountingActor
    a.width = 10f
    assertEquals(a.width, 10f, "the field must actually take the new value")
    assertEquals(a.sizeChangedCalls, 1, "assigning public var `a.width = 10f` must fire sizeChanged()")
    a.height = 11f
    assertEquals(a.height, 11f, "the field must actually take the new value")
    assertEquals(a.sizeChangedCalls, 2, "assigning public var `a.height = 11f` must fire sizeChanged()")
  }

  // ---------------------------------------------------------------------------
  // (3) scale: `a.scaleX = 2f` / `a.scaleY = 3f` must fire scaleChanged()
  // ---------------------------------------------------------------------------

  test("ISS-552 assigning public var scaleX/scaleY fires scaleChanged()") {
    val a = new CountingActor
    // Defaults are scaleX=1,scaleY=1; assign DIFFERENT values.
    a.scaleX = 2f
    assertEquals(a.scaleX, 2f, "the field must actually take the new value")
    assertEquals(a.scaleChangedCalls, 1, "assigning public var `a.scaleX = 2f` must fire scaleChanged()")
    a.scaleY = 3f
    assertEquals(a.scaleY, 3f, "the field must actually take the new value")
    assertEquals(a.scaleChangedCalls, 2, "assigning public var `a.scaleY = 3f` must fire scaleChanged()")
  }

  // ---------------------------------------------------------------------------
  // (4) rotation: `a.rotation = 45f` must fire rotationChanged()
  // ---------------------------------------------------------------------------

  test("ISS-552 assigning public var rotation fires rotationChanged()") {
    val a = new CountingActor
    a.rotation = 45f
    assertEquals(a.rotation, 45f, "the field must actually take the new value")
    assertEquals(a.rotationChangedCalls, 1, "assigning public var `a.rotation = 45f` must fire rotationChanged()")
  }
}
