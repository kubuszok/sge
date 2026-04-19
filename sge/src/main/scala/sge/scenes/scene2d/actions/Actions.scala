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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 532
 * Covenant-baseline-methods: ACTION_POOLS,Actions,action,addAction,addListener,after,afterAction,alpha,color,delay,fadeIn,fadeOut,forever,hide,i,layout,moveBy,moveTo,moveToAligned,n,parallel,pool,registerAction,removeAction,removeActor,removeListener,repeat,rotateBy,rotateTo,run,scaleBy,scaleTo,sequence,show,sizeBy,sizeTo,targeting,timeScale,touchable,visible
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/Actions.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package actions

import sge.graphics.Color
import sge.math.Interpolation
import scala.reflect.ClassTag
import sge.utils.{ Nullable, Pool, PoolManager, Seconds, SgeError }

/** Static convenience methods for using pooled actions, intended for import.
  * @author
  *   Nathan Sweet
  */
object Actions {

  val ACTION_POOLS: PoolManager = PoolManager()

  ACTION_POOLS.addPool[AddAction](() => AddAction())
  ACTION_POOLS.addPool[AddListenerAction](() => AddListenerAction())
  ACTION_POOLS.addPool[AfterAction](() => AfterAction())
  ACTION_POOLS.addPool[AlphaAction](() => AlphaAction())
  ACTION_POOLS.addPool[ColorAction](() => ColorAction())
  ACTION_POOLS.addPool[DelayAction](() => DelayAction())
  ACTION_POOLS.addPool[FloatAction](() => FloatAction())
  ACTION_POOLS.addPool[IntAction](() => IntAction())
  ACTION_POOLS.addPool[LayoutAction](() => LayoutAction())
  ACTION_POOLS.addPool[MoveByAction](() => MoveByAction())
  ACTION_POOLS.addPool[MoveToAction](() => MoveToAction())
  ACTION_POOLS.addPool[ParallelAction](() => ParallelAction())
  ACTION_POOLS.addPool[RemoveAction](() => RemoveAction())
  ACTION_POOLS.addPool[RemoveActorAction](() => RemoveActorAction())
  ACTION_POOLS.addPool[RemoveListenerAction](() => RemoveListenerAction())
  ACTION_POOLS.addPool[RepeatAction](() => RepeatAction())
  ACTION_POOLS.addPool[RotateByAction](() => RotateByAction())
  ACTION_POOLS.addPool[RotateToAction](() => RotateToAction())
  ACTION_POOLS.addPool[RunnableAction](() => RunnableAction())
  ACTION_POOLS.addPool[ScaleByAction](() => ScaleByAction())
  ACTION_POOLS.addPool[ScaleToAction](() => ScaleToAction())
  ACTION_POOLS.addPool[SequenceAction](() => SequenceAction())
  ACTION_POOLS.addPool[SizeByAction](() => SizeByAction())
  ACTION_POOLS.addPool[SizeToAction](() => SizeToAction())
  ACTION_POOLS.addPool[TimeScaleAction](() => TimeScaleAction())
  ACTION_POOLS.addPool[TouchableAction](() => TouchableAction())
  ACTION_POOLS.addPool[VisibleAction](() => VisibleAction())

  def registerAction[T <: Action: ClassTag](supplier: () => T): Unit =
    ACTION_POOLS.addPool[T](supplier)

  /** Returns a new or pooled action of the specified type. */
  def action[T <: Action: ClassTag]: T = {
    val pool = ACTION_POOLS.poolOrNull[T]
    pool.fold(
      throw SgeError.InvalidInput(
        s"No action pool registered for type ${summon[ClassTag[T]].runtimeClass}. Register it with Actions.registerAction."
      )
    ) { p =>
      val a = p.obtain()
      a.pool = Nullable(p.asInstanceOf[Pool[?]])
      a
    }
  }

  def addAction(action: Action): AddAction = {
    val addAction = this.action[AddAction]
    addAction.actionToAdd = Nullable(action)
    addAction
  }

  def addAction(action: Action, targetActor: Actor): AddAction = {
    val addAction = this.action[AddAction]
    addAction.setTarget(Nullable(targetActor))
    addAction.actionToAdd = Nullable(action)
    addAction
  }

  def removeAction(action: Action): RemoveAction = {
    val removeAction = this.action[RemoveAction]
    removeAction.actionToRemove = Nullable(action)
    removeAction
  }

  def removeAction(action: Action, targetActor: Actor): RemoveAction = {
    val removeAction = this.action[RemoveAction]
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
    val action = this.action[MoveToAction]
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
    val action = this.action[MoveToAction]
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
    val action = this.action[MoveByAction]
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
    val action = this.action[SizeToAction]
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
    val action = this.action[SizeByAction]
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
    val action = this.action[ScaleToAction]
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
    val action = this.action[ScaleByAction]
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
    val action = this.action[RotateToAction]
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
    val action = this.action[RotateByAction]
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
    val action = this.action[ColorAction]
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
    val action = this.action[AlphaAction]
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
    val action = this.action[AlphaAction]
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
    val action = this.action[AlphaAction]
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
    val action = this.action[VisibleAction]
    action.visible = visible
    action
  }

