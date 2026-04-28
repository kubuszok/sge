/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 337
 * Covenant-baseline-methods: APPROVE,BLOCK_INPUT,CANCEL,DEFAULT_ALPHA,DEFAULT_FADING_TIME,DEFAULT_LISTENER,DEFAULT_MOVING_TIME,DragAdapter,DragListener,Draggable,INVISIBLE_ON_DRAG,KEEP_WITHIN_PARENT,LAST_POSITION,MIMIC_COORDINATES,MimicActor,STAGE_COORDINATES,_alpha,_blocker,_deadzoneRadius,_offsetX,_offsetY,actor,addBlocker,addMimicHidingAction,alpha,alpha_,attachMimic,attachTo,b,blockInput,blocker,deadzoneRadius,deadzoneRadius_,dragStartX,dragStartY,draw,fadingInterpolation,fadingTime,getFadingTime,getMovingTime,getStageCoordinates,getStageCoordinatesWithDeadzone,getStageCoordinatesWithOffset,getStageCoordinatesWithinParent,handled,i,invisibleWhenDragged,isBlockingInput,isDisabled,isDragged,isInvisibleWhenDragged,isKeptWithinParent,isValid,isWithinDeadzone,keepWithinParent,listener,listener_,listeners,mimic,mouseMoved,movingInterpolation,movingTime,offsetX,offsetY,onDrag,onEnd,onStart,remove,removeBlocker,scrolled,setBlockInput,setFadingInterpolation,setFadingTime,setInvisibleWhenDragged,setKeepWithinParent,setMovingInterpolation,setMovingTime,this,touchDown,touchDragged,touchUp,updateSize
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/Draggable.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.graphics.g2d.Batch
import sge.math.{ Interpolation, MathUtils, Vector2 }
import sge.utils.Seconds
import sge.scenes.scene2d.{ Action, Actor, InputEvent, InputListener, Stage, Touchable }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.utils.Disableable
import sge.utils.Nullable

/** Draws copies of dragged actors which have this listener attached.
  * @author
  *   MJ
  * @since 0.9.3
  */
class Draggable(private var _listener: Nullable[Draggable.DragListener])(using Sge) extends InputListener {
  import Draggable._

  // Settings.
  private var blockInput:           Boolean       = BLOCK_INPUT
  private var invisibleWhenDragged: Boolean       = INVISIBLE_ON_DRAG
  private var keepWithinParent:     Boolean       = KEEP_WITHIN_PARENT
  private var _deadzoneRadius:      Float         = 0f
  private var fadingTime:           Float         = DEFAULT_FADING_TIME
  private var movingTime:           Float         = DEFAULT_MOVING_TIME
  private var _alpha:               Float         = DEFAULT_ALPHA
  private var fadingInterpolation:  Interpolation = Interpolation.fade
  private var movingInterpolation:  Interpolation = Interpolation.sineOut

  // Control variables.
  private val mimic:      MimicActor = new MimicActor()
  private var dragStartX: Float      = 0f
  private var dragStartY: Float      = 0f
  private var _offsetX:   Float      = 0f
  private var _offsetY:   Float      = 0f

  mimic.touchable = Touchable.disabled

  /** Creates a new draggable with default listener. */
  def this()(using Sge) = this(Nullable(Draggable.DEFAULT_LISTENER))

  /** @param actor will have this listener attached and all other [[Draggable]] listeners removed. */
  def attachTo(actor: Actor): Unit = {
    val listeners = actor.listeners
    var i         = listeners.size - 1
    while (i >= 0) {
      listeners(i) match {
        case _: Draggable => listeners.removeIndex(i)
        case _ => ()
      }
      i -= 1
    }
    actor.addListener(this)
  }

  def offsetX: Float = _offsetX
  def offsetY: Float = _offsetY

  def alpha:                 Float = _alpha
  def alpha_=(value: Float): Unit  = _alpha = value

