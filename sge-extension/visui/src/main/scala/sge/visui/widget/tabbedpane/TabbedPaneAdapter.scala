/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package tabbedpane

import sge.utils.Nullable

/** Empty implementation of [[TabbedPaneListener]].
  * @author
  *   Kotcrab
  */
class TabbedPaneAdapter extends TabbedPaneListener {
  override def switchedTab(tab: Nullable[Tab]): Unit = {}
  override def removedTab(tab:  Tab):           Unit = {}
  override def removedAllTabs():                Unit = {}
}
