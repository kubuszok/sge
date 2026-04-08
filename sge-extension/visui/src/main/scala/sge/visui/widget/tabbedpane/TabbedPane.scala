/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab, MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/tabbedpane/TabbedPane.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - "Are you sure you want to close this tab?" Dialogs prompt skipped (Dialogs dependency
 *     not in this batch); tabs with `isDirty` close immediately instead.
 */
package sge
package visui
package widget
package tabbedpane

import scala.language.implicitConversions

import sge.graphics.Color
import sge.scenes.scene2d.{ Actor, Touchable }
import sge.scenes.scene2d.ui.{ Button, ButtonGroup, Cell, Image }
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.{ Nullable, Scaling }
import sge.visui.{ Sizes, VisUI }
import sge.visui.layout.{ DragPane, HorizontalFlowGroup, VerticalFlowGroup }

import scala.collection.mutable.ArrayBuffer
import java.util.IdentityHashMap

/** A tabbed pane, allows to have multiple tabs open and switch between them. TabbedPane does not handle displaying tab content, you have to do that manually using tabbed pane listener to get tab
  * content table.
  * @author
  *   Kotcrab
  * @author
  *   MJ
  * @since 0.7.0
  */
class TabbedPane(style: TabbedPane.TabbedPaneStyle, sizes: Sizes)(using sge: Sge) {

  private val sharedCloseActiveButtonStyle: VisImageButton.VisImageButtonStyle =
    VisUI.getSkin.get[VisImageButton.VisImageButtonStyle]("close-active-tab")

  private val group: ButtonGroup[Button] = new ButtonGroup[Button]()

  private val _mainTable: TabbedPane.TabbedPaneTable = new TabbedPane.TabbedPaneTable(this)
  private val tabsPane:   DragPane                   = new DragPane(
    if (style.vertical) new VerticalFlowGroup() else new HorizontalFlowGroup()
  )

  private val tabs:          ArrayBuffer[Tab]                                = ArrayBuffer.empty
  private val tabsButtonMap: IdentityHashMap[Tab, TabbedPane.TabButtonTable] = new IdentityHashMap[Tab, TabbedPane.TabButtonTable]()

  private var _activeTab:        Nullable[Tab]                   = Nullable.empty
  private val listeners:         ArrayBuffer[TabbedPaneListener] = ArrayBuffer.empty
  private var _allowTabDeselect: Boolean                         = false

  {
    tabsPane.touchable = Touchable.childrenOnly
    tabsPane.setListener(new DragPane.DragPaneListener.AcceptOwnChildren())

    _mainTable.setBackground(Nullable(style.background))

    val tabsPaneCell = _mainTable.add(Nullable[Actor](tabsPane))
    var separatorCell: Nullable[Cell[Image]] = Nullable.empty

    if (style.vertical) {
      tabsPaneCell.top().growY().minSize(0, 0)
    } else {
      tabsPaneCell.left().growX().minSize(0, 0)
    }

    if (style.separatorBar.isDefined) {
      if (style.vertical) {
        separatorCell = Nullable(
          _mainTable.add(Nullable[Actor](new Image(style.separatorBar.get))).growY().width(style.separatorBar.get.minWidth).asInstanceOf[Cell[Image]]
        )
      } else {
        _mainTable.row()
        separatorCell = Nullable(
          _mainTable.add(Nullable[Actor](new Image(style.separatorBar.get))).growX().height(style.separatorBar.get.minHeight).asInstanceOf[Cell[Image]]
        )
      }
    } else {
      if (style.vertical) {
        _mainTable.add(Nullable.empty[Actor]).growY()
      } else {
        _mainTable.add(Nullable.empty[Actor]).growX()
      }
    }

    _mainTable.setPaneCells(tabsPaneCell.asInstanceOf[Cell[DragPane]], separatorCell)
  }

  def this()(using Sge) = this(VisUI.getSkin.get[TabbedPane.TabbedPaneStyle], VisUI.getSizes)

