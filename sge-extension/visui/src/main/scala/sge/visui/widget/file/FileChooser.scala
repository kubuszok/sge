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
package file

import scala.language.implicitConversions

import java.io.{ File, FileFilter, IOException }
import java.text.SimpleDateFormat
import java.util.Comparator
import java.util.concurrent.{ ExecutorService, Executors, Future }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

import sge.Input
import sge.Input.{ Buttons, Keys }
import sge.files.FileHandle
import sge.graphics.g2d.Batch
import sge.math.Vector2
import sge.scenes.scene2d.*
import sge.scenes.scene2d.ui.*
import sge.scenes.scene2d.utils.*
import sge.utils.*
import sge.visui.{ FocusManager, Focusable, Sizes, VisUI }
import sge.visui.layout.GridGroup
import sge.visui.util.OsUtils
import sge.visui.util.dialog.Dialogs
import sge.visui.util.dialog.Dialogs.OptionDialogType
import sge.visui.util.value.{ ConstantIfVisibleValue, PrefHeightIfVisibleValue, PrefWidthIfVisibleValue }
import sge.visui.widget.*
import sge.visui.widget.file.internal.*
import sge.visui.widget.file.internal.FileChooserText.*

/** Widget allowing user to choose files. FileChooser is heavy widget and should be reused whenever possible, typically one instance is enough for application. Chooser is platform dependent and can be
  * only used on desktop.
  *
  * FileChooser will be centered on screen after adding to Stage use [[setCenterOnAdd]] to change this.
  * @author
  *   Kotcrab
  * @since 0.1.0
  */
class FileChooser(private var _mode: FileChooser.Mode)(using Sge) extends VisWindow("") with FileHistoryManager.FileHistoryCallback {

  import FileChooser.*

  private val _sorting:               AtomicReference[FileSorting] = new AtomicReference[FileSorting](FileSorting.NAME)
  private val _sortingOrderAscending: AtomicBoolean                = new AtomicBoolean(true)

  private var _viewMode:           ViewMode                      = ViewMode.DETAILS
  private var _selectionMode:      SelectionMode                 = SelectionMode.FILES
  private var _listener:           FileChooserListener           = new FileChooserAdapter()
  private var _fileFilter:         FileFilter                    = new DefaultFileFilter(this)
  private var _fileDeleter:        FileDeleter                   = new DefaultFileDeleter()
  private var _fileTypeFilter:     Nullable[FileTypeFilter]      = Nullable.empty
  private var _activeFileTypeRule: Nullable[FileTypeFilter.Rule] = Nullable.empty
  private var _iconProvider:       FileIconProvider              = scala.compiletime.uninitialized

  private val driveCheckerService:   DriveCheckerService                                    = DriveCheckerService.getInstance
  private val driveCheckerListeners: DynamicArray[DriveCheckerService.DriveCheckerListener] = DynamicArray()
  private val chooserWinService:     Nullable[FileChooserWinService]                        = FileChooserWinService.getInstance

  private val listDirExecutor: ExecutorService     = Executors.newSingleThreadExecutor(new ServiceThreadFactory("FileChooserListDirThread"))
  private var listDirFuture:   Nullable[Future[?]] = Nullable.empty
  private val showBusyBarTask: ShowBusyBarTask     = new ShowBusyBarTask()

  private val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")

  private var _showSelectionCheckboxes: Boolean = false
  private var _multiSelectionEnabled:   Boolean = false
  private var _groupMultiSelectKey:     Int     = DEFAULT_KEY
  private var _multiSelectKey:          Int     = DEFAULT_KEY

  private var preferencesIO:     PreferencesIO            = scala.compiletime.uninitialized
  private var favorites:         DynamicArray[FileHandle] = scala.compiletime.uninitialized
  private var recentDirectories: DynamicArray[FileHandle] = scala.compiletime.uninitialized

  private var currentDirectory:     FileHandle                                                = scala.compiletime.uninitialized
  private val currentFiles:         DynamicArray[FileHandle]                                  = DynamicArray[FileHandle]()
  private var currentFilesMetadata: java.util.IdentityHashMap[FileHandle, FileHandleMetadata] = new java.util.IdentityHashMap[FileHandle, FileHandleMetadata]()
  private var fileListAdapter:      FileListAdapter                                           = scala.compiletime.uninitialized
  private val selectedItems:        DynamicArray[FileItem]                                    = DynamicArray[FileItem]()
  private var selectedShortcut:     Nullable[ShortcutItem]                                    = Nullable.empty
  private var _defaultFileName:     Nullable[String]                                          = Nullable.empty

  private var _watchingFilesEnabled:         Boolean          = true
  private var fileWatcherThread:             Nullable[Thread] = Nullable.empty
  private var shortcutsListRebuildScheduled: Boolean          = false
  private var filesListRebuildScheduled:     Boolean          = false

  private var historyManager: FileHistoryManager = scala.compiletime.uninitialized

  // UI
  private var _chooserStyle: FileChooserStyle = scala.compiletime.uninitialized
  private var sizes:         Sizes            = scala.compiletime.uninitialized

  private var mainSplitPane:           VisSplitPane         = scala.compiletime.uninitialized
  private var shortcutsTable:          VisTable             = scala.compiletime.uninitialized
  private var shortcutsMainPanel:      VerticalGroup        = scala.compiletime.uninitialized
  private var shortcutsRootsPanel:     VerticalGroup        = scala.compiletime.uninitialized
  private var shortcutsFavoritesPanel: VerticalGroup        = scala.compiletime.uninitialized
  private var fileListView:            ListView[FileHandle] = scala.compiletime.uninitialized
  private var maxDateLabelWidth:       Float                = 0f
  private var fileListBusyBar:         BusyBar              = scala.compiletime.uninitialized

  private var favoriteFolderButton:        VisImageButton                    = scala.compiletime.uninitialized
  private var viewModeButton:              VisImageButton                    = scala.compiletime.uninitialized
  private var favoriteFolderButtonTooltip: sge.visui.widget.Tooltip          = scala.compiletime.uninitialized
  private var currentPath:                 VisTextField                      = scala.compiletime.uninitialized
  private var selectedFileTextField:       VisTextField                      = scala.compiletime.uninitialized
  private var fileTypeSelectBox:           VisSelectBox[FileTypeFilter.Rule] = scala.compiletime.uninitialized

  private var confirmButton:           VisTextButton       = scala.compiletime.uninitialized
  private var fileMenu:                FilePopupMenu       = scala.compiletime.uninitialized
  private var fileNameSuggestionPopup: FileSuggestionPopup = scala.compiletime.uninitialized
  private var dirsSuggestionPopup:     DirsSuggestionPopup = scala.compiletime.uninitialized
  private var fileTypeLabel:           VisLabel            = scala.compiletime.uninitialized
  private var viewModePopupMenu:       PopupMenu           = scala.compiletime.uninitialized

  {
    titleLabel.setText(TITLE_CHOOSE_FILES.get)
    _chooserStyle = VisUI.getSkin.get(classOf[FileChooserStyle])
    sizes = VisUI.getSizes
    init(Nullable.empty)
  }

  def this(directory: FileHandle, mode: FileChooser.Mode)(using Sge) = {
    this(mode)
    _chooserStyle = VisUI.getSkin.get(classOf[FileChooserStyle])
    sizes = VisUI.getSizes
    init(Nullable(directory))
  }

  def this(title: String, mode: FileChooser.Mode)(using Sge) = {
    this(mode)
    titleLabel.setText(title)
  }

  def this(styleName: String, title: String, mode: FileChooser.Mode)(using Sge) = {
    this(mode)
    titleLabel.setText(title)
    _chooserStyle = VisUI.getSkin.get(styleName, classOf[FileChooserStyle])
    sizes = VisUI.getSizes
    init(Nullable.empty)
  }

