/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/Stage.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: InputAdapter -> InputProcessor; dispose() -> close(); SnapshotArray -> DynamicArray
 *   Convention: null -> Nullable; no return (boundary/break); (using Sge) on constructors
 *   Idiom: split packages
 *   Issues: TouchFocus.reset() uses raw null (uninitialized fields, acceptable)
 *   TODO: Java-style getters/setters -- convert to var or def x/def x_= (getViewport/setViewport, getCamera, getRoot, getActors, etc.)
 *   TODO: TouchFocus extends Pool.Poolable → define given Poolable[TouchFocus]
 *   TODO: opaque Pixels for viewport pixel dimensions -- see docs/improvements/opaque-types.md
 *   TODO: typed GL enums -- EnableCap for glEnable/glDisable -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d

import sge.utils.{ DynamicArray, Nullable, Pool, PoolManager, Scaling }
import scala.annotation.nowarn

import sge.graphics.{ Camera, Color, GL20, OrthographicCamera }
import sge.graphics.g2d.{ Batch, SpriteBatch }
import sge.graphics.glutils.ShapeRenderer
import sge.math.{ Matrix4, Rectangle, Vector2 }
import sge.scenes.scene2d.ui.Table
import sge.scenes.scene2d.utils.FocusListener
import sge.utils.viewport.{ ScalingViewport, Viewport }

