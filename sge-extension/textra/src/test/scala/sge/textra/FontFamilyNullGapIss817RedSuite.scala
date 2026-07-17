/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-817 (textra wave 2026-07-17-E), against TextraTypist
 * upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java):
 *
 *   Font.FontFamily(Font[] fonts)               — Font.java:530-532
 *   Font.FontFamily(String[] aliases, Font[])   — Font.java:567-569
 *
 * Both upstream ctors DELEGATE to their offset/length siblings
 * (`this(fonts, 0, fonts.length)` / `this(aliases, fonts, 0, min(len,len))`),
 * whose loop is `for (int i = offset, a = 0; ...; i++, a++)` with a
 * `if (fonts[i] == null) continue;` (Font.java:548, :587). Because Java's
 * `continue` runs the loop-increment step, the alias index `a` advances on
 * EVERY iteration — including null-gap entries. So a null at input index k
 * leaves connected[k & 15] unassigned but still consumes index k: the font
 * that follows the gap lands at its input index, and the index alias "k+1"
 * points at it.
 *
 * The SGE port re-rolls these two 2-arg ctors by hand (Font.scala:4129-4144,
 * :4146-4163) instead of delegating, and increments `a` INSIDE the
 * `if (fonts(idx) != null)` block, so `a` advances only on non-null entries.
 * A font after a null gap is therefore compacted one slot to the left, and
 * the upstream index alignment is lost. (The offset/length ctors,
 * Font.scala:4166 / :4184, DO advance every iteration and are correct — the
 * divergence is only in the two delegating 2-arg ctors.)
 *
 * With fonts = [fontA, <null>, fontB]:
 *   upstream: connected[0]=fontA, connected[2]=fontB, index alias "2" -> fontB
 *   port:     connected[0]=fontA, connected[1]=fontB, index alias "2" absent
 * so upstream `get("2") == fontB` and `connected(2) == fontB`, while the port
 * compacts fontB into slot 1 and `get("2")` falls back to the default-0 slot
 * (fontA). These assertions encode the upstream (Java) contract and fail on
 * the port.
 *
 * Written by the reproducer agent; MUST NOT be modified by the fixer — it
 * encodes upstream TextraTypist FontFamily indexing, not the port's.
 */
package sge
package textra

class FontFamilyNullGapIss817RedSuite extends munit.FunSuite {

  private def named(n: String): Font = {
    val f = new Font()
    f.name = n
    f
  }

  test(
    "ISS-817: FontFamily(Font[]) advances the alias index across a null gap (upstream Font.java:548 `continue` still runs a++)"
  ) {
    val fontA = named("A")
    val fontB = named("B")

    // A genuine null gap at input index 1 (new Array initialises elements to null;
    // this avoids writing a `null` literal).
    val fonts = new Array[Font](3)
    fonts(0) = fontA
    fonts(2) = fontB

    val family = new Font.FontFamily(fonts)

    // Upstream keeps fontB at input index 2 (a advanced past the null).
    assert(
      family.connected(2) eq fontB,
      "ISS-817: upstream leaves the post-gap font at slot 2 (a advances every iteration); the port compacts it into slot 1"
    )
    assert(
      family.get("2") eq fontB,
      "ISS-817: upstream registers index alias \"2\" -> fontB; the port never registers \"2\", so get(\"2\") falls back to the default-0 slot (fontA)"
    )
    // Sanity: fontA is unaffected and still at slot 0.
    assert(family.connected(0) eq fontA, "sanity: the pre-gap font stays at slot 0")
  }

  test(
    "ISS-817: FontFamily(String[], Font[]) advances the alias index across a null gap (upstream Font.java:587 `continue` still runs a++)"
  ) {
    val fontA = named("A")
    val fontB = named("B")

    val aliases = Array("a0", "a1", "a2")
    val fonts   = new Array[Font](3)
    fonts(0) = fontA
    fonts(2) = fontB

    val family = new Font.FontFamily(aliases, fonts)

    assert(
      family.connected(2) eq fontB,
      "ISS-817: upstream leaves the post-gap font at slot 2; the port compacts it into slot 1"
    )
    assert(
      family.get("2") eq fontB,
      "ISS-817: upstream registers index alias \"2\" -> fontB; the port never registers \"2\", so get(\"2\") falls back to the default-0 slot (fontA)"
    )
    assert(family.connected(0) eq fontA, "sanity: the pre-gap font stays at slot 0")
  }
}