  private def init(directory: Nullable[FileHandle]): Unit = {
    isModal = true
    isResizable = true
    isMovable = true
    addCloseButton()
    closeOnEscape()

    _iconProvider = new DefaultFileIconProvider(this)
    preferencesIO = new PreferencesIO()
    reloadPreferences(rebuildUI = false)

    createToolbar()
    viewModePopupMenu = new PopupMenu(_chooserStyle.popupMenuStyle.get)
    createViewModePopupMenu()
    createCenterContentPanel()
    createFileTextBox()
    createBottomButtons()

    createShortcutsMainPanel()
    shortcutsRootsPanel = new VerticalGroup()
    shortcutsFavoritesPanel = new VerticalGroup()
    rebuildShortcutsFavoritesPanel()

    fileMenu = new FilePopupMenu(
      this,
      new FilePopupMenu.FilePopupMenuCallback {
        override def showNewDirDialog():                  Unit = showNewDirectoryDialog()
        override def showFileDelDialog(file: FileHandle): Unit = showFileDeleteDialog(file)
      }
    )

    fileNameSuggestionPopup = new FileSuggestionPopup(this)
    fileNameSuggestionPopup.popupMenuListener = new PopupMenu.PopupMenuListener {
      override def activeItemChanged(newActiveItem: Nullable[MenuItem], changedByKeyboard: Boolean): Unit = {
        if (!changedByKeyboard || newActiveItem.isEmpty) { return; } // @nowarn -- early return
        highlightFiles(currentDirectory.child(newActiveItem.get.getText))
        updateSelectedFileFieldText(ignoreKeyboardFocus = true)
      }
    }
    dirsSuggestionPopup = new DirsSuggestionPopup(this, currentPath)

    rebuildShortcutsList()

    if (directory.isEmpty) {
      var startingDir: Nullable[FileHandle] = Nullable.empty
      if (FileChooser.saveLastDirectory) startingDir = preferencesIO.loadLastDirectory()
      if (startingDir.isEmpty || !startingDir.get.exists()) startingDir = Nullable(getDefaultStartingDirectory)
      setDirectory(startingDir.get, HistoryPolicy.IGNORE)
    } else {
      setDirectory(directory.get, HistoryPolicy.IGNORE)
    }

    setSize(500, 600)
    centerWindow()
    createListeners()
    setFileTypeFilter(Nullable.empty)
    favoriteFolderButton.visible = false
  }

