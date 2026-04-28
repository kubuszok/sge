/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: VisScrollPane,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisScrollPane.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.ScrollPane
import sge.utils.Nullable

/** Compatible with [[ScrollPane]]. Does not provide additional features.
  * @author
  *   Kotcrab
  * @see
  *   [[ScrollPane]]
  */
class VisScrollPane(actor: Nullable[Actor], scrollPaneStyle: ScrollPane.ScrollPaneStyle)(using Sge) extends ScrollPane(actor, scrollPaneStyle) {

  def this(widget: Nullable[Actor], styleName: String)(using Sge) =
    this(widget, VisUI.getSkin.get[ScrollPane.ScrollPaneStyle](styleName))

  def this(widget: Nullable[Actor])(using Sge) =
    this(widget, VisUI.getSkin.get[ScrollPane.ScrollPaneStyle]("list"))
}
