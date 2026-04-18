/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab, MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.kotcrab.vis.ui.widget.tabbedpane` -> `sge.visui.widget.tabbedpane`
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`
 *   Idiom: IdentityMap -> java.util.IdentityHashMap; Array -> ArrayBuffer
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package tabbedpane

import scala.language.implicitConversions

import sge.graphics.Color
import sge.math.{ Rectangle, Vector2 }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.ui.{ Button, ButtonGroup, Cell, Image }
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable, UIUtils }
import sge.utils.{ Nullable, Scaling }
import sge.visui.{ Locales, Sizes, VisUI }
import sge.visui.i18n.BundleText
import sge.visui.layout.{ DragPane, HorizontalFlowGroup, VerticalFlowGroup }
import sge.visui.util.dialog.Dialogs

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
    configureDragPane(style)

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

  private def configureDragPane(style: TabbedPane.TabbedPaneStyle): Unit = {
    tabsPane.touchable = Touchable.childrenOnly
    tabsPane.setListener(new DragPane.DragPaneListener.AcceptOwnChildren())
    if (style.draggable) {
      val draggable = new Draggable()
      draggable.setInvisibleWhenDragged(true)
      draggable.setKeepWithinParent(true)
      draggable.setBlockInput(true)
      draggable.setFadingTime(0f)
      draggable.listener = new DragPane.DefaultDragListener() {
        var dragged: Boolean = false

        override def onStart(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean = {
          dragged = false
          actor match {
            case tbt: TabbedPane.TabButtonTable =>
              if (tbt.closeButton.isOver) CANCEL
              else super.onStart(draggable, actor, stageX, stageY)
            case _ =>
              super.onStart(draggable, actor, stageX, stageY)
          }
        }

        override def onDrag(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Unit = {
          super.onDrag(draggable, actor, stageX, stageY)
          dragged = true
        }

        override def onEnd(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean = {
          val result = super.onEnd(draggable, actor, stageX, stageY)
          if (result == APPROVE) APPROVE
          else if (!dragged) CANCEL
          else {
            // check if any actor corner is over some other tab
            val tmpVector = TabbedPane.tmpVector
            val tmpRect   = TabbedPane.tmpRect
            tabsPane.stageToLocalCoordinates(tmpVector.set(stageX, stageY))
            if (tabsPane.hit(tmpVector.x, tmpVector.y, touchable = true).isDefined) CANCEL
            else if (tabsPane.hit(tmpVector.x + actor.width, tmpVector.y, touchable = true).isDefined) CANCEL
            else if (tabsPane.hit(tmpVector.x, tmpVector.y - actor.height, touchable = true).isDefined) CANCEL
            else if (tabsPane.hit(tmpVector.x + actor.width, tmpVector.y - actor.height, touchable = true).isDefined) CANCEL
            else {
              val stagePos = tabsPane.localToStageCoordinates(tmpVector.setZero())
              tmpRect.set(stagePos.x, stagePos.y, tabsPane.getGroup.width, tabsPane.getGroup.height)
              if (!tmpRect.contains(stageX, stageY)) CANCEL
              else if (tabsPane.isHorizontalFlow || tabsPane.isVerticalFlow) {
                DRAG_POSITION.set(stageX, stageY)
                tabsPane.addActor(actor)
                APPROVE
              } else {
                CANCEL
              }
            }
          }
        }
      }
      tabsPane.setDraggable(draggable)
    }
  }

  /** @return a direct reference to internal [[DragPane]]. Allows to manage [[Draggable]] settings. */
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
        Dialogs.showOptionDialog(
          _mainTable.stage.get,
          TabbedPane.Text.UNSAVED_DIALOG_TITLE.get,
          TabbedPane.Text.UNSAVED_DIALOG_TEXT.get,
          Dialogs.OptionDialogType.YES_NO_CANCEL,
          new Dialogs.OptionDialogAdapter {
            override def yes(): Unit = {
              tab.save()
              removeTab(tab)
            }
            override def no(): Unit =
              removeTab(tab)
          }
        )
        false
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

  /** Returns tabs in order in which they are displayed in the UI - user may drag and move tabs which DOES NOT affect their index. Use [[getTabs]] if you don't care about UI order. This creates new
    * array every time it's called!
    */
  def getUIOrderedTabs: ArrayBuffer[Tab] = {
    val result   = ArrayBuffer.empty[Tab]
    val children = getTabsPane.getChildren
    var i        = 0
    while (i < children.size) {
      children(i) match {
        case tbt: TabbedPane.TabButtonTable => result += tbt.tab
        case _ => ()
      }
      i += 1
    }
    result
  }

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

  private val tmpVector: Vector2   = new Vector2()
  private val tmpRect:   Rectangle = new Rectangle()

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

    def this(
      separatorBar: Nullable[Drawable],
      background:   Drawable,
      buttonStyle:  VisTextButton.VisTextButtonStyle,
      vertical:     Boolean,
      draggable:    Boolean
    ) = {
      this()
      this.separatorBar = separatorBar
      this.background = background
      this.buttonStyle = buttonStyle
      this.vertical = vertical
      this.draggable = draggable
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
  )(using sge: Sge)
      extends VisTable() {

    val button: VisTextButton = new VisTextButton(pane.getTabTitle(tab), style.buttonStyle) {
      override def disabled_=(isDisabled: Boolean): Unit = {
        super.disabled_=(isDisabled)
        closeButton.disabled = isDisabled
        deselect()
      }
    }
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
    private val up: Nullable[Drawable] = buttonStyle.up

    {
      addListeners()

      add(Nullable[Actor](button))
      if (tab.closeableByUser) {
        add(Nullable[Actor](closeButton)).size(14 * sizes.scaleFactor, button.height)
      }
    }

    private def addListeners(): Unit = {
      // add close button listener
      closeButton.addListener(new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          closeTabAsUser()
      })

      button.addListener(
        new InputListener() {
          private var isDown: Boolean = false

          override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, mouseButton: Input.Button): Boolean =
            if (button.disabled) {
              false
            } else {
              isDown = true
              if (UIUtils.left()) {
                setDraggedUpImage()
              }

              if (mouseButton == Input.Buttons.MIDDLE) {
                closeTabAsUser()
              }

              true
            }

          override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Input.Button): Unit = {
            setDefaultUpImage()
            isDown = false
          }

          override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
            if (!button.disabled && pane.activeTab.forall(_ != tab)) {
              setCloseButtonOnMouseMove()
            }
            false
          }

          override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
            if (!button.disabled && !isDown && pane.activeTab.forall(_ != tab) && pointer == -1) {
              setDefaultUpImage()
            }

          override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
            if (!button.disabled && pane.activeTab.forall(_ != tab) && !sge.input.justTouched() && pointer == -1) {
              setCloseButtonOnMouseMove()
            }

          private def setCloseButtonOnMouseMove(): Unit =
            if (isDown) {
              closeButtonStyle.up = buttonStyle.down
            } else {
              closeButtonStyle.up = buttonStyle.over
            }

          private def setDraggedUpImage(): Unit = {
            closeButtonStyle.up = buttonStyle.down
            buttonStyle.up = buttonStyle.down
          }

          private def setDefaultUpImage(): Unit = {
            closeButtonStyle.up = up
            buttonStyle.up = up
          }
        }
      )

      // add button change listener
      button.addListener(new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          switchToNewTab()
      })
    }

    private def switchToNewTab(): Unit =
      pane.handleTabSwitch(tab, this)

    /** Closes tab, does nothing if Tab is not closeable by user */
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

  private enum Text(val entryName: String) extends BundleText {
    case UNSAVED_DIALOG_TITLE extends Text("unsavedDialogTitle")
    case UNSAVED_DIALOG_TEXT extends Text("unsavedDialogText")

    private def getBundle = Locales.getTabbedPaneBundle(using VisUI.sgeInstance)

    override def name:                       String = entryName
    override def get:                        String = getBundle.get(entryName)
    override def format():                   String = getBundle.format(entryName)
    override def format(arguments: AnyRef*): String = getBundle.format(entryName, arguments*)
    override def toString:                   String = get
  }
}
