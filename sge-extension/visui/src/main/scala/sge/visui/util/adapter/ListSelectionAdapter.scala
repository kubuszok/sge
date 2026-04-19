/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 20
 * Covenant-baseline-methods: ListSelectionAdapter,deselected,selected
 * Covenant-source-reference: com/kotcrab/vis/ui/util/adapter/ListSelectionAdapter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util
package adapter

/** Empty [[AbstractListAdapter.ListSelectionListener]] implementation.
  * @author
  *   Kotcrab
  */
class ListSelectionAdapter[ItemT, ViewT] extends AbstractListAdapter.ListSelectionListener[ItemT, ViewT] {
  override def selected(item:   ItemT, view: ViewT): Unit = ()
  override def deselected(item: ItemT, view: ViewT): Unit = ()
}
