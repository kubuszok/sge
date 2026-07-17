/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-825 (textra wave 2026-07-17-E), against TextraTypist
 * upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * TextraLabel.java):
 *
 *   TextraLabel(String text, Styles.LabelStyle style, Font replacementFont)
 *     — TextraLabel.java:243-263
 *
 * Upstream's ctor ends with a size-establishing tail (TextraLabel.java:251-252):
 *
 *     font.markup(storedText, layout);
 *     invalidateHierarchy();
 *     setSize(layout.getWidth(), layout.getHeight());
 *
 * so immediately after construction the label's own width/height equal the
 * laid-out text's dimensions. Every style/Skin ctor chain funnels through this
 * ctor (the Skin ctors delegate `this(text, skin.get(styleName, ...), replacementFont)`),
 * so they all inherit the tail.
 *
 * The SGE port (TextraLabel.scala:109-118) stops at `font.markup(storedText,
 * baseLayout)` and omits BOTH `invalidateHierarchy()` and `setSize(...)`. The
 * label therefore keeps the Actor default size (0x0) even though its layout has
 * real dimensions.
 *
 * `getWidth`/`getHeight` return the raw Actor width/height (TextraLabel.scala:205-206);
 * `baseLayout.getWidth`/`getHeight` (Layout.scala:132/141) return the laid-out
 * text dimensions. Upstream's tail makes these equal; the port leaves the label
 * at 0 while the layout is non-empty. The height axis is pinned because markup
 * sets each line's height from `font.cellHeight` for every appended glyph
 * (Font.scala:1056/1104), so a non-empty label always has a positive layout
 * height.
 *
 * Written by the reproducer agent; MUST NOT be modified by the fixer — it
 * encodes upstream TextraTypist ctor semantics, not the port's.
 */
package sge
package textra

import lowlevel.Nullable

class TextraLabelReplacementFontSizeIss825RedSuite extends munit.FunSuite {

  test(
    "ISS-825: (text, style, replacementFont) ctor sets the label size to the layout size (upstream TextraLabel.java:251-252 invalidateHierarchy + setSize)"
  ) {
    given Sge = HeadlessTextraSge.make()

    // A replacement font with a clearly non-zero line height, so the laid-out
    // text has a positive height regardless of glyph mapping.
    val replacementFont = new Font()
    replacementFont.cellHeight = 20f
    replacementFont.originalCellHeight = 20f

    val style = new Styles.LabelStyle(new Font(), Nullable.empty)

    val label = new TextraLabel("Hello", style, replacementFont)

    // Precondition: the layout is non-empty, so upstream's setSize(layout.getWidth,
    // layout.getHeight) establishes a positive height.
    assert(
      label.baseLayout.getHeight > 0f,
      s"sanity: a non-empty label lays out to a positive height (was ${label.baseLayout.getHeight})"
    )

    assertEquals(
      label.getHeight,
      label.baseLayout.getHeight,
      "ISS-825: upstream setSize(layout.getWidth(), layout.getHeight()) (TextraLabel.java:252) makes the label height equal the layout height; the port omits the tail so the label keeps its 0 default"
    )
    assertEquals(
      label.getWidth,
      label.baseLayout.getWidth,
      "ISS-825: upstream setSize also establishes the label width from the layout; the port leaves it at the 0 default"
    )
  }
}