  private def createToolbar(): Unit = {
    val toolbarTable = new VisTable(true)
    toolbarTable.defaults().minWidth(30).right()
    add(Nullable[Actor](toolbarTable)).fillX().expandX().pad(3).padRight(2)

    historyManager = new FileHistoryManager(_chooserStyle, this)
    currentPath = new VisTextField()
    val showRecentDirButton = new VisImageButton(_chooserStyle.expandDropdown.get)
    showRecentDirButton.focusBorderEnabled = false

    dirsSuggestionPopup = new DirsSuggestionPopup(this, currentPath)
    dirsSuggestionPopup.popupMenuListener = new PopupMenu.PopupMenuListener {
      override def activeItemChanged(newActiveItem: Nullable[MenuItem], changedByKeyboard: Boolean): Unit = {
        if (!changedByKeyboard || newActiveItem.isEmpty) { return; } // @nowarn -- early return
        setCurrentPathFieldText(newActiveItem.get.getText)
      }
    }

    currentPath.addListener(
      new InputListener() {
        override def keyTyped(event: InputEvent, character: Char): Boolean =
          if (event.keyCode == Keys.ENTER) {
            dirsSuggestionPopup.remove()
            false
          } else {
            val targetWidth = currentPath.width + showRecentDirButton.width
            dirsSuggestionPopup.pathFieldKeyTyped(getChooserStage, targetWidth)
            false
          }

        override def keyDown(event: InputEvent, keycode: sge.Input.Key): Boolean = {
          if (keycode == Keys.ENTER) {
            val file = Sge().files.absolute(currentPath.text)
            if (file.exists()) {
              val dir = if (!file.isDirectory()) file.parent() else file
              setDirectory(dir, HistoryPolicy.ADD)
              addRecentDirectory(dir)
            } else {
              showDialog(POPUP_DIRECTORY_DOES_NOT_EXIST.get)
              setCurrentPathFieldText(currentDirectory.path)
            }
            event.stop()
          }
          false
        }
      }
    )

    currentPath.addListener(
      new FocusListener() {
        override def keyboardFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit =
          if (!focused) setCurrentPathFieldText(currentDirectory.path)
      }
    )

    showRecentDirButton.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          val targetWidth = currentPath.width + showRecentDirButton.width
          dirsSuggestionPopup.showRecentDirectories(getChooserStage, recentDirectories, targetWidth)
        }
      }
    )

    val folderParentButton = new VisImageButton(_chooserStyle.iconFolderParent.get)
    new sge.visui.widget.Tooltip.Builder(PARENT_DIRECTORY.get).target(folderParentButton).build()
    favoriteFolderButton = new VisImageButton(_chooserStyle.iconStar.get)
    favoriteFolderButtonTooltip = new sge.visui.widget.Tooltip.Builder(CONTEXT_MENU_ADD_TO_FAVORITES.get).target(favoriteFolderButton).build()
    viewModeButton = new VisImageButton(_chooserStyle.iconListSettings.get)
    new sge.visui.widget.Tooltip.Builder(CHANGE_VIEW_MODE.get).target(viewModeButton).build()
    val folderNewButton = new VisImageButton(_chooserStyle.iconFolderNew.get)
    new sge.visui.widget.Tooltip.Builder(NEW_DIRECTORY.get).target(folderNewButton).build()

    toolbarTable.add(Nullable[Actor](historyManager.getButtonsTable))
    toolbarTable.add(Nullable[Actor](currentPath)).spaceRight(0).expand().fill()
    toolbarTable.add(Nullable[Actor](showRecentDirButton)).width(15 * sizes.scaleFactor).growY()
    toolbarTable.add(Nullable[Actor](folderParentButton))
    toolbarTable.add(Nullable[Actor](favoriteFolderButton)).width(PrefWidthIfVisibleValue.INSTANCE).spaceRight(new ConstantIfVisibleValue(sizes.spacingRight))
    toolbarTable.add(Nullable[Actor](viewModeButton)).width(PrefWidthIfVisibleValue.INSTANCE).spaceRight(new ConstantIfVisibleValue(sizes.spacingRight))
    toolbarTable.add(Nullable[Actor](folderNewButton))

    folderParentButton.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          val parent = currentDirectory.parent()
          if (OsUtils.isWindows && currentDirectory.path.endsWith(":/")) { return; } // @nowarn -- Java interop for early return
          setDirectory(parent, HistoryPolicy.ADD)
        }
      }
    )

    favoriteFolderButton.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          if (favorites.contains(currentDirectory)) removeFavorite(currentDirectory)
          else addFavorite(currentDirectory)
      }
    )

    folderNewButton.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = showNewDirectoryDialog()
    })

    addListener(historyManager.getDefaultClickListener)
  }

  private def createViewModePopupMenu(): Unit = {
    rebuildViewModePopupMenu()
    viewModeButton.addListener(
      new InputListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
          // show menu on next frame, without it menu would be closed instantly it was opened
          // the other solution is to call event.stop but this could lead to some other PopupMenu not being closed
          // on touchDown event because event.stop stops event propagation
          Sge().application.postRunnable(new Runnable {
            override def run(): Unit = viewModePopupMenu.showMenu(getChooserStage, viewModeButton)
          })
          true
        }
      }
    )
  }

  private def rebuildViewModePopupMenu(): Unit = {
    viewModePopupMenu.clearChildren()
    for (mode <- ViewMode.values)
      if (!mode.thumbnailMode || _iconProvider.isThumbnailModesSupported) {
        val capturedMode = mode
        viewModePopupMenu.addItem(
          new MenuItem(
            mode.getBundleText,
            new ChangeListener() {
              override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = setViewMode(capturedMode)
            }
          )
        )
      }
  }

  private def createCenterContentPanel(): Unit = {
    fileListAdapter = new FileListAdapter(this, currentFiles)
    fileListView = new ListView[FileHandle](fileListAdapter)

    val fileScrollPaneTable = new VisTable()
    fileListBusyBar = new BusyBar()
    fileListBusyBar.visible = false
    fileScrollPaneTable.add(Nullable[Actor](fileListBusyBar)).space(0).height(PrefHeightIfVisibleValue.INSTANCE).growX().row()
    fileScrollPaneTable.add(Nullable[Actor](fileListView.getMainTable)).pad(2).top().expand().fillX()
    fileScrollPaneTable.touchable = Touchable.enabled

    shortcutsTable = new VisTable()
    val shortcutsScrollPane      = setupDefaultScrollPane(new VisScrollPane(shortcutsTable))
    val shortcutsScrollPaneTable = new VisTable()
    shortcutsScrollPaneTable.add(Nullable[Actor](shortcutsScrollPane)).pad(2).top().expand().fillX()

    mainSplitPane = new VisSplitPane(shortcutsScrollPaneTable, fileScrollPaneTable, false) {
      override def invalidate(): Unit = {
        super.invalidate()
        invalidateChildHierarchy(shortcutsScrollPane)
      }
    }
    mainSplitPane.setSplitAmount(0.3f)
    mainSplitPane.setMinSplitAmount(0.05f)
    mainSplitPane.setMaxSplitAmount(0.80f)

    row()
    add(Nullable[Actor](mainSplitPane)).expand().fill()
    row()

    fileScrollPaneTable.addListener(
      new InputListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = true

        override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
          if (button == Buttons.RIGHT && !fileMenu.isAddedToStage) {
            fileMenu.build()
            fileMenu.showMenu(getChooserStage, event.stageX, event.stageY)
          }
      }
    )
  }

  private def setCurrentPathFieldText(text: String): Unit = {
    currentPath.setText(text)
    currentPath.setCursorAtTextEnd()
  }

  private def createFileTextBox(): Unit = {
    val table     = new VisTable(true)
    val nameLabel = new VisLabel(FILE_NAME.get)
    selectedFileTextField = new VisTextField()
    selectedFileTextField.setProgrammaticChangeEvents(false)

    fileTypeLabel = new VisLabel(FILE_TYPE.get)
    fileTypeSelectBox = new VisSelectBox[FileTypeFilter.Rule]()
    fileTypeSelectBox.selection.programmaticChangeEvents = false

    fileTypeSelectBox.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          _activeFileTypeRule = fileTypeSelectBox.selected
          rebuildFileList()
        }
      }
    )

    table.defaults().left()
    table.add(Nullable[Actor](nameLabel))
    table.add(Nullable[Actor](selectedFileTextField)).expandX().fillX().row()
    table.add(Nullable[Actor](fileTypeLabel)).height(PrefHeightIfVisibleValue.INSTANCE)
    table.add(Nullable[Actor](fileTypeSelectBox)).height(PrefHeightIfVisibleValue.INSTANCE).expand().fill()

    selectedFileTextField.addListener(
      new InputListener() {
        override def keyDown(event: InputEvent, keycode: sge.Input.Key): Boolean =
          if (keycode == Keys.ENTER) { selectionFinished(); true }
          else false
      }
    )

    selectedFileTextField.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          deselectAll(updateTextField = false)
          fileNameSuggestionPopup.pathFieldKeyTyped(getChooserStage, currentFiles, selectedFileTextField)
          val enteredFile = currentDirectory.child(selectedFileTextField.text)
          if (currentFiles.contains(enteredFile)) highlightFiles(enteredFile)
        }
      }
    )

    add(Nullable[Actor](table)).expandX().fillX().pad(3f).padRight(2f).padBottom(2f)
    row()
  }

  private def createBottomButtons(): Unit = {
    val cancelButton = new VisTextButton(CANCEL.get)
    confirmButton = new VisTextButton(if (_mode == Mode.OPEN) OPEN.get else SAVE.get)

    val buttonTable = new VisTable(true)
    buttonTable.defaults().minWidth(70).right()
    add(Nullable[Actor](buttonTable)).padTop(3).padBottom(3).padRight(2).fillX().expandX()

    val buttonBar = new ButtonBar()
    buttonBar.ignoreSpacing = true
    buttonBar.setButton(ButtonBar.ButtonType.CANCEL, cancelButton)
    buttonBar.setButton(ButtonBar.ButtonType.OK, confirmButton)
    buttonTable.add(Nullable[Actor](buttonBar.createTable())).expand().right()

    cancelButton.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          fadeOut()
          _listener.canceled()
        }
      }
    )

    confirmButton.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = selectionFinished()
    })
  }

  private def createShortcutsMainPanel(): Unit = {
    shortcutsMainPanel = new VerticalGroup()
    val userHome    = System.getProperty("user.home")
    val userName    = System.getProperty("user.name")
    val userDesktop = new File(userHome + "/Desktop")
    if (userDesktop.exists()) shortcutsMainPanel.addActor(new ShortcutItem(userDesktop, DESKTOP.get, _chooserStyle.iconFolder.get))
    shortcutsMainPanel.addActor(new ShortcutItem(new File(userHome), userName, _chooserStyle.iconFolder.get))
  }

  private def createListeners(): Unit =
    addListener(
      new InputListener() {
        override def keyDown(event: InputEvent, keycode: sge.Input.Key): Boolean =
          if (keycode == Keys.A && UIUtils.ctrl() && !getChooserStage.keyboardFocus.isInstanceOf[VisTextField]) {
            selectAll(); true
          } else false

        override def keyTyped(event: InputEvent, character: Char): Boolean =
          if (getChooserStage.keyboardFocus.isInstanceOf[VisTextField]) false
          else if (!Character.isLetterOrDigit(character)) false
          else {
            val name  = String.valueOf(character)
            val iter  = currentFiles.iterator
            var found = false
            while (iter.hasNext && !found) {
              val file = iter.next()
              if (file.name.toLowerCase.startsWith(name)) {
                deselectAll()
                highlightFiles(file)
                found = true
              }
            }
            found
          }
      }
    )

  private def invalidateChildHierarchy(layout: WidgetGroup): Unit =
    if (layout != null) { // @nowarn -- Java interop boundary
      layout.invalidate()
      val children = layout.children
      var i        = 0
      while (i < children.size) {
        children(i) match {
          case wg: WidgetGroup => invalidateChildHierarchy(wg)
          case l:  Layout      => l.invalidate()
          case _ => ()
        }
        i += 1
      }
    }

  def setVisible(v: Boolean): Unit = {
    if (!visible && v) deselectAll() // reset selected item when dialog is changed from invisible to visible
    visible = v
  }

  private def selectionFinished(): Unit = {
    if (selectedItems.size == 1) {
      if (_selectionMode == SelectionMode.FILES) {
        val selected = selectedItems(0).getFile
        if (selected.isDirectory()) { setDirectory(selected, HistoryPolicy.ADD); return; } // @nowarn -- early return
      }
      if (_selectionMode == SelectionMode.DIRECTORIES) {
        val selected = selectedItems(0).getFile
        if (!selected.isDirectory()) { showDialog(POPUP_ONLY_DIRECTORIES.get); return; } // @nowarn -- early return
      }
    }

    if (selectedItems.size > 0 || _mode == Mode.SAVE) {
      val files = getFileListFromSelected()
      notifyListenerAndCloseDialog(files)
    } else {
      if (_selectionMode == SelectionMode.FILES) {
        showDialog(POPUP_CHOOSE_FILE.get)
      } else {
        val files = DynamicArray[FileHandle]()
        if (selectedFileTextField.text.length != 0) files.add(currentDirectory.child(selectedFileTextField.text))
        else files.add(currentDirectory)
        notifyListenerAndCloseDialog(Nullable(files))
      }
    }
  }

  override protected def close(): Unit = {
    _listener.canceled()
    super.close()
  }

  private def notifyListenerAndCloseDialog(files: Nullable[DynamicArray[FileHandle]]): Unit = {
    if (files.isEmpty) { return; } // @nowarn -- early return
    val f = files.get

    if (_mode == Mode.OPEN) {
      val iter = f.iterator
      while (iter.hasNext)
        if (!iter.next().exists()) { showDialog(POPUP_SELECTED_FILE_DOES_NOT_EXIST.get); return; } // @nowarn -- early return
    }

    if (f.size != 0) {
      _listener.selected(f)
      if (FileChooser.saveLastDirectory) preferencesIO.saveLastDirectory(currentDirectory)
    }
    fadeOut()
  }

  override def fadeOut(time: Float): Unit = {
    super.fadeOut(time)
    fileMenu.remove()
    dirsSuggestionPopup.remove()
    fileNameSuggestionPopup.remove()
    viewModePopupMenu.remove()
  }

  protected def setupDefaultScrollPane(scrollPane: VisScrollPane): VisScrollPane = {
    scrollPane.setOverscroll(false, false)
    scrollPane.setFlickScroll(false)
    scrollPane.setFadeScrollBars(false)
    scrollPane.setScrollingDisabled(true, false)
    scrollPane
  }

  private def getFileListFromSelected(): Nullable[DynamicArray[FileHandle]] = {
    val list = DynamicArray[FileHandle]()
    if (_mode == Mode.OPEN) {
      val iter = selectedItems.iterator
      while (iter.hasNext) list.add(iter.next().getFile)
      Nullable(list)
    } else if (selectedItems.size > 0) {
      val iter = selectedItems.iterator
      while (iter.hasNext) list.add(iter.next().getFile)
      showOverwriteQuestion(list)
      Nullable.empty
    } else {
      val fileName = selectedFileTextField.text
      var file     = currentDirectory.child(fileName)
      if (!FileUtils.isValidFileName(fileName)) { showDialog(POPUP_FILENAME_INVALID.get); Nullable.empty }
      else if (file.exists()) { list.add(file); showOverwriteQuestion(list); Nullable.empty }
      else {
        if (_activeFileTypeRule.isDefined) {
          val ruleExts = _activeFileTypeRule.get.getExtensions
          if (ruleExts.size > 0 && !ruleExts.contains(file.extension)) {
            file = file.sibling(file.nameWithoutExtension + "." + ruleExts.first)
          }
        }
        list.add(file)
        if (file.exists()) { showOverwriteQuestion(list); Nullable.empty }
        else Nullable(list)
      }
    }
  }

  private def showDialog(text: String): Unit = Dialogs.showOKDialog(getChooserStage, POPUP_TITLE.get, text)

  private def showOverwriteQuestion(filesList: DynamicArray[FileHandle]): Unit = {
    val text = if (filesList.size == 1) POPUP_FILE_EXIST_OVERWRITE.get else POPUP_MULTIPLE_FILE_EXIST_OVERWRITE.get
    Dialogs.showOptionDialog(
      getChooserStage,
      POPUP_TITLE.get,
      text,
      OptionDialogType.YES_NO,
      new Dialogs.OptionDialogAdapter() {
        override def yes(): Unit = notifyListenerAndCloseDialog(Nullable(filesList))
      }
    )
  }

  private def rebuildShortcutsList(rebuildRootCache: Boolean): Unit = {
    shortcutsTable.clear()
    shortcutsTable.add(shortcutsMainPanel).left().row()
    shortcutsTable.addSeparator()
    if (rebuildRootCache) rebuildFileRootsCache()
    shortcutsTable.add(shortcutsRootsPanel).left().row()
    if (shortcutsFavoritesPanel.children.size > 0) shortcutsTable.addSeparator()
    shortcutsTable.add(shortcutsFavoritesPanel).left().row()
  }

  private def rebuildShortcutsList(): Unit = {
    shortcutsListRebuildScheduled = false
    rebuildShortcutsList(true)
  }

  private def rebuildFileRootsCache(): Unit = {
    shortcutsRootsPanel.clear()
    val roots = File.listRoots()
    driveCheckerListeners.clear()
    for (root <- roots) {
      val listener = new DriveCheckerService.DriveCheckerListener {
        override def rootMode(root: File, mode: DriveCheckerService.RootMode): Unit = {
          if (!driveCheckerListeners.removeValue(this)) { return; } // @nowarn -- early return
          var initialName = root.toString
          if (initialName == "/") initialName = COMPUTER.get
          val item = new ShortcutItem(root, initialName, _chooserStyle.iconDrive.get)
          if (OsUtils.isWindows && chooserWinService.isDefined) chooserWinService.get.addListener(root, item)
          shortcutsRootsPanel.addActor(item)
          shortcutsRootsPanel.children.sort(SHORTCUTS_COMPARATOR)
        }
      }
      driveCheckerListeners.add(listener)
      driveCheckerService.addListener(root, if (_mode == Mode.OPEN) DriveCheckerService.RootMode.READABLE else DriveCheckerService.RootMode.WRITABLE, listener)
    }
  }

  private def rebuildShortcutsFavoritesPanel(): Unit = {
    shortcutsFavoritesPanel.clear()
    if (favorites.size > 0) {
      val iter = favorites.iterator
      while (iter.hasNext) {
        val f = iter.next()
        shortcutsFavoritesPanel.addActor(new ShortcutItem(f.file, f.name, _chooserStyle.iconFolder.get))
      }
    }
  }

  private def rebuildFileList(): Unit = rebuildFileList(stageChanged = false)

  private def rebuildFileList(stageChanged: Boolean): Unit = {
    filesListRebuildScheduled = false
    val selectedFiles = new scala.Array[FileHandle](selectedItems.size)
    var i             = 0; while (i < selectedFiles.length) { selectedFiles(i) = selectedItems(i).getFile; i += 1 }
    deselectAll()
    setCurrentPathFieldText(currentDirectory.path)

    if (!showBusyBarTask.isScheduled) Timer.schedule(showBusyBarTask, Seconds(0.2f))

    if (listDirFuture.isDefined) listDirFuture.get.cancel(true)
    listDirFuture = Nullable(
      listDirExecutor.submit(
        new Runnable {
          override def run(): Unit = {
            if (!currentDirectory.exists() || !currentDirectory.isDirectory()) {
              Sge().application.postRunnable(new Runnable { override def run(): Unit = setDirectory(getDefaultStartingDirectory, HistoryPolicy.ADD) })
              return; // @nowarn -- early return
            }
            val files = FileUtils.sortFiles(listFilteredCurrentDirectory(), _sorting.get().comparator, !_sortingOrderAscending.get())
            if (Thread.currentThread().isInterrupted) { return; } // @nowarn -- early return
            val metadata = new java.util.IdentityHashMap[FileHandle, FileHandleMetadata](files.size)
            val iter     = files.iterator
            while (iter.hasNext) { val file = iter.next(); metadata.put(file, FileHandleMetadata.of(file)) }
            if (Thread.currentThread().isInterrupted) { return; } // @nowarn -- early return
            Sge().application.postRunnable(new Runnable { override def run(): Unit = buildFileList(files, metadata, selectedFiles, stageChanged) })
          }
        }
      )
    )
  }

  private def buildFileList(
    files:         DynamicArray[FileHandle],
    metadata:      java.util.IdentityHashMap[FileHandle, FileHandleMetadata],
    selectedFiles: scala.Array[FileHandle],
    stageChanged:  Boolean
  ): Unit = {
    currentFiles.clear()
    currentFilesMetadata.clear()
    showBusyBarTask.cancel()
    fileListBusyBar.visible = false
    if (files.size == 0) { fileListAdapter.itemsChanged(); return; } // @nowarn -- early return
    maxDateLabelWidth = 0
    currentFiles.addAll(files)
    currentFilesMetadata = metadata
    fileListAdapter.itemsChanged()
    fileListView.getScrollPane.setScrollX(0)
    fileListView.getScrollPane.setScrollY(0)
    highlightFiles(selectedFiles*)
    if (stageChanged && selectedFiles.isEmpty && _defaultFileName.isDefined) {
      selectedFileTextField.setText(_defaultFileName.get)
      val enteredFile = currentDirectory.child(selectedFileTextField.text)
      if (currentFiles.contains(enteredFile)) highlightFiles(enteredFile)
    }
  }

  /** Sets chooser selected files. */
  def setSelectedFiles(files: FileHandle*): Unit = {
    deselectAll(updateTextField = false)
    for (file <- files)
      fileListAdapter.getViews.get(file).foreach(_.select(deselectIfAlreadySelected = false))
    removeInvalidSelections()
    updateSelectedFileFieldText()
  }

  def defaultFileName_=(text: Nullable[String]): Unit = _defaultFileName = text

  /** Refresh chooser lists content */
  def refresh(): Unit = refresh(stageChanged = false)

  private def refresh(stageChanged: Boolean): Unit = {
    rebuildShortcutsList()
    rebuildFileList(stageChanged)
  }

  def addFavorite(favourite: FileHandle): Unit = {
    favorites.add(favourite)
    preferencesIO.saveFavorites(favorites)
    rebuildShortcutsFavoritesPanel()
    rebuildShortcutsList(false)
    updateFavoriteFolderButton()
  }

  def removeFavorite(favorite: FileHandle): Boolean = {
    val removed = favorites.removeValue(favorite)
    preferencesIO.saveFavorites(favorites)
    rebuildShortcutsFavoritesPanel()
    rebuildShortcutsList(false)
    updateFavoriteFolderButton()
    removed
  }

  private def addRecentDirectory(file: FileHandle): Unit = {
    if (recentDirectories.contains(file)) { return; } // @nowarn -- early return
    recentDirectories.insert(0, file)
    if (recentDirectories.size > AbstractSuggestionPopup.MAX_SUGGESTIONS) recentDirectories.pop()
    preferencesIO.saveRecentDirectories(recentDirectories)
  }

  def clearRecentDirectories(): Unit = {
    recentDirectories.clear()
    preferencesIO.saveRecentDirectories(recentDirectories)
  }

  private def deselectAll(): Unit = deselectAll(updateTextField = true)

  private def deselectAll(updateTextField: Boolean): Unit = {
    val iter = selectedItems.iterator
    while (iter.hasNext) iter.next().deselect(removeFromList = false)
    selectedItems.clear()
    if (updateTextField) updateSelectedFileFieldText()
  }

  private def selectAll(): Unit = {
    val iter = fileListAdapter.getOrderedViews.iterator
    while (iter.hasNext) iter.next().select(deselectIfAlreadySelected = false)
    removeInvalidSelections()
    updateSelectedFileFieldText()
  }

  def highlightFiles(files: FileHandle*): Unit = {
    for (file <- files)
      fileListAdapter.getViews.get(file).foreach(_.select(deselectIfAlreadySelected = false))
    if (files.nonEmpty) {
      fileListAdapter.getViews.get(files.head).foreach { item =>
        item.parent.foreach {
          case t: Table => t.layout()
          case _ => ()
        }
        val tmpVector = new Vector2()
        item.localToParentCoordinates(tmpVector.setZero())
        fileListView.getScrollPane.scrollTo(tmpVector.x, tmpVector.y, item.width, item.height, false, true)
      }
    }
    updateSelectedFileFieldText()
  }

  private def updateSelectedFileFieldText(): Unit = updateSelectedFileFieldText(ignoreKeyboardFocus = false)

  private def updateSelectedFileFieldText(ignoreKeyboardFocus: Boolean): Unit = {
    if (!ignoreKeyboardFocus && stage.isDefined) {
      if (stage.get.keyboardFocus.exists(_ eq selectedFileTextField)) { return; } // @nowarn -- early return
    }
    if (selectedItems.size == 0) selectedFileTextField.setText("")
    else if (selectedItems.size == 1) selectedFileTextField.setText(selectedItems(0).getFile.name)
    else {
      val builder = new java.lang.StringBuilder()
      val iter    = selectedItems.iterator
      while (iter.hasNext) {
        builder.append('"'); builder.append(iter.next().file.name); builder.append("\" ")
      }
      selectedFileTextField.setText(builder.toString)
    }
    selectedFileTextField.setCursorAtTextEnd()
  }

  private def removeInvalidSelections(): Unit = {
    if (_selectionMode == SelectionMode.FILES) {
      var i = selectedItems.size - 1
      while (i >= 0) {
        val item = selectedItems(i)
        if (item.file.isDirectory()) { item.deselect(removeFromList = false); selectedItems.removeIndex(i) }
        i -= 1
      }
    }
    if (_selectionMode == SelectionMode.DIRECTORIES) {
      var i = selectedItems.size - 1
      while (i >= 0) {
        val item = selectedItems(i)
        if (!item.file.isDirectory()) { item.deselect(removeFromList = false); selectedItems.removeIndex(i) }
        i -= 1
      }
    }
  }

  private def updateFavoriteFolderButton(): Unit = {
    val label = favoriteFolderButtonTooltip.content.get.asInstanceOf[VisLabel]
    if (favorites.contains(currentDirectory)) {
      favoriteFolderButton.style.asInstanceOf[VisImageButton.VisImageButtonStyle].imageUp = _chooserStyle.iconStar.get
      label.setText(CONTEXT_MENU_REMOVE_FROM_FAVORITES.get)
    } else {
      favoriteFolderButton.style.asInstanceOf[VisImageButton.VisImageButtonStyle].imageUp = _chooserStyle.iconStarOutline.get
      label.setText(CONTEXT_MENU_ADD_TO_FAVORITES.get)
    }
    favoriteFolderButtonTooltip.pack()
  }

  private def updateFileTypeSelectBox(): Unit =
    if (_fileTypeFilter.isEmpty || _selectionMode == SelectionMode.DIRECTORIES) {
      fileTypeLabel.visible = false
      fileTypeSelectBox.visible = false
      fileTypeSelectBox.invalidateHierarchy()
    } else {
      fileTypeLabel.visible = true
      fileTypeSelectBox.visible = true
      fileTypeSelectBox.invalidateHierarchy()
      val rules = DynamicArray[FileTypeFilter.Rule]()
      rules.addAll(_fileTypeFilter.get.getRules)
      if (_fileTypeFilter.get.allTypesAllowed) rules.add(new FileTypeFilter.Rule(ALL_FILES.get))
      fileTypeSelectBox.setItems(rules)
      if (_activeFileTypeRule.isDefined) fileTypeSelectBox.setSelected(_activeFileTypeRule.get)
    }

  def getMode: Mode = _mode

  def setMode(mode: Mode): Unit = {
    _mode = mode
    confirmButton.setText(if (mode == Mode.OPEN) OPEN.get else SAVE.get)
    refresh()
  }

  def getViewMode: ViewMode = _viewMode

  def setViewMode(viewMode: ViewMode): Unit = {
    if (_viewMode == viewMode) { return; } // @nowarn -- early return
    _viewMode = viewMode
    _iconProvider.viewModeChanged(viewMode)
    rebuildFileList()
  }

  def setDirectory(directory: String):     Unit = setDirectory(Sge().files.absolute(directory), HistoryPolicy.CLEAR)
  def setDirectory(directory: File):       Unit = setDirectory(Sge().files.absolute(directory.getAbsolutePath), HistoryPolicy.CLEAR)
  def setDirectory(directory: FileHandle): Unit = setDirectory(directory, HistoryPolicy.CLEAR)

  override def setDirectory(directory: FileHandle, historyPolicy: HistoryPolicy): Unit = {
    if (directory.equals(currentDirectory)) { return; } // @nowarn -- early return
    if (historyPolicy == HistoryPolicy.ADD) historyManager.historyAdd()
    currentDirectory = directory
    _iconProvider.directoryChanged(directory)
    rebuildFileList()
    if (historyPolicy == HistoryPolicy.CLEAR) historyManager.historyClear()
    updateFavoriteFolderButton()
  }

  override def getCurrentDirectory: FileHandle = currentDirectory

  override def getHistoryStage: Stage = getChooserStage

  private def getDefaultStartingDirectory: FileHandle = Sge().files.absolute(System.getProperty("user.home"))

  private def listFilteredCurrentDirectory(): scala.Array[FileHandle] = {
    val files = currentDirectory.list(_fileFilter)
    if (_fileTypeFilter.isEmpty || _activeFileTypeRule.isEmpty) files
    else {
      val rule     = _activeFileTypeRule.get
      val filtered = new scala.Array[FileHandle](files.length)
      var count    = 0
      for (file <- files)
        if (file.isDirectory() || rule.accept(file)) { filtered(count) = file; count += 1 }
      if (count == 0) new scala.Array[FileHandle](0)
      else { val r = new scala.Array[FileHandle](count); System.arraycopy(filtered, 0, r, 0, count); r }
    }
  }

  def getFileFilter: FileFilter = _fileFilter

  def setFileFilter(fileFilter: FileFilter): Unit = { _fileFilter = fileFilter; rebuildFileList() }

  def fileFilter_=(ff: FileFilter): Unit = { _fileFilter = ff; rebuildFileList() }

  def setFileTypeFilter(ftf: Nullable[FileTypeFilter]): Unit = {
    if (ftf.isEmpty) { _fileTypeFilter = Nullable.empty; _activeFileTypeRule = Nullable.empty }
    else {
      require(ftf.get.getRules.size != 0, "FileTypeFilter doesn't have any rules added")
      _fileTypeFilter = Nullable(new FileTypeFilter(ftf.get))
      _activeFileTypeRule = Nullable(_fileTypeFilter.get.getRules.first)
    }
    updateFileTypeSelectBox()
    rebuildFileList()
  }

  def getActiveFileTypeFilterRule: FileTypeFilter.Rule = if (_activeFileTypeRule.isDefined) _activeFileTypeRule.get else null // @nowarn -- Java interop boundary

  def selectionMode:                      SelectionMode = _selectionMode
  def selectionMode_=(sm: SelectionMode): Unit          = {
    _selectionMode = if (sm == null) SelectionMode.FILES else sm // @nowarn -- Java interop boundary
    _selectionMode match {
      case SelectionMode.FILES                 => titleLabel.setText(TITLE_CHOOSE_FILES.get)
      case SelectionMode.DIRECTORIES           => titleLabel.setText(TITLE_CHOOSE_DIRECTORIES.get)
      case SelectionMode.FILES_AND_DIRECTORIES => titleLabel.setText(TITLE_CHOOSE_FILES_AND_DIRECTORIES.get)
    }
    updateFileTypeSelectBox()
    rebuildFileList()
  }

  def getSorting:              FileSorting = _sorting.get()
  def isSortingOrderAscending: Boolean     = _sortingOrderAscending.get()

  def setSorting(sorting: FileSorting, sortingOrderAscending: Boolean): Unit = {
    _sorting.set(sorting); _sortingOrderAscending.set(sortingOrderAscending); rebuildFileList()
  }

  def sortingOrderAscending_=(asc: Boolean): Unit = { _sortingOrderAscending.set(asc); rebuildFileList() }

  def setSortingOrderAscending(sortingOrderAscending: Boolean): Unit = { _sortingOrderAscending.set(sortingOrderAscending); rebuildFileList() }

  def setSorting(sorting: FileSorting): Unit = { _sorting.set(sorting); rebuildFileList() }

  def setListener(newListener: FileChooserListener): Unit =
    _listener = if (newListener == null) new FileChooserAdapter() else newListener // @nowarn -- Java interop boundary

  def showSelectionCheckboxes:               Boolean = _showSelectionCheckboxes
  def showSelectionCheckboxes_=(v: Boolean): Unit    = { _showSelectionCheckboxes = v; rebuildFileList() }

  def isShowSelectionCheckboxes: Boolean = _showSelectionCheckboxes

  def setShowSelectionCheckboxes(showSelectionCheckboxes: Boolean): Unit = { _showSelectionCheckboxes = showSelectionCheckboxes; rebuildFileList() }

  def multiSelectionEnabled:               Boolean = _multiSelectionEnabled
  def multiSelectionEnabled_=(v: Boolean): Unit    = _multiSelectionEnabled = v

  def isMultiSelectionEnabled: Boolean = _multiSelectionEnabled

  def setMultiSelectionEnabled(multiSelectionEnabled: Boolean): Unit = _multiSelectionEnabled = multiSelectionEnabled

  def getSelectionMode: SelectionMode = _selectionMode

  def setSelectionMode(sm: SelectionMode): Unit = selectionMode = sm

  def setFavoriteFolderButtonVisible(v: Boolean): Unit = favoriteFolderButton.visible = v

  def isFavoriteFolderButtonVisible: Boolean = favoriteFolderButton.visible

  def setViewModeButtonVisible(v: Boolean): Unit = viewModeButton.visible = v

  def isViewModeButtonVisible: Boolean = viewModeButton.visible

  def getMultiSelectKey: Int = _multiSelectKey

  /** @param multiSelectKey from [[Keys]] or [[FileChooser.DEFAULT_KEY]] to restore default */
  def setMultiSelectKey(multiSelectKey: Int): Unit = _multiSelectKey = multiSelectKey

  def getGroupMultiSelectKey: Int = _groupMultiSelectKey

  /** @param groupMultiSelectKey from [[Keys]] or [[FileChooser.DEFAULT_KEY]] to restore default */
  def setGroupMultiSelectKey(groupMultiSelectKey: Int): Unit = _groupMultiSelectKey = groupMultiSelectKey

  def setDefaultFileName(text: Nullable[String]): Unit = _defaultFileName = text

  /** If false file chooser won't pool directories for changes, adding new files or connecting new drive won't refresh file list. This must be called when file chooser is not added to Stage.
    */
  def setWatchingFilesEnabled(watchingFilesEnabled: Boolean): Unit = {
    if (stage.isDefined) throw new IllegalStateException("Pooling setting cannot be changed when file chooser is added to Stage!")
    _watchingFilesEnabled = watchingFilesEnabled
  }

  def isWatchingFilesEnabled: Boolean = _watchingFilesEnabled

  def setPrefsName(prefsName: String): Unit = {
    preferencesIO = new PreferencesIO(prefsName)
    reloadPreferences(rebuildUI = true)
  }

  def isSaveLastDirectory: Boolean = FileChooser.saveLastDirectory

  def getChooserStyle: FileChooserStyle = _chooserStyle
  def getSizes:        Sizes            = sizes

  private def getChooserStage: Stage = stage.getOrElse(throw new IllegalStateException("FileChooser must be added to a Stage"))

  private def reloadPreferences(rebuildUI: Boolean): Unit = {
    favorites = preferencesIO.loadFavorites()
    recentDirectories = preferencesIO.loadRecentDirectories()
    if (rebuildUI) rebuildShortcutsFavoritesPanel()
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    if (shortcutsListRebuildScheduled) rebuildShortcutsList()
    if (filesListRebuildScheduled) rebuildFileList()
  }

  override protected[sge] def setStage(stage: Nullable[Stage]): Unit = {
    super.setStage(stage)
    stage.foreach { stg =>
      refresh(stageChanged = true)
      rebuildShortcutsFavoritesPanel()
      deselectAll()
      if (FileChooser.focusFileScrollPaneOnShow) stg.setScrollFocus(fileListView.getScrollPane)
      if (FileChooser.focusSelectedFileTextFieldOnShow) {
        FocusManager.switchFocus(stg, selectedFileTextField)
        stg.setKeyboardFocus(selectedFileTextField)
      }
    }
    if (_watchingFilesEnabled) {
      if (stage.isDefined) startFileWatcher()
      else stopFileWatcher()
    }
  }

  private def startFileWatcher(): Unit = {
    if (fileWatcherThread.isDefined) { return; } // @nowarn -- early return
    val thread = new Thread(
      new Runnable {
        override def run(): Unit = {
          var lastRoots            = File.listRoots()
          var lastCurrentDirectory = currentDirectory
          var lastCurrentFiles     = currentDirectory.list()
          while (fileWatcherThread.isDefined) {
            val roots = File.listRoots()
            if (roots.length != lastRoots.length || !java.util.Arrays.equals(lastRoots.asInstanceOf[scala.Array[AnyRef]], roots.asInstanceOf[scala.Array[AnyRef]]))
              shortcutsListRebuildScheduled = true
            lastRoots = roots
            if (lastCurrentDirectory.equals(currentDirectory)) {
              val cf = currentDirectory.list()
              if (lastCurrentFiles.length != cf.length || !java.util.Arrays.equals(lastCurrentFiles.asInstanceOf[scala.Array[AnyRef]], cf.asInstanceOf[scala.Array[AnyRef]]))
                filesListRebuildScheduled = true
              lastCurrentFiles = cf
            } else lastCurrentFiles = currentDirectory.list()
            lastCurrentDirectory = currentDirectory
            try Thread.sleep(2000)
            catch { case _: InterruptedException => () }
          }
        }
      },
      "FileWatcherThread"
    )
    thread.setDaemon(true)
    thread.start()
    fileWatcherThread = Nullable(thread)
  }

  private def stopFileWatcher(): Unit = {
    if (fileWatcherThread.isEmpty) { return; } // @nowarn -- early return
    fileWatcherThread.get.interrupt()
    fileWatcherThread = Nullable.empty
  }

  private def showNewDirectoryDialog(): Unit =
    Dialogs.showInputDialog(
      getChooserStage,
      NEW_DIRECTORY_DIALOG_TITLE.get,
      NEW_DIRECTORY_DIALOG_TEXT.get,
      true,
      new Dialogs.InputDialogAdapter() {
        override def finished(input: String): Unit = scala.util.boundary {
          if (!FileUtils.isValidFileName(input)) { Dialogs.showErrorDialog(getChooserStage, NEW_DIRECTORY_DIALOG_ILLEGAL_CHARACTERS.get); scala.util.boundary.break(()) }
          val listing = currentDirectory.list()
          for (file <- listing)
            if (file.name.equals(input)) { Dialogs.showErrorDialog(getChooserStage, NEW_DIRECTORY_DIALOG_ALREADY_EXISTS.get); scala.util.boundary.break(()) }
          val newDir = currentDirectory.child(input)
          newDir.mkdirs()
          refresh()
          highlightFiles(newDir)
        }
      }
    )

  private def showFileDeleteDialog(fileToDelete: FileHandle): Unit =
    Dialogs.showOptionDialog(
      getChooserStage,
      POPUP_TITLE.get,
      if (_fileDeleter.hasTrash) CONTEXT_MENU_MOVE_TO_TRASH_WARNING.get else CONTEXT_MENU_DELETE_WARNING.get,
      OptionDialogType.YES_NO,
      new Dialogs.OptionDialogAdapter() {
        override def yes(): Unit = {
          try
            if (!_fileDeleter.delete(fileToDelete)) Dialogs.showErrorDialog(getChooserStage, POPUP_DELETE_FILE_FAILED.get)
          catch { case e: IOException => Dialogs.showErrorDialog(getChooserStage, POPUP_DELETE_FILE_FAILED.get, e) }
          refresh()
        }
      }
    )

  def setFileDeleter(fileDeleter: FileDeleter): Unit = {
    require(fileDeleter != null, "fileDeleter can't be null") // @nowarn -- Java interop boundary
    _fileDeleter = fileDeleter
    fileMenu.fileDeleterChanged(fileDeleter.hasTrash)
  }

  def setIconProvider(iconProvider: FileIconProvider): Unit = {
    _iconProvider = iconProvider
    rebuildViewModePopupMenu()
  }

  def getIconProvider: FileIconProvider = _iconProvider

  /** Internal factory method for creating FileItem instances. */
  private[file] def createFileItem(file: FileHandle, viewMode: ViewMode): FileItem = new FileItem(file, viewMode)

  // Inner classes

  private class ShowBusyBarTask extends Timer.Task {
    override def run(): Unit = {
      fileListBusyBar.resetSegment()
      fileListBusyBar.visible = true
      currentFiles.clear()
      currentFilesMetadata.clear()
      fileListAdapter.itemsChanged()
    }

    override def cancel(): Unit = { // Note: using direct override since synchronized not needed in single-threaded scene2d
      super.cancel()
      fileListBusyBar.visible = false
    }
  }

  /** Internal FileChooser API. */
  class FileItem(val file: FileHandle, viewMode: ViewMode) extends Table with Focusable {
    private val metadata: FileHandleMetadata = {
      val m = currentFilesMetadata.get(file)
      if (m == null) FileHandleMetadata.of(file) else m // @nowarn -- Java interop boundary
    }
    private val selectCheckBox: VisCheckBox = new VisCheckBox("")
    private var iconImage:      VisImage    = scala.compiletime.uninitialized

    {
      touchable = Touchable.enabled
      val name = new VisLabel(metadata.name, if (viewMode == ViewMode.SMALL_ICONS) "small" else "default")
      name.setEllipsis(true)
      val icon = _iconProvider.provideIcon(this)

      selectCheckBox.focusBorderEnabled = false
      selectCheckBox.programmaticChangeEvents = false
      val shouldShowCheckBox = _showSelectionCheckboxes && (
        (_selectionMode == SelectionMode.FILES_AND_DIRECTORIES)
          || (_selectionMode == SelectionMode.FILES && !metadata.isDirectory)
          || (_selectionMode == SelectionMode.DIRECTORIES && metadata.isDirectory)
      )

      left()
      if (viewMode.isThumbnailMode) {
        if (shouldShowCheckBox) {
          iconImage = new VisImage(icon.getOrElse(null: Drawable), Scaling.none) // @nowarn -- Java interop boundary
          val stack = new IconStack(iconImage, selectCheckBox)
          add(Nullable[Actor](stack)).padTop(3).grow().row()
          add(Nullable[Actor](name)).minWidth(1)
        } else {
          iconImage = new VisImage(icon.getOrElse(null: Drawable), Scaling.none) // @nowarn -- Java interop boundary
          add(Nullable[Actor](iconImage)).padTop(3).grow().row()
          add(Nullable[Actor](name)).minWidth(1)
        }
      } else {
        if (shouldShowCheckBox) add(Nullable[Actor](selectCheckBox)).padLeft(3)
        iconImage = new VisImage(icon, Scaling.stretch, Align.center)
        add(Nullable[Actor](iconImage)).padTop(3).minWidth(22 * sizes.scaleFactor)
        add(Nullable[Actor](name)).minWidth(1).growX().padRight(10)

        val sizeLabel = new VisLabel(if (isDirectory) "" else metadata.readableFileSize, "small")
        val dateLabel = new VisLabel(dateFormat.format(metadata.lastModified), "small")
        sizeLabel.setAlignment(Align.right)

        if (viewMode == ViewMode.DETAILS) {
          maxDateLabelWidth = Math.max(dateLabel.width, maxDateLabelWidth)
          add(Nullable[Actor](sizeLabel)).right().padRight(if (isDirectory) 0 else 10)
          add(Nullable[Actor](dateLabel))
            .padRight(6)
            .width(new Value() {
              override def get(context: Nullable[Actor]): Float = maxDateLabelWidth
            })
        }
      }
      addListeners()
    }

    def setIcon(icon: Drawable, scaling: Scaling): Unit = {
      iconImage.drawable = Nullable(icon)
      iconImage.scaling = scaling
      iconImage.invalidateHierarchy()
    }

    private def addListeners(): Unit = {
      addListener(
        new InputListener() {
          override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
            FocusManager.switchFocus(getChooserStage, FileItem.this)
            getChooserStage.setKeyboardFocus(FileItem.this)
            true
          }
          override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
            if (event.button == Buttons.RIGHT) {
              fileMenu.build(favorites, file)
              fileMenu.showMenu(getChooserStage, event.stageX, event.stageY)
            }
          override def keyDown(event: InputEvent, keycode: sge.Input.Key): Boolean =
            if (keycode == Keys.FORWARD_DEL) { showFileDeleteDialog(file); true }
            else false
        }
      )

      addListener(
        new ClickListener() {
          override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean =
            if (!handleSelectClick(checkboxClicked = false)) false
            else super.touchDown(event, x, y, pointer, button)
          override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
            super.clicked(event, x, y)
            if (tapCount == 2 && selectedItems.contains(FileItem.this)) {
              if (file.isDirectory()) setDirectory(file, HistoryPolicy.ADD)
              else selectionFinished()
            }
          }
        }
      )

      selectCheckBox.addListener(
        new InputListener() {
          override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = { event.stop(); true }
        }
      )
      selectCheckBox.addListener(
        new ChangeListener() {
          override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = { event.stop(); handleSelectClick(checkboxClicked = true) }
        }
      )
    }

    private def handleSelectClick(checkboxClicked: Boolean): Boolean = {
      if (selectedShortcut.isDefined) selectedShortcut.get.deselect()
      if (checkboxClicked) {
        if (!_multiSelectionEnabled && !selectedItems.contains(FileItem.this)) deselectAll()
      } else {
        if (!_multiSelectionEnabled || (!isMultiSelectKeyPressed && !isGroupMultiSelectKeyPressed)) deselectAll()
      }
      val itemSelected = select()
      if (selectedItems.size > 1 && _multiSelectionEnabled && isGroupMultiSelectKeyPressed) selectGroup()
      if (selectedItems.size > 1) removeInvalidSelections()
      updateSelectedFileFieldText()
      itemSelected
    }

    private def selectGroup(): Unit = {
      val actors             = fileListAdapter.getOrderedViews.asInstanceOf[DynamicArray[FileItem]]
      val thisSelectionIndex = getItemId(actors, FileItem.this)
      val lastSelectionIndex = getItemId(actors, selectedItems(selectedItems.size - 2))
      val start              = Math.min(thisSelectionIndex, lastSelectionIndex)
      val end                = Math.max(thisSelectionIndex, lastSelectionIndex)
      var i                  = start
      while (i < end) { actors(i).select(deselectIfAlreadySelected = false); i += 1 }
    }

    private def getItemId(actors: DynamicArray[FileItem], item: FileItem): Int = {
      var i     = 0
      var found = -1
      while (i < actors.size && found == -1) { if (actors(i).eq(item)) found = i; i += 1 }
      if (found == -1) throw new IllegalStateException("Item not found in cells")
      found
    }

    private def select(): Boolean = select(deselectIfAlreadySelected = true)

    private[file] def select(deselectIfAlreadySelected: Boolean): Boolean =
      if (deselectIfAlreadySelected && selectedItems.containsByRef(this)) { deselect(); false }
      else {
        setBackground(_chooserStyle.highlight.get)
        selectCheckBox.setChecked(true)
        if (!selectedItems.containsByRef(this)) selectedItems.add(this)
        true
      }

    private def deselect(): Unit = deselect(removeFromList = true)

    private[file] def deselect(removeFromList: Boolean): Unit = {
      setBackground(Nullable.empty[Drawable])
      selectCheckBox.setChecked(false)
      if (removeFromList) selectedItems.removeValue(this)
    }

    override def focusLost():   Unit       = ()
    override def focusGained(): Unit       = ()
    def getFile:                FileHandle = file
    def isDirectory:            Boolean    = metadata.isDirectory
  }

  private class ShortcutItem(val file: File, customName: String, icon: Drawable) extends Table with FileChooserWinService.RootNameListener with Focusable {
    private val _nameLabel: VisLabel = new VisLabel(customName)

    {
      _nameLabel.setEllipsis(true)
      add(Nullable[Actor](new Image(icon))).padTop(3)
      val labelCell = add(Nullable[Actor](_nameLabel)).padRight(6)
      labelCell.width(new Value() {
        override def get(context: Nullable[Actor]): Float = mainSplitPane.getFirstWidgetBounds.width - 30
      })

      addListener(
        new InputListener() {
          override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
            FocusManager.switchFocus(getChooserStage, ShortcutItem.this)
            getChooserStage.setKeyboardFocus(ShortcutItem.this)
            true
          }
          override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
            if (event.button == Buttons.RIGHT) {
              fileMenu.buildForFavorite(favorites, file)
              fileMenu.showMenu(getChooserStage, event.stageX, event.stageY)
            }
          override def keyDown(event: InputEvent, keycode: sge.Input.Key): Boolean =
            if (keycode == Keys.FORWARD_DEL) {
              val gdxFile = Sge().files.absolute(file.getAbsolutePath)
              if (favorites.contains(gdxFile)) removeFavorite(gdxFile)
              true
            } else false
        }
      )

      addListener(
        new ClickListener() {
          override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
            deselectAll()
            updateSelectedFileFieldText()
            selectShortcut()
            super.touchDown(event, x, y, pointer, button)
          }
          override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
            super.clicked(event, x, y)
            if (tapCount == 1) {
              if (!file.exists()) { showDialog(POPUP_DIRECTORY_DOES_NOT_EXIST.get); refresh() }
              else if (file.isDirectory()) {
                setDirectory(Sge().files.absolute(file.getAbsolutePath), HistoryPolicy.ADD)
                getChooserStage.setScrollFocus(fileListView.getScrollPane)
              }
            }
          }
        }
      )
    }

    def setLabelText(text: String): Unit   = _nameLabel.setText(text)
    def getLabelText:               String = _nameLabel.text.toString

    private def selectShortcut(): Unit = {
      if (selectedShortcut.isDefined) selectedShortcut.get.deselect()
      selectedShortcut = Nullable(ShortcutItem.this)
      setBackground(_chooserStyle.highlight.get)
    }

    def deselect(): Unit = setBackground(Nullable.empty[Drawable])

    override def setRootName(newName: String): Unit = setLabelText(newName)
    override def focusGained():                Unit = ()
    override def focusLost():                  Unit = ()
  }

  private def isMultiSelectKeyPressed: Boolean =
    if (_multiSelectKey == DEFAULT_KEY) UIUtils.ctrl()
    else Sge().input.isKeyPressed(Input.Key(_multiSelectKey))

  private def isGroupMultiSelectKeyPressed: Boolean =
    if (_groupMultiSelectKey == DEFAULT_KEY) UIUtils.shift()
    else Sge().input.isKeyPressed(Input.Key(_groupMultiSelectKey))
}

