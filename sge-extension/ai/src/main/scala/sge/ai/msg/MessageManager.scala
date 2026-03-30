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
