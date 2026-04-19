# Deep Body-Level Audit: FileChooser.scala

**Java source**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/FileChooser.java` (2167 LOC)
**Scala port**: `sge-extension/visui/src/main/scala/sge/visui/widget/file/FileChooser.scala` (1559 LOC)
**Delta**: -608 LOC (28% smaller, expected for Scala idioms)

**Automated tool results**:
- `re-scale enforce shortcuts`: **clean** (no shortcut markers)
- `re-scale enforce compare --strict`: 46 "missing" (all false positives: Java types, imports, inner class refs not detected cross-language), 111 "extra" (Scala backing fields, idiomatic accessors), 1 "short body" (primary ctor), 22 "dropped ctor args" (enum ctor params reported as dropped)

---

## P0 -- BROKEN FUNCTIONALITY

### 1. Constructor double-init bug (DIFFERENT_BEHAVIOR)

**Java** (lines 163-205): Four constructors with clean delegation:
- `FileChooser(Mode)` -> delegates to `FileChooser(FileHandle, Mode)` with null
- `FileChooser(FileHandle, Mode)` -> calls `init(directory)` once
- `FileChooser(String, Mode)` -> delegates to `FileChooser(String, String, Mode)` with "default"
- `FileChooser(String, String, Mode)` -> calls `init(null)` once

**Scala** (lines 48-150): Primary constructor's initializer block (lines 125-130) **always** calls `init(Nullable.empty)`. Then:
- `this(directory, mode)` (line 132): calls `this(mode)` which triggers `init(Nullable.empty)`, then calls `init(Nullable(directory))` **a second time**. This double-init creates all UI components twice, adds toolbar/buttons to the window twice, and corrupts layout.
- `this(styleName, title, mode)` (line 144): same double-init problem -- primary ctor calls `init`, then this ctor calls `init` again.
- `this(title, mode)` (line 139): does NOT call `init` again, but since the primary ctor already called init, this works. However, it does not pass through the "default" styleName -- it just uses the default skin style from the primary ctor's initializer. This happens to be equivalent behavior.

**Severity**: P0 -- the `FileChooser(FileHandle, Mode)` and `FileChooser(String, String, Mode)` constructors are broken.

### 2. `createFileTextBox` missing `spaceBottom` / `ConstantIfVisibleValue` spacing (SIMPLIFIED)

**Java** (lines 536-542):
```java
table.add(nameLabel).spaceBottom(new ConstantIfVisibleValue(fileTypeSelectBox, 5f));
table.add(selectedFileTextField).expandX().fillX()
        .spaceBottom(new ConstantIfVisibleValue(fileTypeSelectBox, 5f)).row();
table.add(fileTypeLabel).height(PrefHeightIfVisibleValue.INSTANCE)
        .spaceBottom(new ConstantIfVisibleValue(sizes.spacingBottom));
table.add(fileTypeSelectBox).height(PrefHeightIfVisibleValue.INSTANCE)
        .spaceBottom(new ConstantIfVisibleValue(sizes.spacingBottom)).expand().fill();