  def isBlockingInput:               Boolean = blockInput
  def setBlockInput(value: Boolean): Unit    = blockInput = value

  def isInvisibleWhenDragged:                  Boolean = invisibleWhenDragged
  def setInvisibleWhenDragged(value: Boolean): Unit    = invisibleWhenDragged = value

  def isKeptWithinParent:                  Boolean = keepWithinParent
  def setKeepWithinParent(value: Boolean): Unit    = keepWithinParent = value

  def deadzoneRadius:                 Float = _deadzoneRadius
  def deadzoneRadius_=(value: Float): Unit  = _deadzoneRadius = value

  def getFadingTime:               Float = fadingTime
  def setFadingTime(value: Float): Unit  = fadingTime = value

  def getMovingTime:               Float = movingTime
  def setMovingTime(value: Float): Unit  = movingTime = value

  def setMovingInterpolation(interp: Interpolation): Unit = movingInterpolation = interp
  def setFadingInterpolation(interp: Interpolation): Unit = fadingInterpolation = interp

  def listener:                              Nullable[Draggable.DragListener] = _listener
  def listener_=(l: Draggable.DragListener): Unit                             = _listener = Nullable(l)

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean =
    event.listenerActor match {
      case la if la.isDefined =>
        val actor = la.get
        if (!isValid(actor) || isDisabled(actor)) false
        else if (_listener.isEmpty || _listener.get.onStart(this, actor, event.stageX, event.stageY)) {
          attachMimic(actor, event, x, y)
          true
        } else {
          false
        }
      case _ => false
    }

  protected def isValid(actor: Actor): Boolean =
    actor.stage.isDefined

  protected def isDisabled(actor: Actor): Boolean =
    actor match {
      case d: Disableable => d.disabled
      case _ => false
    }

  protected def attachMimic(actor: Actor, event: InputEvent, x: Float, y: Float): Unit = {
    mimic.clearActions()
    mimic.color.a = _alpha
    mimic.actor = Nullable(actor)
    _offsetX = -x
    _offsetY = -y
    getStageCoordinates(event)
    dragStartX = MIMIC_COORDINATES.x
    dragStartY = MIMIC_COORDINATES.y
    mimic.setPosition(dragStartX, dragStartY)
    actor.stage.foreach { stg =>
      stg.addActor(mimic)
      mimic.toFront()
    }
    actor.visible = !invisibleWhenDragged
    if (blockInput) {
      actor.stage.foreach(addBlocker)
    }
  }

  protected def getStageCoordinates(event: InputEvent): Unit =
    if (keepWithinParent) {
      getStageCoordinatesWithinParent(event)
    } else if (_deadzoneRadius > 0f) {
      getStageCoordinatesWithDeadzone(event)
    } else {
      getStageCoordinatesWithOffset(event)
    }

  private def getStageCoordinatesWithDeadzone(event: InputEvent): Unit = {
    var handled = false
    mimic.actor.flatMap(_.parent).foreach { par =>
      MIMIC_COORDINATES.set(new Vector2(0, 0))
      par.localToStageCoordinates(MIMIC_COORDINATES)
      val parentX    = MIMIC_COORDINATES.x
      val parentY    = MIMIC_COORDINATES.y
      val parentEndX = parentX + par.width
      val parentEndY = parentY + par.height
      if (isWithinDeadzone(event, parentX, parentY, parentEndX, parentEndY)) {
        MIMIC_COORDINATES.set(event.stageX + _offsetX, event.stageY + _offsetY)
        if (MIMIC_COORDINATES.x < parentX) MIMIC_COORDINATES.x = parentX
        else if (MIMIC_COORDINATES.x + mimic.width > parentEndX) MIMIC_COORDINATES.x = parentEndX - mimic.width
        if (MIMIC_COORDINATES.y < parentY) MIMIC_COORDINATES.y = parentY
        else if (MIMIC_COORDINATES.y + mimic.height > parentEndY) MIMIC_COORDINATES.y = parentEndY - mimic.height
        STAGE_COORDINATES.set(MathUtils.clamp(event.stageX, parentX, parentEndX - 1f), MathUtils.clamp(event.stageY, parentY, parentEndY - 1f))
        handled = true
      }
    }
    if (!handled) getStageCoordinatesWithOffset(event)
  }

