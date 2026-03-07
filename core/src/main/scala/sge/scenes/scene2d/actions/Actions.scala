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
 *   Convention: opaque Seconds for duration params in factory methods
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.graphics.Color
import sge.math.Interpolation
import sge.utils.{ Nullable, Pool, PoolManager, Seconds, SgeError }

/** Static convenience methods for using pooled actions, intended for import.
  * @author
  *   Nathan Sweet
  */
object Actions {

  val ACTION_POOLS: PoolManager = PoolManager()

  ACTION_POOLS.addPool(classOf[AddAction], () => AddAction())
  ACTION_POOLS.addPool(classOf[AddListenerAction], () => AddListenerAction())
  ACTION_POOLS.addPool(classOf[AfterAction], () => AfterAction())
  ACTION_POOLS.addPool(classOf[AlphaAction], () => AlphaAction())
  ACTION_POOLS.addPool(classOf[ColorAction], () => ColorAction())
  ACTION_POOLS.addPool(classOf[DelayAction], () => DelayAction())
  ACTION_POOLS.addPool(classOf[FloatAction], () => FloatAction())
  ACTION_POOLS.addPool(classOf[IntAction], () => IntAction())
  ACTION_POOLS.addPool(classOf[LayoutAction], () => LayoutAction())
  ACTION_POOLS.addPool(classOf[MoveByAction], () => MoveByAction())
  ACTION_POOLS.addPool(classOf[MoveToAction], () => MoveToAction())
  ACTION_POOLS.addPool(classOf[ParallelAction], () => ParallelAction())
  ACTION_POOLS.addPool(classOf[RemoveAction], () => RemoveAction())
  ACTION_POOLS.addPool(classOf[RemoveActorAction], () => RemoveActorAction())
  ACTION_POOLS.addPool(classOf[RemoveListenerAction], () => RemoveListenerAction())
  ACTION_POOLS.addPool(classOf[RepeatAction], () => RepeatAction())
  ACTION_POOLS.addPool(classOf[RotateByAction], () => RotateByAction())
  ACTION_POOLS.addPool(classOf[RotateToAction], () => RotateToAction())
  ACTION_POOLS.addPool(classOf[RunnableAction], () => RunnableAction())
  ACTION_POOLS.addPool(classOf[ScaleByAction], () => ScaleByAction())
  ACTION_POOLS.addPool(classOf[ScaleToAction], () => ScaleToAction())
  ACTION_POOLS.addPool(classOf[SequenceAction], () => SequenceAction())
  ACTION_POOLS.addPool(classOf[SizeByAction], () => SizeByAction())
  ACTION_POOLS.addPool(classOf[SizeToAction], () => SizeToAction())
  ACTION_POOLS.addPool(classOf[TimeScaleAction], () => TimeScaleAction())
  ACTION_POOLS.addPool(classOf[TouchableAction], () => TouchableAction())
  ACTION_POOLS.addPool(classOf[VisibleAction], () => VisibleAction())

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
      a.pool = Nullable(p.asInstanceOf[Pool[?]])
      a
    }
  }

  def addAction(action: Action): AddAction = {
    val addAction = this.action(classOf[AddAction])
    addAction.actionToAdd = Nullable(action)
    addAction
  }

  def addAction(action: Action, targetActor: Actor): AddAction = {
    val addAction = this.action(classOf[AddAction])
    addAction.setTarget(Nullable(targetActor))
    addAction.actionToAdd = Nullable(action)
    addAction
  }

  def removeAction(action: Action): RemoveAction = {
    val removeAction = this.action(classOf[RemoveAction])
    removeAction.actionToRemove = Nullable(action)
    removeAction
  }

  def removeAction(action: Action, targetActor: Actor): RemoveAction = {
    val removeAction = this.action(classOf[RemoveAction])
    removeAction.setTarget(Nullable(targetActor))
    removeAction.actionToRemove = Nullable(action)
    removeAction
  }

  /** Moves the actor instantly. */
  def moveTo(x: Float, y: Float): MoveToAction =
    moveTo(x, y, Seconds.zero, Nullable.empty)

  def moveTo(x: Float, y: Float, duration: Seconds): MoveToAction =
    moveTo(x, y, duration, Nullable.empty)

  def moveTo(x: Float, y: Float, duration: Seconds, interpolation: Nullable[Interpolation]): MoveToAction = {
    val action = this.action(classOf[MoveToAction])
    action.setPosition(x, y)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  def moveToAligned(x: Float, y: Float, alignment: sge.utils.Align): MoveToAction =
    moveToAligned(x, y, alignment, Seconds.zero, Nullable.empty)

  def moveToAligned(x: Float, y: Float, alignment: sge.utils.Align, duration: Seconds): MoveToAction =
    moveToAligned(x, y, alignment, duration, Nullable.empty)

  def moveToAligned(x: Float, y: Float, alignment: sge.utils.Align, duration: Seconds, interpolation: Nullable[Interpolation]): MoveToAction = {
    val action = this.action(classOf[MoveToAction])
    action.setPosition(x, y, alignment)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Moves the actor instantly. */
  def moveBy(amountX: Float, amountY: Float): MoveByAction =
    moveBy(amountX, amountY, Seconds.zero, Nullable.empty)

  def moveBy(amountX: Float, amountY: Float, duration: Seconds): MoveByAction =
    moveBy(amountX, amountY, duration, Nullable.empty)

  def moveBy(amountX: Float, amountY: Float, duration: Seconds, interpolation: Nullable[Interpolation]): MoveByAction = {
    val action = this.action(classOf[MoveByAction])
    action.setAmount(amountX, amountY)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Sizes the actor instantly. */
  def sizeTo(x: Float, y: Float): SizeToAction =
    sizeTo(x, y, Seconds.zero, Nullable.empty)

  def sizeTo(x: Float, y: Float, duration: Seconds): SizeToAction =
    sizeTo(x, y, duration, Nullable.empty)

  def sizeTo(x: Float, y: Float, duration: Seconds, interpolation: Nullable[Interpolation]): SizeToAction = {
    val action = this.action(classOf[SizeToAction])
    action.setSize(x, y)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Sizes the actor instantly. */
  def sizeBy(amountX: Float, amountY: Float): SizeByAction =
    sizeBy(amountX, amountY, Seconds.zero, Nullable.empty)

  def sizeBy(amountX: Float, amountY: Float, duration: Seconds): SizeByAction =
    sizeBy(amountX, amountY, duration, Nullable.empty)

  def sizeBy(amountX: Float, amountY: Float, duration: Seconds, interpolation: Nullable[Interpolation]): SizeByAction = {
    val action = this.action(classOf[SizeByAction])
    action.setAmount(amountX, amountY)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Scales the actor instantly. */
  def scaleTo(x: Float, y: Float): ScaleToAction =
    scaleTo(x, y, Seconds.zero, Nullable.empty)

  def scaleTo(x: Float, y: Float, duration: Seconds): ScaleToAction =
    scaleTo(x, y, duration, Nullable.empty)

  def scaleTo(x: Float, y: Float, duration: Seconds, interpolation: Nullable[Interpolation]): ScaleToAction = {
    val action = this.action(classOf[ScaleToAction])
    action.setScale(x, y)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Scales the actor instantly. */
  def scaleBy(amountX: Float, amountY: Float): ScaleByAction =
    scaleBy(amountX, amountY, Seconds.zero, Nullable.empty)

  def scaleBy(amountX: Float, amountY: Float, duration: Seconds): ScaleByAction =
    scaleBy(amountX, amountY, duration, Nullable.empty)

  def scaleBy(amountX: Float, amountY: Float, duration: Seconds, interpolation: Nullable[Interpolation]): ScaleByAction = {
    val action = this.action(classOf[ScaleByAction])
    action.setAmount(amountX, amountY)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Rotates the actor instantly. */
  def rotateTo(rotation: Float): RotateToAction =
    rotateTo(rotation, Seconds.zero, Nullable.empty)

  def rotateTo(rotation: Float, duration: Seconds): RotateToAction =
    rotateTo(rotation, duration, Nullable.empty)

  def rotateTo(rotation: Float, duration: Seconds, interpolation: Nullable[Interpolation]): RotateToAction = {
    val action = this.action(classOf[RotateToAction])
    action.rotation = rotation
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Rotates the actor instantly. */
  def rotateBy(rotationAmount: Float): RotateByAction =
    rotateBy(rotationAmount, Seconds.zero, Nullable.empty)

  def rotateBy(rotationAmount: Float, duration: Seconds): RotateByAction =
    rotateBy(rotationAmount, duration, Nullable.empty)

  def rotateBy(rotationAmount: Float, duration: Seconds, interpolation: Nullable[Interpolation]): RotateByAction = {
    val action = this.action(classOf[RotateByAction])
    action.amount = rotationAmount
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Sets the actor's color instantly. */
  def color(color: Color): ColorAction =
    this.color(color, Seconds.zero, Nullable.empty)

  /** Transitions from the color at the time this action starts to the specified color. */
  def color(color: Color, duration: Seconds): ColorAction =
    this.color(color, duration, Nullable.empty)

  /** Transitions from the color at the time this action starts to the specified color. */
  def color(color: Color, duration: Seconds, interpolation: Nullable[Interpolation]): ColorAction = {
    val action = this.action(classOf[ColorAction])
    action.setEndColor(color)
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Sets the actor's alpha instantly. */
  def alpha(a: Float): AlphaAction =
    alpha(a, Seconds.zero, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to the specified alpha. */
  def alpha(a: Float, duration: Seconds): AlphaAction =
    alpha(a, duration, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to the specified alpha. */
  def alpha(a: Float, duration: Seconds, interpolation: Nullable[Interpolation]): AlphaAction = {
    val action = this.action(classOf[AlphaAction])
    action.alpha = a
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Transitions from the alpha at the time this action starts to an alpha of 0. */
  def fadeOut(duration: Seconds): AlphaAction =
    alpha(0, duration, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to an alpha of 0. */
  def fadeOut(duration: Seconds, interpolation: Nullable[Interpolation]): AlphaAction = {
    val action = this.action(classOf[AlphaAction])
    action.alpha = 0
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  /** Transitions from the alpha at the time this action starts to an alpha of 1. */
  def fadeIn(duration: Seconds): AlphaAction =
    alpha(1, duration, Nullable.empty)

  /** Transitions from the alpha at the time this action starts to an alpha of 1. */
  def fadeIn(duration: Seconds, interpolation: Nullable[Interpolation]): AlphaAction = {
    val action = this.action(classOf[AlphaAction])
    action.alpha = 1
    action.duration = duration
    action.interpolation = interpolation
    action
  }

  def show(): VisibleAction =
    visible(true)

  def hide(): VisibleAction =
    visible(false)

  def visible(visible: Boolean): VisibleAction = {
    val action = this.action(classOf[VisibleAction])
    action.visible = visible
    action
  }

  def touchable(touchable: Touchable): TouchableAction = {
    val action = this.action(classOf[TouchableAction])
    action.touchable = touchable
    action
  }

  def removeActor(): RemoveActorAction =
    action(classOf[RemoveActorAction])

  def removeActor(removeActor: Actor): RemoveActorAction = {
    val action = this.action(classOf[RemoveActorAction])
    action.setTarget(Nullable(removeActor))
    action
  }

  def delay(duration: Seconds): DelayAction = {
    val action = this.action(classOf[DelayAction])
    action.duration = duration
    action
  }

  def delay(duration: Seconds, delayedAction: Action): DelayAction = {
    val action = this.action(classOf[DelayAction])
    action.duration = duration
    action.action = Nullable(delayedAction)
    action
  }

  def timeScale(scale: Float, scaledAction: Action): TimeScaleAction = {
    val action = this.action(classOf[TimeScaleAction])
    action.scale = scale
    action.action = Nullable(scaledAction)
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
    action.count = count
    action.action = Nullable(repeatedAction)
    action
  }

  def forever(repeatedAction: Action): RepeatAction = {
    val action = this.action(classOf[RepeatAction])
    action.count = RepeatAction.FOREVER
    action.action = Nullable(repeatedAction)
    action
  }

  def run(runnable: Runnable): RunnableAction = {
    val action = this.action(classOf[RunnableAction])
    action.runnable = Nullable(runnable)
    action
  }

  def layout(enabled: Boolean): LayoutAction = {
    val action = this.action(classOf[LayoutAction])
    action.enabled = enabled
    action
  }

  def after(action: Action): AfterAction = {
    val afterAction = this.action(classOf[AfterAction])
    afterAction.action = Nullable(action)
    afterAction
  }

  def addListener(listener: EventListener, capture: Boolean): AddListenerAction = {
    val addAction = action(classOf[AddListenerAction])
    addAction.listener = Nullable(listener)
    addAction.capture = capture
    addAction
  }

  def addListener(listener: EventListener, capture: Boolean, targetActor: Actor): AddListenerAction = {
    val addAction = action(classOf[AddListenerAction])
    addAction.setTarget(Nullable(targetActor))
    addAction.listener = Nullable(listener)
    addAction.capture = capture
    addAction
  }

  def removeListener(listener: EventListener, capture: Boolean): RemoveListenerAction = {
    val removeAction = action(classOf[RemoveListenerAction])
    removeAction.listener = Nullable(listener)
    removeAction.capture = capture
    removeAction
  }

  def removeListener(listener: EventListener, capture: Boolean, targetActor: Actor): RemoveListenerAction = {
    val removeAction = action(classOf[RemoveListenerAction])
    removeAction.setTarget(Nullable(targetActor))
    removeAction.listener = Nullable(listener)
    removeAction.capture = capture
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