```

**Scala** (lines 418-421):
```scala
table.add(Nullable[Actor](nameLabel))
table.add(Nullable[Actor](selectedFileTextField)).expandX().fillX().row()
table.add(Nullable[Actor](fileTypeLabel)).height(PrefHeightIfVisibleValue.INSTANCE)
table.add(Nullable[Actor](fileTypeSelectBox)).height(PrefHeightIfVisibleValue.INSTANCE).expand().fill()
```

The Scala port is missing ALL `spaceBottom` calls (4 of them). The Java uses `ConstantIfVisibleValue(fileTypeSelectBox, 5f)` to add conditional spacing when the file type select box is visible, and `ConstantIfVisibleValue(sizes.spacingBottom)` for the bottom row. All four are dropped.

**Severity**: P0 -- layout is visually broken when file type filter is set.

---

## P1 -- MISSING FEATURES

### 3. `getChooserStage` throws instead of returning null (DIFFERENT_BEHAVIOR)

**Java** (line 1386): `return getStage();` -- can return null.

**Scala** (line 1048): `stage.getOrElse(throw new IllegalStateException("FileChooser must be added to a Stage"))` -- throws instead of returning null.

This changes behavior for code paths that check null, e.g. `updateSelectedFileFieldText` where Java checks `getChooserStage() != null` (line 1094). The Scala version (line 830) does `stage.isDefined` instead, which is correct, but any *other* code path that calls `getChooserStage` before the chooser is added to stage will crash instead of gracefully handling null.

**Severity**: P1 -- could crash in edge cases.

### 4. `setVisible` not calling `super.setVisible` (DIFFERENT_BEHAVIOR)

**Java** (lines 1038-1043):
```java
public void setVisible (boolean visible) {
    if (isVisible() == false && visible) deselectAll();
    super.setVisible(visible);
}
```

**Scala** (lines 526-529):
```scala
def setVisible(v: Boolean): Unit = {
    if (!visible && v) deselectAll()
    visible = v
}
```

The Scala port assigns `visible = v` directly instead of calling `super.setVisible(v)`. If `Actor.setVisible` (or any superclass override) has side effects beyond setting the field, those would be lost. In practice, `Actor.visible` is likely just a field setter, but this is still a behavioral divergence from the original.

**Severity**: P1 -- potential side-effect loss if superclass `setVisible` has logic.

### 5. `dirsSuggestionPopup` created twice -- listener lost (DIFFERENT_BEHAVIOR)

**Java**: `dirsSuggestionPopup` is created once in `createToolbar()` (line 299) with its `PopupMenuListener` attached (lines 300-306).

**Scala**: The execution flow is:
1. `init()` line 163 calls `createToolbar()`, which creates `dirsSuggestionPopup` at line 221 and attaches its listener at lines 222-227
2. Control returns to `init()`, which at line 191 **overwrites** `dirsSuggestionPopup` with a brand new `DirsSuggestionPopup` instance -- no listener attached

The result: the `dirsSuggestionPopup` keyboard navigation listener (which updates the current path text field when arrow-keying through directory suggestions) is on the **discarded** instance. The live instance has no listener.

Additionally, `currentPath.addListener` in `createToolbar()` (lines 229-256) captures the `dirsSuggestionPopup` reference from line 221 in its closure. After `init()` line 191 overwrites the field, the `keyTyped` handler calls `dirsSuggestionPopup.pathFieldKeyTyped(...)` on the **new** instance (because it captures `this.dirsSuggestionPopup` by field reference, not by value). However, the `remove()` call on line 233 (`dirsSuggestionPopup.remove()`) also goes to the new instance. So the key-typed behavior might partially work, but the `activeItemChanged` listener will never fire on the new instance because it was never set.

**Severity**: P0 -- directory suggestion keyboard navigation is broken.

### 6. `showFileDeleteDialog` missing `e.printStackTrace()` (SIMPLIFIED)

**Java** (line 1539): `e.printStackTrace();` is called after showing the error dialog.

**Scala** (line 1148): Only shows the error dialog, no stack trace printing.

**Severity**: P2 -- minor diagnostic gap.

---

## P2 -- MINOR GAPS

### 7. `setupDefaultScrollPane` not called on `fileListView.getScrollPane` (SIMPLIFIED)

**Java** (line 454): `setupDefaultScrollPane(fileListView.getScrollPane());`

**Scala** (lines 351-352): `fileListView = new ListView[FileHandle](fileListAdapter)` -- no call to `setupDefaultScrollPane` on the scroll pane.

**Severity**: P1 -- the file list scroll pane won't have overscroll/flick/fade settings configured.

### 8. ViewMode.setupGridGroup error message truncated (SIMPLIFIED)

**Java** (lines 1628-1629):
```java
throw new IllegalStateException("FileChooser's ViewMode " + this.toString() + " has invalid size defined in Sizes. " +
        "Expected value greater than 0, got: " + gridSize + ". Check your skin Sizes definition.");
