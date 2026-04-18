/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingListBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: List -> TextraListBox (base class), Cullable -> setCullingArea method,
 *     Skin -> sge.scenes.scene2d.ui.Skin
 *   Convention: TypingListBox extends TextraListBox. The main reason to use this instead
 *     of a TextraListBox containing TypingLabels is so that you can use the extra APIs
 *     available for TypingLabel, instead of having to cast a TextraLabel to TypingLabel
 *     from TextraListBox.
 *   Idiom: No additional logic beyond constructors.
 */
package sge
package textra

import sge.scenes.scene2d.ui.Skin

/** A TypingListBox (based on {@link com.badlogic.gdx.scenes.scene2d.ui.List}) displays {@link TypingLabel}s and highlights the currently selected item. <br> A ChangeEvent is fired when the list
  * selection changes. <br> The preferred size of the list is determined by the text bounds of the items and the size of the {@link Styles.ListStyle#selection}. <br> The main reason to use this
  * instead of a {@link TextraListBox} containing {@link TypingLabel}s is so that you can use the extra APIs available for TypingLabel, instead of having to cast a TextraLabel to TypingLabel from
  * TextraListBox.
  *
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class TypingListBox(style: Styles.ListStyle) extends TextraListBox(style) {

  def this(skin: Skin) = this(skin.get(classOf[Styles.ListStyle]))

  def this(skin: Skin, styleName: String) = this(skin.get(styleName, classOf[Styles.ListStyle]))
}
