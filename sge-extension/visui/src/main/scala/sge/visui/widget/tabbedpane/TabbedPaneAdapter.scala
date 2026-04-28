/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 23
 * Covenant-baseline-methods: TabbedPaneAdapter,removedAllTabs,removedTab,switchedTab
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/tabbedpane/TabbedPaneAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
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