  def this(styleName: String)(using Sge) = this(VisUI.getSkin.get[TabbedPane.TabbedPaneStyle](styleName), VisUI.getSizes)

  def this(style: TabbedPane.TabbedPaneStyle)(using Sge) = this(style, VisUI.getSizes)

  /** @return a direct reference to internal [[DragPane]]. */
  def getTabsPane: DragPane = tabsPane

  def allowTabDeselect_=(allow: Boolean): Unit = {
    _allowTabDeselect = allow
    if (allow) group.minCheckCount = 0
    else group.minCheckCount = 1
  }

  def allowTabDeselect: Boolean = _allowTabDeselect

  def add(tab: Tab): Unit = {
    tab.pane = Nullable(this)
    tabs += tab
    addTab(tab, tabsPane.getChildren.size)
    switchTab(tab)
  }

  def insert(index: Int, tab: Tab): Unit = {
    tab.pane = Nullable(this)
    tabs.insert(index, tab)
    addTab(tab, index)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  protected def addTab(tab: Tab, index: Int): Unit = {
    var buttonTable = tabsButtonMap.get(tab) // @nowarn -- IdentityHashMap returns null
    if (buttonTable == null) {
      buttonTable = new TabbedPane.TabButtonTable(tab, this, style, sizes, sharedCloseActiveButtonStyle)
      tabsButtonMap.put(tab, buttonTable)
    }

    buttonTable.touchable = Touchable.enabled
    if (index >= tabsPane.getChildren.size) {
      tabsPane.addActor(buttonTable)
    } else {
      tabsPane.addActorAt(index, buttonTable)
    }
    group.add(buttonTable.button)

    if (tabs.size == 1 && _activeTab.isDefined) {
      buttonTable.select()
      notifyListenersSwitched(Nullable(tab))
    } else if (_activeTab.isDefined && tab == _activeTab.get) {
      buttonTable.select()
    }
  }

  def disableTab(tab: Tab, disable: Boolean): Unit = {
    checkIfTabsBelongsToThisPane(tab)
    val buttonTable = tabsButtonMap.get(tab)
    buttonTable.button.disabled = disable

    if (_activeTab.isDefined && _activeTab.get == tab && disable) {
      if (selectFirstEnabledTab()) return
      _activeTab = Nullable.empty
      notifyListenersSwitched(Nullable.empty)
    }

    if (_activeTab.isEmpty && !_allowTabDeselect) {
      selectFirstEnabledTab()
    }
  }

  def isTabDisabled(tab: Tab): Boolean = {
    val table = tabsButtonMap.get(tab) // @nowarn -- IdentityHashMap returns null
    if (table == null) throwNotBelongingTabException(tab)
    table.button.disabled
  }

  private def selectFirstEnabledTab(): Boolean = {
    val iter  = tabsButtonMap.entrySet().iterator()
    var found = false
    while (iter.hasNext && !found) {
      val entry = iter.next()
      if (!entry.getValue.button.disabled) {
        switchTab(entry.getKey)
        found = true
      }
    }
    found
  }

  private def checkIfTabsBelongsToThisPane(tab: Tab): Unit =
    if (!tabs.contains(tab)) throwNotBelongingTabException(tab)

  protected def throwNotBelongingTabException(tab: Tab): Nothing =
    throw new IllegalArgumentException(s"Tab '${tab.tabTitle}' does not belong to this TabbedPane")

  /** Removes tab from pane, if tab is dirty this won't cause to display "Unsaved changes" dialog! */
  def remove(tab: Tab): Boolean = remove(tab, ignoreTabDirty = true)

  def remove(tab: Tab, ignoreTabDirty: Boolean): Boolean = {
    checkIfTabsBelongsToThisPane(tab)
    if (ignoreTabDirty) {
      removeTab(tab)
    } else {
      if (tab.dirty && _mainTable.stage.isDefined) {
        // In simplified port, just remove directly (Dialogs dependency not ported in this batch)
        removeTab(tab)
      } else {
        removeTab(tab)
      }
    }
  }

  private def removeTab(tab: Tab): Boolean = {
    val index   = tabs.indexOf(tab)
    val success = index >= 0
    if (success) tabs.remove(index)

    if (success) {
      val buttonTable = tabsButtonMap.get(tab)
      tabsPane.removeActor(buttonTable, true)
      tabsPane.invalidateHierarchy()
      tabsButtonMap.remove(tab)
      group.remove(buttonTable.button)

      tab.pane = Nullable.empty
      tab.onHide()
      tab.dispose()
      notifyListenersRemoved(tab)

      if (tabs.size == 0) {
        _activeTab = Nullable.empty
        notifyListenersRemovedAll()
      } else if (_activeTab.isDefined && _activeTab.get == tab) {
        if (index > 0) {
          switchTab(index - 1)
        } else {
          switchTab(index)
        }
      }
    }

    success
  }

  /** Removes all tabs, ignores if tab is dirty */
  def removeAll(): Unit = {
    tabs.foreach { tab =>
      tab.pane = Nullable.empty
      tab.onHide()
      tab.dispose()
    }

    tabs.clear()
    tabsButtonMap.clear()
    tabsPane.clear()
    _activeTab = Nullable.empty

    notifyListenersRemovedAll()
  }

  def switchTab(index: Int): Unit =
    tabsButtonMap.get(tabs(index)).select()

  def switchTab(tab: Tab): Unit = {
    val table = tabsButtonMap.get(tab) // @nowarn -- IdentityHashMap returns null
    if (table == null) throwNotBelongingTabException(tab)
    table.select()
  }

  /** Must be called when you want to update tab title. */
  def updateTabTitle(tab: Tab): Unit = {
    val table = tabsButtonMap.get(tab) // @nowarn -- IdentityHashMap returns null
    if (table == null) throwNotBelongingTabException(tab)
    table.button.setText(Nullable(getTabTitle(tab)))
  }

  protected def getTabTitle(tab: Tab): String =
    if (tab.dirty) "*" + tab.tabTitle else tab.tabTitle

  def table: TabbedPane.TabbedPaneTable = _mainTable

  /** @return active tab or null if no tab is selected. */
  def activeTab: Nullable[Tab] = _activeTab

  def addListener(listener: TabbedPaneListener): Unit = listeners += listener

  def removeListener(listener: TabbedPaneListener): Boolean = {
    val idx = listeners.indexOf(listener)
    if (idx >= 0) { listeners.remove(idx); true }
    else false
  }

  private def notifyListenersSwitched(tab: Nullable[Tab]): Unit =
    listeners.foreach(_.switchedTab(tab))

  private def notifyListenersRemoved(tab: Tab): Unit =
    listeners.foreach(_.removedTab(tab))

  private def notifyListenersRemovedAll(): Unit =
    listeners.foreach(_.removedAllTabs())

  /** Returns tabs in order in which they are stored in tabbed pane, sorted by their index and ignoring their order in UI.
    */
  def getTabs: ArrayBuffer[Tab] = tabs

  // switchToNewTab is called from TabButtonTable; we expose it via package-private
  private[tabbedpane] def handleTabSwitch(tab: Tab, buttonTable: TabbedPane.TabButtonTable): Unit = {
    if (_activeTab.isDefined && _activeTab.get != tab) {
      val prevTable = tabsButtonMap.get(_activeTab.get)
      if (prevTable != null) { // @nowarn -- IdentityMap returns null
        prevTable.deselect()
        _activeTab.get.onHide()
      }
    }

    if (buttonTable.button.checked && (_activeTab.isEmpty || tab != _activeTab.get)) {
      _activeTab = Nullable(tab)
      notifyListenersSwitched(Nullable(tab))
      tab.onShow()
      buttonTable.closeButton.setStyle(sharedCloseActiveButtonStyle)
    } else if (group.checkedIndex == -1) {
      _activeTab = Nullable.empty
      notifyListenersSwitched(Nullable.empty)
    }
  }
}

object TabbedPane {

