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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: Telegraph,handleMessage
 * Covenant-source-reference: com/badlogic/gdx/ai/msg/Telegraph.java
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: Telegraph,handleMessage
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
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
