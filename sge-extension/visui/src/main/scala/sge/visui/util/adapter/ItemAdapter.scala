/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 22
 * Covenant-baseline-methods: ItemAdapter,getView
 * Covenant-source-reference: com/kotcrab/vis/ui/util/adapter/ItemAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package adapter

import sge.scenes.scene2d.Actor

/** Generic use adapter used to create views for given objects.
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
trait ItemAdapter[ItemT] {
  def getView(item: ItemT): Actor
}
