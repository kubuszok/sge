/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: CachedItemAdapter,createView,getView,getViews,updateView,views
 * Covenant-source-reference: com/kotcrab/vis/ui/util/adapter/CachedItemAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package adapter

import scala.collection.mutable

import sge.scenes.scene2d.Actor

/** Implementation of [[ItemAdapter]] that caches created views. Provides two methods that are called when new view should be created and when old view should be updated (see [[createView]] and
  * [[updateView]]). Internal cache is not cleared automatically and obsolete entries must be removed manually.
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
abstract class CachedItemAdapter[ItemT, ViewT <: Actor] extends ItemAdapter[ItemT] {
  private val views: mutable.HashMap[ItemT, ViewT] = mutable.HashMap[ItemT, ViewT]()

  final override def getView(item: ItemT): ViewT =
    views.get(item) match {
      case Some(existing) =>
        updateView(existing, item)
        existing
      case None =>
        val created = createView(item)
        views.put(item, created)
        created
    }

  /** @return internal views cache map */
  protected def getViews: mutable.HashMap[ItemT, ViewT] = views

  protected def createView(item: ItemT): ViewT

  protected def updateView(view: ViewT, item: ItemT): Unit
}
