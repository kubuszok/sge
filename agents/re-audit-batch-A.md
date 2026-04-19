# Re-Audit Batch A: VisUI (all sub-packages)

Auditor: Claude Opus 4 re-audit agent
Date: 2026-04-18
Scope: ~153 files in `sge-extension/visui/src/main/scala/sge/visui/`

## Architecture Note

The original VisUI `VisTextField` extends `Widget` and is a **complete reimplementation** of LibGDX `TextField`.
The original `VisTextArea` extends `VisTextField` (itself a complete reimplementation).
The SGE port takes a different approach: `VisTextField extends TextField` and `VisTextArea extends TextArea`,
adding only the VisUI-specific features (focus border, error border, cursor percent height, etc.).
This means many "missing" methods in the compare output are actually inherited from the base classes.
This is a legitimate and well-documented design decision.

---

## HIGH-SUSPICION FILES

### VisTextField.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisTextField.java`
- **Prior status**: pass (with notes)
- **New status**: MINOR_ISSUES
- **Missing methods**: none (87 "missing" are inherited from base TextField)
- **Simplified methods**: `focusField()` -- original sets `selectionStart = 0` explicitly and uses `stage.setKeyboardFocus(this)` directly; port uses `stage.foreach { s => s.setKeyboardFocus(...) }` and `keyboard.show(this)` instead of `keyboard.show(true)` -- slightly different API
- **Missing branches**: none significant
- **Mechanism changes without tests**: `focusField()` behavior difference (keyboard.show parameter)
- **Notes**: Compare output shows 87 missing but all are methods present in the base `TextField` class (copy, cut, paste, next, findNextTextField, calculateOffsets, updateDisplayText, etc.). The port correctly delegates these to the superclass. The `TextFieldClickListener` inner class is also in the base TextField. `KeyRepeatTask` is in base TextField. Checked: `draw()`, `drawCursor()`, `changeText()`, `initialize()`, `backgroundDrawable` -- all equivalent. The `VisTextFieldStyle` copy constructor correctly copies all fields including `focusBorder`, `errorBorder`, `backgroundOver`. Minor: The 5-arg `VisTextFieldStyle(font, fontColor, cursor, selection, background)` constructor from the original is missing -- only no-arg and copy constructors exist.

### FileChooser.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/FileChooser.java`
- **Prior status**: pass (with notes)
- **New status**: NEEDS_DEEP_REVIEW
- **Missing methods**: Compare shows 46 missing but most are type references. Port has 157 common + 111 extra members suggesting thorough porting.
- **Simplified methods**: Too large to fully verify body-by-body (FileChooser.java is ~1800 lines)
- **Missing branches**: Cannot verify without reading entire file
- **Mechanism changes without tests**: unknown -- file is massive
- **Notes**: The 46 "missing" items from compare are mostly type references (e.g., `BusyBar`, `ButtonBar`, `ChangeListener`, `File`, `Image`, `MenuItem`, `Thread`, etc.) and low-level methods like `add`, `addListener`, `getStage` which are inherited. The 111 "extra" items include fields, properties, and implementation details that match the original. No shortcut markers found. Due to the extreme size (~1800 lines original, ~1400 lines port), I cannot verify every method body. This file needs a dedicated deep review pass.

