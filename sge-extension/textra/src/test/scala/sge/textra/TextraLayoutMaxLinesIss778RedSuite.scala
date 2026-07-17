/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-778 (review-release): Layout.add returns `this` at the
 * maxLines cap where the original returns `null`.
 *
 * Original semantics (TextraTypist, upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4;
 * original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Layout.java:139-160):
 *
 *   public Layout add(long glyph, ...) {
 *       if (!atLimit) {
 *           if ((glyph & 0xFFFFL) == 10L) {          // a newline glyph
 *               if (lines.size >= maxLines) {
 *                   atLimit = true;
 *                   return null;                     // <-- Layout.java:144
 *               }
 *               ...
 *           } ...
 *       }
 *       return this;
 *   }
 *
 * Upstream signals "this line could not be added because the line cap was
 * reached" by returning `null` from `add`. Callers that append glyph-by-glyph
 * (e.g. Font.markup / regenerateLayout) rely on that `null` to stop feeding
 * the Layout once it is full.
 *
 * The port (sge-extension/textra/src/main/scala/sge/textra/Layout.scala:95-116
 * at the red commit) instead does `break(this)` at line 100, so `add` returns
 * the Layout itself at the cap — the external caller contract silently
 * changes from "null at cap" to "always non-null".
 *
 * This test is written by the reproducer agent and MUST NOT be modified by the
 * fixer: it encodes the original TextraTypist semantics (null at the cap), not
 * the port's `this`.
 */
package sge
package textra

class TextraLayoutMaxLinesIss778RedSuite extends munit.FunSuite {

  test(
    "ISS-778: Layout.add returns null when a newline is added at the maxLines cap (Layout.java:144); the port returns `this`"
  ) {
    val layout = new Layout()
    // A fresh Layout already holds one Line (Layout.scala:31); cap the layout
    // at that single line so the next newline glyph hits the maxLines branch.
    layout.maxLines = 1

    // 0xFFFF of this glyph is 10 ('\n') — the newline path Layout.java:141 takes.
    val newlineGlyph = 10L
    val result       = layout.add(newlineGlyph)

    // Control: reaching the cap sets atLimit (true both before and after the
    // fix), proving the newline branch and the `lines.size >= maxLines` guard
    // were actually exercised.
    assert(layout.atLimit, "control: adding a newline at the cap must set atLimit")

    // The behaviour under test: upstream returns null at the cap (Layout.java:144).
    // The port returns `this` (Layout.scala:100 `break(this)`), so this fails RED.
    assert(
      result == null,
      "ISS-778: Layout.add must return null at the maxLines cap (Layout.java:144); the port returns `this`"
    )
  }
}
