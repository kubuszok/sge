/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d

import sge.utils.Nullable

class Scene2DTest extends munit.FunSuite {

  private def makeContext(): Sge =
    SgeTestFixture.testSge()

  // ---------------------------------------------------------------------------
  // 1. Actor properties
  // ---------------------------------------------------------------------------

  test("Actor default properties") {
    given Sge = makeContext()
    val actor = Actor()
    assertEquals(actor.x, 0f)
    assertEquals(actor.y, 0f)
    assertEquals(actor.width, 0f)
    assertEquals(actor.height, 0f)
    assert(actor.visible)
    assertEquals(actor.touchable, Touchable.enabled)
    assert(actor.name.isEmpty)
    assert(actor.getParent.isEmpty)
    assert(actor.stage.isEmpty)
  }

  test("Actor setPosition and setBounds") {
    given Sge = makeContext()
    val actor = Actor()
    actor.setPosition(10f, 20f)
    assertEquals(actor.x, 10f)
    assertEquals(actor.y, 20f)

    actor.setBounds(5f, 6f, 100f, 200f)
    assertEquals(actor.x, 5f)
    assertEquals(actor.y, 6f)
    assertEquals(actor.width, 100f)
    assertEquals(actor.height, 200f)
  }

  test("Actor setSize and getRight/getTop") {
    given Sge = makeContext()
    val actor = Actor()
    actor.setPosition(10f, 20f)
    actor.setSize(30f, 40f)
    assertEquals(actor.getRight, 40f)
    assertEquals(actor.getTop, 60f)
  }

  test("Actor visibility and touchability") {
    given Sge = makeContext()
    val actor = Actor()
    assert(actor.isTouchable)

    actor.touchable = Touchable.disabled
    assert(!actor.isTouchable)

    actor.visible = false
    assert(!actor.visible)
  }

  test("Actor hit detection respects bounds") {
    given Sge = makeContext()
    val actor = Actor()
    actor.setSize(100f, 100f)

    assert(actor.hit(50f, 50f, false).isDefined)
    assert(actor.hit(0f, 0f, false).isDefined)
    assert(actor.hit(99f, 99f, false).isDefined)
    assert(actor.hit(100f, 100f, false).isEmpty)
    assert(actor.hit(-1f, 0f, false).isEmpty)
  }

  test("Actor hit returns empty when not visible") {
    given Sge = makeContext()
    val actor = Actor()
    actor.setSize(100f, 100f)
    actor.visible = false
    assert(actor.hit(50f, 50f, false).isEmpty)
  }

  test("Actor hit returns empty when touchable disabled and touchable=true") {
    given Sge = makeContext()
    val actor = Actor()
    actor.setSize(100f, 100f)
    actor.touchable = Touchable.disabled
    assert(actor.hit(50f, 50f, true).isEmpty)
    // But still hits when touchable check is false
    assert(actor.hit(50f, 50f, false).isDefined)
  }

  // ---------------------------------------------------------------------------
  // 2. Actor hierarchy — adding/removing actors from Groups, parent tracking
  // ---------------------------------------------------------------------------

  test("Group addActor sets parent") {
    given Sge = makeContext()
    val group = Group()
    val actor = Actor()
    group.addActor(actor)

    assert(actor.getParent.isDefined)
    assert(actor.getParent.exists(_ eq group))
    assertEquals(group.children.size, 1)
  }

  test("Group addActor moves actor from previous parent") {
    given Sge  = makeContext()
    val group1 = Group()
    val group2 = Group()
    val actor  = Actor()

    group1.addActor(actor)
    assertEquals(group1.children.size, 1)

    group2.addActor(actor)
    assertEquals(group1.children.size, 0)
    assertEquals(group2.children.size, 1)
    assert(actor.getParent.exists(_ eq group2))
  }

  test("Group removeActor clears parent") {
    given Sge = makeContext()
    val group = Group()
    val actor = Actor()
    group.addActor(actor)

    val removed = group.removeActor(actor)
    assert(removed)
    assert(actor.getParent.isEmpty)
    assertEquals(group.children.size, 0)
  }

