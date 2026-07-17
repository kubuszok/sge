/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-798:
 *
 *   VisImageTextButton.getText (and toString) called `_label.text.toString`.
 *   Label.text is a DynamicArray[Char], whose toString is a bracketed element
 *   list ("[P, l, a, y]"), not the plain string. Upstream returns
 *   label.getText().toString() == "Play". The fix builds the String from the
 *   char array: `new String(_label.text.toArray)` (matching MenuItem.getText).
 */
package sge
package visui
package widget

class VisImageTextButtonGetTextIss798RedSuite extends munit.FunSuite {

  override def afterEach(context: AfterEach): Unit = VisUI.dispose()

  test("ISS-798: VisImageTextButton.getText returns the plain label string, not the bracketed char list") {
    given Sge = VisUITestFixture.headlessSge()
    VisUI.load()

    val button = new VisImageTextButton("Play", "default")

    assertEquals(
      button.getText,
      "Play",
      "getText must build the String from Label.text's chars (new String(_label.text.toArray)); DynamicArray[Char].toString would render \"[P, l, a, y]\""
    )
    assert(
      button.toString.endsWith(": Play"),
      s"toString must end with the plain label text, got: ${button.toString}"
    )
  }
}
