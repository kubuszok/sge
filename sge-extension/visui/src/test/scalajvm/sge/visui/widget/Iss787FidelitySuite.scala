/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Fidelity tests for ISS-787:
 *
 *  (1) MenuItem.setShortcut(int keycode) must route through Keys.toString
 *      (raw key name), not OsUtils.getShortcutFor — see MenuItem.java:306-308.
 *      For a modifier keycode the difference is observable: the single-keycode
 *      overload renders the raw key name (e.g. CONTROL_LEFT -> "L-Ctrl"),
 *      whereas OsUtils.getShortcutFor collapses modifiers to "Ctrl"/"Alt"/
 *      "Shift" (or the Mac glyphs) — that collapsing belongs only to the
 *      varargs overload (setShortcut(int...) -> MenuItem.java:336-340).
 *
 *  (3) ButtonBar.ButtonType.toString must return the localized text, not the
 *      raw key — see ButtonBar.java:184-186 (toString returns getText()).
 *
 * Both assertions are OS-independent: Keys.toString(CONTROL_LEFT) is "L-Ctrl"
 * on every platform, and the ButtonBar bundle "ok" entry is "Ok" everywhere.
 */
package sge
package visui
package widget

import sge.Input.Keys

class Iss787FidelitySuite extends munit.FunSuite {

  override def afterEach(context: AfterEach): Unit = VisUI.dispose()

  test("ISS-787(1): MenuItem.setShortcut(keycode) renders the raw key name via Keys.toString") {
    given Sge = VisUITestFixture.headlessSge()
    VisUI.load()

    val item = new MenuItem("Copy")
    item.setShortcut(Keys.CONTROL_LEFT)

    assertEquals(
      item.getShortcut,
      "L-Ctrl",
      "single-keycode setShortcut must use Keys.toString (MenuItem.java:306-308); routing it through OsUtils.getShortcutFor would render \"Ctrl\"/\"⌘\" instead"
    )
  }

  test("ISS-787(3): ButtonBar.ButtonType.toString returns the localized text, not the raw key") {
    given Sge = VisUITestFixture.headlessSge()
    VisUI.load()

    assertEquals(
      ButtonBar.ButtonType.OK.toString,
      "Ok",
      "ButtonType.toString must return getText() (ButtonBar.java:184-186); the raw key would be \"ok\" (ButtonBar.properties: ok=Ok)"
    )
    assertEquals(
      ButtonBar.ButtonType.YES.toString,
      "Yes",
      "ButtonType.toString must return getText() (ButtonBar.java:184-186); the raw key would be \"yes\" (ButtonBar.properties: yes=Yes)"
    )
  }
}