  test("Actor.remove removes from parent") {
    given Sge = makeContext()
    val group = Group()
    val actor = Actor()
    group.addActor(actor)

    assert(actor.remove())
    assert(actor.getParent.isEmpty)
    assertEquals(group.children.size, 0)
  }

  test("Actor.remove returns false when no parent") {
    given Sge = makeContext()
    val actor = Actor()
    assert(!actor.remove())
  }

  test("isDescendantOf and isAscendantOf") {
    given Sge       = makeContext()
    val grandparent = Group()
    val parent      = Group()
    val child       = Actor()

    grandparent.addActor(parent)
    parent.addActor(child)

    assert(child.isDescendantOf(grandparent))
    assert(child.isDescendantOf(parent))
    assert(child.isDescendantOf(child))
    assert(!grandparent.isDescendantOf(child))

    assert(grandparent.isAscendantOf(child))
    assert(parent.isAscendantOf(child))
    assert(!child.isAscendantOf(grandparent))
  }

  test("Group addActorAt inserts at specific index") {
    given Sge = makeContext()
    val group = Group()
    val a1    = Actor()
    val a2    = Actor()
    val a3    = Actor()
    a1.name = Nullable("a1")
    a2.name = Nullable("a2")
    a3.name = Nullable("a3")

    group.addActor(a1)
    group.addActor(a3)
    group.addActorAt(1, a2)

    assertEquals(group.children.size, 3)
    assert(group.getChild(0) eq a1)
    assert(group.getChild(1) eq a2)
    assert(group.getChild(2) eq a3)
  }

  // ---------------------------------------------------------------------------
  // 3. Group operations — findActor, clear, transforms
  // ---------------------------------------------------------------------------

  test("Group findActor by name") {
    given Sge = makeContext()
    val group = Group()
    val actor = Actor()
    actor.name = Nullable("hero")
    group.addActor(actor)

    val found: Nullable[Actor] = group.findActor("hero")
    assert(found.isDefined)
    assert(found.exists(_ eq actor))
  }

  test("Group findActor searches recursively") {
    given Sge = makeContext()
    val root  = Group()
    val inner = Group()
    val leaf  = Actor()
    leaf.name = Nullable("leaf")

    root.addActor(inner)
    inner.addActor(leaf)

    val found: Nullable[Actor] = root.findActor("leaf")
    assert(found.isDefined)
    assert(found.exists(_ eq leaf))
  }

  test("Group findActor returns empty for missing name") {
    given Sge = makeContext()
    val group = Group()
    val found: Nullable[Actor] = group.findActor("nonexistent")
    assert(found.isEmpty)
  }

  test("Group clear removes all children and clears listeners") {
    given Sge = makeContext()
    val group = Group()
    val a1    = Actor()
    val a2    = Actor()
    group.addActor(a1)
    group.addActor(a2)

    group.addListener(new EventListener {
      def handle(event: Event): Boolean = false
    })

    group.clear()
    assertEquals(group.children.size, 0)
    assert(a1.getParent.isEmpty)
    assert(a2.getParent.isEmpty)
    assertEquals(group.getListeners.size, 0)
  }

  test("Group clearChildren removes children but keeps listeners") {
    given Sge = makeContext()
    val group = Group()
    val actor = Actor()
    group.addActor(actor)

    group.addListener(new EventListener {
      def handle(event: Event): Boolean = false
    })

    group.clearChildren()
    assertEquals(group.children.size, 0)
    assertEquals(group.getListeners.size, 1)
  }

  test("Group transform property") {
    given Sge = makeContext()
    val group = Group()
    assert(group.isTransform)
    group.setTransform(false)
    assert(!group.isTransform)
  }

  test("Group swapActor by index") {
    given Sge = makeContext()
    val group = Group()
    val a1    = Actor()
    val a2    = Actor()
    group.addActor(a1)
    group.addActor(a2)

    assert(group.swapActor(0, 1))
    assert(group.getChild(0) eq a2)
    assert(group.getChild(1) eq a1)
  }

  test("Group swapActor returns false for out of bounds") {
    given Sge = makeContext()
    val group = Group()
    val actor = Actor()
    group.addActor(actor)
    assert(!group.swapActor(0, 5))
  }

