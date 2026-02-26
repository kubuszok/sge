# Scene2D UI Widget Migration Plan — COMPLETE

Created: 2026-02-24 | Completed: 2026-02-26

All 16 scene2d UI widgets have been ported and compile with 0 errors, 0 warnings.

## Batch A — Base widgets (~2600 lines)

| File | Lines | Extends | Status |
|------|-------|---------|--------|
| `ProgressBar.java` | 417 | `Widget` | done |
| `List.java` | 465 | `Widget` | done |
| `Container.java` | 779 | `WidgetGroup` | done |
| `Window.java` | 367 | `Table` | done |
| `Tooltip.java` | 142 | `InputListener` | done |

## Batch B — Layout groups (~2400 lines)

Depends on: nothing new (all extend `WidgetGroup`, already converted)

| File | Lines | Extends | Status |
|------|-------|---------|--------|
| `HorizontalGroup.java` | 580 | `WidgetGroup` | done |
| `VerticalGroup.java` | 563 | `WidgetGroup` | done |
| `SplitPane.java` | 423 | `WidgetGroup` | done |

## Batch C — Complex widgets (~3275 lines)

Depends on: Batch A (ScrollPane uses Container internally)

| File | Lines | Extends | Status |
|------|-------|---------|--------|
| `ScrollPane.java` | 1103 | `WidgetGroup` | done |
| `TextField.java` | 1263 | `Widget` | done |
| `Tree.java` | 909 | `WidgetGroup` | done |

## Batch D — Derived widgets (~1790 lines)

Depends on: Batch A + C

| File | Lines | Depends on | Status |
|------|-------|------------|--------|
| `Slider.java` | 275 | `ProgressBar` (A) | done |
| `Dialog.java` | 282 | `Window` (A) | done |
| `TextArea.java` | 483 | `TextField` (C) | done |
| `SelectBox.java` | 649 | `List` (A) + `ScrollPane` (C) | done |
| `TextTooltip.java` | 100 | `Tooltip` (A) | done |

## Additional work completed

- `TooltipManager.java` — fully implemented (was previously a stub)
- Skin constructors uncommented across all 16 files
- All files compile with 0 errors, 0 warnings

## After Scene2D

The entire scene2d UI module is now complete.
Remaining work after that:
- `Pools.java` — deferred (deprecated, heavy scene2d deps)
- `ModelLoader.java` — deferred (depends on g3d module)
- g3d module (~130 files) — not yet started, largest remaining chunk
- Quality fixes on `ai_converted` files (return, null, Java syntax — see quality-issues.md)