  def touchable(touchable: Touchable): TouchableAction = {
    val action = this.action[TouchableAction]
    action.touchable = touchable
    action
  }

  def removeActor(): RemoveActorAction =
    action[RemoveActorAction]

  def removeActor(removeActor: Actor): RemoveActorAction = {
    val action = this.action[RemoveActorAction]
    action.setTarget(Nullable(removeActor))
    action
  }

  def delay(duration: Seconds): DelayAction = {
    val action = this.action[DelayAction]
    action.duration = duration
    action
  }

  def delay(duration: Seconds, delayedAction: Action): DelayAction = {
    val action = this.action[DelayAction]
    action.duration = duration
    action.action = Nullable(delayedAction)
    action
  }

  def timeScale(scale: Float, scaledAction: Action): TimeScaleAction = {
    val action = this.action[TimeScaleAction]
    action.scale = scale
    action.action = Nullable(scaledAction)
    action
  }

  def sequence(action1: Action): SequenceAction = {
    val action = this.action[SequenceAction]
    action.addAction(action1)
    action
  }

  def sequence(action1: Action, action2: Action): SequenceAction = {
    val action = this.action[SequenceAction]
    action.addAction(action1)
    action.addAction(action2)
    action
  }

  def sequence(action1: Action, action2: Action, action3: Action): SequenceAction = {
    val action = this.action[SequenceAction]
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action
  }

  def sequence(action1: Action, action2: Action, action3: Action, action4: Action): SequenceAction = {
    val action = this.action[SequenceAction]
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action
  }

  def sequence(action1: Action, action2: Action, action3: Action, action4: Action, action5: Action): SequenceAction = {
    val action = this.action[SequenceAction]
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action.addAction(action5)
    action
  }

  def sequence(actions: Action*): SequenceAction = {
    val action = this.action[SequenceAction]
    var i      = 0
    val n      = actions.length
    while (i < n) {
      action.addAction(actions(i))
      i += 1
    }
    action
  }

  def sequence(): SequenceAction =
    action[SequenceAction]

  def parallel(action1: Action): ParallelAction = {
    val action = this.action[ParallelAction]
    action.addAction(action1)
    action
  }

  def parallel(action1: Action, action2: Action): ParallelAction = {
    val action = this.action[ParallelAction]
    action.addAction(action1)
    action.addAction(action2)
    action
  }

  def parallel(action1: Action, action2: Action, action3: Action): ParallelAction = {
    val action = this.action[ParallelAction]
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action
  }

  def parallel(action1: Action, action2: Action, action3: Action, action4: Action): ParallelAction = {
    val action = this.action[ParallelAction]
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action
  }

  def parallel(action1: Action, action2: Action, action3: Action, action4: Action, action5: Action): ParallelAction = {
    val action = this.action[ParallelAction]
    action.addAction(action1)
    action.addAction(action2)
    action.addAction(action3)
    action.addAction(action4)
    action.addAction(action5)
    action
  }

  def parallel(actions: Action*): ParallelAction = {
    val action = this.action[ParallelAction]
    var i      = 0
    val n      = actions.length
    while (i < n) {
      action.addAction(actions(i))
      i += 1
    }
    action
  }

  def parallel(): ParallelAction =
    action[ParallelAction]

  def repeat(count: Int, repeatedAction: Action): RepeatAction = {
    val action = this.action[RepeatAction]
    action.count = count
    action.action = Nullable(repeatedAction)
    action
  }

  def forever(repeatedAction: Action): RepeatAction = {
    val action = this.action[RepeatAction]
    action.count = RepeatAction.FOREVER
    action.action = Nullable(repeatedAction)
    action
  }

  def run(runnable: Runnable): RunnableAction = {
    val action = this.action[RunnableAction]
    action.runnable = Nullable(runnable)
    action
  }

  def layout(enabled: Boolean): LayoutAction = {
    val action = this.action[LayoutAction]
    action.enabled = enabled
    action
  }

  def after(action: Action): AfterAction = {
    val afterAction = this.action[AfterAction]
    afterAction.action = Nullable(action)
    afterAction
  }

  def addListener(listener: EventListener, capture: Boolean): AddListenerAction = {
    val addAction = action[AddListenerAction]
    addAction.listener = Nullable(listener)
    addAction.capture = capture
    addAction
  }

  def addListener(listener: EventListener, capture: Boolean, targetActor: Actor): AddListenerAction = {
    val addAction = action[AddListenerAction]
    addAction.setTarget(Nullable(targetActor))
    addAction.listener = Nullable(listener)
    addAction.capture = capture
    addAction
  }

  def removeListener(listener: EventListener, capture: Boolean): RemoveListenerAction = {
    val removeAction = action[RemoveListenerAction]
    removeAction.listener = Nullable(listener)
    removeAction.capture = capture
    removeAction
  }

  def removeListener(listener: EventListener, capture: Boolean, targetActor: Actor): RemoveListenerAction = {
    val removeAction = action[RemoveListenerAction]
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
