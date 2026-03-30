/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.graphics.Color
import sge.graphics.Cursor
import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.scenes.scene2d.{ Actor, InputEvent }
import sge.scenes.scene2d.ui.Label
import sge.scenes.scene2d.utils.{ ClickListener, Drawable }
import sge.utils.Nullable
import sge.visui.VisUI
import sge.visui.util.CursorManager

/** Simple LinkLabel allows to create label with clickable link and underline on mouse over. Link can have custom text. By default clicking link will open it in default browser, this can be changed by
  * settings label listener.
  * @author
  *   Kotcrab
  * @since 0.7.2
  */
class LinkLabel(text: CharSequence, private var _url: CharSequence, linkStyle: LinkLabel.LinkLabelStyle)(using Sge) extends VisLabel(text, linkStyle) {

  private val _style:        LinkLabel.LinkLabelStyle = linkStyle
  private val clickListener: ClickListener            = new ClickListener(sge.Input.Buttons.LEFT) {
    override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
      super.clicked(event, x, y)
      if (_listener.isEmpty) {
        Sge().net.openURI(_url.toString)
      } else {
        _listener.get.clicked(_url.toString)
      }
    }

    override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit = {
      super.enter(event, x, y, pointer, fromActor)
      Sge().graphics.setSystemCursor(Cursor.SystemCursor.Hand)
    }

    override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
      super.exit(event, x, y, pointer, toActor)
      CursorManager.restoreDefaultCursor()
    }
  }

  private var _listener: Nullable[LinkLabel.LinkLabelListener] = Nullable.empty

  addListener(clickListener)

  def this(url: CharSequence)(using Sge) = this(url, url, VisUI.getSkin.get[LinkLabel.LinkLabelStyle])

  def this(text: CharSequence, url: CharSequence)(using Sge) = this(text, url, VisUI.getSkin.get[LinkLabel.LinkLabelStyle])

  def this(text: CharSequence, url: CharSequence, styleName: String)(using Sge) = this(text, url, VisUI.getSkin.get[LinkLabel.LinkLabelStyle](styleName))

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    val underline = _style.underline
    if (underline.isDefined && clickListener.over) {
      val c = new Color(color)
      c.a *= parentAlpha
      if (_style.fontColor.isDefined) c.mul(_style.fontColor.get)
      batch.setColor(c.r, c.g, c.b, c.a)
      underline.get.draw(batch, x, y, width, 1)
    }
  }

  def url:                      CharSequence = _url
  def url_=(url: CharSequence): Unit         = _url = url

  def listener:                                                    Nullable[LinkLabel.LinkLabelListener] = _listener
  def listener_=(listener: Nullable[LinkLabel.LinkLabelListener]): Unit                                  = _listener = listener
}

object LinkLabel {

  trait LinkLabelListener {
    def clicked(url: String): Unit
  }

  class LinkLabelStyle extends Label.LabelStyle {

    /** Optional */
    var underline: Nullable[Drawable] = Nullable.empty

    def this(font: BitmapFont, fontColor: Nullable[Color], underline: Nullable[Drawable]) = {
      this()
      this.font = font
      this.fontColor = fontColor
      this.underline = underline
    }

    def this(style: LinkLabelStyle) = {
      this()
      this.font = style.font
      this.fontColor = style.fontColor
      this.background = style.background
      this.underline = style.underline
    }
  }
}
