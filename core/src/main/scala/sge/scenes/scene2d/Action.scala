/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/Action.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Pool.Poolable (Java interface) -> Pool.Poolable (Scala trait)
 *   Convention: null -> Nullable[A]; no return statements; split packages
 *   Idiom: setActor null-check -> Nullable.fold; Pool raw type -> Pool[?]
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (getActor/setActor, getTarget/setTarget, getPool/setPool)
 *   TODO: extends Pool.Poolable → define given Poolable[Action] in companion
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d

import sge.utils.Nullable
import sge.utils.Pool

/** Actions attach to an {@link Actor} and perform some task, often over time.
  * @author
  *   Nathan Sweet
  */
abstract class Action extends Pool.Poolable {

  /** The actor this action is attached to, or null if it is not attached. */
  protected var actor: Nullable[Actor] = Nullable.empty

  /** The actor this action targets, or null if a target has not been set. */
  protected var target: Nullable[Actor] = Nullable.empty

  private var pool: Nullable[Pool[?]] = Nullable.empty

  /** Updates the action based on time. Typically this is called each frame by {@link Actor#act(float)}.
    * @param delta
    *   Time in seconds since the last frame.
    * @return
    *   true if the action is done. This method may continue to be called after the action is done.
    */
  def act(delta: Float): Boolean

  /** Sets the state of the action so it can be run again. */
  def restart(): Unit = {}

  /** Sets the actor this action is attached to. This also sets the {@link #setTarget(Actor) target} actor if it is null. This method is called automatically when an action is added to an actor. This
    * method is also called with null when an action is removed from an actor. <p> When set to null, if the action has a {@link #setPool(Pool) pool} then the action is
    * {@link Pool#free(Object) returned} to the pool (which calls {@link #reset()}) and the pool is set to null. If the action does not have a pool, {@link #reset()} is not called. <p> This method is
    * not typically a good place for an action subclass to query the actor's state because the action may not be executed for some time, eg it may be {@link DelayAction delayed}. The actor's state is
    * best queried in the first call to {@link #act(float)}. For a {@link TemporalAction}, use TemporalAction#begin().
    */
  def setActor(actor: Nullable[Actor]): Unit = {
    this.actor = actor
    if (target.isEmpty) setTarget(actor)
    actor.fold {
      pool.foreach { p =>
        p.asInstanceOf[Pool[Action]].free(this)
        this.pool = Nullable.empty
      }
    }(_ => ())
  }

  /** @return null if the action is not attached to an actor. */
  def getActor: Nullable[Actor] = actor

  /** Sets the actor this action will manipulate. If no target actor is set, {@link #setActor(Actor)} will set the target actor when the action is added to an actor.
    */
  def setTarget(target: Nullable[Actor]): Unit =
    this.target = target

  /** @return null if the action has no target. */
  def getTarget: Nullable[Actor] = target

  /** Resets the optional state of this action to as if it were newly created, allowing the action to be pooled and reused. State required to be set for every usage of this action or computed during
    * the action does not need to be reset. <p> The default implementation calls {@link #restart()}. <p> If a subclass has optional state, it must override this method, call super, and reset the
    * optional state.
    */
  def reset(): Unit = {
    actor = Nullable.empty
    target = Nullable.empty
    pool = Nullable.empty
    restart()
  }

  def getPool: Nullable[Pool[?]] = pool

  /** Sets the pool that the action will be returned to when removed from the actor.
    * @param pool
    *   May be null.
    * @see
    *   #setActor(Actor)
    */
  def setPool(pool: Nullable[Pool[?]]): Unit =
    this.pool = pool

  override def toString: String = {
    var name     = getClass.getName
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex != -1) name = name.substring(dotIndex + 1)
    if (name.endsWith("Action")) name = name.substring(0, name.length - 6)
    name
  }
}
