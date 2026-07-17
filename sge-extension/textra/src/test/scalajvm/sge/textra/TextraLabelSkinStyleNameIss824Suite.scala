/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Guard test for ISS-824 (textra wave 2026-07-17-E), against TextraTypist
 * upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * TextraLabel.java / TypingLabel.java):
 *
 *   TextraLabel(String, Skin, String styleName)  — resolves the *named* style
 *   TypingLabel(String, Skin, String styleName)  — likewise
 *
 * The wave-D port (ISS-716) delegates these to
 * `this(text, skin.get(styleName, classOf[Styles.LabelStyle]))`
 * (TextraLabel.scala:169-170, TypingLabel.scala:178-179). No existing suite
 * exercises the styleName argument, so a mutant ctor that ignored styleName
 * and always resolved the default style survived the whole module suite
 * (auditor M finding 2). This fixture registers TWO distinct LabelStyles under
 * different names and asserts the label built with the NON-default name picks
 * the named style's font and base color — killing the "ignore styleName"
 * mutant while passing against the correct delegation.
 */
package sge
package textra

import sge.graphics.Color
import lowlevel.Nullable

class TextraLabelSkinStyleNameIss824Suite extends munit.FunSuite {

  private def twoStyleSkin()(using Sge): (FWSkin, Font, Color) = {
    val defaultFont = new Font()
    defaultFont.name = "default-font"
    val specialFont = new Font()
    specialFont.name = "special-font"

    val defaultColor = new Color(1f, 1f, 1f, 1f)
    val specialColor = new Color(0.25f, 0.5f, 0.75f, 1f)

    val defaultStyle = new Styles.LabelStyle(defaultFont, Nullable(defaultColor))
    val specialStyle = new Styles.LabelStyle(specialFont, Nullable(specialColor))

    val skin = new FWSkin()
    skin.add("default", defaultStyle, classOf[Styles.LabelStyle])
    skin.add("special", specialStyle, classOf[Styles.LabelStyle])

    (skin, specialFont, specialColor)
  }

  test("ISS-824: TextraLabel(text, skin, styleName) resolves the NAMED style's font and color, not the default") {
    given Sge                             = HeadlessTextraSge.make()
    val (skin, specialFont, specialColor) = twoStyleSkin()

    val label = new TextraLabel("x", skin, "special")

    assert(
      label.getFont eq specialFont,
      "ISS-824: the label must use the font of the style named \"special\" (skin.get(styleName, ...)); a ctor ignoring styleName would pick the default style's font"
    )
    assertEquals(
      label.baseLayout.getBaseColor,
      specialColor.toFloatBits(),
      "ISS-824: the label's base color must come from the named style's fontColor"
    )
  }

  test("ISS-824: TypingLabel(text, skin, styleName) resolves the NAMED style's font, not the default") {
    given Sge                  = HeadlessTextraSge.make()
    val (skin, specialFont, _) = twoStyleSkin()

    val label = new TypingLabel("x", skin, "special")

    assert(
      label.getFont eq specialFont,
      "ISS-824: the TypingLabel must use the font of the style named \"special\"; a ctor ignoring styleName would pick the default style's font"
    )
  }
}
