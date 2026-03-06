/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/Actor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: DelayedRemovalArray -> DynamicArray + manual deferred removal; Align int -> Align opaque type;
 *     Object userObject -> AnyRef userObject; debug field -> _debug (avoid keyword clash)
 *   Convention: null -> Nullable[A]; no return (boundary/break); split packages; (using Sge) on constructor (class-level context)
 *   Idiom: Alignment bitfield ops -> Align methods (isRight, isLeft, etc.); do-while -> while with Nullable;
 *     POOLS static init block -> companion object vals; ancestorsVisible() deprecated method dropped
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (~15 pairs: setX/getX, setVisible/isVisible, setTouchable/getTouchable, etc.)
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d

import sge.utils.{ Align, DynamicArray, MkArray, Nullable, PoolManager }

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.graphics.g2d.GlyphLayout
import sge.graphics.glutils.ShapeRenderer
import sge.math.{ MathUtils, Rectangle, Vector2 }
import sge.scenes.scene2d.utils.ScissorStack

/** 2D scene graph node. An actor has a position, rectangular size, origin, scale, rotation, Z index, and color. The position corresponds to the unrotated, unscaled bottom left corner of the actor.
  * The position is relative to the actor's parent. The origin is relative to the position and is used for scale and rotation. <p> An actor has a list of in progress {@link Action actions} that are
  * applied to the actor (often over time). These are generally used to change the presentation of the actor (moving it, resizing it, etc). See {@link #act(float)}, {@link Action}, and its many
  * subclasses. <p> An actor has two kinds of listeners associated with it: "capture" and regular. The listeners are notified of events the actor or its children receive. The regular listeners are
  * designed to allow an actor to respond to events that have been delivered. The capture listeners are designed to allow a parent or container actor to handle events before child actors. See
  * {@link #fire} for more details. <p> An {@link InputListener} can receive all the basic input events. More complex listeners (like {@link ClickListener} and {@link ActorGestureListener}) can listen
  * for and combine primitive events and recognize complex interactions like multi-touch or pinch.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class Actor()(using Sge) {

  private var stage:            Nullable[Stage]             = Nullable.empty
  private[scene2d] var parent:  Nullable[Group]             = Nullable.empty
  private val listeners:        DynamicArray[EventListener] = DynamicArray[EventListener]()
  private val captureListeners: DynamicArray[EventListener] = DynamicArray[EventListener]()
  private val actions:          DynamicArray[Action]        = DynamicArray[Action]()

  // Deferred removal support for listeners
  private var iteratingListeners:             Int                         = 0
  private var iteratingCaptureListeners:      Int                         = 0
  private val pendingListenerRemovals:        DynamicArray[EventListener] = DynamicArray[EventListener]()
  private val pendingCaptureListenerRemovals: DynamicArray[EventListener] = DynamicArray[EventListener]()

  private var name:       Nullable[String] = Nullable.empty
  private var touchable:  Touchable        = Touchable.enabled
  private var visible:    Boolean          = true
  private var _debug:     Boolean          = false
  var x:                  Float            = 0
  var y:                  Float            = 0
  var width:              Float            = 0
  var height:             Float            = 0
  var originX:            Float            = 0
  var originY:            Float            = 0
  var scaleX:             Float            = 1
  var scaleY:             Float            = 1
  var rotation:           Float            = 0
  val color:              Color            = Color(1, 1, 1, 1)
  private var userObject: Nullable[AnyRef] = Nullable.empty

  /** Draws the actor. The batch is configured to draw in the parent's coordinate system. {@link Batch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion, float, float, float, float, float, float,
    * float, float, float) This draw method} is convenient to draw a rotated and scaled TextureRegion. {@link Batch#begin()} has already been called on the batch. If {@link Batch#end()} is called to
    * draw without the batch then {@link Batch#begin()} must be called before the method returns. <p> The default implementation does nothing.
    * @param parentAlpha
    *   The parent alpha, to be multiplied with this actor's alpha, allowing the parent's alpha to affect all children.
    */
  def draw(batch: Batch, parentAlpha: Float): Unit = {}

  /** Updates the actor based on time. Typically this is called each frame by {@link Stage#act(float)}. <p> The default implementation calls {@link Action#act(float)} on each action and removes
    * actions that are complete.
    * @param delta
    *   Time in seconds since the last frame.
    */
  def act(delta: Float): Unit =
    if (actions.nonEmpty) {
      stage.foreach { s =>
        if (s.getActionsRequestRendering) Sge().graphics.requestRendering()
      }
      try {
        var i = 0
        while (i < actions.size) {
          val action = actions(i)
          if (action.act(delta) && i < actions.size) {
            val current     = actions(i)
            val actionIndex = if (current eq action) i else actions.indexOf(action)
            if (actionIndex != -1) {
              actions.removeIndex(actionIndex)
              action.setActor(Nullable.empty)
              i -= 1
            }
          }
          i += 1
        }
      } catch {
        case ex: RuntimeException =>
          val context = toString
          throw new RuntimeException("Actor: " + context.substring(0, Math.min(context.length, 128)), ex)
      }
    }

  /** Sets this actor as the event {@link Event#setTarget(Actor) target} and propagates the event to this actor and ascendants as necessary. If this actor is not in the stage, the stage must be set
    * before calling this method. <p> Events are fired in 2 phases: <ol> <li>The first phase (the "capture" phase) notifies listeners on each actor starting at the root and propagating down the
    * hierarchy to (and including) this actor.</li> <li>The second phase notifies listeners on each actor starting at this actor and, if {@link Event#getBubbles()} is true, propagating upward to the
    * root.</li> </ol> If the event is {@link Event#stop() stopped} at any time, it will not propagate to the next actor.
    * @return
    *   true if the event was {@link Event#cancel() cancelled}.
    */
  def fire(event: Event): Boolean = scala.util.boundary {
    if (event.getStage.isEmpty) stage.foreach(s => event.setStage(s))
    event.setTarget(this)

    // Collect ascendants so event propagation is unaffected by hierarchy changes.
    val ascendants = DynamicArray[Group]()
    var p          = this.parent
    p.foreach { pp =>
      ascendants.add(pp)
      p = pp.parent
    }
    while (p.isDefined)
      p.foreach { pp =>
        ascendants.add(pp)
        p = pp.parent
      }

    try {
      // Notify ascendants' capture listeners, starting at the root. Ascendants may stop an event before children receive it.
      var i = ascendants.size - 1
      while (i >= 0) {
        val currentTarget = ascendants(i)
        currentTarget.notify(event, capture = true)
        if (event.isStopped) scala.util.boundary.break(event.isCancelled)
        i -= 1
      }

      // Notify the target capture listeners.
      notify(event, capture = true)
      if (event.isStopped) scala.util.boundary.break(event.isCancelled)

      // Notify the target listeners.
      notify(event, capture = false)
      if (!event.getBubbles) scala.util.boundary.break(event.isCancelled)
      if (event.isStopped) scala.util.boundary.break(event.isCancelled)

      // Notify ascendants' actor listeners, starting at the target. Children may stop an event before ascendants receive it.
      i = 0
      val n = ascendants.size
      while (i < n) {
        ascendants(i).notify(event, capture = false)
        if (event.isStopped) scala.util.boundary.break(event.isCancelled)
        i += 1
      }

      event.isCancelled
    } finally
      ascendants.clear()
  }

  /** Notifies this actor's listeners of the event. The event is not propagated to any ascendants. The event {@link Event#setTarget(Actor) target} must be set before calling this method. Before
    * notifying the listeners, this actor is set as the {@link Event#getListenerActor() listener actor}. If this actor is not in the stage, the stage must be set before calling this method.
    * @param capture
    *   If true, the capture listeners will be notified instead of the regular listeners.
    * @return
    *   true if the event was {@link Event#cancel() cancelled}.
    */
  def notify(event: Event, capture: Boolean): Boolean = scala.util.boundary {
    if (Nullable(event.getTarget).isEmpty) throw new IllegalArgumentException("The event target cannot be null.")

    val listenersToNotify = if (capture) captureListeners else listeners
    if (listenersToNotify.isEmpty) scala.util.boundary.break(event.isCancelled)

    event.setListenerActor(this)
    event.setCapture(capture)
    if (event.getStage.isEmpty) stage.foreach(s => event.setStage(s))

    try {
      if (capture) iteratingCaptureListeners += 1 else iteratingListeners += 1
      val snapshot = listenersToNotify.toArray
      var i        = 0
      while (i < snapshot.length) {
        if (snapshot(i).handle(event)) event.handle()
        i += 1
      }
      if (capture) {
        iteratingCaptureListeners -= 1
        if (iteratingCaptureListeners == 0) {
          pendingCaptureListenerRemovals.foreach(l => captureListeners.removeValue(l))
          pendingCaptureListenerRemovals.clear()
        }
      } else {
        iteratingListeners -= 1
        if (iteratingListeners == 0) {
          pendingListenerRemovals.foreach(l => listeners.removeValue(l))
          pendingListenerRemovals.clear()
        }
      }
    } catch {
      case ex: RuntimeException =>
        val context = toString
        throw new RuntimeException("Actor: " + context.substring(0, Math.min(context.length, 128)), ex)
    }

    event.isCancelled
  }

  /** Returns the deepest {@link #isVisible() visible} (and optionally, {@link #getTouchable() touchable}) actor that contains the specified point, or null if no actor was hit. The point is specified
    * in the actor's local coordinate system (0,0 is the bottom left of the actor and width,height is the upper right). <p> This method is used to delegate touchDown, mouse, and enter/exit events. If
    * this method returns null, those events will not occur on this Actor. <p> The default implementation returns this actor if the point is within this actor's bounds and this actor is visible.
    * @param touchable
    *   If true, hit detection will respect the {@link #setTouchable(Touchable) touchability}.
    * @see
    *   Touchable
    */
  def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] =
    if (touchable && this.touchable != Touchable.enabled) Nullable.empty
    else if (!isVisible) Nullable.empty
    else if (x >= 0 && x < width && y >= 0 && y < height) Nullable(this)
    else Nullable.empty

  /** Removes this actor from its parent, if it has a parent.
    * @see
    *   Group#removeActor(Actor)
    */
  def remove(): Boolean =
    parent.exists(_.removeActor(this, unfocus = true))

  /** Add a listener to receive events that {@link #hit(float, float, boolean) hit} this actor. See {@link #fire(Event)}.
    * @see
    *   InputListener
    * @see
    *   ClickListener
    */
  def addListener(listener: EventListener): Boolean =
    if (!listeners.contains(listener)) {
      listeners.add(listener)
      true
    } else false

  def removeListener(listener: EventListener): Boolean =
    if (iteratingListeners > 0) {
      val removed = listeners.contains(listener)
      if (removed) pendingListenerRemovals.add(listener)
      removed
    } else {
      val idx = listeners.indexOf(listener)
      if (idx >= 0) { listeners.removeIndex(idx); true }
      else false
    }

  def getListeners: DynamicArray[EventListener] = listeners

  /** Adds a listener that is only notified during the capture phase.
    * @see
    *   #fire(Event)
    */
  def addCaptureListener(listener: EventListener): Boolean = {
    if (!captureListeners.contains(listener)) captureListeners.add(listener)
    true
  }

  def removeCaptureListener(listener: EventListener): Boolean =
    if (iteratingCaptureListeners > 0) {
      val removed = captureListeners.contains(listener)
      if (removed) pendingCaptureListenerRemovals.add(listener)
      removed
    } else {
      val idx = captureListeners.indexOf(listener)
      if (idx >= 0) { captureListeners.removeIndex(idx); true }
      else false
    }

  def getCaptureListeners: DynamicArray[EventListener] = captureListeners

  def addAction(action: Action): Unit = {
    action.setActor(Nullable(this))
    actions.add(action)

    stage.foreach { s =>
      if (s.getActionsRequestRendering) Sge().graphics.requestRendering()
    }
  }

  /** @param action May be null, in which case nothing is done. */
  def removeAction(action: Nullable[Action]): Unit =
    action.foreach { a =>
      val idx = actions.indexOf(a)
      if (idx >= 0) {
        actions.removeIndex(idx)
        a.setActor(Nullable.empty)
      }
    }

  def getActions: DynamicArray[Action] = actions

  /** Returns true if the actor has one or more actions. */
  def hasActions: Boolean = actions.nonEmpty

  /** Removes all actions on this actor. */
  def clearActions(): Unit = {
    var i = actions.size - 1
    while (i >= 0) {
      actions(i).setActor(Nullable.empty)
      i -= 1
    }
    actions.clear()
  }

  /** Removes all listeners on this actor. */
  def clearListeners(): Unit = {
    listeners.clear()
    captureListeners.clear()
  }

  /** Removes all actions and listeners on this actor. */
  def clear(): Unit = {
    clearActions()
    clearListeners()
  }

  /** Returns the stage that this actor is currently in, or null if not in a stage. */
  def getStage: Nullable[Stage] = stage

  /** Called by the framework when this actor or any ascendant is added to a group that is in the stage.
    * @param stage
    *   May be null if the actor or any ascendant is no longer in a stage.
    */
  protected[scene2d] def setStage(stage: Nullable[Stage]): Unit =
    this.stage = stage

  /** Returns true if this actor is the same as or is the descendant of the specified actor. */
  def isDescendantOf(actor: Actor): Boolean = scala.util.boundary {
    var p: Nullable[Actor] = Nullable(this)
    while (p.isDefined)
      p.foreach { a =>
        if (a eq actor) scala.util.boundary.break(true)
        p = a.parent.map(_.asInstanceOf[Actor])
      }
    false
  }

  /** Returns true if this actor is the same as or is the ascendant of the specified actor. */
  def isAscendantOf(actor: Actor): Boolean = scala.util.boundary {
    var a: Nullable[Actor] = Nullable(actor)
    while (a.isDefined)
      a.foreach { current =>
        if (current eq this) scala.util.boundary.break(true)
        a = current.parent.map(_.asInstanceOf[Actor])
      }
    false
  }

  /** Returns this actor or the first ascendant of this actor that is assignable with the specified type, or null if none were found.
    */
  def firstAscendant[T <: Actor](using tag: scala.reflect.ClassTag[T]): Nullable[T] = scala.util.boundary {
    var a: Nullable[Actor] = Nullable(this)
    while (a.isDefined)
      a.foreach { current =>
        if (tag.runtimeClass.isInstance(current)) scala.util.boundary.break(Nullable(current.asInstanceOf[T]))
        a = current.parent.map(_.asInstanceOf[Actor])
      }
    Nullable.empty
  }

  /** Returns true if the actor's parent is not null. */
  def hasParent: Boolean = parent.isDefined

  /** Returns the parent actor, or null if not in a group. */
  def getParent: Nullable[Group] = parent

  /** Called by the framework when an actor is added to or removed from a group.
    * @param parent
    *   May be null if the actor has been removed from the parent.
    */
  protected[scene2d] def setParent(parent: Nullable[Group]): Unit =
    this.parent = parent

  /** Returns true if input events are processed by this actor. */
  def isTouchable: Boolean = touchable == Touchable.enabled

  def getTouchable: Touchable = touchable

  /** Determines how touch events are distributed to this actor. Default is {@link Touchable#enabled}. */
  def setTouchable(touchable: Touchable): Unit =
    this.touchable = touchable

  def isVisible: Boolean = visible

  /** If false, the actor will not be drawn and will not receive touch events. Default is true. */
  def setVisible(visible: Boolean): Unit =
    this.visible = visible

  /** Returns true if this actor and all ascendants are visible. */
  def ascendantsVisible(): Boolean = scala.util.boundary {
    var a: Nullable[Actor] = Nullable(this)
    while (a.isDefined)
      a.foreach { actor =>
        if (!actor.isVisible) scala.util.boundary.break(false)
        a = actor.parent.map(_.asInstanceOf[Actor])
      }
    true
  }

  /** Returns true if this actor is the {@link Stage#getKeyboardFocus() keyboard focus} actor. */
  def hasKeyboardFocus: Boolean =
    getStage.exists(_.getKeyboardFocus.exists(_ eq this))

  /** Returns true if this actor is the {@link Stage#getScrollFocus() scroll focus} actor. */
  def hasScrollFocus: Boolean =
    getStage.exists(_.getScrollFocus.exists(_ eq this))

  /** Returns true if this actor is a target actor for touch focus.
    * @see
    *   Stage#addTouchFocus(EventListener, Actor, Actor, int, int)
    */
  def isTouchFocusTarget: Boolean =
    getStage.exists { stage =>
      stage.touchFocuses.exists(_.target eq this)
    }

  /** Returns true if this actor is a listener actor for touch focus.
    * @see
    *   Stage#addTouchFocus(EventListener, Actor, Actor, int, int)
    */
  def isTouchFocusListener: Boolean =
    getStage.exists { stage =>
      stage.touchFocuses.exists(_.listenerActor eq this)
    }

  /** Returns an application specific object for convenience, or null. */
  def getUserObject: Nullable[AnyRef] = userObject

  /** Sets an application specific object for convenience. */
  def setUserObject(userObject: Nullable[AnyRef]): Unit =
    this.userObject = userObject

  /** Returns the X position of the actor's left edge. */
  def getX: Float = x

  /** Returns the X position of the specified {@link Align alignment}. */
  def getX(alignment: Align): Float = {
    var result = this.x
    if (alignment.isRight)
      result += width
    else if (!alignment.isLeft) //
      result += width / 2
    result
  }

  def setX(x: Float): Unit =
    if (this.x != x) {
      this.x = x
      positionChanged()
    }

  /** Sets the x position using the specified {@link Align alignment}. Note this may set the position to non-integer coordinates.
    */
  def setX(x: Float, alignment: Align): Unit = {
    var adjusted = x
    if (alignment.isRight)
      adjusted -= width
    else if (!alignment.isLeft) //
      adjusted -= width / 2

    if (this.x != adjusted) {
      this.x = adjusted
      positionChanged()
    }
  }

  /** Returns the Y position of the actor's bottom edge. */
  def getY: Float = y

  def setY(y: Float): Unit =
    if (this.y != y) {
      this.y = y
      positionChanged()
    }

  /** Sets the y position using the specified {@link Align alignment}. Note this may set the position to non-integer coordinates.
    */
  def setY(y: Float, alignment: Align): Unit = {
    var adjusted = y
    if (alignment.isTop)
      adjusted -= height
    else if (!alignment.isBottom) //
      adjusted -= height / 2

    if (this.y != adjusted) {
      this.y = adjusted
      positionChanged()
    }
  }

  /** Returns the Y position of the specified {@link Align alignment}. */
  def getY(alignment: Align): Float = {
    var result = this.y
    if (alignment.isTop)
      result += height
    else if (!alignment.isBottom) //
      result += height / 2
    result
  }

  /** Sets the position of the actor's bottom left corner. */
  def setPosition(x: Float, y: Float): Unit =
    if (this.x != x || this.y != y) {
      this.x = x
      this.y = y
      positionChanged()
    }

  /** Sets the position using the specified {@link Align alignment}. Note this may set the position to non-integer coordinates.
    */
  def setPosition(x: Float, y: Float, alignment: Align): Unit = {
    var ax = x
    var ay = y
    if (alignment.isRight)
      ax -= width
    else if (!alignment.isLeft) //
      ax -= width / 2

    if (alignment.isTop)
      ay -= height
    else if (!alignment.isBottom) //
      ay -= height / 2

    if (this.x != ax || this.y != ay) {
      this.x = ax
      this.y = ay
      positionChanged()
    }
  }

  /** Add x and y to current position */
  def moveBy(x: Float, y: Float): Unit =
    if (x != 0 || y != 0) {
      this.x += x
      this.y += y
      positionChanged()
    }

  def getWidth: Float = width

  def setWidth(width: Float): Unit =
    if (this.width != width) {
      this.width = width
      sizeChanged()
    }

  def getHeight: Float = height

  def setHeight(height: Float): Unit =
    if (this.height != height) {
      this.height = height
      sizeChanged()
    }

  /** Returns y plus height. */
  def getTop: Float = y + height

  /** Returns x plus width. */
  def getRight: Float = x + width

  /** Called when the actor's position has been changed. */
  protected def positionChanged(): Unit = {}

  /** Called when the actor's size has been changed. */
  protected def sizeChanged(): Unit = {}

  /** Called when the actor's scale has been changed. */
  protected def scaleChanged(): Unit = {}

  /** Called when the actor's rotation has been changed. */
  protected def rotationChanged(): Unit = {}

  /** Sets the width and height. */
  def setSize(width: Float, height: Float): Unit =
    if (this.width != width || this.height != height) {
      this.width = width
      this.height = height
      sizeChanged()
    }

  /** Adds the specified size to the current size. */
  def sizeBy(size: Float): Unit =
    if (size != 0) {
      width += size
      height += size
      sizeChanged()
    }

  /** Adds the specified size to the current size. */
  def sizeBy(width: Float, height: Float): Unit =
    if (width != 0 || height != 0) {
      this.width += width
      this.height += height
      sizeChanged()
    }

  /** Set bounds the x, y, width, and height. */
  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    if (this.x != x || this.y != y) {
      this.x = x
      this.y = y
      positionChanged()
    }
    if (this.width != width || this.height != height) {
      this.width = width
      this.height = height
      sizeChanged()
    }
  }

  def getOriginX: Float = originX

  def setOriginX(originX: Float): Unit =
    this.originX = originX

  def getOriginY: Float = originY

  def setOriginY(originY: Float): Unit =
    this.originY = originY

  /** Sets the origin position which is relative to the actor's bottom left corner. */
  def setOrigin(originX: Float, originY: Float): Unit = {
    this.originX = originX
    this.originY = originY
  }

  /** Sets the origin position to the specified {@link Align alignment}. */
  def setOrigin(alignment: Align): Unit = {
    if (alignment.isLeft)
      originX = 0
    else if (alignment.isRight)
      originX = width
    else
      originX = width / 2

    if (alignment.isBottom)
      originY = 0
    else if (alignment.isTop)
      originY = height
    else
      originY = height / 2
  }

  def getScaleX: Float = scaleX

  def setScaleX(scaleX: Float): Unit =
    if (this.scaleX != scaleX) {
      this.scaleX = scaleX
      scaleChanged()
    }

  def getScaleY: Float = scaleY

  def setScaleY(scaleY: Float): Unit =
    if (this.scaleY != scaleY) {
      this.scaleY = scaleY
      scaleChanged()
    }

  /** Sets the scale for both X and Y */
  def setScale(scaleXY: Float): Unit =
    if (this.scaleX != scaleXY || this.scaleY != scaleXY) {
      this.scaleX = scaleXY
      this.scaleY = scaleXY
      scaleChanged()
    }

  /** Sets the scale X and scale Y. */
  def setScale(scaleX: Float, scaleY: Float): Unit =
    if (this.scaleX != scaleX || this.scaleY != scaleY) {
      this.scaleX = scaleX
      this.scaleY = scaleY
      scaleChanged()
    }

  /** Adds the specified scale to the current scale. */
  def scaleBy(scale: Float): Unit =
    if (scale != 0) {
      scaleX += scale
      scaleY += scale
      scaleChanged()
    }

  /** Adds the specified scale to the current scale. */
  def scaleBy(scaleX: Float, scaleY: Float): Unit =
    if (scaleX != 0 || scaleY != 0) {
      this.scaleX += scaleX
      this.scaleY += scaleY
      scaleChanged()
    }

  def getRotation: Float = rotation

  def setRotation(degrees: Float): Unit =
    if (this.rotation != degrees) {
      this.rotation = degrees
      rotationChanged()
    }

  /** Adds the specified rotation to the current rotation. */
  def rotateBy(amountInDegrees: Float): Unit =
    if (amountInDegrees != 0) {
      rotation = (rotation + amountInDegrees) % 360
      rotationChanged()
    }

  def setColor(color: Color): Unit =
    this.color.set(color)

  def setColor(r: Float, g: Float, b: Float, a: Float): Unit =
    color.set(r, g, b, a)

  /** Returns the color the actor will be tinted when drawn. The returned instance can be modified to change the color. */
  def getColor: Color = color

  /** @see
    *   #setName(String)
    * @return
    *   May be null.
    */
  def getName: Nullable[String] = name

  /** Set the actor's name, which is used for identification convenience and by {@link #toString()}.
    * @param name
    *   May be null.
    * @see
    *   Group#findActor(String)
    */
  def setName(name: Nullable[String]): Unit =
    this.name = name

  /** Changes the z-order for this actor so it is in front of all siblings. */
  def toFront(): Unit =
    setZIndex(Int.MaxValue)

  /** Changes the z-order for this actor so it is in back of all siblings. */
  def toBack(): Unit =
    setZIndex(0)

  /** Sets the z-index of this actor. The z-index is the index into the parent's {@link Group#getChildren() children}, where a lower index is below a higher index. Setting a z-index higher than the
    * number of children will move the child to the front. Setting a z-index less than zero is invalid.
    * @return
    *   true if the z-index changed.
    */
  def setZIndex(index: Int): Boolean = {
    if (index < 0) throw new IllegalArgumentException("ZIndex cannot be < 0.")
    parent.exists { p =>
      val children = p.children
      if (children.size <= 1) false
      else {
        val clampedIndex = Math.min(index, children.size - 1)
        if (children(clampedIndex) eq this) false
        else {
          val idx = children.indexOf(this)
          if (idx < 0) false
          else {
            children.removeIndex(idx)
            children.insert(clampedIndex, this)
            true
          }
        }
      }
    }
  }

  /** Returns the z-index of this actor, or -1 if the actor is not in a group.
    * @see
    *   #setZIndex(int)
    */
  def getZIndex: Int =
    parent.map(_.children.indexOf(this)).getOrElse(-1)

  /** Calls {@link #clipBegin(float, float, float, float)} to clip this actor's bounds. */
  def clipBegin(): Boolean = clipBegin(x, y, width, height)

  /** Clips the specified screen aligned rectangle, specified relative to the transform matrix of the stage's Batch. The transform matrix and the stage's camera must not have rotational components.
    * Calling this method must be followed by a call to {@link #clipEnd()} if true is returned.
    * @return
    *   false if the clipping area is zero and no drawing should occur.
    * @see
    *   ScissorStack
    */
  def clipBegin(x: Float, y: Float, width: Float, height: Float): Boolean =
    if (width <= 0 || height <= 0) false
    else {
      stage.exists { stage =>
        val tableBounds = Rectangle.tmp
        tableBounds.x = x
        tableBounds.y = y
        tableBounds.width = width
        tableBounds.height = height
        val scissorBounds = Actor.POOLS.obtain(classOf[Rectangle])
        stage.calculateScissors(tableBounds, scissorBounds)
        if (ScissorStack.pushScissors(scissorBounds)) true
        else {
          Actor.POOLS.free(scissorBounds)
          false
        }
      }
    }

  /** Ends clipping begun by {@link #clipBegin(float, float, float, float)}. */
  def clipEnd(): Unit =
    Actor.POOLS.free(ScissorStack.popScissors())

  /** Transforms the specified point in screen coordinates to the actor's local coordinate system.
    * @see
    *   Stage#screenToStageCoordinates(Vector2)
    */
  def screenToLocalCoordinates(screenCoords: Vector2): Vector2 =
    stage.fold(screenCoords) { s =>
      stageToLocalCoordinates(s.screenToStageCoordinates(screenCoords))
    }

  /** Transforms the specified point in the stage's coordinates to the actor's local coordinate system. */
  def stageToLocalCoordinates(stageCoords: Vector2): Vector2 = {
    parent.foreach(_.stageToLocalCoordinates(stageCoords))
    parentToLocalCoordinates(stageCoords)
    stageCoords
  }

  /** Converts the coordinates given in the parent's coordinate system to this actor's coordinate system. */
  def parentToLocalCoordinates(parentCoords: Vector2): Vector2 = {
    val rotation = this.rotation
    val scaleX   = this.scaleX
    val scaleY   = this.scaleY
    val childX   = x
    val childY   = y
    if (rotation == 0) {
      if (scaleX == 1 && scaleY == 1) {
        parentCoords.x -= childX
        parentCoords.y -= childY
      } else {
        val originX = this.originX
        val originY = this.originY
        parentCoords.x = (parentCoords.x - childX - originX) / scaleX + originX
        parentCoords.y = (parentCoords.y - childY - originY) / scaleY + originY
      }
    } else {
      val cos     = Math.cos(rotation * MathUtils.degreesToRadians).toFloat
      val sin     = Math.sin(rotation * MathUtils.degreesToRadians).toFloat
      val originX = this.originX
      val originY = this.originY
      val tox     = parentCoords.x - childX - originX
      val toy     = parentCoords.y - childY - originY
      parentCoords.x = (tox * cos + toy * sin) / scaleX + originX
      parentCoords.y = (tox * -sin + toy * cos) / scaleY + originY
    }
    parentCoords
  }

  /** Transforms the specified point in the actor's coordinates to be in screen coordinates.
    * @see
    *   Stage#stageToScreenCoordinates(Vector2)
    */
  def localToScreenCoordinates(localCoords: Vector2): Vector2 =
    stage.fold(localCoords) { s =>
      s.stageToScreenCoordinates(localToAscendantCoordinates(Nullable.empty, localCoords))
    }

  /** Transforms the specified point in the actor's coordinates to be in the stage's coordinates. */
  def localToStageCoordinates(localCoords: Vector2): Vector2 =
    localToAscendantCoordinates(Nullable.empty, localCoords)

  /** Transforms the specified point in the actor's coordinates to be in the parent's coordinates. */
  def localToParentCoordinates(localCoords: Vector2): Vector2 = {
    val rotation = -this.rotation
    val scaleX   = this.scaleX
    val scaleY   = this.scaleY
    val x        = this.x
    val y        = this.y
    if (rotation == 0) {
      if (scaleX == 1 && scaleY == 1) {
        localCoords.x += x
        localCoords.y += y
      } else {
        val originX = this.originX
        val originY = this.originY
        localCoords.x = (localCoords.x - originX) * scaleX + originX + x
        localCoords.y = (localCoords.y - originY) * scaleY + originY + y
      }
    } else {
      val cos     = Math.cos(rotation * MathUtils.degreesToRadians).toFloat
      val sin     = Math.sin(rotation * MathUtils.degreesToRadians).toFloat
      val originX = this.originX
      val originY = this.originY
      val tox     = (localCoords.x - originX) * scaleX
      val toy     = (localCoords.y - originY) * scaleY
      localCoords.x = (tox * cos + toy * sin) + originX + x
      localCoords.y = (tox * -sin + toy * cos) + originY + y
    }
    localCoords
  }

  /** Converts coordinates for this actor to those of an ascendant. The ascendant is not required to be the immediate parent.
    * @throws IllegalArgumentException
    *   if the specified actor is not an ascendant of this actor.
    */
  def localToAscendantCoordinates(ascendant: Nullable[Actor], localCoords: Vector2): Vector2 = scala.util.boundary {
    var a: Actor = this
    while (true) {
      a.localToParentCoordinates(localCoords)
      a.parent.fold {
        if (ascendant.isEmpty) scala.util.boundary.break(localCoords)
        throw new IllegalArgumentException("Actor is not an ascendant: " + ascendant)
      } { pp =>
        if (ascendant.exists(_ eq pp)) scala.util.boundary.break(localCoords)
        a = pp
      }
    }
    localCoords // unreachable but needed for type
  }

  /** Converts coordinates for this actor to those of another actor, which can be anywhere in the stage. */
  def localToActorCoordinates(actor: Actor, localCoords: Vector2): Vector2 = {
    localToStageCoordinates(localCoords)
    actor.stageToLocalCoordinates(localCoords)
  }

  /** Draws this actor's debug lines if {@link #getDebug()} is true. */
  def drawDebug(shapes: ShapeRenderer): Unit =
    drawDebugBounds(shapes)

  /** Draws a rectangle for the bounds of this actor if {@link #getDebug()} is true. */
  protected def drawDebugBounds(shapes: ShapeRenderer): Unit =
    if (_debug) {
      shapes.set(ShapeRenderer.ShapeType.Line)
      stage.foreach(s => shapes.setColor(s.getDebugColor))
      shapes.rectangle(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

  /** If true, {@link #drawDebug(ShapeRenderer)} will be called for this actor. */
  def setDebug(enabled: Boolean): Unit = {
    _debug = enabled
    if (enabled) Stage.debug = true
  }

  def getDebug: Boolean = _debug

  /** Calls {@link #setDebug(boolean)} with {@code true}. */
  def debug(): Actor = {
    setDebug(true)
    this
  }

  override def toString: String =
    name.fold {
      var n        = getClass.getName
      val dotIndex = n.lastIndexOf('.')
      if (dotIndex != -1) n = n.substring(dotIndex + 1)
      n
    }(identity)
}

object Actor {
  val POOLS: PoolManager = PoolManager()

  POOLS.addPool(classOf[Rectangle], () => Rectangle())
  POOLS.addPool(classOf[DynamicArray[?]], () => DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[Any]], 16, true))
  POOLS.addPool(classOf[GlyphLayout], () => GlyphLayout())
  POOLS.addPool(classOf[utils.ChangeListener.ChangeEvent], () => utils.ChangeListener.ChangeEvent())
}
