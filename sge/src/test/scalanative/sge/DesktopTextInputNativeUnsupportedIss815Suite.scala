/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import munit.FunSuite
import sge.utils.SgeError

/** ISS-815 clause 3: green proof of the getTextInput platform split (Scala Native).
  *
  * Before the split, `DefaultDesktopInput.getTextInput` used java.lang reflection (Class.forName / Method.invoke / Field.get) to reach Swing from shared scaladesktop code. Those symbols are
  * unreachable on Scala Native; the only reason the native test binary linked at all was dead-code elimination — no native test referenced the getTextInput path. This test DELIBERATELY references
  * that path (the Native [[DesktopTextInputPlatform]] half), so the native test link must resolve it with NO reflection symbols. That it links AND asserts [[SgeError.Unsupported]] is the proof the
  * reflection was removed, not merely hidden by DCE. This suite could not have existed before the fix (the native link would have failed on the reflection symbols).
  */
class DesktopTextInputNativeUnsupportedIss815Suite extends FunSuite {

  private val recordingListener = new Input.TextInputListener {
    override def input(text: String): Unit = ()
    override def canceled():          Unit = ()
  }

  test("ISS-815 clause 3: getTextInput signals SgeError.Unsupported on Scala Native (no Swing/AWT, no reflection)") {
    val err = intercept[SgeError.Unsupported] {
      DesktopTextInputPlatform.getTextInput(
        (r: Runnable) => r.run(),
        recordingListener,
        "title",
        "text",
        "hint"
      )
    }
    assert(
      err.getMessage.contains("getTextInput"),
      s"expected an Unsupported error naming the getTextInput capability; got: ${err.getMessage}"
    )
  }
}