  class TabbedPaneStyle {
    var background:  Drawable                         = scala.compiletime.uninitialized
    var buttonStyle: VisTextButton.VisTextButtonStyle = scala.compiletime.uninitialized

    /** Optional. */
    var separatorBar: Nullable[Drawable] = Nullable.empty

    /** Optional, defaults to false. */
    var vertical: Boolean = false

    /** Optional, defaults to true. */
    var draggable: Boolean = true

    def this(style: TabbedPaneStyle) = {
      this()
      this.background = style.background
      this.buttonStyle = style.buttonStyle
      this.separatorBar = style.separatorBar
      this.vertical = style.vertical
      this.draggable = style.draggable
    }

    def this(background: Drawable, separatorBar: Nullable[Drawable], buttonStyle: VisTextButton.VisTextButtonStyle) = {
      this()
      this.background = background
      this.separatorBar = separatorBar
      this.buttonStyle = buttonStyle
    }
  }

  class TabbedPaneTable(val tabbedPane: TabbedPane)(using Sge) extends VisTable() {
    private var _tabsPaneCell:  Cell[DragPane]        = scala.compiletime.uninitialized
    private var _separatorCell: Nullable[Cell[Image]] = Nullable.empty

    private[tabbedpane] def setPaneCells(tabsPaneCell: Cell[DragPane], separatorCell: Nullable[Cell[Image]]): Unit = {
      _tabsPaneCell = tabsPaneCell
      _separatorCell = separatorCell
    }

