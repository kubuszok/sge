/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/Actions.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; split packages
 *   Renames: static class -> object; static methods -> def; GdxRuntimeException -> SgeError;
 *            PoolSupplier<T> -> () => T; setTarget(actor) -> setTarget(Nullable(actor));
 *            static import pattern -> object import
 *   Idiom: Java static init block -> top-level object statements;
 *          pool == null throw -> pool.fold(throw ...)(p => ...);
 *          @Null Interpolation -> Nullable[Interpolation]
 *   TODO: opaque Seconds for duration params in ~13 factory methods -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.graphics.Color
import sge.math.Interpolation
import sge.utils.{ Nullable, Pool, PoolManager, SgeError }

/** Static convenience methods for using pooled actions, intended for import.
  * @author
  *   Nathan Sweet
  */
object Actions {

  val ACTION_POOLS: PoolManager = new PoolManager()

  ACTION_POOLS.addPool(classOf[AddAction], () => new AddAction())
  ACTION_POOLS.addPool(classOf[AddListenerAction], () => new AddListenerAction())
  ACTION_POOLS.addPool(classOf[AfterAction], () => new AfterAction())
  ACTION_POOLS.addPool(classOf[AlphaAction], () => new AlphaAction())
  ACTION_POOLS.addPool(classOf[ColorAction], () => new ColorAction())
  ACTION_POOLS.addPool(classOf[DelayAction], () => new DelayAction())
  ACTION_POOLS.addPool(classOf[FloatAction], () => new FloatAction())
  ACTION_POOLS.addPool(classOf[IntAction], () => new IntAction())
  ACTION_POOLS.addPool(classOf[LayoutAction], () => new LayoutAction())
  ACTION_POOLS.addPool(classOf[MoveByAction], () => new MoveByAction())
  ACTION_POOLS.addPool(classOf[MoveToAction], () => new MoveToAction())
  ACTION_POOLS.addPool(classOf[ParallelAction], () => new ParallelAction())
  ACTION_POOLS.addPool(classOf[RemoveAction], () => new RemoveAction())
  ACTION_POOLS.addPool(classOf[RemoveActorAction], () => new RemoveActorAction())
  ACTION_POOLS.addPool(classOf[RemoveListenerAction], () => new RemoveListenerAction())
  ACTION_POOLS.addPool(classOf[RepeatAction], () => new RepeatAction())
  ACTION_POOLS.addPool(classOf[RotateByAction], () => new RotateByAction())
  ACTION_POOLS.addPool(classOf[RotateToAction], () => new RotateToAction())
  ACTION_POOLS.addPool(classOf[RunnableAction], () => new RunnableAction())
  ACTION_POOLS.addPool(classOf[ScaleByAction], () => new ScaleByAction())
  ACTION_POOLS.addPool(classOf[ScaleToAction], () => new ScaleToAction())
  ACTION_POOLS.addPool(classOf[SequenceAction], () => new SequenceAction())
  ACTION_POOLS.addPool(classOf[SizeByAction], () => new SizeByAction())
  ACTION_POOLS.addPool(classOf[SizeToAction], () => new SizeToAction())
  ACTION_POOLS.addPool(classOf[TimeScaleAction], () => new TimeScaleAction())
  ACTION_POOLS.addPool(classOf[TouchableAction], () => new TouchableAction())
  ACTION_POOLS.addPool(classOf[VisibleAction], () => new VisibleAction())

  def registerAction[T <: Action](poolClass: Class[T], supplier: () => T): Unit =
    ACTION_POOLS.addPool(poolClass, supplier)

  /** Returns a new or pooled action of the specified type. */
  def action[T <: Action](tpe: Class[T]): T = {
    val pool = ACTION_POOLS.getPoolOrNull(tpe)
    pool.fold(
      throw SgeError.InvalidInput(
        "No action pool registered for type " + tpe + ". Register it with Actions.registerAction."
      )
    ) { p =>
      val a = p.obtain()
      a.setPool(Nullable(p.asInstanceOf[Pool[?]]))
      a
    }
  }

  def addAction(action: Action): AddAction = {
    val addAction = this.action(classOf[AddAction])
    addAction.setAction(action)
    addAction
  }

  def addAction(action: Action, targetActor: Actor): AddAction = {
    val addAction = this.action(classOf[AddAction])
    addAction.setTarget(Nullable(targetActor))
    addAction.setAction(action)
    addAction
  }