### TabbedPane.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/tabbedpane/TabbedPane.java`
- **Prior status**: pass (with notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `TabButtonTable.addListeners()` InputListener is entirely missing
- **Simplified methods**: `TabButtonTable` constructor -- missing the InputListener with touchDown/touchUp/mouseMoved/exit/enter handlers
- **Missing branches**: middle-click to close tab, hover visual feedback for close button, dragged-up-image styling
- **Mechanism changes without tests**: Tab close button hover styling (setCloseButtonOnMouseMove, setDraggedUpImage, setDefaultUpImage)
- **Notes**: The `TabButtonTable` inner class in the original has both a `ChangeListener` on the close button AND a detailed `InputListener` on the main button with 5 handler methods. The port only has the `ChangeListener` on close button and a `ChangeListener` on the main button. **Missing entirely**: (1) middle-click (`Buttons.MIDDLE`) to close tab, (2) `setDraggedUpImage()` -- when left-click drag starts, close button and button style are changed, (3) `setCloseButtonOnMouseMove()` -- when mouse moves over an inactive tab, close button style changes to show button's `over` drawable, (4) `setDefaultUpImage()` -- restores original up drawable on exit, (5) the `up` field storing the original drawable. Also: `setDisabled` override on the button (which also disables closeButton and calls deselect) is missing. `TabbedPaneStyle` is missing the 5-arg constructor. These are visual polish features but represent real behavioral differences.

### FloatDigitsOnlyFilter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/FloatDigitsOnlyFilter.java`
- **Prior status**: pass (with notes)
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Fully equivalent. Uses `field.text` instead of `field.getText()` (Scala property), `field.selection.nonEmpty` instead of `field.isTextSelected()`, `field.getSelectionStart`/`field.cursorPosition` instead of `field.getSelectionStart()`/`field.getCursorPosition()`. All idiomatic Scala equivalents. Logic preserved including issue #131 handling.

---

## TOP-LEVEL PACKAGE (sge.visui)

### FocusManager.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/FocusManager.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Notes**: 4 common methods, 1 extra (focusedWidget field). No shortcut markers. All methods (switchFocus, resetFocus, getFocusedWidget) present.

### Focusable.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/Focusable.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: 3 common methods (focusLost, focusGained, isFocusBorderEnabled). Simple trait, fully equivalent.

### Locales.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/Locales.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `Locale` (Java type), `getName` -> `name`, `setLocale` -> `locale_=` (property setter)
- **Notes**: 18 common methods. All "missing" are property renames (getX/setX -> Scala property). CommonText enum has name property. All bundle getters present.

### Sizes.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/Sizes.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none (all are fields, present as public vars)
- **Notes**: 1 common + 15 extra. Original is a class with public fields; port has same fields as vars. Copy constructor `this(other: Sizes)` present.

### VisUI.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/VisUI.java`
- **Prior status**: pass
- **New status**: MINOR_ISSUES
- **Missing methods**: `getSizesName` (returns the skin scale sizes name), `setSkipGdxVersionCheck` (skips version check on load)
- **Notes**: 11 common methods. `getSizesName` is a minor accessor. `setSkipGdxVersionCheck` is a configuration flag. Both are low-impact but still represent missing functionality.

---

## BUILDING PACKAGE

### CenteredTableBuilder.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/CenteredTableBuilder.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getDefaultWidgetPadding`, `getLowestCommonMultiple`, `getRowSizes` -- these are inherited from base `TableBuilder`
- **Notes**: 4 common methods. Missing are inherited methods from parent.

### GridTableBuilder.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/GridTableBuilder.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getDefaultWidgetPadding`, `getWidgets` -- inherited from parent
- **Notes**: 2 common. buildTable logic present.

### OneColumnTableBuilder.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/OneColumnTableBuilder.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited from parent
- **Notes**: 2 common. Clean.

### OneRowTableBuilder.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/OneRowTableBuilder.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited from parent
- **Notes**: 2 common. Clean.

### StandardTableBuilder.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/StandardTableBuilder.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited from parent
- **Notes**: 2 common. buildTable present.

### TableBuilder.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/TableBuilder.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `IntArray` (type reference only)
- **Notes**: 15 common methods. All logic methods present.

---

## BUILDING/UTILITIES PACKAGE

### Alignment.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/utilities/Alignment.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getAlignment` -> property, `values` -> enum built-in
- **Notes**: 11 common. All alignment enum values present.

### CellWidget.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/utilities/CellWidget.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none (24 common)
- **Notes**: Builder pattern fully ported. CellWidgetBuilder inner class present.

### Nullables.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/utilities/Nullables.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none (10 common)
- **Notes**: All utility methods present.

### Padding.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/utilities/Padding.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getTop/getBottom/getLeft/getRight` -> Scala properties
- **Notes**: 9 common. Constants PAD_0/2/4/8 present. All padding fields accessible.

---

## BUILDING/UTILITIES/LAYOUTS PACKAGE

### ActorLayout.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/utilities/layouts/ActorLayout.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. convertToActorFromCells helper present.

### GridTableLayout.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/utilities/layouts/GridTableLayout.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Missing `GridTableBuilder` is type reference.

### TableLayout.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/building/utilities/layouts/TableLayout.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 4 common. All methods present.

---

## I18N PACKAGE

### BundleText.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/i18n/BundleText.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getName` -> `name` (property rename)
- **Notes**: 3 common. Simple trait with get/format/toString/name.

---

## LAYOUT PACKAGE

### FloatingGroup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/layout/FloatingGroup.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: all "missing" are inherited from WidgetGroup or are property renames
- **Notes**: 4 common. layout() method present with proper child positioning logic.

### FlowGroup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/layout/FlowGroup.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited or property renames
- **Notes**: 9 common. layout/computeSize logic present.

### GridGroup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/layout/GridGroup.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited or property renames
- **Notes**: 5 common. Grid layout logic with items present.

### HorizontalFlowGroup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/layout/HorizontalFlowGroup.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited or property renames
- **Notes**: 4 common. layout/computeSize present.

### VerticalFlowGroup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/layout/VerticalFlowGroup.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited or property renames
- **Notes**: 4 common. layout/computeSize present.

### DragPane.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/layout/DragPane.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getActor` (inherited), type references
- **Notes**: 54 common methods. Very thorough port. DragPaneListener, AcceptOwnChildren, DefaultDragListener all present.

---

## UTIL PACKAGE

### ActorUtils.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/ActorUtils.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. keepWithinStage method present.

### BorderOwner.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/BorderOwner.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `isFocusBorderEnabled/setFocusBorderEnabled` -> `focusBorderEnabled/focusBorderEnabled_=`
- **Notes**: Property rename. Fully equivalent.

### ColorUtils.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/ColorUtils.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. HSVtoRGB and RGBtoHSV methods present.

### CursorManager.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/CursorManager.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. restoreDefaultCursor, setDefaultCursor, setDefaultSystemCursor present.

### InputValidator.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/InputValidator.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Simple trait with validateInput method.

### IntDigitsOnlyFilter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/IntDigitsOnlyFilter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. acceptChar logic present.

### NumberDigitsTextFieldFilter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/NumberDigitsTextFieldFilter.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `isAcceptNegativeValues/isUseFieldCursorPosition` -> properties
- **Notes**: 3 common. Property renames only.

### OsUtils.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/OsUtils.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 8 common. OS detection and shortcut key string building present.

### TableUtils.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/TableUtils.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. setSpacingDefaults present.

### ToastManager.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/ToastManager.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getScreenPadding` -> property; type refs (Toast, VisTable, WidgetGroup)
- **Notes**: 17 common. show/remove/resize/clear/getToasts all present. UNTIL_CLOSED constant. Timer tasks for toast timing.

### Validators.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/Validators.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `setGreaterThan/setLesserThan/setUseEquals` -> public `var` fields (direct assignment in Scala)
- **Notes**: 6 common. Verified: `GreaterThanValidator` has `var greaterThan: Float` and `var useEquals: Boolean`, `LesserThanValidator` has `var lesserThan: Float` and `var useEquals: Boolean`. All mutable. IntegerValidator, FloatValidator, shared instances INTEGERS/FLOATS all present.

---

## UTIL/ADAPTER PACKAGE

### AbstractListAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/adapter/AbstractListAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getView` (abstract), `iterable` (abstract) -- these are defined in the trait/class as abstract
- **Notes**: 42 common. Thorough port. ListClickListener inner class present. Selection handling with multi-select/group-select present.

### ArrayListAdapter.scala (replaces ArrayAdapter.java)
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/adapter/ArrayListAdapter.java` AND `ArrayAdapter.java`
- **Prior status**: pass
- **New status**: MINOR_ISSUES
- **Missing methods**: `itemRemoved` callback not called from `remove(item)` method when item not found (minor edge case); `ArrayAdapter.java` methods like `shuffle`, `reverse`, `pop`, `swap`, `removeRange` are not ported as they are LibGDX Array-specific
- **Notes**: 12 common. The `ArrayAdapter.java` (using LibGDX Array) is replaced by `ArrayListAdapter.java` (using Java ArrayList). The port follows the `ArrayListAdapter.java` original faithfully. The `ArrayAdapter.java` is not ported directly, which is correct since LibGDX Array is replaced by ArrayList in SGE.

### CachedItemAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/adapter/CachedItemAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. View caching logic present.

### ItemAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/adapter/ItemAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Simple trait.

### ListAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/adapter/ListAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 9 common. All interface methods present.

### ListSelectionAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/adapter/ListSelectionAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Simple adapter.

### SimpleListAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/adapter/SimpleListAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. SimpleListAdapterStyle inner class present.

---

## UTIL/ASYNC PACKAGE

### AsyncTask.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/async/AsyncTask.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getStatus/getThreadName` -> property renames; `run/Runnable/Thread` are implementation details
- **Notes**: 11 common. Task execution, status tracking, listener notification present.

### AsyncTaskListener.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/async/AsyncTaskListener.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. All callback methods present.

### AsyncTaskProgressDialog.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/async/AsyncTaskProgressDialog.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 7 common. Progress bar and status label integration present.

### SteppedAsyncTask.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/async/SteppedAsyncTask.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Step/totalSteps tracking present.

---

## UTIL/DIALOG PACKAGE

### Dialogs.scala (merges 6 original files)
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/dialog/Dialogs.java` + 5 listener/adapter files
- **Prior status**: pass (with notes)
- **New status**: MAJOR_ISSUES
- **Missing methods**: `showConfirmDialog`, `showDetailsDialog`, `DetailsDialog` inner class entirely missing, `InputDialog` inner class (replaced with inline anonymous dialog), `OptionDialog` inner class (replaced with inline anonymous dialog)
- **Simplified methods**:
  1. `showErrorDialog(stage, text, details)` -- the `details` parameter is **accepted but completely ignored** (lines 64-72). No details button, no scroll pane, no copy button. The original creates a full `DetailsDialog` with expandable details section, copy-to-clipboard functionality.
  2. `showOptionDialog` -- returns `VisDialog` instead of `OptionDialog`. The original `OptionDialog` has `setYesButtonText`, `setNoButtonText`, `setCancelButtonText` methods for customizing button text after creation. These are not available.
  3. `showInputDialog` -- returns `VisDialog` instead of `InputDialog`. The original `InputDialog` has `setText`, `setStage` (auto-focuses field), Enter-key submission (when OK not disabled), and validation-based OK button disabling. The port is missing: (a) `setText` method, (b) auto-focus on stage attach, (c) Enter key to submit, (d) OK button disabled when validation fails.
- **Missing branches**: Details visibility toggle, copy-to-clipboard for details, validation-driven OK disable, Enter key submission
- **Mechanism changes without tests**: All of the above
- **Notes**: The merged listener/adapter interfaces (OptionDialogListener, OptionDialogAdapter, InputDialogListener, InputDialogAdapter) are present as inner types of Dialogs object. `ConfirmDialogListener` interface from the original is missing entirely. This is one of the most significant gaps found in this audit.

---

## UTIL/FORM PACKAGE

### FormInputValidator.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/form/FormInputValidator.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 9 common. Error message, hide-on-empty, result tracking all present.

### FormValidator.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/form/FormValidator.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `isMustBeEmpty/setMustBeEmpty/setMustNotExist/setRelativeToFile/setRelativeToTextField/setErrorIfRelativeEmpty` -- these are methods on the `FileExistsValidator` inner class (property renames or present differently)
- **Notes**: 10 common. File existence validation logic present.

### SimpleFormValidator.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/form/SimpleFormValidator.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 24 common. FormValidatorStyle, CheckedButtonWrapper inner classes present. Validation pipeline with color transitions present.

### ValidatorWrapper.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/form/ValidatorWrapper.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Simple wrapper class.

---

## UTIL/HIGHLIGHT PACKAGE

### BaseHighlighter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/highlight/BaseHighlighter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 4 common. Rule management and processing present.

### Highlight.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/highlight/Highlight.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getColor/getStart/getEnd` -> Scala properties
- **Notes**: 2 common + compareTo. All fields accessible.

### HighlightRule.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/highlight/HighlightRule.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Simple trait with process method.

### Highlighter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/highlight/Highlighter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Interface with getHighlights method.

### RegexHighlightRule.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/highlight/RegexHighlightRule.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Regex matching logic present.

### WordHighlightRule.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/highlight/WordHighlightRule.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Word search logic present.

---

## UTIL/VALUE PACKAGE

### ConstantIfVisibleValue.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/value/ConstantIfVisibleValue.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Visibility-conditional value logic present.

### PrefHeightIfVisibleValue.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/value/PrefHeightIfVisibleValue.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. INSTANCE singleton present.

### PrefWidthIfVisibleValue.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/value/PrefWidthIfVisibleValue.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. INSTANCE singleton present.

### VisValue.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/value/VisValue.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. percentHeight/percentWidth factory methods present.

### VisWidgetValue.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/value/VisWidgetValue.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Widget-based value calculation present.

---

## WIDGET PACKAGE (main widgets)

### BusyBar.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/BusyBar.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: getter renames only
- **Notes**: 4 common. Animation draw logic present. BusyBarStyle inner class present.

### ButtonBar.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/ButtonBar.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getId/getText/getOrder/setOrder/isIgnoreSpacing/setIgnoreSpacing` -> properties
- **Notes**: 8 common. ButtonType enum with all values. Platform-specific ordering (LINUX/OSX/WINDOWS) present.

### CollapsibleWidget.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/CollapsibleWidget.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited from WidgetGroup or property renames
- **Notes**: 9 common. Collapse animation with interpolation and duration present.

### Draggable.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/Draggable.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames, inherited
- **Notes**: 39 common. Very thorough port. MimicActor inner class present. DragListener interface with APPROVE/CANCEL constants.

### HorizontalCollapsibleWidget.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/HorizontalCollapsibleWidget.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 9 common. Horizontal collapse logic present.

### LinkLabel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/LinkLabel.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `init` (initialization helper), property renames
- **Notes**: 7 common. LinkLabelStyle present. Click listener with URL opening present.

### ListView.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/ListView.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames
- **Notes**: 14 common. UpdatePolicy enum, data invalidation, header/footer management present.

### ListViewStyle.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/ListViewStyle.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. Style class with scrollPaneStyle field.

### Menu.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/Menu.java`
- **Prior status**: pass
- **New status**: MAJOR_ISSUES
- **Missing methods**: `showMenu()`, `switchMenu()`, `remove()` override, `InputListener` on openButton
- **Simplified methods**: none -- they are entirely absent
- **Missing branches**: (1) touchDown on openButton that toggles menu open/close, (2) enter handler that auto-switches menu when hovering with another menu already open, (3) remove() override that clears MenuBar.currentMenu when menu is removed from stage
- **Mechanism changes without tests**: The entire menu open/close mechanism is missing. Without the InputListener on openButton, clicking a menu title in the MenuBar does nothing. The original adds an InputListener in the constructor with touchDown (which calls switchMenu/closeMenu) and enter (which handles hover-to-switch). The port has selectButton/deselectButton and setMenuBar but no way to actually trigger menu display.
- **Notes**: 6 common methods. This is a critical functional gap -- the MenuBar will render menu titles but clicking them will not open any popup menu. The original's showMenu() positions the popup below the button using localToStageCoordinates and adds it to the stage. The remove() override notifies MenuBar when the popup is dismissed.

### MenuBar.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/MenuBar.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 15 common. Menu management, menu listener, close-on-background-click all present.

### MenuItem.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/MenuItem.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `init` (helper), property renames, inherited
- **Notes**: 23 common. Image, label, shortcut label, sub-menu handling all present. MenuItemStyle with subMenu drawable.

### MultiSplitPane.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/MultiSplitPane.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited from WidgetGroup, property renames
- **Notes**: 24 common. Split handle dragging, widget bounds calculation, scissors clipping present. MultiSplitPaneStyle present.

### PopupMenu.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/PopupMenu.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited, property renames
- **Notes**: 32 common. Sub-menu management, stage listener for close-on-click, shared item listeners present.

### ScrollableTextArea.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/ScrollableTextArea.java`
- **Prior status**: pass
- **New status**: MAJOR_ISSUES
- **Missing methods**: Inherits VisTextArea's gap -- all VisTextField-specific features (readOnly, cursorPercentHeight, backgroundOver, focusField, etc.) are missing
- **Notes**: 12 common. ScrollPane integration, culling area, scroll-to-cursor present. However, since this extends VisTextArea which extends TextArea (not VisTextField), all VisTextField additions are unavailable. Same gap as VisTextArea.

### Separator.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/Separator.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. SeparatorStyle present. Draw logic present.

### Tooltip.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `init` (helper), property renames
- **Notes**: 19 common. TooltipStyle, Builder inner class, appear delay, fade time, mouse-move-fadeout all present. Target/content management present.

### VisCheckBox.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisCheckBox.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited, property renames
- **Notes**: 11 common. VisCheckBoxStyle with tick/background drawables. Image stack for compositing. Focus/error border drawing.

### VisDialog.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisDialog.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited
- **Notes**: 19 common. Button result mapping, fade in/out, close on escape, keyboard/scroll focus save/restore present.

### VisImage.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisImage.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. Thin wrapper over Image. Constructor overloads present.

### VisImageButton.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisImageButton.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `init`, inherited, property renames
- **Notes**: 10 common. VisImageButtonStyle with all drawable fields. Focus border, generateDisabledImage, image cell management present.

### VisImageTextButton.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisImageTextButton.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `init`, inherited, property renames
- **Notes**: 15 common. VisImageTextButtonStyle, orientation support, image/label cell management, focus border present.

### VisLabel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisLabel.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. Thin wrapper. Constructor overloads present.

### VisList.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisList.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Thin wrapper with FocusManager listener.

### VisProgressBar.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisProgressBar.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. Thin wrapper.

### VisRadioButton.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisRadioButton.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. Thin wrapper over VisCheckBox.

### VisScrollPane.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisScrollPane.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. Thin wrapper.

### VisSelectBox.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisSelectBox.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. FocusManager listener present.

### VisSlider.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisSlider.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. Thin wrapper.

### VisSplitPane.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisSplitPane.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited from WidgetGroup, property renames
- **Notes**: 27 common. Complete reimplementation with handle dragging, scissors clipping, cursor management. VisSplitPaneStyle, handleOver support present.

### VisTable.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisTable.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. addSeparator method present.

### VisTextArea.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisTextArea.java`
- **Prior status**: pass
- **New status**: MAJOR_ISSUES
- **Missing methods**: `readOnly`, `ignoreEqualsTextChange`, `cursorPercentHeight`, `enterKeyFocusTraversal`, `backgroundOver`, `isEmpty`, `clearText`, `setCursorAtTextEnd`, `focusField`, `changeText` override (with ignoreEqualsTextChange), `drawCursor` override (with cursorPercentHeight), `backgroundDrawable` override (with backgroundOver), `disabled_=` override (with FocusManager.resetFocus), I-beam cursor on hover
- **Simplified methods**: The entire VisTextField feature set is missing -- VisTextArea only has focus border and error border
- **Missing branches**: readOnly blocking, ignoreEqualsTextChange in changeText, cursor percent height drawing, background over on hover/focus, enter key focus traversal
- **Mechanism changes without tests**: The original VisTextArea extends VisTextField (which adds readOnly, cursorPercentHeight, backgroundOver, focusField, etc.). The port extends plain TextArea and only adds focus/error border. All VisTextField-specific features are lost for text areas.
- **Notes**: 1 common. The port correctly adds focusBorderEnabled, inputValid, focusLost/focusGained, and the draw override for borders. However, since the original VisTextArea extends VisTextField (not TextArea), it inherits ALL of VisTextField's additions. The SGE port's VisTextArea extends SGE's TextArea directly, bypassing VisTextField entirely. This means VisTextArea doesn't support: readOnly mode, cursorPercentHeight, backgroundOver drawable, ignoreEqualsTextChange, enterKeyFocusTraversal, focusField(), isEmpty(), clearText(), setCursorAtTextEnd(), or the I-beam cursor hover effect. These are real functional gaps.

### VisTextButton.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisTextButton.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `init`, inherited, property renames
- **Notes**: 6 common. VisTextButtonStyle, focus border, FocusManager integration present.

### VisTree.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisTree.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. FocusManager listener, ascendant check present.

### VisWindow.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisWindow.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited
- **Notes**: 19 common. Close button, fade in/out, center on add, keep within parent all present.

### VisValidatableTextField.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/VisValidatableTextField.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `init`, `isInputValid/isRestoreLastValid/getText/setText` -- inherited or property renames
- **Notes**: 11 common. Validator management, beforeChangeEventFired override, restoreLastValid with FocusListener, validation-enabled flag present.

### HighlightTextArea.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/HighlightTextArea.java`
- **Prior status**: pass
- **New status**: MAJOR_ISSUES
- **Missing methods**: Inherits VisTextArea's gap via ScrollableTextArea -- all VisTextField-specific features (readOnly, cursorPercentHeight, backgroundOver, focusField, etc.) are missing
- **Notes**: 8 common. Chunk rendering, highlight processing, softwrap support, Chunk inner class present. However, since this extends ScrollableTextArea -> VisTextArea -> TextArea (not VisTextField), all VisTextField additions are unavailable. Same gap as VisTextArea.

---

## WIDGET/COLOR PACKAGE

### BasicColorPicker.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/BasicColorPicker.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `dispose` -> `close`, property renames
- **Notes**: 20 common. Palette, vertical bar, hex field, color previews, alpha edit support all present.

### ColorPicker.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/ColorPicker.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames, `fadeOut` inherited
- **Notes**: 12 common. OK/Cancel/Restore buttons, extended picker integration present.

### ColorPickerAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/ColorPickerAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. Simple adapter with no-op defaults.

### ColorPickerListener.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/ColorPickerListener.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. All callback methods.

### ColorPickerStyle.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/ColorPickerStyle.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. pickerStyle field present.

### ColorPickerWidgetStyle.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/ColorPickerWidgetStyle.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. All drawable fields present (barSelector, cross, selectors, iconArrowRight).

### ExtendedColorPicker.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/ExtendedColorPicker.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `setAllowAlphaEdit` -> `allowAlphaEdit_=`
- **Notes**: 12 common. RGB and HSV channel widgets, extended table layout present.

---

## WIDGET/COLOR/INTERNAL PACKAGE

### AlphaChannelBar.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/AlphaChannelBar.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Grid image overlay present.

### AlphaImage.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/AlphaImage.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common.

### ChannelBar.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/ChannelBar.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 10 common. Mode constants, input handling, selector drawing present.

### ColorChannelWidget.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/ColorChannelWidget.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 7 common. Bar + input field integration, value change handling present.

### ColorInputField.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/ColorInputField.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 13 common. Value clamping, focus listener for restore, ColorFieldValidator present.

### ColorPickerText.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/ColorPickerText.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. All text entries (TITLE, OK, CANCEL, RESTORE, HEX) present.

### GridSubImage.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/GridSubImage.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Shader swap for drawing present.

### Palette.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/Palette.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 10 common. Hue/saturation/value input handling, selector drawing, picker hue tracking present.

### PickerCommons.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/PickerCommons.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `dispose` -> `close`
- **Notes**: 5 common. All shaders (palette, hsv, rgb, verticalChannel, grid), white pixmap/texture present.

### ShaderImage.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/ShaderImage.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Shader swap for custom draw present.

### VerticalChannelBar.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/color/internal/VerticalChannelBar.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 7 common. Vertical value input, selector drawing present.

---

## WIDGET/FILE PACKAGE

### FileChooserAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/FileChooserAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Simple adapter.

### FileChooserListener.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/FileChooserListener.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. selected/canceled/fileDeleted callbacks.

### FileChooserStyle.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/FileChooserStyle.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 common. All icon drawables present (21 fields).

### FileTypeFilter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/FileTypeFilter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 8 common. Rule inner class, extension filtering present.

### FileUtils.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/FileUtils.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 7 common. Comparators (name/size/date), sortFiles, readableFileSize, toFileHandleArray present.

### SingleFileChooserListener.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/SingleFileChooserListener.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common.

### StreamingFileChooserListener.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/StreamingFileChooserListener.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. Streaming iteration over selected files present.

---

## WIDGET/FILE/INTERNAL PACKAGE

### AbstractSuggestionPopup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/AbstractSuggestionPopup.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. MAX_SUGGESTIONS constant present.

### DirsSuggestionPopup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/DirsSuggestionPopup.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 7 common. Async directory listing, suggestion creation present.

### DriveCheckerService.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/DriveCheckerService.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 12 common. Singleton instance, thread pool, readable/writable drive checking present.

### FileChooserText.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/FileChooserText.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. All 51 text entries present as enum values.

### FileChooserWinService.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/FileChooserWinService.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 12 common. Shell folder reflection, name caching present.

### FileHandleMetadata.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/FileHandleMetadata.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `isDirectory/lastModified/length/name/readableFileSize` -> Scala `val` fields
- **Notes**: 2 common. Final case class with immutable fields.

### FileHistoryManager.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/FileHistoryManager.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 16 common. History forward/back, button management present.

### FileListAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/FileListAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. View mode support, grid group integration present.

### FilePopupMenu.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/FilePopupMenu.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 10 common. Context menu items (add/remove favorites, delete, new directory, refresh, show in explorer, sort by) present.

### FileSuggestionPopup.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/FileSuggestionPopup.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 5 common. File name suggestions present.

### IconStack.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/IconStack.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: inherited from WidgetGroup
- **Notes**: 5 common. Size computation, layout logic present.

### PreferencesIO.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/PreferencesIO.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `toFileHandleArray` -> `loadFileArray`; `FileArrayData/FileHandleData` inner classes may be simplified
- **Notes**: 10 common. Favorites, last directory, recent directories persistence present.

### ServiceThreadFactory.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/ServiceThreadFactory.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Daemon thread creation present.

### SortingPopupMenu.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/file/internal/SortingPopupMenu.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 3 common. Sort by name/size/date/ascending/descending menu items with icon indicators present.

---

## WIDGET/INTERNAL PACKAGE

### SplitPaneCursorManager.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/internal/SplitPaneCursorManager.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 9 common. Cursor state tracking, enter/exit/touchDown/touchUp handlers present.

---

## WIDGET/SPINNER PACKAGE

### AbstractSpinnerModel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/spinner/AbstractSpinnerModel.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames
- **Notes**: 6 common. Wrap, allowRebind, spinner reference, valueChanged notification present.

### ArraySpinnerModel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/spinner/ArraySpinnerModel.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 14 common. Array-based spin with increment/decrement/wrap, setCurrent, textChanged present.

### FloatSpinnerModel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/spinner/FloatSpinnerModel.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames, type references
- **Notes**: 7 common. BigDecimal precision, bounds validator, step handling present.

### IntSpinnerModel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/spinner/IntSpinnerModel.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames
- **Notes**: 7 common. Bounds validation, step, min/max present.

### SimpleFloatSpinnerModel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/spinner/SimpleFloatSpinnerModel.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames, type references
- **Notes**: 7 common. Precision-based formatting, BigDecimal arithmetic present.

### Spinner.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/spinner/Spinner.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: property renames, type references
- **Notes**: 18 common. Up/down buttons, button repeat task, model binding, text field event policy, focus listener, SpinnerStyle, ButtonInputListener present.

### SpinnerModel.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/spinner/SpinnerModel.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getText/isWrap/setWrap` -> properties
- **Notes**: 5 common. Interface fully equivalent.

---

## WIDGET/TABBEDPANE PACKAGE

### Tab.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/tabbedpane/Tab.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getContentTable/getPane/getTabTitle/isCloseableByUser/isDirty/isSavable/setDirty/setPane` -> properties
- **Notes**: 9 common. All lifecycle methods (onShow, onHide, save, dispose) present. Dirty flag management with pane update.

### TabbedPaneAdapter.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/tabbedpane/TabbedPaneAdapter.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 4 common. No-op defaults.

### TabbedPaneListener.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/tabbedpane/TabbedPaneListener.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 4 common. switchedTab, removedTab, removedAllTabs callbacks.

---

## WIDGET/TOAST PACKAGE

### MessageToast.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/toast/MessageToast.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 common. Label and link label table present.

### Toast.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/toast/Toast.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `getContentTable/getMainTable/getToastManager/setToastManager` -> properties
- **Notes**: 8 common. Close button, fade out on close, ToastStyle present.

### ToastTable.scala
- **Original**: `original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/widget/toast/ToastTable.java`
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 4 common. Toast reference tracking.

---

## MISSING FILES (Java files with no Scala counterpart)

1. **ArrayAdapter.java** -- Replaced by ArrayListAdapter.scala (uses ArrayList instead of LibGDX Array). This is intentional.
2. **ConfirmDialogListener.java** -- Missing entirely. Should be a trait/interface in the dialog package.
3. **InputDialogListener.java** -- Merged into Dialogs.scala as inner trait. Present.
4. **InputDialogAdapter.java** -- Merged into Dialogs.scala as inner class. Present.
5. **OptionDialogListener.java** -- Merged into Dialogs.scala as inner trait. Present.
6. **OptionDialogAdapter.java** -- Merged into Dialogs.scala as inner class. Present.
7. **package-info.java** (color/internal, file/internal) -- Not needed in Scala.

---

## SUMMARY OF FINDINGS

### MAJOR_ISSUES (6 files)
1. **Dialogs.scala** -- `showErrorDialog` silently drops details parameter. Missing: `showConfirmDialog`, `showDetailsDialog`, `ConfirmDialog` class, `DetailsDialog` class, `InputDialog` class (auto-focus, Enter submission, validation-driven OK disable), `OptionDialog` class (button text customization).
2. **TabbedPane.scala** -- `TabButtonTable` missing InputListener with middle-click close, hover visual feedback (setCloseButtonOnMouseMove, setDraggedUpImage, setDefaultUpImage), button setDisabled override that also disables close button.
3. **Menu.scala** -- Missing the entire menu open/close mechanism: no InputListener on openButton (touchDown to toggle, enter to hover-switch), no showMenu() to position and display popup, no remove() override to clear MenuBar.currentMenu. Clicking menu titles in MenuBar does nothing.
4. **VisTextArea.scala** -- Extends TextArea instead of VisTextField, losing all VisTextField-specific features: readOnly, cursorPercentHeight, backgroundOver, ignoreEqualsTextChange, enterKeyFocusTraversal, focusField(), isEmpty(), clearText(), setCursorAtTextEnd(), I-beam cursor hover.
5. **ScrollableTextArea.scala** -- Inherits VisTextArea's gap (extends VisTextArea -> TextArea, not VisTextField). Same missing features.
6. **HighlightTextArea.scala** -- Inherits VisTextArea's gap (extends ScrollableTextArea -> VisTextArea -> TextArea). Same missing features.

### MINOR_ISSUES (2 files)
1. **VisTextField.scala** -- Missing 5-arg `VisTextFieldStyle` constructor. Minor `focusField()` API difference.
2. **VisUI.scala** -- Missing `getSizesName` and `setSkipGdxVersionCheck`.

### NEEDS_DEEP_REVIEW (1 file)
1. **FileChooser.scala** -- Too large (~1800 lines original) for full body-level comparison within this audit pass.

### PASS (all remaining ~144 files)
All other files show clean compare output with "missing" items being either property renames (getX->x), inherited methods from base classes, or type references. No shortcut markers found in any file.
