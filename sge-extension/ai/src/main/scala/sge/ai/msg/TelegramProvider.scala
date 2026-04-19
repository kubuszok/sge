/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/msg/TelegramProvider.java
 * Original authors: avianey
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`
 *   Convention: split packages; `null` return -> `Nullable`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: TelegramProvider,provideMessageInfo
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package msg

import sge.utils.Nullable

/** Telegram providers respond to [[MessageDispatcher.addListener]] by providing optional [[Telegram.extraInfo]] to be sent in a Telegram of a given type to the newly registered [[Telegraph]].
  *
  * @author
  *   avianey (original implementation)
  */
trait TelegramProvider {

  /** Provides [[Telegram.extraInfo]] to dispatch immediately when a [[Telegraph]] is registered for the given message type.
    * @param msg
    *   the message type to provide
    * @param receiver
    *   the newly registered Telegraph. Providers can provide different info depending on the targeted Telegraph.
    * @return
    *   extra info to dispatch in a Telegram or `Nullable.empty` if nothing to dispatch
    */
  def provideMessageInfo(msg: Int, receiver: Telegraph): Nullable[Any]
}
