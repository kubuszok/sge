/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/msg/Telegraph.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package msg

/** Any object implementing the `Telegraph` trait can act as the sender or the receiver of a [[Telegram]].
  *
  * @author
  *   davebaol (original implementation)
  */
trait Telegraph {

  /** Handles the telegram just received.
    * @param msg
    *   The telegram
    * @return
    *   `true` if the telegram has been successfully handled; `false` otherwise.
    */
  def handleMessage(msg: Telegram): Boolean
}
