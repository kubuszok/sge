/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: MessageToast,addLinkLabel,label,linkLabelTable
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/toast/MessageToast.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package toast

import sge.scenes.scene2d.Actor
import sge.utils.Nullable

/** Toast with provided user message and arbitrary amount of [[LinkLabel]] below it acting as action buttons.
  * @author
  *   Kotcrab
  * @see
  *   [[ToastTable]]
  * @since 1.1.0
  */
class MessageToast(message: String)(using Sge) extends ToastTable() {

  private val linkLabelTable: VisTable = new VisTable()

  add(Nullable[Actor](new VisLabel(message))).left().row()
  add(Nullable[Actor](linkLabelTable)).right()

  /** Adds new link label below toast message.
    * @param text
    *   link label text
    * @param labelListener
    *   will be called upon label click. Note that toast won't be closed automatically so [[Toast.fadeOut]] must be called
    */
  def addLinkLabel(text: String, labelListener: LinkLabel.LinkLabelListener): Unit = {
    val label = new LinkLabel(text)
    label.listener = Nullable(labelListener)
    linkLabelTable.add(Nullable[Actor](label)).spaceRight(12)
  }
}
