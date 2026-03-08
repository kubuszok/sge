/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original: noop DesktopAudio for when audio is disabled or init fails
 *   Convention: extends DesktopAudio (Audio + AutoCloseable + update())
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package noop

/** A no-op [[DesktopAudio]] for when audio is disabled or initialization fails. Delegates all factory methods to [[NoopAudio]].
  */
class NoopDesktopAudio extends NoopAudio with DesktopAudio {

  override def update(): Unit = ()

  override def close(): Unit = ()
}