```

**Scala** (line 1484):
```scala
throw new IllegalStateException("FileChooser's ViewMode " + this.toString + " has invalid size defined in Sizes.")
```

Missing the `"Expected value greater than 0, got: " + gridSize + ". Check your skin Sizes definition."` suffix.

**Severity**: P2 -- error message less informative.

### 9. `ConstantIfVisibleValue` usage difference in toolbar

**Java** (line 367): `new ConstantIfVisibleValue(sizes.spacingRight)` -- uses the single-arg constructor.

**Scala** (line 288-289): Same pattern -- `new ConstantIfVisibleValue(sizes.spacingRight)`. This matches.

No gap here.

---

## METHOD-BY-METHOD COMPARISON

### Static members (companion object)

| Java method | Scala equivalent | Status |
|-------------|-----------------|--------|
| `static saveLastDirectory` field | `FileChooser.saveLastDirectory` var | OK |
| `static focusFileScrollPaneOnShow` | `FileChooser.focusFileScrollPaneOnShow` var | OK |
| `static focusSelectedFileTextFieldOnShow` | `FileChooser.focusSelectedFileTextFieldOnShow` var | OK |
| `static setDefaultPrefsName(String)` | `FileChooser.setDefaultPrefsName(String)` | OK |
| `static setFavoritesPrefsName(String)` (deprecated) | `FileChooser.setFavoritesPrefsName(String)` (deprecated) | OK |
| `static isSaveLastDirectory()` | `FileChooser.isSaveLastDirectory` | OK |
| `static setSaveLastDirectory(boolean)` | `FileChooser.setSaveLastDirectory(Boolean)` | OK |
| `static SHORTCUTS_COMPARATOR` | `FileChooser.SHORTCUTS_COMPARATOR` | OK |
| `static DEFAULT_KEY` | `FileChooser.DEFAULT_KEY` | OK |
| `static tmpVector` | Local `val tmpVector` in `highlightFiles` | OK (not shared, no concurrency issue in scene2d) |

### Constructors

| Java constructor | Scala equivalent | Status |
|-----------------|-----------------|--------|
| `FileChooser(Mode)` | Primary `class FileChooser(_mode: Mode)` | **P0: double-init when used via other ctors** |
| `FileChooser(FileHandle, Mode)` | `this(directory, mode)` | **P0: calls init twice** |
| `FileChooser(String, Mode)` | `this(title, mode)` | OK (style matches, init runs once via primary) |
| `FileChooser(String, String, Mode)` | `this(styleName, title, mode)` | **P0: calls init twice** |

### Instance fields

| Java field | Scala equivalent | Status |
|-----------|-----------------|--------|
| `mode` | `_mode` | OK |
| `viewMode` | `_viewMode` | OK |
| `selectionMode` | `_selectionMode` | OK |
| `sorting` (AtomicReference) | `_sorting` (AtomicReference) | OK |
| `sortingOrderAscending` (AtomicBoolean) | `_sortingOrderAscending` (AtomicBoolean) | OK |
| `listener` | `_listener` | OK |
| `fileFilter` | `_fileFilter` | OK |
| `fileDeleter` | `_fileDeleter` | OK |
| `fileTypeFilter` | `_fileTypeFilter` (Nullable) | OK |
| `activeFileTypeRule` | `_activeFileTypeRule` (Nullable) | OK |
| `iconProvider` | `_iconProvider` | OK |
| `driveCheckerService` | `driveCheckerService` | OK |
| `driveCheckerListeners` | `driveCheckerListeners` | OK |
| `chooserWinService` | `chooserWinService` (Nullable) | OK |
| `listDirExecutor` | `listDirExecutor` | OK |
| `listDirFuture` | `listDirFuture` (Nullable) | OK |
| `showBusyBarTask` | `showBusyBarTask` | OK |
| `dateFormat` | `dateFormat` | OK |
| `showSelectionCheckboxes` | `_showSelectionCheckboxes` | OK |
| `multiSelectionEnabled` | `_multiSelectionEnabled` | OK |
| `groupMultiSelectKey` | `_groupMultiSelectKey` | OK |
| `multiSelectKey` | `_multiSelectKey` | OK |
| `preferencesIO` | `preferencesIO` | OK |
| `favorites` | `favorites` | OK |
| `recentDirectories` | `recentDirectories` | OK |
| `currentDirectory` | `currentDirectory` | OK |
| `currentFiles` | `currentFiles` | OK |
| `currentFilesMetadata` (IdentityMap) | `currentFilesMetadata` (IdentityHashMap) | OK (equivalent) |
| `fileListAdapter` | `fileListAdapter` | OK |
| `selectedItems` | `selectedItems` | OK |
| `selectedShortcut` | `selectedShortcut` (Nullable) | OK |
| `defaultFileName` | `_defaultFileName` (Nullable) | OK |
| `watchingFilesEnabled` | `_watchingFilesEnabled` | OK |
| `fileWatcherThread` | `fileWatcherThread` (Nullable) | OK |
| `shortcutsListRebuildScheduled` | `shortcutsListRebuildScheduled` | OK |
| `filesListRebuildScheduled` | `filesListRebuildScheduled` | OK |
| `historyManager` | `historyManager` | OK |
| `style` | `_chooserStyle` | OK |
| `sizes` | `sizes` | OK |
| All UI fields | All present | OK |

### Private methods

| Java method | Scala equivalent | Status |
|-------------|-----------------|--------|
| `init(FileHandle)` | `init(Nullable[FileHandle])` | **P0: double-init + P0: dirsSuggestionPopup created twice** |
| `createToolbar()` | `createToolbar()` | OK (logic matches) |
| `createViewModePopupMenu()` | `createViewModePopupMenu()` | OK |
| `rebuildViewModePopupMenu()` | `rebuildViewModePopupMenu()` | OK |
| `updateFavoriteFolderButton()` | `updateFavoriteFolderButton()` | OK |
| `createCenterContentPanel()` | `createCenterContentPanel()` | **P1: missing setupDefaultScrollPane call** |
| `invalidateChildHierarchy(WidgetGroup)` | `invalidateChildHierarchy(WidgetGroup)` | OK |
| `setCurrentPathFieldText(String)` | `setCurrentPathFieldText(String)` | OK |
| `createFileTextBox()` | `createFileTextBox()` | **P0: missing 4 spaceBottom calls** |
| `updateFileTypeSelectBox()` | `updateFileTypeSelectBox()` | OK |
| `createBottomButtons()` | `createBottomButtons()` | OK |
| `createShortcutsMainPanel()` | `createShortcutsMainPanel()` | OK |
| `createListeners()` | `createListeners()` | OK |
| `selectionFinished()` | `selectionFinished()` | OK |
| `notifyListenerAndCloseDialog(Array<FileHandle>)` | `notifyListenerAndCloseDialog(Nullable[DynamicArray[FileHandle]])` | OK |
| `getFileListFromSelected()` | `getFileListFromSelected()` | OK |
| `showDialog(String)` | `showDialog(String)` | OK |
| `showOverwriteQuestion(Array<FileHandle>)` | `showOverwriteQuestion(DynamicArray[FileHandle])` | OK |
| `rebuildShortcutsList(boolean)` | `rebuildShortcutsList(Boolean)` | OK |
| `rebuildShortcutsList()` | `rebuildShortcutsList()` | OK |
| `rebuildFileRootsCache()` | `rebuildFileRootsCache()` | OK |
| `rebuildShortcutsFavoritesPanel()` | `rebuildShortcutsFavoritesPanel()` | OK |
| `rebuildFileList()` | `rebuildFileList()` | OK |
| `rebuildFileList(boolean)` | `rebuildFileList(Boolean)` | OK |
| `buildFileList(...)` | `buildFileList(...)` | OK |
| `listFilteredCurrentDirectory()` | `listFilteredCurrentDirectory()` | OK |
| `deselectAll()` | `deselectAll()` | OK |
| `deselectAll(boolean)` | `deselectAll(Boolean)` | OK |
| `selectAll()` | `selectAll()` | OK |
| `updateSelectedFileFieldText()` | `updateSelectedFileFieldText()` | OK |
| `updateSelectedFileFieldText(boolean)` | `updateSelectedFileFieldText(Boolean)` | OK |
| `removeInvalidSelections()` | `removeInvalidSelections()` | OK |
| `showNewDirectoryDialog()` | `showNewDirectoryDialog()` | OK |
| `showFileDeleteDialog(FileHandle)` | `showFileDeleteDialog(FileHandle)` | OK (P2: missing printStackTrace) |
| `addRecentDirectory(FileHandle)` | `addRecentDirectory(FileHandle)` | OK |
| `startFileWatcher()` | `startFileWatcher()` | OK |
| `stopFileWatcher()` | `stopFileWatcher()` | OK |
| `reloadPreferences(boolean)` | `reloadPreferences(Boolean)` | OK |
| `getDefaultStartingDirectory()` | `getDefaultStartingDirectory` | OK |
| `isMultiSelectKeyPressed()` | `isMultiSelectKeyPressed` | OK |
| `isGroupMultiSelectKeyPressed()` | `isGroupMultiSelectKeyPressed` | OK |
| `getChooserStage()` | `getChooserStage` | **P1: throws vs returns null** |

### Public methods

| Java method | Scala equivalent | Status |
|-------------|-----------------|--------|
| `setSelectedFiles(FileHandle...)` | `setSelectedFiles(FileHandle*)` | OK |
| `setDefaultFileName(String)` | `defaultFileName_=` / `setDefaultFileName` | OK |
| `refresh()` | `refresh()` | OK |
| `addFavorite(FileHandle)` | `addFavorite(FileHandle)` | OK |
| `removeFavorite(FileHandle)` | `removeFavorite(FileHandle)` | OK |
| `clearRecentDirectories()` | `clearRecentDirectories()` | OK |
| `setVisible(boolean)` | `setVisible(Boolean)` | **P1: no super.setVisible call** |
| `highlightFiles(FileHandle...)` | `highlightFiles(FileHandle*)` | OK |
| `getMode()` | `getMode` | OK |
| `setMode(Mode)` | `setMode(Mode)` | OK |
| `getViewMode()` | `getViewMode` | OK |
| `setViewMode(ViewMode)` | `setViewMode(ViewMode)` | OK |
| `setDirectory(String)` | `setDirectory(String)` | OK |
| `setDirectory(File)` | `setDirectory(File)` | OK |
| `setDirectory(FileHandle)` | `setDirectory(FileHandle)` | OK |
| `setDirectory(FileHandle, HistoryPolicy)` | `setDirectory(FileHandle, HistoryPolicy)` | OK |
| `getCurrentDirectory()` | `getCurrentDirectory` | OK |
| `getFileFilter()` | `getFileFilter` | OK |
| `setFileFilter(FileFilter)` | `setFileFilter(FileFilter)` | OK |
| `setFileTypeFilter(FileTypeFilter)` | `setFileTypeFilter(Nullable[FileTypeFilter])` | OK |
| `getActiveFileTypeFilterRule()` | `getActiveFileTypeFilterRule` | OK |
| `getSelectionMode()` | `getSelectionMode` / `selectionMode` | OK |
| `setSelectionMode(SelectionMode)` | `setSelectionMode(SelectionMode)` / `selectionMode_=` | OK |
| `getSorting()` | `getSorting` | OK |
| `setSorting(FileSorting, boolean)` | `setSorting(FileSorting, Boolean)` | OK |
| `setSorting(FileSorting)` | `setSorting(FileSorting)` | OK |
| `isSortingOrderAscending()` | `isSortingOrderAscending` | OK |
| `setSortingOrderAscending(boolean)` | `setSortingOrderAscending(Boolean)` | OK |
| `setFavoriteFolderButtonVisible(boolean)` | `setFavoriteFolderButtonVisible(Boolean)` | OK |
| `isFavoriteFolderButtonVisible()` | `isFavoriteFolderButtonVisible` | OK |
| `setViewModeButtonVisible(boolean)` | `setViewModeButtonVisible(Boolean)` | OK |
| `isViewModeButtonVisible()` | `isViewModeButtonVisible` | OK |
| `isMultiSelectionEnabled()` | `isMultiSelectionEnabled` / `multiSelectionEnabled` | OK |
| `setMultiSelectionEnabled(boolean)` | `setMultiSelectionEnabled(Boolean)` | OK |
| `setListener(FileChooserListener)` | `setListener(FileChooserListener)` | OK |
| `isShowSelectionCheckboxes()` | `isShowSelectionCheckboxes` / `showSelectionCheckboxes` | OK |
| `setShowSelectionCheckboxes(boolean)` | `setShowSelectionCheckboxes(Boolean)` | OK |
| `getMultiSelectKey()` | `getMultiSelectKey` | OK |
| `setMultiSelectKey(int)` | `setMultiSelectKey(Int)` | OK |
| `getGroupMultiSelectKey()` | `getGroupMultiSelectKey` | OK |
| `setGroupMultiSelectKey(int)` | `setGroupMultiSelectKey(Int)` | OK |
| `getChooserStyle()` | `getChooserStyle` | OK |
| `getSizes()` | `getSizes` | OK |
| `setWatchingFilesEnabled(boolean)` | `setWatchingFilesEnabled(Boolean)` | OK |
| `setPrefsName(String)` | `setPrefsName(String)` | OK |
| `setFileDeleter(FileDeleter)` | `setFileDeleter(FileDeleter)` | OK |
| `setIconProvider(FileIconProvider)` | `setIconProvider(FileIconProvider)` | OK |
| `getIconProvider()` | `getIconProvider` | OK |

### Override methods

| Java method | Scala equivalent | Status |
|-------------|-----------------|--------|
| `close()` | `close()` | OK |
| `fadeOut(float)` | `fadeOut(Float)` | OK |
| `draw(Batch, float)` | `draw(Batch, Float)` | OK |
| `setStage(Stage)` | `setStage(Nullable[Stage])` | OK |

### Enums

| Java enum | Scala equivalent | Status |
|-----------|-----------------|--------|
| `Mode` | `Mode` | OK |
| `SelectionMode` | `SelectionMode` | OK |
| `FileSorting` | `FileSorting` | OK |
| `HistoryPolicy` | `HistoryPolicy` | OK |
| `ViewMode` | `ViewMode` | OK (all values, methods match) |

### Inner classes / interfaces

| Java class | Scala equivalent | Status |
|-----------|-----------------|--------|
| `FileIconProvider` (interface) | `FileIconProvider` (trait) | OK |
| `DefaultFileIconProvider` | `DefaultFileIconProvider` | OK |
| `DefaultFileFilter` | `DefaultFileFilter` | OK |
| `FileDeleter` (interface) | `FileDeleter` (trait) | OK |
| `DefaultFileDeleter` | `DefaultFileDeleter` | OK |
| `ShowBusyBarTask` | `ShowBusyBarTask` | OK |
| `FileItem` | `FileItem` | OK |
| `ShortcutItem` | `ShortcutItem` | OK |
| `ShortcutsComparator` | `SHORTCUTS_COMPARATOR` (Ordering) | OK (inlined as val) |

### FileItem inner class -- detailed comparison

| Java method | Scala equivalent | Status |
|-------------|-----------------|--------|
| Constructor | Initializer block | OK (logic equivalent) |
| `setIcon(Drawable, Scaling)` | `setIcon(Drawable, Scaling)` | OK |
| `addListeners()` | `addListeners()` | OK |
| `handleSelectClick(boolean)` | `handleSelectClick(Boolean)` | OK |
| `selectGroup()` | `selectGroup()` | OK |
| `getItemId(Array, FileItem)` | `getItemId(DynamicArray, FileItem)` | OK |
| `select()` | `select()` | OK |
| `select(boolean)` | `select(Boolean)` | OK |
| `deselect()` | `deselect()` | OK |
| `deselect(boolean)` | `deselect(Boolean)` | OK |
| `focusLost()` | `focusLost()` | OK |
| `focusGained()` | `focusGained()` | OK |
| `getFile()` | `getFile` | OK |
| `isDirectory()` | `isDirectory` | OK |

### ShortcutItem inner class -- detailed comparison

| Java method | Scala equivalent | Status |
|-------------|-----------------|--------|
| Constructor | Initializer block | OK |
| `addListener()` | Inline in initializer | OK |
| `setLabelText(String)` | `setLabelText(String)` | OK |
| `getLabelText()` | `getLabelText` | OK |
| `select()` | `selectShortcut()` | OK (renamed, same logic) |
| `deselect()` | `deselect()` | OK |
| `setRootName(String)` | `setRootName(String)` | OK |
| `focusGained()` | `focusGained()` | OK |
| `focusLost()` | `focusLost()` | OK |

---

## HIGH-RISK AREAS CHECKLIST

| Area | Status | Notes |
|------|--------|-------|
| File browsing / navigation | OK | `setDirectory`, history back/forward via `FileHistoryManager` all present |
| Selection handling (single/multi) | OK | `handleSelectClick`, `selectGroup`, `removeInvalidSelections` all match |
| Threading (background file listing) | OK | `listDirExecutor`, `listDirFuture`, `Thread.isInterrupted` checks match |
| Favorites / recent / bookmarks | OK | `addFavorite`, `removeFavorite`, `addRecentDirectory`, `clearRecentDirectories` match |
| Drag and drop | N/A | Not in original Java source |
| File deletion confirmation | OK | `showFileDeleteDialog` matches (minus `e.printStackTrace`) |
| Drive/root listing | OK | `rebuildFileRootsCache` matches, platform-specific Windows service present |
| Sorting | OK | `FileSorting` enum, `setSorting`, `setSortingOrderAscending` all match |
| File type filters | OK | `setFileTypeFilter`, `updateFileTypeSelectBox`, `listFilteredCurrentDirectory` match |
| Context menu (right-click) | OK | `fileMenu.build` / `buildForFavorite` calls match |
| Keyboard navigation | OK | Ctrl+A, letter key search, Enter in text fields, Delete key all present |
| Input listeners on interactive elements | OK | All listeners from Java are present in Scala |

---

## SUMMARY OF GAPS

| # | Gap | Classification | Priority | Java Lines | Scala Lines |
|---|-----|---------------|----------|------------|-------------|
| 1 | Constructor double-init (FileHandle and styleName ctors call init twice) | DIFFERENT_BEHAVIOR | P0 | 163-205 | 125-150 |
| 2 | `createFileTextBox` missing 4 `spaceBottom` calls with `ConstantIfVisibleValue` | SIMPLIFIED | P0 | 536-542 | 418-421 |
| 3 | `dirsSuggestionPopup` created in both `init()` and `createToolbar()`, listener lost | DIFFERENT_BEHAVIOR | P0 | 299-306 | 191, 221-227 |
| 4 | `createCenterContentPanel` missing `setupDefaultScrollPane` on file list scroll pane | SIMPLIFIED | P1 | 454 | 351-352 |
| 5 | `getChooserStage` throws instead of returning null | DIFFERENT_BEHAVIOR | P1 | 1386 | 1048 |
| 6 | `setVisible` doesn't call `super.setVisible` | DIFFERENT_BEHAVIOR | P1 | 1038-1043 | 526-529 |
| 7 | `ViewMode.setupGridGroup` error message truncated | SIMPLIFIED | P2 | 1628-1629 | 1484 |
| 8 | `showFileDeleteDialog` missing `e.printStackTrace()` | SIMPLIFIED | P2 | 1539 | 1148 |
