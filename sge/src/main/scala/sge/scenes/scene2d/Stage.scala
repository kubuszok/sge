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
 *   Issues: TouchFocus fields now Nullable; .get used at access sites where pool guarantees non-null
 *   Convention: TouchFocus extends Pool.Poolable — given Poolable[TouchFocus] auto-derived via Poolable.fromTrait
 *   Convention: opaque Pixels for screen coordinate params in InputProcessor overrides
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 945
 * Covenant-baseline-methods: Stage,TouchFocus,_actionsRequestRendering,_debugAll,_debugColor,_keyboardFocus,_root,_scrollFocus,act,actionsRequestRendering,actionsRequestRendering_,actorRemoved,actors,addAction,addActor,addCaptureListener,addListener,addTouchFocus,appType,button,calculateScissors,camera,cancelTouchFocus,cancelTouchFocusExcept,clear,close,debug,debugAll,debugColor,debugInvisible,debugParentUnderMouse,debugShapes,debugTableUnderMouse,debugUnderMouse,disableDebug,draw,drawDebug,event,fireEnterAndExit,fireExit,flippedY,focus,handled,height,hit,i,isInsideViewport,keyDown,keyTyped,keyUp,keyboardFocus,listener,listenerActor,mouseMoved,mouseOverActor,mouseScreenX,mouseScreenY,over,pointer,pointerOverActors,pointerScreenX,pointerScreenY,pointerTouched,pools,removeCaptureListener,removeListener,removeTouchFocus,reset,root,screenToStageCoordinates,scrollFocus,scrolled,setDebugAll,setDebugInvisible,setDebugParentUnderMouse,setDebugTableUnderMouse,setDebugUnderMouse,setKeyboardFocus,setRoot,setScrollFocus,shouldDraw,snapshot,stageToScreenCoordinates,target,tempCoords,this,toScreenCoordinates,touchCancelled,touchDown,touchDragged,touchFocuses,touchUp,transformMatrix,unfocus,unfocusAll,viewport,viewport_,width,x0,x1,y0,y1
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/Stage.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 80398bf4c2814b27b5234e6a85487a0691968a31
 */
package sge
package scenes
package scene2d

import sge.Input.{ Button, Key }
import sge.utils.{ DynamicArray, Nullable, Pool, PoolManager, Scaling, Seconds }
import scala.annotation.nowarn

import sge.graphics.{ Camera, Color, EnableCap, OrthographicCamera }
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
class Stage(private var _viewport: Viewport, val batch: Batch, private val ownsBatch: Boolean)(using Sge) extends InputProcessor with AutoCloseable {

  protected val pools:                  PoolManager                    = PoolManager()
  private var _root:                    Group                          = Group()
  private val tempCoords:               Vector2                        = Vector2()
  private val pointerOverActors:        Array[Nullable[Actor]]         = Array.fill(20)(Nullable.empty)
  private val pointerTouched:           Array[Boolean]                 = new Array[Boolean](20)
  private val pointerScreenX:           Array[Int]                     = new Array[Int](20)
  private val pointerScreenY:           Array[Int]                     = new Array[Int](20)
  private var mouseScreenX:             Int                            = 0
  private var mouseScreenY:             Int                            = 0
  private var mouseOverActor:           Nullable[Actor]                = Nullable.empty
  private var _keyboardFocus:           Nullable[Actor]                = Nullable.empty
  private var _scrollFocus:             Nullable[Actor]                = Nullable.empty
  private[scene2d] val touchFocuses:    DynamicArray[Stage.TouchFocus] = DynamicArray[Stage.TouchFocus]()
  private var _actionsRequestRendering: Boolean                        = true

  private var debugShapes: Nullable[ShapeRenderer] = Nullable.empty
  @nowarn("msg=not read") // set via setter, will be read in debug drawing
  private var debugInvisible:        Boolean     = false
  private var _debugAll:             Boolean     = false
  private var debugUnderMouse:       Boolean     = false
  private var debugParentUnderMouse: Boolean     = false
  private var debugTableUnderMouse:  Table.Debug = Table.Debug.none
  private val _debugColor:           Color       = Color(0, 1, 0, 0.85f)

  pools.addPool[InputEvent](() => InputEvent())
  pools.addPool[FocusListener.FocusEvent](() => FocusListener.FocusEvent())
  pools.addPool[Stage.TouchFocus](() => Stage.TouchFocus())