object FileChooser {
  val DEFAULT_KEY: Int = -1

  private val SHORTCUTS_COMPARATOR: Ordering[Actor] = new Ordering[Actor] {
    override def compare(o1: Actor, o2: Actor): Int = (o1, o2) match {
      case (s1: FileChooser#ShortcutItem, s2: FileChooser#ShortcutItem) => s1.getLabelText.compareTo(s2.getLabelText)
      case _                                                            => 0
    }
  }

  var saveLastDirectory:                Boolean = false
  var focusFileScrollPaneOnShow:        Boolean = true
  var focusSelectedFileTextFieldOnShow: Boolean = true

  def setDefaultPrefsName(prefsName: String): Unit = PreferencesIO.setDefaultPrefsName(prefsName)

  /** @deprecated replaced by [[setDefaultPrefsName]] */
  @deprecated("replaced by setDefaultPrefsName", "1.0.2")
  def setFavoritesPrefsName(name: String): Unit = PreferencesIO.setDefaultPrefsName(name)

  def isSaveLastDirectory: Boolean = saveLastDirectory

  /** @param saveLastDirectory
    *   if true then chooser will store last directory user browsed in preferences file. Note that this only applies to using chooser between separate app launches. When single instance of chooser is
    *   reused in single app session then last directory is always remembered. Default is false. This must be called before creating FileChooser.
    */
  def setSaveLastDirectory(v: Boolean): Unit = saveLastDirectory = v

  enum Mode { case OPEN, SAVE }
  enum SelectionMode { case FILES, DIRECTORIES, FILES_AND_DIRECTORIES }

  enum FileSorting(val comparator: Comparator[FileHandle]) {
    case NAME extends FileSorting(FileUtils.FILE_NAME_COMPARATOR)
    case MODIFIED_DATE extends FileSorting(FileUtils.FILE_MODIFIED_DATE_COMPARATOR)
    case SIZE extends FileSorting(FileUtils.FILE_SIZE_COMPARATOR)
  }

  enum HistoryPolicy { case ADD, CLEAR, IGNORE }

  enum ViewMode(val thumbnailMode: Boolean, val bundleText: FileChooserText) {
    case DETAILS extends ViewMode(false, VIEW_MODE_DETAILS)
    case BIG_ICONS extends ViewMode(true, VIEW_MODE_BIG_ICONS)
    case MEDIUM_ICONS extends ViewMode(true, VIEW_MODE_MEDIUM_ICONS)
    case SMALL_ICONS extends ViewMode(true, VIEW_MODE_SMALL_ICONS)
    case LIST extends ViewMode(false, VIEW_MODE_LIST)

    def getBundleText: String = bundleText.get

    def setupGridGroup(sizes: Sizes, group: GridGroup): Unit = {
      if (!isGridMode) { return; } // @nowarn -- early return
      val gridSize = getGridSize(sizes)
      if (gridSize < 0) throw new IllegalStateException("FileChooser's ViewMode " + this.toString + " has invalid size defined in Sizes.")
      if (this == LIST) { group.setItemSize(gridSize, 22 * sizes.scaleFactor); return; } // @nowarn -- early return
      group.setItemSize(gridSize)
    }

    def isGridMode:      Boolean = isThumbnailMode || this == LIST
    def isThumbnailMode: Boolean = thumbnailMode

    def getGridSize(sizes: Sizes): Float = this match {
      case DETAILS      => -1
      case BIG_ICONS    => sizes.fileChooserViewModeBigIconsSize
      case MEDIUM_ICONS => sizes.fileChooserViewModeMediumIconsSize
      case SMALL_ICONS  => sizes.fileChooserViewModeSmallIconsSize
      case LIST         => sizes.fileChooserViewModeListWidthSize
    }
  }

  trait FileIconProvider {
    def provideIcon(item:              FileChooser#FileItem): Nullable[Drawable]
    def isThumbnailModesSupported:                            Boolean
    def directoryChanged(newDirectory: FileHandle):           Unit
    def viewModeChanged(newViewMode:   ViewMode):             Unit
  }

  class DefaultFileIconProvider(protected val chooser: FileChooser) extends FileIconProvider {
    protected val style: FileChooserStyle = chooser.getChooserStyle

    override def provideIcon(item: FileChooser#FileItem): Nullable[Drawable] =
      if (item.isDirectory) getDirIcon(item)
      else {
        val ext = item.getFile.extension.toLowerCase
        if (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "bmp") getImageIcon(item)
        else if (ext == "wav" || ext == "ogg" || ext == "mp3") getAudioIcon(item)
        else if (ext == "pdf") getPdfIcon(item)
        else if (ext == "txt") getTextIcon(item)
        else getDefaultIcon(item)
      }

    protected def getDirIcon(item:     FileChooser#FileItem): Nullable[Drawable] = style.iconFolder
    protected def getImageIcon(item:   FileChooser#FileItem): Nullable[Drawable] = style.iconFileImage
    protected def getAudioIcon(item:   FileChooser#FileItem): Nullable[Drawable] = style.iconFileAudio
    protected def getPdfIcon(item:     FileChooser#FileItem): Nullable[Drawable] = style.iconFilePdf
    protected def getTextIcon(item:    FileChooser#FileItem): Nullable[Drawable] = style.iconFileText
    protected def getDefaultIcon(item: FileChooser#FileItem): Nullable[Drawable] = Nullable.empty

    override def isThumbnailModesSupported:                  Boolean = false
    override def directoryChanged(newDirectory: FileHandle): Unit    = ()
    override def viewModeChanged(newViewMode:   ViewMode):   Unit    = ()
  }

  class DefaultFileFilter(chooser: FileChooser) extends FileFilter {
    private var _ignoreChooserSelectionMode: Boolean = false

    override def accept(f: File): Boolean =
      if (f.isHidden) false
      else if (if (chooser.getMode == Mode.OPEN) !f.canRead else !f.canWrite) false
      else if (!_ignoreChooserSelectionMode && !f.isDirectory && chooser.selectionMode == SelectionMode.DIRECTORIES) false
      else true

    def ignoreChooserSelectionMode:               Boolean = _ignoreChooserSelectionMode
    def ignoreChooserSelectionMode_=(v: Boolean): Unit    = _ignoreChooserSelectionMode = v

    def isIgnoreChooserSelectionMode: Boolean = _ignoreChooserSelectionMode

    def setIgnoreChooserSelectionMode(v: Boolean): Unit = _ignoreChooserSelectionMode = v
  }

  trait FileDeleter {
    def hasTrash:                 Boolean
    def delete(file: FileHandle): Boolean
  }

  class DefaultFileDeleter extends FileDeleter {
    override def hasTrash:                 Boolean = false
    override def delete(file: FileHandle): Boolean = file.deleteDirectory()
  }
}
