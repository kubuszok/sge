/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
  override def selected(item: ItemT, view: ViewT): Unit = ()
  override def deselected(item: ItemT, view: ViewT): Unit = ()
}