  _root.setStage(Nullable(this))
  _viewport.update(Sge().graphics.width, Sge().graphics.height, true)

  /** Creates a stage with the specified viewport. The stage will use its own {@link Batch} which will be disposed when the stage is disposed.
    */
  def this(viewport: Viewport)(using Sge) =
    this(viewport, SpriteBatch()(using Sge()), true)

  /** Creates a stage with a {@link ScalingViewport} set to {@link Scaling#stretch}. The stage will use its own {@link Batch} which will be disposed when the stage is disposed.
    */
  def this()(using Sge) =
    this(
      ScalingViewport(
        Scaling.stretch,
        WorldUnits(Sge().graphics.width.toFloat),
        WorldUnits(Sge().graphics.height.toFloat),
        OrthographicCamera()
      )
    )

  /** Creates a stage with the specified viewport and batch. This can be used to specify an existing batch or to customize which batch implementation is used.
    * @param batch
    *   Will not be disposed if {@link #close()} is called, handle disposal yourself.
    */
  def this(viewport: Viewport, batch: Batch)(using Sge) =
    this(viewport, batch, false)

  def draw(): Unit = {
    val camera = _viewport.camera
    camera.update()

    if (_root.visible) {
      batch.projectionMatrix = camera.combined
      batch.begin()
      _root.draw(batch, 1)
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
      screenToStageCoordinates(tempCoords.set(Sge().input.x.toFloat, Sge().input.y.toFloat))
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
                    actor = a.parent.asInstanceOf[Nullable[Actor]]
                }
              }
          }
          if (actor.isEmpty) shouldDraw = false
        }

        if (_debugAll) actor.foreach {
          case g: Group => g.debugAll()
          case _ =>
        }

        actor.foreach(a => disableDebug(_root, a))
      }
    } else {
      if (_debugAll) _root.debugAll()
    }

    if (shouldDraw) {
      Sge().graphics.gl.glEnable(EnableCap.Blend)
      debugShapes.foreach { shapes =>
        shapes.setProjectionMatrix(_viewport.camera.combined)
        shapes.setAutoShapeType(true)
        shapes.begin(ShapeRenderer.ShapeType.Line)
        _root.drawDebug(shapes)
        shapes.end()
      }
      Sge().graphics.gl.glDisable(EnableCap.Blend)
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
    act(Seconds(Math.min(Sge().graphics.deltaTime.toFloat, 1 / 30f)))

  /** Calls the {@link Actor#act(float)} method on each actor in the stage. Typically called each frame. This method also fires enter and exit events.
    * @param delta
    *   Time in seconds since the last frame.
    */
  def act(delta: Seconds): Unit = {
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
    val appType = Sge().application.applicationType
    if (appType == Application.ApplicationType.Desktop || appType == Application.ApplicationType.Applet || appType == Application.ApplicationType.WebGL)
      mouseOverActor = fireEnterAndExit(mouseOverActor, mouseScreenX, mouseScreenY, -1)

    _root.act(delta)
  }

  private def fireEnterAndExit(overLast: Nullable[Actor], screenX: Int, screenY: Int, pointer: Int): Nullable[Actor] = {
    // Find the actor under the point.
    screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))
    val over = hit(tempCoords.x, tempCoords.y, true)
    if (over == overLast) overLast
    else {
      // Exit overLast.
      overLast.foreach { lastActor =>
        val event = pools.obtain[InputEvent]
        event.eventType = InputEvent.Type.exit
        event.stage = Nullable(this)
        event.stageX = tempCoords.x
        event.stageY = tempCoords.y
        event.pointer = pointer
        event.relatedActor = over
        lastActor.fire(event)
        pools.free(event)
      }

      // Enter over.
      over.foreach { overActor =>
        val event = pools.obtain[InputEvent]
        event.eventType = InputEvent.Type.enter
        event.stage = Nullable(this)
        event.stageX = tempCoords.x
        event.stageY = tempCoords.y
        event.pointer = pointer
        event.relatedActor = overLast
        overActor.fire(event)
        pools.free(event)
      }
      over
    }
  }

  private def fireExit(actor: Actor, screenX: Int, screenY: Int, pointer: Int): Unit = {
    screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))
    val event = pools.obtain[InputEvent]
    event.eventType = InputEvent.Type.exit
    event.stage = Nullable(this)
    event.stageX = tempCoords.x
    event.stageY = tempCoords.y
    event.pointer = pointer
    event.relatedActor = Nullable(actor)
    actor.fire(event)
    pools.free(event)
  }

  /** Applies a touch down event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event.
    */
  override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean =
    if (!isInsideViewport(screenX.toInt, screenY.toInt)) false
    else {
      pointerTouched(pointer) = true
      pointerScreenX(pointer) = screenX.toInt
      pointerScreenY(pointer) = screenY.toInt

      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain[InputEvent]
      event.eventType = InputEvent.Type.touchDown
      event.stage = Nullable(this)
      event.stageX = tempCoords.x
      event.stageY = tempCoords.y
      event.pointer = pointer
      event.button = button

      val target = hit(tempCoords.x, tempCoords.y, true)
      target.fold {
        if (_root.touchable == Touchable.enabled) _root.fire(event)
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
  override def touchDragged(screenX: Pixels, screenY: Pixels, pointer: Int): Boolean = {
    pointerScreenX(pointer) = screenX.toInt
    pointerScreenY(pointer) = screenY.toInt
    mouseScreenX = screenX.toInt
    mouseScreenY = screenY.toInt

    if (touchFocuses.isEmpty) false
    else {
      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain[InputEvent]
      event.eventType = InputEvent.Type.touchDragged
      event.stage = Nullable(this)
      event.stageX = tempCoords.x
      event.stageY = tempCoords.y
      event.pointer = pointer

      val snapshot = touchFocuses.toArray
      var i        = 0
      while (i < snapshot.length) {
        val focus = snapshot(i)
        if (focus.pointer == pointer && touchFocuses.contains(focus)) {
          event.target = focus.target
          event.listenerActor = focus.listenerActor
          if (focus.listener.get.handle(event)) event.handle()
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
  override def touchUp(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
    pointerTouched(pointer) = false
    pointerScreenX(pointer) = screenX.toInt
    pointerScreenY(pointer) = screenY.toInt

    if (touchFocuses.isEmpty) false
    else {
      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain[InputEvent]
      event.eventType = InputEvent.Type.touchUp
      event.stage = Nullable(this)
      event.stageX = tempCoords.x
      event.stageY = tempCoords.y
      event.pointer = pointer
      event.button = button

      val snapshot = touchFocuses.toArray
      var i        = 0
      while (i < snapshot.length) {
        val focus = snapshot(i)
        if (focus.pointer == pointer && focus.button == button) {
          val idx = touchFocuses.indexOf(focus)
          if (idx >= 0) {
            touchFocuses.removeIndex(idx)
            event.target = focus.target
            event.listenerActor = focus.listenerActor
            if (focus.listener.get.handle(event)) event.handle()
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

  override def touchCancelled(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
    cancelTouchFocus()
    false
  }

  /** Applies a mouse moved event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event. This event only occurs on the desktop.
    */
  override def mouseMoved(screenX: Pixels, screenY: Pixels): Boolean = {
    mouseScreenX = screenX.toInt
    mouseScreenY = screenY.toInt

    if (!isInsideViewport(screenX.toInt, screenY.toInt)) false
    else {
      screenToStageCoordinates(tempCoords.set(screenX.toFloat, screenY.toFloat))

      val event = pools.obtain[InputEvent]
      event.eventType = InputEvent.Type.mouseMoved
      event.stage = Nullable(this)
      event.stageX = tempCoords.x
      event.stageY = tempCoords.y

      var target = hit(tempCoords.x, tempCoords.y, true)
      if (target.isEmpty) target = Nullable(_root)

      target.foreach(_.fire(event))
      val handled = event.isHandled
      pools.free(event)
      handled
    }
  }

  /** Applies a mouse scroll event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event. This event only occurs on the desktop.
    */
  override def scrolled(amountX: Float, amountY: Float): Boolean = {
    val target: Actor = _scrollFocus.getOrElse(_root)

    screenToStageCoordinates(tempCoords.set(mouseScreenX.toFloat, mouseScreenY.toFloat))

    val event = pools.obtain[InputEvent]
    event.eventType = InputEvent.Type.scrolled
    event.stage = Nullable(this)
    event.stageX = tempCoords.x
    event.stageY = tempCoords.y
    event.scrollAmountX = amountX
    event.scrollAmountY = amountY
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Applies a key down event to the actor that has {@link Stage#setKeyboardFocus(Actor) keyboard focus}, if any, and returns true if the event was {@link Event#handle() handled}.
    */
  override def keyDown(keyCode: Key): Boolean = {
    val target: Actor = _keyboardFocus.getOrElse(_root)
    val event = pools.obtain[InputEvent]
    event.eventType = InputEvent.Type.keyDown
    event.stage = Nullable(this)
    event.keyCode = keyCode
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Applies a key up event to the actor that has {@link Stage#setKeyboardFocus(Actor) keyboard focus}, if any, and returns true if the event was {@link Event#handle() handled}.
    */
  override def keyUp(keyCode: Key): Boolean = {
    val target: Actor = _keyboardFocus.getOrElse(_root)
    val event = pools.obtain[InputEvent]
    event.eventType = InputEvent.Type.keyUp
    event.stage = Nullable(this)
    event.keyCode = keyCode
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Applies a key typed event to the actor that has {@link Stage#setKeyboardFocus(Actor) keyboard focus}, if any, and returns true if the event was {@link Event#handle() handled}.
    */
  override def keyTyped(character: Char): Boolean = {
    val target: Actor = _keyboardFocus.getOrElse(_root)
    val event = pools.obtain[InputEvent]
    event.eventType = InputEvent.Type.keyTyped
    event.stage = Nullable(this)
    event.character = character
    target.fire(event)
    val handled = event.isHandled
    pools.free(event)
    handled
  }

  /** Adds the listener to be notified for all touchDragged and touchUp events for the specified pointer and button. Touch focus is added automatically when true is returned from {@link
    * InputListener#touchDown(InputEvent, float, float, int, int) touchDown}. The specified actors will be used as the {@link Event#getListenerActor() listener actor} and
    * {@link Event#getTarget() target} for the touchDragged and touchUp events.
    */
  def addTouchFocus(listener: EventListener, listenerActor: Actor, target: Actor, pointer: Int, button: Button): Unit = {
    val focus = pools.obtain[Stage.TouchFocus]
    focus.listenerActor = Nullable(listenerActor)
    focus.target = Nullable(target)
    focus.listener = Nullable(listener)
    focus.pointer = pointer
    focus.button = button
    touchFocuses.add(focus)
  }

  /** Removes touch focus for the specified listener, pointer, and button. Note the listener will not receive a touchUp event when this method is used.
    */
  def removeTouchFocus(listener: EventListener, listenerActor: Actor, target: Actor, pointer: Int, button: Button): Unit = {
    var i = touchFocuses.size - 1
    while (i >= 0) {
      val focus = touchFocuses(i)
      if (
        focus.listener.exists(_ eq listener) && focus.listenerActor.exists(_ eq listenerActor) && focus.target.exists(_ eq target)
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
      if (focus.listenerActor.exists(_ eq listenerActor)) {
        val idx = touchFocuses.indexOf(focus)
        if (idx >= 0) {
          touchFocuses.removeIndex(idx)

          if (event.isEmpty) {
            val e = pools.obtain[InputEvent]
            e.eventType = InputEvent.Type.touchUp
            e.stage = Nullable(this)
            e.stageX = Int.MinValue
            e.stageY = Int.MinValue
            event = Nullable(e)
          }

          event.foreach { e =>
            e.target = focus.target
            e.listenerActor = focus.listenerActor
            e.pointer = focus.pointer
            e.button = focus.button
            focus.listener.get.handle(e)
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
    val event = pools.obtain[InputEvent]
    event.eventType = InputEvent.Type.touchUp
    event.stage = Nullable(this)
    event.stageX = Int.MinValue
    event.stageY = Int.MinValue

    // Cancel all current touch focuses except for the specified listener, allowing for concurrent modification, and never
    // cancel the same focus twice.
    val snapshot = touchFocuses.toArray
    var i        = 0
    while (i < snapshot.length) {
      val focus       = snapshot(i)
      val isException = focus.listener.exists(fl => exceptListener.exists(_ eq fl)) && focus.listenerActor.exists(fla => exceptActor.exists(_ eq fla))
      if (!isException) {
        val idx = touchFocuses.indexOf(focus)
        if (idx >= 0) {
          touchFocuses.removeIndex(idx)
          event.target = focus.target
          event.listenerActor = focus.listenerActor
          event.pointer = focus.pointer
          event.button = focus.button
          focus.listener.get.handle(event)
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
  def addActor(actor: Actor): Unit = _root.addActor(actor)

  /** Adds an action to the root of the stage.
    * @see
    *   Group#addAction(Action)
    */
  def addAction(action: Action): Unit = _root.addAction(action)

  /** Returns the root's child actors.
    * @see
    *   Group#children
    */
  def actors: DynamicArray[Actor] = _root.children

  /** Adds a listener to the root.
    * @see
    *   Actor#addListener(EventListener)
    */
  def addListener(listener: EventListener): Boolean = _root.addListener(listener)

  /** Removes a listener from the root.
    * @see
    *   Actor#removeListener(EventListener)
    */
  def removeListener(listener: EventListener): Boolean = _root.removeListener(listener)

  /** Adds a capture listener to the root.
    * @see
    *   Actor#addCaptureListener(EventListener)
    */
  def addCaptureListener(listener: EventListener): Boolean = _root.addCaptureListener(listener)

  /** Removes a listener from the root.
    * @see
    *   Actor#removeCaptureListener(EventListener)
    */
  def removeCaptureListener(listener: EventListener): Boolean = _root.removeCaptureListener(listener)

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
    _root.clear()
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
    _scrollFocus.foreach { sf =>
      if (sf.isDescendantOf(actor)) setScrollFocus(Nullable.empty)
    }
    _keyboardFocus.foreach { kf =>
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
    if (_keyboardFocus == actor) true
    else {
      val event = pools.obtain[FocusListener.FocusEvent]
      event.stage = Nullable(this)
      event.focusType = FocusListener.FocusEvent.Type.keyboard
      val oldKeyboardFocus = _keyboardFocus
      oldKeyboardFocus.foreach { old =>
        event.focused = false
        event.relatedActor = actor
        old.fire(event)
      }
      var success = !event.isCancelled
      if (success) {
        _keyboardFocus = actor
        actor.foreach { a =>
          event.focused = true
          event.relatedActor = oldKeyboardFocus
          a.fire(event)
          success = !event.isCancelled
          if (!success) _keyboardFocus = oldKeyboardFocus
        }
      }
      pools.free(event)
      success
    }

  /** Gets the actor that will receive key events.
    * @return
    *   May be null.
    */
  def keyboardFocus: Nullable[Actor] = _keyboardFocus

  /** Sets the actor that will receive scroll events.
    * @param actor
    *   May be null.
    * @return
    *   true if the unfocus and focus events were not cancelled by a {@link FocusListener}.
    */
  def setScrollFocus(actor: Nullable[Actor]): Boolean =
    if (_scrollFocus == actor) true
    else {
      val event = pools.obtain[FocusListener.FocusEvent]
      event.stage = Nullable(this)
      event.focusType = FocusListener.FocusEvent.Type.scroll
      val oldScrollFocus = _scrollFocus
      oldScrollFocus.foreach { old =>
        event.focused = false
        event.relatedActor = actor
        old.fire(event)
      }
      var success = !event.isCancelled
      if (success) {
        _scrollFocus = actor
        actor.foreach { a =>
          event.focused = true
          event.relatedActor = oldScrollFocus
          a.fire(event)
          success = !event.isCancelled
          if (!success) _scrollFocus = oldScrollFocus
        }
      }
      pools.free(event)
      success
    }

  /** Gets the actor that will receive scroll events.
    * @return
    *   May be null.
    */
  def scrollFocus: Nullable[Actor] = _scrollFocus

  def viewport: Viewport = _viewport

  def viewport_=(viewport: Viewport): Unit =
    this._viewport = viewport

  /** The viewport's world width. */
  def width: Float = _viewport.worldWidth.toFloat

  /** The viewport's world height. */
  def height: Float = _viewport.worldHeight.toFloat

  /** The viewport's camera. */
  def camera: Camera = _viewport.camera

  /** Returns the root group which holds all actors in the stage. */
  def root: Group = _root

  /** Replaces the root group. This can be useful, for example, to subclass the root group to be notified by {@link Group#childrenChanged()}.
    */
  def setRoot(root: Group): Unit = {
    root.parent.foreach(_.removeActor(root, unfocus = false))
    this._root = root
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
    _root.parentToLocalCoordinates(tempCoords.set(stageX, stageY))
    _root.hit(tempCoords.x, tempCoords.y, touchable)
  }

  /** Transforms the screen coordinates to stage coordinates.
    * @param screenCoords
    *   Input screen coordinates and output for resulting stage coordinates.
    */
  def screenToStageCoordinates(screenCoords: Vector2): Vector2 = {
    _viewport.unproject(screenCoords)
    screenCoords
  }

  /** Transforms the stage coordinates to screen coordinates.
    * @param stageCoords
    *   Input stage coordinates and output for resulting screen coordinates.
    */
  def stageToScreenCoordinates(stageCoords: Vector2): Vector2 = {
    _viewport.project(stageCoords)
    stageCoords.y = Sge().graphics.height.toFloat - stageCoords.y
    stageCoords
  }

  /** Transforms the coordinates to screen coordinates. The coordinates can be anywhere in the stage since the transform matrix describes how to convert them. The transform matrix is typically
    * obtained from {@link Batch#getTransformMatrix()} during {@link Actor#draw(Batch, float)}.
    * @see
    *   Actor#localToStageCoordinates(Vector2)
    */
  def toScreenCoordinates(coords: Vector2, transformMatrix: Matrix4): Vector2 =
    _viewport.toScreenCoordinates(coords, transformMatrix)

  /** Calculates window scissor coordinates from local coordinates using the batch's current transformation matrix.
    * @see
    *   ScissorStack#calculateScissors(Camera, float, float, float, float, Matrix4, Rectangle, Rectangle)
    */
  def calculateScissors(localRect: Rectangle, scissorRect: Rectangle): Unit = {
    val transformMatrix =
      debugShapes.fold(batch.transformMatrix) { shapes =>
        if (shapes.isDrawing) shapes.transformMatrix
        else batch.transformMatrix
      }
    _viewport.calculateScissors(transformMatrix, localRect, scissorRect)
  }

  /** If true, any actions executed during a call to {@link #act()}) will result in a call to {@link Graphics#requestRendering()}. Widgets that animate or otherwise require additional rendering may
    * check this setting before calling {@link Graphics#requestRendering()}. Default is true.
    */
  def actionsRequestRendering_=(actionsRequestRendering: Boolean): Unit =
    this._actionsRequestRendering = actionsRequestRendering

  def actionsRequestRendering: Boolean = _actionsRequestRendering

  /** The default color that can be used by actors to draw debug lines. */
  def debugColor: Color = _debugColor

  /** If true, debug lines are shown for actors even when {@link Actor#isVisible()} is false. */
  def setDebugInvisible(debugInvisible: Boolean): Unit =
    this.debugInvisible = debugInvisible

  /** If true, debug lines are shown for all actors. */
  def setDebugAll(debugAll: Boolean): Unit =
    if (this._debugAll != debugAll) {
      this._debugAll = debugAll
      if (debugAll)
        Stage.debug = true
      else
        _root.setDebug(false, true)
    }

  def debugAll: Boolean = _debugAll

  /** If true, debug is enabled only for the actor under the mouse. Can be combined with {@link #setDebugAll(boolean)}. */
  def setDebugUnderMouse(debugUnderMouse: Boolean): Unit =
    if (this.debugUnderMouse != debugUnderMouse) {
      this.debugUnderMouse = debugUnderMouse
      if (debugUnderMouse)
        Stage.debug = true
      else
        _root.setDebug(false, true)
    }

  /** If true, debug is enabled only for the parent of the actor under the mouse. Can be combined with {@link #setDebugAll(boolean)}.
    */
  def setDebugParentUnderMouse(debugParentUnderMouse: Boolean): Unit =
    if (this.debugParentUnderMouse != debugParentUnderMouse) {
      this.debugParentUnderMouse = debugParentUnderMouse
      if (debugParentUnderMouse)
        Stage.debug = true
      else
        _root.setDebug(false, true)
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
        _root.setDebug(false, true)
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
    val x0       = _viewport.screenX.toInt
    val x1       = x0 + _viewport.screenWidth.toInt
    val y0       = _viewport.screenY.toInt
    val y1       = y0 + _viewport.screenHeight.toInt
    val flippedY = Sge().graphics.height.toInt - 1 - screenY
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
    var listener:      Nullable[EventListener] = Nullable.empty
    var listenerActor: Nullable[Actor]         = Nullable.empty
    var target:        Nullable[Actor]         = Nullable.empty
    var pointer:       Int                     = 0
    var button:        Button                  = Button(0)

    def reset(): Unit = {
      listenerActor = Nullable.empty
      listener = Nullable.empty
      target = Nullable.empty
    }
  }
}