/** A 2D scene graph containing hierarchies of {@link Actor actors}. Stage handles the viewport and distributes input events. <p> {@link #setViewport(Viewport)} controls the coordinates used within
  * the stage and sets up the camera used to convert between stage coordinates and screen coordinates. <p> A stage must receive input events so it can distribute them to actors. This is typically done
  * by passing the stage to {@link Input#setInputProcessor(com.badlogic.gdx.InputProcessor) Gdx.input.setInputProcessor}. An {@link InputMultiplexer} may be used to handle input events before or after
  * the stage does. If an actor handles an event by returning true from the input method, then the stage's input method will also return true, causing subsequent InputProcessors to not receive the
  * event. <p> The Stage and its constituents (like Actors and Listeners) are not thread-safe and should only be updated and queried from a single thread (presumably the main render thread). Methods
  * should be reentrant, so you can update Actors and Stages from within callbacks and handlers.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class Stage(private var viewport: Viewport, val batch: Batch, private val ownsBatch: Boolean)(using Sge) extends InputProcessor with AutoCloseable {

  protected val pools:                 PoolManager                    = PoolManager()
  private var root:                    Group                          = Group()
  private val tempCoords:              Vector2                        = Vector2()
  private val pointerOverActors:       Array[Nullable[Actor]]         = Array.fill(20)(Nullable.empty)
  private val pointerTouched:          Array[Boolean]                 = new Array[Boolean](20)
  private val pointerScreenX:          Array[Int]                     = new Array[Int](20)
  private val pointerScreenY:          Array[Int]                     = new Array[Int](20)
  private var mouseScreenX:            Int                            = 0
  private var mouseScreenY:            Int                            = 0
  private var mouseOverActor:          Nullable[Actor]                = Nullable.empty
  private var keyboardFocus:           Nullable[Actor]                = Nullable.empty
  private var scrollFocus:             Nullable[Actor]                = Nullable.empty
  private[scene2d] val touchFocuses:   DynamicArray[Stage.TouchFocus] = DynamicArray[Stage.TouchFocus]()
  private var actionsRequestRendering: Boolean                        = true

  private var debugShapes: Nullable[ShapeRenderer] = Nullable.empty
  @nowarn("msg=not read") // set via setter, will be read in debug drawing
  private var debugInvisible:        Boolean     = false
  private var debugAll:              Boolean     = false
  private var debugUnderMouse:       Boolean     = false
  private var debugParentUnderMouse: Boolean     = false
  private var debugTableUnderMouse:  Table.Debug = Table.Debug.none
  private val debugColor:            Color       = Color(0, 1, 0, 0.85f)

  pools.addPool(classOf[InputEvent], () => InputEvent())
  pools.addPool(classOf[FocusListener.FocusEvent], () => FocusListener.FocusEvent())
  pools.addPool(classOf[Stage.TouchFocus], () => Stage.TouchFocus())

  root.setStage(Nullable(this))
  viewport.update(Sge().graphics.getWidth(), Sge().graphics.getHeight(), true)

  /** Creates a stage with the specified viewport. The stage will use its own {@link Batch} which will be disposed when the stage is disposed.
    */
  def this(viewport: Viewport)(using Sge) =
    this(viewport, SpriteBatch()(using Sge()), true)

  /** Creates a stage with a {@link ScalingViewport} set to {@link Scaling#stretch}. The stage will use its own {@link Batch} which will be disposed when the stage is disposed.
    */
  def this()(using Sge) =
    this(
      ScalingViewport(Scaling.stretch, Sge().graphics.getWidth().toFloat, Sge().graphics.getHeight().toFloat, OrthographicCamera())
    )

  /** Creates a stage with the specified viewport and batch. This can be used to specify an existing batch or to customize which batch implementation is used.
    * @param batch
    *   Will not be disposed if {@link #close()} is called, handle disposal yourself.
    */
  def this(viewport: Viewport, batch: Batch)(using Sge) =
    this(viewport, batch, false)

  def draw(): Unit = {
    val camera = viewport.camera
    camera.update()

    if (root.isVisible) {
      batch.projectionMatrix = camera.combined
      batch.begin()
      root.draw(batch, 1)
      batch.end()

      if (Stage.debug) drawDebug()
    }
  }

  private def drawDebug(): Unit = {
    if (debugShapes.isEmpty) {
      debugShapes = Nullable(ShapeRenderer())
      debugShapes.foreach(_.setAutoShapeType(true))
    }

    var shouldDraw = true
    if (debugUnderMouse || debugParentUnderMouse || debugTableUnderMouse != Table.Debug.none) {
      screenToStageCoordinates(tempCoords.set(Sge().input.getX().toFloat, Sge().input.getY().toFloat))
      var actor: Nullable[Actor] = hit(tempCoords.x, tempCoords.y, true)

      if (actor.isEmpty) {
        shouldDraw = false
      } else {
        if (debugParentUnderMouse) {
          actor.foreach { a =>
            a.parent.foreach(p => actor = Nullable(p))
          }
        }

        if (debugTableUnderMouse == Table.Debug.none) {
          actor.foreach(_.setDebug(true))
        } else {
          scala.util.boundary {
            while (actor.isDefined)
              actor.foreach { a =>
                a match {
                  case t: Table =>
                    t.debug(debugTableUnderMouse)
                    actor = Nullable.empty
                    scala.util.boundary.break(())
                  case _ =>
                    actor = a.getParent.asInstanceOf[Nullable[Actor]]
                }
              }
          }
          if (actor.isEmpty) shouldDraw = false
        }

        if (debugAll) actor.foreach {
          case g: Group => g.debugAll()
          case _ =>
        }

        actor.foreach(a => disableDebug(root, a))
      }
    } else {
      if (debugAll) root.debugAll()
    }

    if (shouldDraw) {
      Sge().graphics.gl.glEnable(GL20.GL_BLEND)
      debugShapes.foreach { shapes =>
        shapes.setProjectionMatrix(viewport.camera.combined)
        shapes.setAutoShapeType(true)
        shapes.begin(shapes.ShapeType.Line)
        root.drawDebug(shapes)
        shapes.end()
      }
      Sge().graphics.gl.glDisable(GL20.GL_BLEND)
    }
  }

  /** Disables debug on all actors recursively except the specified actor and any children. */
  private def disableDebug(actor: Actor, except: Actor): Unit =
    if (!(actor eq except)) {
      actor.setDebug(false)
      actor match {
        case group: Group =>
          var i = 0
          while (i < group.children.size) {
            disableDebug(group.children(i), except)
            i += 1
          }
        case _ =>
      }
    }

  /** Calls {@link #act(float)} with {@link Graphics#getDeltaTime()}, limited to a minimum of 30fps. */
  def act(): Unit =
    act(Math.min(Sge().graphics.getDeltaTime(), 1 / 30f))

  /** Calls the {@link Actor#act(float)} method on each actor in the stage. Typically called each frame. This method also fires enter and exit events.
    * @param delta
    *   Time in seconds since the last frame.
    */
  def act(delta: Float): Unit = {
    // Update over actors. Done in act() because actors may change position, which can fire enter/exit without an input event.
    var pointer = 0
    while (pointer < pointerOverActors.length) {
      val overLast = pointerOverActors(pointer)
      if (pointerTouched(pointer)) {
        // Update the over actor for the pointer.
        pointerOverActors(pointer) = fireEnterAndExit(overLast, pointerScreenX(pointer), pointerScreenY(pointer), pointer)
      } else if (overLast.isDefined) {
        // The pointer is gone, exit the over actor for the pointer, if any.
        pointerOverActors(pointer) = Nullable.empty
        overLast.foreach(a => fireExit(a, pointerScreenX(pointer), pointerScreenY(pointer), pointer))
      }
      pointer += 1
    }

    // Update over actor for the mouse on the desktop.
    val appType = Sge().application.getType()
    if (appType == Application.ApplicationType.Desktop || appType == Application.ApplicationType.Applet || appType == Application.ApplicationType.WebGL)
      mouseOverActor = fireEnterAndExit(mouseOverActor, mouseScreenX, mouseScreenY, -1)

    root.act(delta)
  }

  private def fireEnterAndExit(overLast: Nullable[Actor], screenX: Int, screenY: Int, pointer: Int): Nullable[Actor] = {
    // Find the actor under the point.
    screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))
    val over = hit(tempCoords.x, tempCoords.y, true)
    if (over == overLast) overLast
    else {
      // Exit overLast.
      overLast.foreach { lastActor =>
        val event = pools.obtain(classOf[InputEvent])
        event.setType(InputEvent.Type.exit)
        event.setStage(this)
        event.setStageX(tempCoords.x)
        event.setStageY(tempCoords.y)
        event.setPointer(pointer)
        event.setRelatedActor(over)
        lastActor.fire(event)
        pools.free(event)
      }

      // Enter over.
      over.foreach { overActor =>
        val event = pools.obtain(classOf[InputEvent])
        event.setType(InputEvent.Type.enter)
        event.setStage(this)
        event.setStageX(tempCoords.x)
        event.setStageY(tempCoords.y)
        event.setPointer(pointer)
        event.setRelatedActor(overLast)
        overActor.fire(event)
        pools.free(event)
      }
      over
    }
  }

  private def fireExit(actor: Actor, screenX: Int, screenY: Int, pointer: Int): Unit = {
    screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))
    val event = pools.obtain(classOf[InputEvent])
    event.setType(InputEvent.Type.exit)
    event.setStage(this)
    event.setStageX(tempCoords.x)
    event.setStageY(tempCoords.y)
    event.setPointer(pointer)
    event.setRelatedActor(Nullable(actor))
    actor.fire(event)
    pools.free(event)
  }

  /** Applies a touch down event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event.
    */
  override def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    if (!isInsideViewport(screenX, screenY)) false
    else {
      pointerTouched(pointer) = true
      pointerScreenX(pointer) = screenX
      pointerScreenY(pointer) = screenY

      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain(classOf[InputEvent])
      event.setType(InputEvent.Type.touchDown)
      event.setStage(this)
      event.setStageX(tempCoords.x)
      event.setStageY(tempCoords.y)
      event.setPointer(pointer)
      event.setButton(button)

      val target = hit(tempCoords.x, tempCoords.y, true)
      target.fold {
        if (root.getTouchable == Touchable.enabled) root.fire(event)
      } { t =>
        t.fire(event)
      }

      val handled = event.isHandled
      pools.free(event)
      handled
    }

  /** Applies a touch moved event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event. Only {@link InputListener listeners} that returned true for touchDown
    * will receive this event.
    */
  override def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = {
    pointerScreenX(pointer) = screenX
    pointerScreenY(pointer) = screenY
    mouseScreenX = screenX
    mouseScreenY = screenY

    if (touchFocuses.isEmpty) false
    else {
      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain(classOf[InputEvent])
      event.setType(InputEvent.Type.touchDragged)
      event.setStage(this)
      event.setStageX(tempCoords.x)
      event.setStageY(tempCoords.y)
      event.setPointer(pointer)

      val snapshot = touchFocuses.toArray
      var i        = 0
      while (i < snapshot.length) {
        val focus = snapshot(i)
        if (focus.pointer == pointer && touchFocuses.contains(focus)) {
          event.setTarget(focus.target)
          event.setListenerActor(focus.listenerActor)
          if (focus.listener.handle(event)) event.handle()
        }
        i += 1
      }

      val handled = event.isHandled
      pools.free(event)
      handled
    }
  }

  /** Applies a touch up event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event. Only {@link InputListener listeners} that returned true for touchDown
    * will receive this event.
    */
  override def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
    pointerTouched(pointer) = false
    pointerScreenX(pointer) = screenX
    pointerScreenY(pointer) = screenY

    if (touchFocuses.isEmpty) false
    else {
      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain(classOf[InputEvent])
      event.setType(InputEvent.Type.touchUp)
      event.setStage(this)
      event.setStageX(tempCoords.x)
      event.setStageY(tempCoords.y)
      event.setPointer(pointer)
      event.setButton(button)

      val snapshot = touchFocuses.toArray
      var i        = 0
      while (i < snapshot.length) {
        val focus = snapshot(i)
        if (focus.pointer == pointer && focus.button == button) {
          val idx = touchFocuses.indexOf(focus)
          if (idx >= 0) {
            touchFocuses.removeIndex(idx)
            event.setTarget(focus.target)
            event.setListenerActor(focus.listenerActor)
            if (focus.listener.handle(event)) event.handle()
            pools.free(focus)
          }
        }
        i += 1
      }

      val handled = event.isHandled
      pools.free(event)
      handled
    }
  }

  override def touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
    cancelTouchFocus()
    false
  }

  /** Applies a mouse moved event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event. This event only occurs on the desktop.
    */
  override def mouseMoved(screenX: Int, screenY: Int): Boolean = {
    mouseScreenX = screenX
    mouseScreenY = screenY

    if (!isInsideViewport(screenX, screenY)) false
    else {
      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain(classOf[InputEvent])
      event.setType(InputEvent.Type.mouseMoved)
      event.setStage(this)
      event.setStageX(tempCoords.x)
      event.setStageY(tempCoords.y)

      var target = hit(tempCoords.x, tempCoords.y, true)
      if (target.isEmpty) target = Nullable(root)

      target.foreach(_.fire(event))
      val handled = event.isHandled
      pools.free(event)
      handled
    }
  }

  /** Applies a mouse scroll event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event. This event only occurs on the desktop.
    */
  override def scrolled(amountX: Float, amountY: Float): Boolean = {
    val target: Actor = scrollFocus.getOrElse(root)

    screenToStageCoordinates(tempCoords.set(mouseScreenX.toFloat, mouseScreenY.toFloat))

    val event = pools.obtain(classOf[InputEvent])
    event.setType(InputEvent.Type.scrolled)
    event.setStage(this)
    event.setStageX(tempCoords.x)
    event.setStageY(tempCoords.y)
    event.setScrollAmountX(amountX)
    event.setScrollAmountY(amountY)
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Applies a key down event to the actor that has {@link Stage#setKeyboardFocus(Actor) keyboard focus}, if any, and returns true if the event was {@link Event#handle() handled}.
    */
  override def keyDown(keyCode: Int): Boolean = {
    val target: Actor = keyboardFocus.getOrElse(root)
    val event = pools.obtain(classOf[InputEvent])
    event.setType(InputEvent.Type.keyDown)
    event.setStage(this)
    event.setKeyCode(keyCode)
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Applies a key up event to the actor that has {@link Stage#setKeyboardFocus(Actor) keyboard focus}, if any, and returns true if the event was {@link Event#handle() handled}.
    */
  override def keyUp(keyCode: Int): Boolean = {
    val target: Actor = keyboardFocus.getOrElse(root)
    val event = pools.obtain(classOf[InputEvent])
    event.setType(InputEvent.Type.keyUp)
    event.setStage(this)
    event.setKeyCode(keyCode)
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Applies a key typed event to the actor that has {@link Stage#setKeyboardFocus(Actor) keyboard focus}, if any, and returns true if the event was {@link Event#handle() handled}.
    */
  override def keyTyped(character: Char): Boolean = {
    val target: Actor = keyboardFocus.getOrElse(root)
    val event = pools.obtain(classOf[InputEvent])
    event.setType(InputEvent.Type.keyTyped)
    event.setStage(this)
    event.setCharacter(character)
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Adds the listener to be notified for all touchDragged and touchUp events for the specified pointer and button. Touch focus is added automatically when true is returned from {@link
    * InputListener#touchDown(InputEvent, float, float, int, int) touchDown}. The specified actors will be used as the {@link Event#getListenerActor() listener actor} and
    * {@link Event#getTarget() target} for the touchDragged and touchUp events.
    */
  def addTouchFocus(listener: EventListener, listenerActor: Actor, target: Actor, pointer: Int, button: Int): Unit = {
    val focus = pools.obtain(classOf[Stage.TouchFocus])
    focus.listenerActor = listenerActor
    focus.target = target
    focus.listener = listener
    focus.pointer = pointer
    focus.button = button
    touchFocuses.add(focus)
  }

  /** Removes touch focus for the specified listener, pointer, and button. Note the listener will not receive a touchUp event when this method is used.
    */
  def removeTouchFocus(listener: EventListener, listenerActor: Actor, target: Actor, pointer: Int, button: Int): Unit = {
    var i = touchFocuses.size - 1
    while (i >= 0) {
      val focus = touchFocuses(i)
      if (
        (focus.listener eq listener) && (focus.listenerActor eq listenerActor) && (focus.target eq target)
        && focus.pointer == pointer && focus.button == button
      ) {
        touchFocuses.removeIndex(i)
        pools.free(focus)
      }
      i -= 1
    }
  }

  /** Cancels touch focus for all listeners with the specified listener actor.
    * @see
    *   #cancelTouchFocus()
    */
  def cancelTouchFocus(listenerActor: Actor): Unit = {
    // Cancel all current touch focuses for the specified listener, allowing for concurrent modification, and never cancel the
    // same focus twice.
    var event: Nullable[InputEvent] = Nullable.empty
    val snapshot = touchFocuses.toArray
    var i        = 0
    while (i < snapshot.length) {
      val focus = snapshot(i)
      if (focus.listenerActor eq listenerActor) {
        val idx = touchFocuses.indexOf(focus)
        if (idx >= 0) {
          touchFocuses.removeIndex(idx)

          if (event.isEmpty) {
            val e = pools.obtain(classOf[InputEvent])
            e.setType(InputEvent.Type.touchUp)
            e.setStage(this)
            e.setStageX(Int.MinValue)
            e.setStageY(Int.MinValue)
            event = Nullable(e)
          }

          event.foreach { e =>
            e.setTarget(focus.target)
            e.setListenerActor(focus.listenerActor)
            e.setPointer(focus.pointer)
            e.setButton(focus.button)
            focus.listener.handle(e)
          }
          // Cannot return TouchFocus to pool, as it may still be in use (eg if cancelTouchFocus is called from touchDragged).
        }
      }
      i += 1
    }

    event.foreach(pools.free(_))
  }

  /** Removes all touch focus listeners, sending a touchUp event to each listener. Listeners typically expect to receive a touchUp event when they have touch focus. The location of the touchUp is
    * {@link Integer#MIN_VALUE}. Listeners can use {@link InputEvent#isTouchFocusCancel()} to ignore this event if needed.
    */
  def cancelTouchFocus(): Unit =
    cancelTouchFocusExcept(Nullable.empty, Nullable.empty)

  /** Cancels touch focus for all listeners except the specified listener.
    * @see
    *   #cancelTouchFocus()
    */
  def cancelTouchFocusExcept(exceptListener: Nullable[EventListener], exceptActor: Nullable[Actor]): Unit = {
    val event = pools.obtain(classOf[InputEvent])
    event.setType(InputEvent.Type.touchUp)
    event.setStage(this)
    event.setStageX(Int.MinValue)
    event.setStageY(Int.MinValue)

    // Cancel all current touch focuses except for the specified listener, allowing for concurrent modification, and never
    // cancel the same focus twice.
    val snapshot = touchFocuses.toArray
    var i        = 0
    while (i < snapshot.length) {
      val focus       = snapshot(i)
      val isException = exceptListener.exists(_ eq focus.listener) && exceptActor.exists(_ eq focus.listenerActor)
      if (!isException) {
        val idx = touchFocuses.indexOf(focus)
        if (idx >= 0) {
          touchFocuses.removeIndex(idx)
          event.setTarget(focus.target)
          event.setListenerActor(focus.listenerActor)
          event.setPointer(focus.pointer)
          event.setButton(focus.button)
          focus.listener.handle(event)
          // Cannot return TouchFocus to pool, as it may still be in use (eg if cancelTouchFocus is called from touchDragged).
        }
      }
      i += 1
    }

    pools.free(event)
  }

  /** Adds an actor to the root of the stage.
    * @see
    *   Group#addActor(Actor)
    */
  def addActor(actor: Actor): Unit = root.addActor(actor)

  /** Adds an action to the root of the stage.
    * @see
    *   Group#addAction(Action)
    */
  def addAction(action: Action): Unit = root.addAction(action)

  /** Returns the root's child actors.
    * @see
    *   Group#getChildren()
    */
  def getActors: DynamicArray[Actor] = root.children

  /** Adds a listener to the root.
    * @see
    *   Actor#addListener(EventListener)
    */
  def addListener(listener: EventListener): Boolean = root.addListener(listener)

  /** Removes a listener from the root.
    * @see
    *   Actor#removeListener(EventListener)
    */
  def removeListener(listener: EventListener): Boolean = root.removeListener(listener)

  /** Adds a capture listener to the root.
    * @see
    *   Actor#addCaptureListener(EventListener)
    */
  def addCaptureListener(listener: EventListener): Boolean = root.addCaptureListener(listener)

  /** Removes a listener from the root.
    * @see
    *   Actor#removeCaptureListener(EventListener)
    */
  def removeCaptureListener(listener: EventListener): Boolean = root.removeCaptureListener(listener)

  /** Called just before an actor is removed from a group. <p> The default implementation fires an {@link InputEvent.Type#exit} event if a pointer had entered the actor.
    */
  protected[scene2d] def actorRemoved(actor: Actor): Unit = {
    var pointer = 0
    while (pointer < pointerOverActors.length) {
      pointerOverActors(pointer).foreach { overActor =>
        if (actor eq overActor) {
          pointerOverActors(pointer) = Nullable.empty
          fireExit(actor, pointerScreenX(pointer), pointerScreenY(pointer), pointer)
        }
      }
      pointer += 1
    }

    mouseOverActor.foreach { moa =>
      if (actor eq moa) {
        mouseOverActor = Nullable.empty
        fireExit(actor, mouseScreenX, mouseScreenY, -1)
      }
    }
  }

  /** Removes the root's children, actions, and listeners. */
  def clear(): Unit = {
    unfocusAll()
    root.clear()
  }

  /** Removes the touch, keyboard, and scroll focused actors. */
  def unfocusAll(): Unit = {
    setScrollFocus(Nullable.empty)
    setKeyboardFocus(Nullable.empty)
    cancelTouchFocus()
  }

  /** Removes the touch, keyboard, and scroll focus for the specified actor and any descendants. */
  def unfocus(actor: Actor): Unit = {
    cancelTouchFocus(actor)
    scrollFocus.foreach { sf =>
      if (sf.isDescendantOf(actor)) setScrollFocus(Nullable.empty)
    }
    keyboardFocus.foreach { kf =>
      if (kf.isDescendantOf(actor)) setKeyboardFocus(Nullable.empty)
    }
  }

  /** Sets the actor that will receive key events.
    * @param actor
    *   May be null.
    * @return
    *   true if the unfocus and focus events were not cancelled by a {@link FocusListener}.
    */
  def setKeyboardFocus(actor: Nullable[Actor]): Boolean =
    if (keyboardFocus == actor) true
    else {
      val event = pools.obtain(classOf[FocusListener.FocusEvent])
      event.setStage(this)
      event.setType(FocusListener.FocusEvent.Type.keyboard)
      val oldKeyboardFocus = keyboardFocus
      oldKeyboardFocus.foreach { old =>
        event.setFocused(false)
        event.setRelatedActor(actor)
        old.fire(event)
      }
      var success = !event.isCancelled
      if (success) {
        keyboardFocus = actor
        actor.foreach { a =>
          event.setFocused(true)
          event.setRelatedActor(oldKeyboardFocus)
          a.fire(event)
          success = !event.isCancelled
          if (!success) keyboardFocus = oldKeyboardFocus
        }
      }
      pools.free(event)
      success
    }

  /** Gets the actor that will receive key events.
    * @return
    *   May be null.
    */
  def getKeyboardFocus: Nullable[Actor] = keyboardFocus

  /** Sets the actor that will receive scroll events.
    * @param actor
    *   May be null.
    * @return
    *   true if the unfocus and focus events were not cancelled by a {@link FocusListener}.
    */
  def setScrollFocus(actor: Nullable[Actor]): Boolean =
    if (scrollFocus == actor) true
    else {
      val event = pools.obtain(classOf[FocusListener.FocusEvent])
      event.setStage(this)
      event.setType(FocusListener.FocusEvent.Type.scroll)
      val oldScrollFocus = scrollFocus
      oldScrollFocus.foreach { old =>
        event.setFocused(false)
        event.setRelatedActor(actor)
        old.fire(event)
      }
      var success = !event.isCancelled
      if (success) {
        scrollFocus = actor
        actor.foreach { a =>
          event.setFocused(true)
          event.setRelatedActor(oldScrollFocus)
          a.fire(event)
          success = !event.isCancelled
          if (!success) scrollFocus = oldScrollFocus
        }
      }
      pools.free(event)
      success
    }

  /** Gets the actor that will receive scroll events.
    * @return
    *   May be null.
    */
  def getScrollFocus: Nullable[Actor] = scrollFocus

  def getBatch: Batch = batch

  def getViewport: Viewport = viewport

  def setViewport(viewport: Viewport): Unit =
    this.viewport = viewport

  /** The viewport's world width. */
  def getWidth: Float = viewport.worldWidth

  /** The viewport's world height. */
  def getHeight: Float = viewport.worldHeight

  /** The viewport's camera. */
  def getCamera: Camera = viewport.camera

  /** Returns the root group which holds all actors in the stage. */
  def getRoot: Group = root

  /** Replaces the root group. This can be useful, for example, to subclass the root group to be notified by {@link Group#childrenChanged()}.
    */
  def setRoot(root: Group): Unit = {
    root.parent.foreach(_.removeActor(root, unfocus = false))
    this.root = root
    root.setParent(Nullable.empty)
    root.setStage(Nullable(this))
  }

  /** Returns the {@link Actor} at the specified location in stage coordinates. Hit testing is performed in the order the actors were inserted into the stage, last inserted actors being tested first.
    * To get stage coordinates from screen coordinates, use {@link #screenToStageCoordinates(Vector2)}.
    * @param touchable
    *   If true, the hit detection will respect the {@link Actor#setTouchable(Touchable) touchability}.
    * @return
    *   May be null if no actor was hit.
    */
  def hit(stageX: Float, stageY: Float, touchable: Boolean): Nullable[Actor] = {
    root.parentToLocalCoordinates(tempCoords.set(stageX, stageY))
    root.hit(tempCoords.x, tempCoords.y, touchable)
  }

  /** Transforms the screen coordinates to stage coordinates.
    * @param screenCoords
    *   Input screen coordinates and output for resulting stage coordinates.
    */
  def screenToStageCoordinates(screenCoords: Vector2): Vector2 = {
    viewport.unproject(screenCoords)
    screenCoords
  }

  /** Transforms the stage coordinates to screen coordinates.
    * @param stageCoords
    *   Input stage coordinates and output for resulting screen coordinates.
    */
  def stageToScreenCoordinates(stageCoords: Vector2): Vector2 = {
    viewport.project(stageCoords)
    stageCoords.y = Sge().graphics.getHeight() - stageCoords.y
    stageCoords
  }

  /** Transforms the coordinates to screen coordinates. The coordinates can be anywhere in the stage since the transform matrix describes how to convert them. The transform matrix is typically
    * obtained from {@link Batch#getTransformMatrix()} during {@link Actor#draw(Batch, float)}.
    * @see
    *   Actor#localToStageCoordinates(Vector2)
    */
  def toScreenCoordinates(coords: Vector2, transformMatrix: Matrix4): Vector2 =
    viewport.toScreenCoordinates(coords, transformMatrix)

  /** Calculates window scissor coordinates from local coordinates using the batch's current transformation matrix.
    * @see
    *   ScissorStack#calculateScissors(Camera, float, float, float, float, Matrix4, Rectangle, Rectangle)
    */
  def calculateScissors(localRect: Rectangle, scissorRect: Rectangle): Unit = {
    val transformMatrix =
      debugShapes.fold(batch.transformMatrix) { shapes =>
        if (shapes.isDrawing) shapes.getTransformMatrix()
        else batch.transformMatrix
      }
    viewport.calculateScissors(transformMatrix, localRect, scissorRect)
  }

  /** If true, any actions executed during a call to {@link #act()}) will result in a call to {@link Graphics#requestRendering()}. Widgets that animate or otherwise require additional rendering may
    * check this setting before calling {@link Graphics#requestRendering()}. Default is true.
    */
  def setActionsRequestRendering(actionsRequestRendering: Boolean): Unit =
    this.actionsRequestRendering = actionsRequestRendering

  def getActionsRequestRendering: Boolean = actionsRequestRendering

  /** The default color that can be used by actors to draw debug lines. */
  def getDebugColor: Color = debugColor

  /** If true, debug lines are shown for actors even when {@link Actor#isVisible()} is false. */
  def setDebugInvisible(debugInvisible: Boolean): Unit =
    this.debugInvisible = debugInvisible

  /** If true, debug lines are shown for all actors. */
  def setDebugAll(debugAll: Boolean): Unit =
    if (this.debugAll != debugAll) {
      this.debugAll = debugAll
      if (debugAll)
        Stage.debug = true
      else
        root.setDebug(false, true)
    }

  def isDebugAll: Boolean = debugAll

  /** If true, debug is enabled only for the actor under the mouse. Can be combined with {@link #setDebugAll(boolean)}. */
  def setDebugUnderMouse(debugUnderMouse: Boolean): Unit =
    if (this.debugUnderMouse != debugUnderMouse) {
      this.debugUnderMouse = debugUnderMouse
      if (debugUnderMouse)
        Stage.debug = true
      else
        root.setDebug(false, true)
    }

  /** If true, debug is enabled only for the parent of the actor under the mouse. Can be combined with {@link #setDebugAll(boolean)}.
    */
  def setDebugParentUnderMouse(debugParentUnderMouse: Boolean): Unit =
    if (this.debugParentUnderMouse != debugParentUnderMouse) {
      this.debugParentUnderMouse = debugParentUnderMouse
      if (debugParentUnderMouse)
        Stage.debug = true
      else
        root.setDebug(false, true)
    }

  /** If not {@link Debug#none}, debug is enabled only for the first ascendant of the actor under the mouse that is a table. Can be combined with {@link #setDebugAll(boolean)}.
    * @param debugTableUnderMouse
    *   May be null for {@link Debug#none}.
    */
  def setDebugTableUnderMouse(debugTableUnderMouse: Nullable[Table.Debug]): Unit = {
    val debug = debugTableUnderMouse.getOrElse(Table.Debug.none)
    if (this.debugTableUnderMouse != debug) {
      this.debugTableUnderMouse = debug
      if (debug != Table.Debug.none)
        Stage.debug = true
      else
        root.setDebug(false, true)
    }
  }

  /** If true, debug is enabled only for the first ascendant of the actor under the mouse that is a table. Can be combined with {@link #setDebugAll(boolean)}.
    */
  def setDebugTableUnderMouse(debugTableUnderMouse: Boolean): Unit =
    setDebugTableUnderMouse(Nullable(if (debugTableUnderMouse) Table.Debug.all else Table.Debug.none))

  def close(): Unit = {
    clear()
    if (ownsBatch) batch.close()
    debugShapes.foreach(_.close())
  }

  /** Check if screen coordinates are inside the viewport's screen area. */
  protected def isInsideViewport(screenX: Int, screenY: Int): Boolean = {
    val x0       = viewport.screenX
    val x1       = x0 + viewport.screenWidth
    val y0       = viewport.screenY
    val y1       = y0 + viewport.screenHeight
    val flippedY = Sge().graphics.getHeight() - 1 - screenY
    screenX >= x0 && screenX < x1 && flippedY >= y0 && flippedY < y1
  }
}

object Stage {

  /** True if any actor has ever had debug enabled. */
  var debug: Boolean = false

  /** Internal class for managing touch focus.
    * @author
    *   Nathan Sweet
    */
  final class TouchFocus extends Pool.Poolable {
    var listener:      EventListener = scala.compiletime.uninitialized
    var listenerActor: Actor         = scala.compiletime.uninitialized
    var target:        Actor         = scala.compiletime.uninitialized
    var pointer:       Int           = 0
    var button:        Int           = 0

    def reset(): Unit = {
      listenerActor = null
      listener = null
      target = null
    }
  }
}