    def tabsPaneCell:  Cell[DragPane]        = _tabsPaneCell
    def separatorCell: Nullable[Cell[Image]] = _separatorCell
  }

  private[tabbedpane] class TabButtonTable(
    val tab:                      Tab,
    pane:                         TabbedPane,
    style:                        TabbedPaneStyle,
    sizes:                        Sizes,
    sharedCloseActiveButtonStyle: VisImageButton.VisImageButtonStyle
  )(using Sge)
      extends VisTable() {

    val button: VisTextButton = new VisTextButton(pane.getTabTitle(tab), style.buttonStyle)
    button.focusBorderEnabled = false
    button.programmaticChangeEvents = false

    private val closeButtonStyle: VisImageButton.VisImageButtonStyle =
      new VisImageButton.VisImageButtonStyle(VisUI.getSkin.get[VisImageButton.VisImageButtonStyle]("close"))

    val closeButton: VisImageButton = new VisImageButton(closeButtonStyle)
    closeButton.setGenerateDisabledImage(true)
    closeButton.image.scaling = Scaling.fill
    closeButton.image.color.set(Color.RED)

    private val buttonStyle: VisTextButton.VisTextButtonStyle =
      new VisTextButton.VisTextButtonStyle(button.style.asInstanceOf[VisTextButton.VisTextButtonStyle])
    button.setStyle(buttonStyle)

    {
      // add close button listener
      closeButton.addListener(new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          closeTabAsUser()
      })

      // add button change listener
      button.addListener(new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          switchToNewTab()
      })

      add(Nullable[Actor](button))
      if (tab.closeableByUser) {
        add(Nullable[Actor](closeButton)).size(14 * sizes.scaleFactor, button.height)
      }
    }

    private def switchToNewTab(): Unit =
      pane.handleTabSwitch(tab, this)

    private def closeTabAsUser(): Unit =
      if (tab.closeableByUser) {
        pane.remove(tab, ignoreTabDirty = false)
      }

    def select(): Unit = {
      button.setChecked(true)
      switchToNewTab()
    }

    def deselect(): Unit =
      closeButton.setStyle(closeButtonStyle)
  }
}