  def removeAction(action: Action): RemoveAction = {
    val removeAction = this.action(classOf[RemoveAction])
    removeAction.setAction(action)
    removeAction
  }

  def removeAction(action: Action, targetActor: Actor): RemoveAction = {
    val removeAction = this.action(classOf[RemoveAction])
    removeAction.setTarget(Nullable(targetActor))
    removeAction.setAction(action)
    removeAction
  }

  /** Moves the actor instantly. */
  def moveTo(x: Float, y: Float): MoveToAction =
    moveTo(x, y, 0, Nullable.empty)

  def moveTo(x: Float, y: Float, duration: Float): MoveToAction =
    moveTo(x, y, duration, Nullable.empty)

  def moveTo(x: Float, y: Float, duration: Float, interpolation: Nullable[Interpolation]): MoveToAction = {
    val action = this.action(classOf[MoveToAction])
    action.setPosition(x, y)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  def moveToAligned(x: Float, y: Float, alignment: sge.utils.Align): MoveToAction =
    moveToAligned(x, y, alignment, 0, Nullable.empty)

  def moveToAligned(x: Float, y: Float, alignment: sge.utils.Align, duration: Float): MoveToAction =
    moveToAligned(x, y, alignment, duration, Nullable.empty)

  def moveToAligned(x: Float, y: Float, alignment: sge.utils.Align, duration: Float, interpolation: Nullable[Interpolation]): MoveToAction = {
    val action = this.action(classOf[MoveToAction])
    action.setPosition(x, y, alignment)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Moves the actor instantly. */
  def moveBy(amountX: Float, amountY: Float): MoveByAction =
    moveBy(amountX, amountY, 0, Nullable.empty)

  def moveBy(amountX: Float, amountY: Float, duration: Float): MoveByAction =
    moveBy(amountX, amountY, duration, Nullable.empty)

  def moveBy(amountX: Float, amountY: Float, duration: Float, interpolation: Nullable[Interpolation]): MoveByAction = {
    val action = this.action(classOf[MoveByAction])
    action.setAmount(amountX, amountY)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Sizes the actor instantly. */
  def sizeTo(x: Float, y: Float): SizeToAction =
    sizeTo(x, y, 0, Nullable.empty)

  def sizeTo(x: Float, y: Float, duration: Float): SizeToAction =
    sizeTo(x, y, duration, Nullable.empty)

  def sizeTo(x: Float, y: Float, duration: Float, interpolation: Nullable[Interpolation]): SizeToAction = {
    val action = this.action(classOf[SizeToAction])
    action.setSize(x, y)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Sizes the actor instantly. */
  def sizeBy(amountX: Float, amountY: Float): SizeByAction =
    sizeBy(amountX, amountY, 0, Nullable.empty)

  def sizeBy(amountX: Float, amountY: Float, duration: Float): SizeByAction =
    sizeBy(amountX, amountY, duration, Nullable.empty)

  def sizeBy(amountX: Float, amountY: Float, duration: Float, interpolation: Nullable[Interpolation]): SizeByAction = {
    val action = this.action(classOf[SizeByAction])
    action.setAmount(amountX, amountY)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Scales the actor instantly. */
  def scaleTo(x: Float, y: Float): ScaleToAction =
    scaleTo(x, y, 0, Nullable.empty)

  def scaleTo(x: Float, y: Float, duration: Float): ScaleToAction =
    scaleTo(x, y, duration, Nullable.empty)

  def scaleTo(x: Float, y: Float, duration: Float, interpolation: Nullable[Interpolation]): ScaleToAction = {
    val action = this.action(classOf[ScaleToAction])
    action.setScale(x, y)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Scales the actor instantly. */
  def scaleBy(amountX: Float, amountY: Float): ScaleByAction =
    scaleBy(amountX, amountY, 0, Nullable.empty)

  def scaleBy(amountX: Float, amountY: Float, duration: Float): ScaleByAction =
    scaleBy(amountX, amountY, duration, Nullable.empty)

  def scaleBy(amountX: Float, amountY: Float, duration: Float, interpolation: Nullable[Interpolation]): ScaleByAction = {
    val action = this.action(classOf[ScaleByAction])
    action.setAmount(amountX, amountY)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Rotates the actor instantly. */
  def rotateTo(rotation: Float): RotateToAction =
    rotateTo(rotation, 0, Nullable.empty)

  def rotateTo(rotation: Float, duration: Float): RotateToAction =
    rotateTo(rotation, duration, Nullable.empty)

  def rotateTo(rotation: Float, duration: Float, interpolation: Nullable[Interpolation]): RotateToAction = {
    val action = this.action(classOf[RotateToAction])
    action.setRotation(rotation)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Rotates the actor instantly. */
  def rotateBy(rotationAmount: Float): RotateByAction =
    rotateBy(rotationAmount, 0, Nullable.empty)

  def rotateBy(rotationAmount: Float, duration: Float): RotateByAction =
    rotateBy(rotationAmount, duration, Nullable.empty)

  def rotateBy(rotationAmount: Float, duration: Float, interpolation: Nullable[Interpolation]): RotateByAction = {
    val action = this.action(classOf[RotateByAction])
    action.setAmount(rotationAmount)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Sets the actor's color instantly. */
  def color(color: Color): ColorAction =
    this.color(color, 0, Nullable.empty)

  /** Transitions from the color at the time this action starts to the specified color. */
  def color(color: Color, duration: Float): ColorAction =
    this.color(color, duration, Nullable.empty)

  /** Transitions from the color at the time this action starts to the specified color. */
  def color(color: Color, duration: Float, interpolation: Nullable[Interpolation]): ColorAction = {
    val action = this.action(classOf[ColorAction])
    action.setEndColor(color)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Sets the actor's alpha instantly. */
  def alpha(a: Float): AlphaAction =
    alpha(a, 0, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to the specified alpha. */
  def alpha(a: Float, duration: Float): AlphaAction =
    alpha(a, duration, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to the specified alpha. */
  def alpha(a: Float, duration: Float, interpolation: Nullable[Interpolation]): AlphaAction = {
    val action = this.action(classOf[AlphaAction])
    action.setAlpha(a)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Transitions from the alpha at the time this action starts to an alpha of 0. */
  def fadeOut(duration: Float): AlphaAction =
    alpha(0, duration, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to an alpha of 0. */
  def fadeOut(duration: Float, interpolation: Nullable[Interpolation]): AlphaAction = {
    val action = this.action(classOf[AlphaAction])
    action.setAlpha(0)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  /** Transitions from the alpha at the time this action starts to an alpha of 1. */
  def fadeIn(duration: Float): AlphaAction =
    alpha(1, duration, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to an alpha of 1. */
  def fadeIn(duration: Float, interpolation: Nullable[Interpolation]): AlphaAction = {
    val action = this.action(classOf[AlphaAction])
    action.setAlpha(1)
    action.setDuration(duration)
    action.setInterpolation(interpolation)
    action
  }

  def show(): VisibleAction =
    visible(true)

  def hide(): VisibleAction =
    visible(false)

  def visible(visible: Boolean): VisibleAction = {
    val action = this.action(classOf[VisibleAction])
    action.setVisible(visible)
    action
  }

  def touchable(touchable: Touchable): TouchableAction = {
    val action = this.action(classOf[TouchableAction])
    action.setTouchable(touchable)
    action
  }

  def removeActor(): RemoveActorAction =
    action(classOf[RemoveActorAction])

  def removeActor(removeActor: Actor): RemoveActorAction = {
    val action = this.action(classOf[RemoveActorAction])
    action.setTarget(Nullable(removeActor))
    action
  }

  def delay(duration: Float): DelayAction = {
    val action = this.action(classOf[DelayAction])
    action.setDuration(duration)
    action
  }

  def delay(duration: Float, delayedAction: Action): DelayAction = {
    val action = this.action(classOf[DelayAction])
    action.setDuration(duration)
    action.setAction(delayedAction)
    action
  }

  def timeScale(scale: Float, scaledAction: Action): TimeScaleAction = {
    val action = this.action(classOf[TimeScaleAction])
    action.setScale(scale)
    action.setAction(scaledAction)
    action
  }

  def sequence(action1: Action): SequenceAction = {
    val action = this.action(classOf[SequenceAction])
    action.addAction(action1)
    action
  }

  def sequence(action1: Action, action2: Action): SequenceAction = {
    val action = this.action(classOf[SequenceAction])
    action.addAction(action1)
    action.addAction(action2)
    action
  }

  def sequence(action1: Action, action2: Action, action3: Action): SequenceAction = {
    val action = this.action(classOf[SequenceAction])
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action
  }

  def sequence(action1: Action, action2: Action, action3: Action, action4: Action): SequenceAction = {
    val action = this.action(classOf[SequenceAction])
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action
  }

  def sequence(action1: Action, action2: Action, action3: Action, action4: Action, action5: Action): SequenceAction = {
    val action = this.action(classOf[SequenceAction])
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action.addAction(action5)
    action
  }

  def sequence(actions: Action*): SequenceAction = {
    val action = this.action(classOf[SequenceAction])
    var i      = 0
    val n      = actions.length
    while (i < n) {
      action.addAction(actions(i))
      i += 1
    }
    action
  }

  def sequence(): SequenceAction =
    action(classOf[SequenceAction])

  def parallel(action1: Action): ParallelAction = {
    val action = this.action(classOf[ParallelAction])
    action.addAction(action1)
    action
  }

  def parallel(action1: Action, action2: Action): ParallelAction = {
    val action = this.action(classOf[ParallelAction])
    action.addAction(action1)
    action.addAction(action2)
    action
  }

  def parallel(action1: Action, action2: Action, action3: Action): ParallelAction = {
    val action = this.action(classOf[ParallelAction])
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action
  }

  def parallel(action1: Action, action2: Action, action3: Action, action4: Action): ParallelAction = {
    val action = this.action(classOf[ParallelAction])
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action
  }

  def parallel(action1: Action, action2: Action, action3: Action, action4: Action, action5: Action): ParallelAction = {
    val action = this.action(classOf[ParallelAction])
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action.addAction(action5)
    action
  }

  def parallel(actions: Action*): ParallelAction = {
    val action = this.action(classOf[ParallelAction])
    var i      = 0
    val n      = actions.length
    while (i < n) {
      action.addAction(actions(i))
      i += 1
    }
    action
  }

  def parallel(): ParallelAction =
    action(classOf[ParallelAction])

  def repeat(count: Int, repeatedAction: Action): RepeatAction = {
    val action = this.action(classOf[RepeatAction])
    action.setCount(count)
    action.setAction(repeatedAction)
    action
  }

  def forever(repeatedAction: Action): RepeatAction = {
    val action = this.action(classOf[RepeatAction])
    action.setCount(RepeatAction.FOREVER)
    action.setAction(repeatedAction)
    action
  }

  def run(runnable: Runnable): RunnableAction = {
    val action = this.action(classOf[RunnableAction])
    action.setRunnable(runnable)
    action
  }

  def layout(enabled: Boolean): LayoutAction = {
    val action = this.action(classOf[LayoutAction])
    action.setLayoutEnabled(enabled)
    action
  }

  def after(action: Action): AfterAction = {
    val afterAction = this.action(classOf[AfterAction])
    afterAction.setAction(action)
    afterAction
  }

  def addListener(listener: EventListener, capture: Boolean): AddListenerAction = {
    val addAction = action(classOf[AddListenerAction])
    addAction.setListener(listener)
    addAction.setCapture(capture)
    addAction
  }

  def addListener(listener: EventListener, capture: Boolean, targetActor: Actor): AddListenerAction = {
    val addAction = action(classOf[AddListenerAction])
    addAction.setTarget(Nullable(targetActor))
    addAction.setListener(listener)
    addAction.setCapture(capture)
    addAction
  }

  def removeListener(listener: EventListener, capture: Boolean): RemoveListenerAction = {
    val removeAction = action(classOf[RemoveListenerAction])
    removeAction.setListener(listener)
    removeAction.setCapture(capture)
    removeAction
  }

  def removeListener(listener: EventListener, capture: Boolean, targetActor: Actor): RemoveListenerAction = {
    val removeAction = action(classOf[RemoveListenerAction])
    removeAction.setTarget(Nullable(targetActor))
    removeAction.setListener(listener)
    removeAction.setCapture(capture)
    removeAction
  }

  /** Sets the target of an action and returns the action.
    * @param target
    *   the desired target of the action
    * @param action
    *   the action on which to set the target
    * @return
    *   the action with its target set
    */
  def targeting(target: Actor, action: Action): Action = {
    action.setTarget(Nullable(target))
    action
  }
}