  test("Group hasChildren") {
    given Sge = makeContext()
    val group = Group()
    assert(!group.hasChildren)
    group.addActor(Actor())
    assert(group.hasChildren)
  }

  // NOTE: Stage tests (stage propagation, focus, event dispatch) require a GL context
  // and cannot run with NoopGraphics. They are tested via integration tests instead.

  // ---------------------------------------------------------------------------
  // 4. Listener management
  // ---------------------------------------------------------------------------

  test("addListener returns false for duplicates") {
    given Sge    = makeContext()
    val actor    = Actor()
    val listener = new EventListener {
      def handle(event: Event): Boolean = false
    }
    assert(actor.addListener(listener))
    assert(!actor.addListener(listener))
    assertEquals(actor.getListeners.size, 1)
  }

  test("removeListener removes the listener") {
    given Sge    = makeContext()
    val actor    = Actor()
    val listener = new EventListener {
      def handle(event: Event): Boolean = false
    }
    actor.addListener(listener)
    assert(actor.removeListener(listener))
    assertEquals(actor.getListeners.size, 0)
  }

  test("clearListeners removes all listeners") {
    given Sge = makeContext()
    val actor = Actor()
    actor.addListener(new EventListener { def handle(event: Event): Boolean = false })
    actor.addCaptureListener(new EventListener { def handle(event: Event): Boolean = false })
    actor.clearListeners()
    assertEquals(actor.getListeners.size, 0)
    assertEquals(actor.getCaptureListeners.size, 0)
  }

  // ---------------------------------------------------------------------------
  // 7. Game/Screen lifecycle delegation
  // ---------------------------------------------------------------------------

  test("Game.screen setter calls show on new screen and hide on old") {
    given Sge  = makeContext()
    val events = scala.collection.mutable.ArrayBuffer[String]()

    class TestScreen extends Screen {
      val id:                                    String = ""
      def show():                                Unit   = events += s"show-$id"
      def hide():                                Unit   = events += s"hide-$id"
      def render(delta: Float):                  Unit   = {}
      def resize(width: Pixels, height: Pixels): Unit   = events += s"resize-$id"
      def pause():                               Unit   = events += s"pause-$id"
      def resume():                              Unit   = events += s"resume-$id"
      def close():                               Unit   = {}
    }

    val screen1 = new TestScreen { override val id = "1" }
    val screen2 = new TestScreen { override val id = "2" }

    val game = new Game() {
      def create()(using Sge): Unit = {}
    }

    game.screen = Nullable(screen1)
    assertEquals(events.toList, List("show-1", "resize-1"))

    events.clear()
    game.screen = Nullable(screen2)
    assertEquals(events.toList, List("hide-1", "show-2", "resize-2"))
  }

  test("Game delegates pause/resume/resize to current screen") {
    given Sge  = makeContext()
    val events = scala.collection.mutable.ArrayBuffer[String]()

    val screen = new Screen {
      def show():                                Unit = {}
      def hide():                                Unit = {}
      def render(delta: Float):                  Unit = events += "render"
      def resize(width: Pixels, height: Pixels): Unit = events += "resize"
      def pause():                               Unit = events += "pause"
      def resume():                              Unit = events += "resume"
      def close():                               Unit = {}
    }

    val game = new Game() {
      def create()(using Sge): Unit = {}
    }

    game.screen = Nullable(screen)
    events.clear()

    game.pause()
    game.resume()
    game.resize(Pixels(800), Pixels(600))
    game.render()

    assertEquals(events.toList, List("pause", "resume", "resize", "render"))
  }

  test("Game dispose hides current screen") {
    given Sge  = makeContext()
    var hidden = false
    val screen = new Screen {
      def show():                                Unit = {}
      def hide():                                Unit = hidden = true
      def render(delta: Float):                  Unit = {}
      def resize(width: Pixels, height: Pixels): Unit = {}
      def pause():                               Unit = {}
      def resume():                              Unit = {}
      def close():                               Unit = {}
    }

    val game = new Game() {
      def create()(using Sge): Unit = {}
    }
    game.screen = Nullable(screen)
    hidden = false

    game.dispose()
    assert(hidden)
  }
}