  private def isWithinDeadzone(event: InputEvent, parentX: Float, parentY: Float, parentEndX: Float, parentEndY: Float): Boolean =
    parentX - _deadzoneRadius <= event.stageX && parentEndX + _deadzoneRadius >= event.stageX &&
      parentY - _deadzoneRadius <= event.stageY && parentEndY + _deadzoneRadius >= event.stageY

  private def getStageCoordinatesWithinParent(event: InputEvent): Unit = {
    var handled = false
    mimic.actor.flatMap(_.parent).foreach { par =>
      MIMIC_COORDINATES.set(new Vector2(0, 0))
      par.localToStageCoordinates(MIMIC_COORDINATES)
      val parentX    = MIMIC_COORDINATES.x
      val parentY    = MIMIC_COORDINATES.y
      val parentEndX = parentX + par.width
      val parentEndY = parentY + par.height
      MIMIC_COORDINATES.set(event.stageX + _offsetX, event.stageY + _offsetY)
      if (MIMIC_COORDINATES.x < parentX) MIMIC_COORDINATES.x = parentX
      else if (MIMIC_COORDINATES.x + mimic.width > parentEndX) MIMIC_COORDINATES.x = parentEndX - mimic.width
      if (MIMIC_COORDINATES.y < parentY) MIMIC_COORDINATES.y = parentY
      else if (MIMIC_COORDINATES.y + mimic.height > parentEndY) MIMIC_COORDINATES.y = parentEndY - mimic.height
      STAGE_COORDINATES.set(MathUtils.clamp(event.stageX, parentX, parentEndX - 1f), MathUtils.clamp(event.stageY, parentY, parentEndY - 1f))
      handled = true
    }
    if (!handled) getStageCoordinatesWithOffset(event)
  }

  private def getStageCoordinatesWithOffset(event: InputEvent): Unit = {
    MIMIC_COORDINATES.set(event.stageX + _offsetX, event.stageY + _offsetY)
    STAGE_COORDINATES.set(event.stageX, event.stageY)
  }

  override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
    if (isDragged) {
      getStageCoordinates(event)
      mimic.setPosition(MIMIC_COORDINATES.x, MIMIC_COORDINATES.y)
      _listener.foreach(_.onDrag(this, mimic.actor.get, STAGE_COORDINATES.x, STAGE_COORDINATES.y))
    }

  override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
    if (isDragged) {
      removeBlocker()
      getStageCoordinates(event)
      mimic.setPosition(MIMIC_COORDINATES.x, MIMIC_COORDINATES.y)
      if (
        _listener.isEmpty || (mimic.actor.isDefined && mimic.actor.get.stage.isDefined &&
          _listener.get.onEnd(this, mimic.actor.get, STAGE_COORDINATES.x, STAGE_COORDINATES.y))
      ) {
        // Drag end approved - fading out.
        addMimicHidingAction(Actions.fadeOut(Seconds(fadingTime), fadingInterpolation), fadingTime)
      } else {
        // Drag end cancelled - returning to the original position.
        addMimicHidingAction(Actions.moveTo(dragStartX, dragStartY, Seconds(movingTime), movingInterpolation), movingTime)
      }
    }

  /** @return true if some actor with this listener attached is currently dragged. */
  def isDragged: Boolean = mimic.actor.isDefined

  protected def addMimicHidingAction(hidingAction: Action, delay: Float): Unit = {
    mimic.addAction(Actions.sequence(hidingAction, Actions.removeActor()))
    mimic.actor.foreach(_.addAction(Actions.delay(Seconds(delay), Actions.visible(true))))
  }
}

