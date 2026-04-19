/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: ServiceThreadFactory,count,newThread,prefixWithDash,thread
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/ServiceThreadFactory.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package file
package internal

import java.util.concurrent.{ Executors, ThreadFactory }
import java.util.concurrent.atomic.AtomicLong

/** @author Kotcrab */
class ServiceThreadFactory(threadPrefix: String) extends ThreadFactory {
  private val count:          AtomicLong = new AtomicLong(0)
  private val prefixWithDash: String     = threadPrefix + "-"

  override def newThread(runnable: Runnable): Thread = {
    val thread = Executors.defaultThreadFactory().newThread(runnable)
    thread.setName(prefixWithDash + count.getAndIncrement())
    thread.setDaemon(true)
    thread
  }
}
