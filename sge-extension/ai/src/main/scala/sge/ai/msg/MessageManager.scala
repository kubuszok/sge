/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/msg/MessageManager.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`
 *   Convention: split packages; singleton `MessageManager.getInstance()` -> `object MessageManager`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 22
 * Covenant-baseline-methods: MessageManager
 * Covenant-source-reference: com/badlogic/gdx/ai/msg/MessageManager.java
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`
 *   Convention: split packages; singleton `MessageManager.getInstance()` -> `object MessageManager`
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 22
 * Covenant-baseline-methods: MessageManager
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package msg

/** The `MessageManager` is a singleton [[MessageDispatcher]] in charge of the creation, dispatch, and management of telegrams.
  *
  * @author
  *   davebaol (original implementation)
  */
object MessageManager extends MessageDispatcher