object Draggable {
  private val MIMIC_COORDINATES: Vector2 = new Vector2()
  private val STAGE_COORDINATES: Vector2 = new Vector2()

  /** Initial fading time value of dragged actors. */
  var DEFAULT_FADING_TIME: Float = 0.1f

  /** Initial moving time value of dragged actors. */
  var DEFAULT_MOVING_TIME: Float = 0.1f

  /** Initial invisibility setting of dragged actors. */
  var INVISIBLE_ON_DRAG: Boolean = false

  /** Initial setting of keeping the dragged widget within its parent's bounds. */
  var KEEP_WITHIN_PARENT: Boolean = false

  /** Initial alpha setting of dragged actors. */
  var DEFAULT_ALPHA: Float = 1f

  /** Initial listener of draggables, unless a different listener is specified in the constructor. */
  var DEFAULT_LISTENER: DragListener = new Draggable.DragAdapter()

  /** If true, other actors will not receive mouse events while the actor is dragged. */
  var BLOCK_INPUT: Boolean = true

  /** Blocks mouse input during dragging. Lazily initialized. */
  private var _blocker: Nullable[Actor] = Nullable.empty

  private def blocker(using Sge): Actor = {
    if (_blocker.isEmpty) {
      val a = new Actor() {}
      a.addListener(
        new InputListener() {
          override def mouseMoved(event: InputEvent, x: Float, y: Float):                                            Boolean = true
          override def touchDown(event:  InputEvent, x: Float, y: Float, pointer: Int, button:    sge.Input.Button): Boolean = true
          override def scrolled(event:   InputEvent, x: Float, y: Float, amountX: Float, amountY: Float):            Boolean = true
        }
      )
      _blocker = Nullable(a)
    }
    _blocker.get
  }

  def addBlocker(stage: Stage)(using Sge): Unit = {
    val b = blocker
    stage.addActor(b)
    b.setBounds(0f, 0f, stage.width, stage.height)
    b.toFront()
  }

  def removeBlocker()(using Sge): Unit =
    _blocker.foreach(_.remove())

  /** Allows to control [[Draggable]] behavior.
    * @author
    *   MJ
    * @since 0.9.3
    */
  trait DragListener {
    val CANCEL:  Boolean = false
    val APPROVE: Boolean = true

    def onStart(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean
    def onDrag(draggable:  Draggable, actor: Actor, stageX: Float, stageY: Float): Unit
    def onEnd(draggable:   Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean
  }

  /** Default, empty implementation of [[DragListener]]. Approves all drag requests.
    * @author
    *   MJ
    * @since 0.9.3
    */
  class DragAdapter extends DragListener {
    override def onStart(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean = APPROVE
    override def onDrag(draggable:  Draggable, actor: Actor, stageX: Float, stageY: Float): Unit    = ()
    override def onEnd(draggable:   Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean = APPROVE
  }

  /** Draws the chosen actor with modified alpha value in a custom position. Clears mimicked actor upon removing from the stage.
    * @author
    *   MJ
    * @since 0.9.3
    */
  class MimicActor(using Sge) extends Actor {
    private val LAST_POSITION: Vector2         = new Vector2()
    var actor:                 Nullable[Actor] = Nullable.empty

    override def remove(): Boolean = {
      actor = Nullable.empty
      super.remove()
    }

    // Note: width/height are vars inherited from Actor; we update them when actor changes
    def updateSize(): Unit =
      actor.foreach { a => this.width = a.width; this.height = a.height }

    override def draw(batch: Batch, parentAlpha: Float): Unit =
      actor.foreach { a =>
        LAST_POSITION.set(a.x, a.y)
        a.setPosition(x, y)
        a.draw(batch, color.a * parentAlpha)
        a.setPosition(LAST_POSITION.x, LAST_POSITION.y)
      }
  }
}
