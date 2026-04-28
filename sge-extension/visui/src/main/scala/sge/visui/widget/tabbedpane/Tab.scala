/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 84
 * Covenant-baseline-methods: Tab,_activeTab,_dirty,_pane,checkSavable,close,contentTable,dirty,dirty_,dispose,isActiveTab,markDirty,onHide,onShow,pane,pane_,removeFromTabPane,save,tabTitle,update
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/tabbedpane/Tab.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package tabbedpane

import sge.scenes.scene2d.ui.Table
import sge.utils.Nullable

/** Base class for tabs used in TabbedPane. Tab can be savable, meaning that it can be saved and will display warning dialog 'do you want to save changes' before closing. Tab can be also closeable by
  * user meaning that user can close this tab manually from tabbed pane (using 'X' button or by pressing mouse wheel on tab).
  * @author
  *   Kotcrab
  */
abstract class Tab(val savable: Boolean = false, val closeableByUser: Boolean = true) extends AutoCloseable {

  private var _activeTab: Boolean              = false
  private var _pane:      Nullable[TabbedPane] = Nullable.empty
  private var _dirty:     Boolean              = false

  /** @return tab title used by tabbed pane. */
  def tabTitle: String

  /** @return
    *   table that contains this tab view, will be passed to tabbed pane listener. Should return same table every time this is called.
    */
  def contentTable: Table

  /** Called by pane when this tab becomes shown. Class overriding this should call super.onShow(). */
  def onShow(): Unit = _activeTab = true

  /** Called by pane when this tab becomes hidden. Class overriding this should call super.onHide(). */
  def onHide(): Unit = _activeTab = false

  /** @return true is this tab is currently active. */
  def isActiveTab: Boolean = _activeTab

  /** @return pane that this tab belongs to, or null. */
  def pane: Nullable[TabbedPane] = _pane

  /** Should be called by TabbedPane only, when tab is added to pane. */
  def pane_=(pane: Nullable[TabbedPane]): Unit = _pane = pane

  def dirty: Boolean = _dirty

  def dirty_=(dirty: Boolean): Unit = {
    checkSavable()
    val update = dirty != _dirty
    if (update) {
      _dirty = dirty
      if (_pane.isDefined) _pane.get.updateTabTitle(this)
    }
  }

  /** Marks this tab as dirty */
  def markDirty(): Unit = dirty = true

  /** Called when this tab should save its own state. After saving setDirty(false) must be called manually to remove dirty state.
    * @return
    *   true when save succeeded, false otherwise.
    */
  def save(): Boolean = {
    checkSavable()
    false
  }

  private def checkSavable(): Unit =
    if (!savable) throw new IllegalStateException(s"Tab $tabTitle is not savable!")

  /** Removes this tab from pane (if any). */
  def removeFromTabPane(): Unit =
    if (_pane.isDefined) _pane.get.remove(this)

  /** Called when tab is being removed from scene. */
  def dispose(): Unit = {}

  override def close(): Unit = dispose()
}
