/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RunnableAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: setPool(null) -> setPool(Nullable.empty); runnable.run() -> runnable.foreach(_.run())
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }

/** An action that runs a {@link Runnable}. Alternatively, the {@link #run()} method can be overridden instead of setting a runnable.
  * @author
  *   Nathan Sweet
  */
class RunnableAction extends Action {
  var runnable:    Nullable[Runnable] = Nullable.empty
  private var ran: Boolean            = false

  def act(delta: Seconds): Boolean = {
    if (!ran) {
      ran = true
      run()
    }
    true
  }

  /** Called to run the runnable. */
  def run(): Unit = {
    val savedPool = pool
    pool = Nullable.empty // Ensure this action can't be returned to the pool inside the runnable.
    try
      runnable.foreach(_.run())
    finally
      pool = savedPool
  }

  override def restart(): Unit = ran = false

  override def reset(): Unit = {
    super.reset()
    runnable = Nullable.empty
  }
}
