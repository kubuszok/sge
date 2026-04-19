/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: TabbedPaneListener,removedAllTabs,removedTab,switchedTab
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/tabbedpane/TabbedPaneListener.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package tabbedpane

import sge.utils.Nullable

/** Listener used to get events from [[TabbedPane]].
  * @author
  *   Kotcrab
  */
trait TabbedPaneListener {

  /** Called when TabbedPane switched to new tab.
    * @param tab
    *   that TabbedPane switched to. May be null if all tabs were disabled or if [[TabbedPane.allowTabDeselect]] was set to true and all tabs were deselected.
    */
  def switchedTab(tab: Nullable[Tab]): Unit

  /** Called when Tab was removed from TabbedPane.
    * @param tab
    *   that was removed.
    */
  def removedTab(tab: Tab): Unit

  /** Called when all tabs were removed from TabbedPane. */
  def removedAllTabs(): Unit
}
