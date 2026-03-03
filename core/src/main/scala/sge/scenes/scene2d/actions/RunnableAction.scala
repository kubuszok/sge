/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RunnableAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: setPool(null) -> setPool(Nullable.empty); runnable.run() -> runnable.foreach(_.run())
 *   TODO: Java-style getters/setters -- getRunnable/setRunnable, getResult
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable

/** An action that runs a {@link Runnable}. Alternatively, the {@link #run()} method can be overridden instead of setting a runnable.
  * @author
  *   Nathan Sweet
  */
class RunnableAction extends Action {
  private var runnable: Nullable[Runnable] = Nullable.empty
  private var ran:      Boolean            = false

  def act(delta: Float): Boolean = {
    if (!ran) {
      ran = true
      run()
    }
    true
  }

  /** Called to run the runnable. */
  def run(): Unit = {
    val pool = getPool
    setPool(Nullable.empty) // Ensure this action can't be returned to the pool inside the runnable.
    try
      runnable.foreach(_.run())
    finally
      setPool(pool)
  }

  override def restart(): Unit = ran = false

  override def reset(): Unit = {
    super.reset()
    runnable = Nullable.empty
  }

  def getRunnable: Nullable[Runnable] = runnable

  def setRunnable(runnable: Runnable): Unit = this.runnable = Nullable(runnable)
}
