/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 287
 * Covenant-baseline-methods: Builder,DEFAULT_APPEAR_DELAY_TIME,DEFAULT_FADE_TIME,DisplayTask,MOUSE_MOVED_FADEOUT,Tooltip,TooltipInputListener,TooltipStyle,_appearDelayTime,_content,_contentCell,_displayTask,_fadeTime,_listener,_mouseMoveFadeOut,_style,_target,_width,appearDelayTime,appearDelayTime_,attach,background,build,content,contentCell,content_,detach,doFadeIn,doFadeOut,enter,exit,fadeTime,fadeTime_,i,listeners,mouseMoveFadeOut,mouseMoveFadeOut_,mouseMoved,removeTooltip,run,setPosition,setText,style,target,target_,this,touchDown,width
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/Tooltip.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.math.{ Interpolation, Vector2 }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.ui.Cell
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable, Seconds, Timer }
import sge.visui.VisUI
import sge.visui.util.ActorUtils

/** Tooltips are widgets that appear below other widget on mouse pointer hover. Each actor can have only one tooltip.
  * @author
  *   Kotcrab
  * @since 0.5.0
  */
class Tooltip private (initStyle: Nullable[Tooltip.TooltipStyle], initTarget: Nullable[Actor], initContent: Nullable[Actor], initWidth: Float)(using Sge) extends VisTable(true) {

  private var _target:      Nullable[Actor] = Nullable.empty
  private var _content:     Nullable[Actor] = Nullable.empty
  private var _contentCell: Cell[Actor]     = scala.compiletime.uninitialized

  private var _mouseMoveFadeOut: Boolean                      = Tooltip.MOUSE_MOVED_FADEOUT
  private var _listener:         Tooltip.TooltipInputListener = scala.compiletime.uninitialized
  private var _displayTask:      Tooltip.DisplayTask          = scala.compiletime.uninitialized

  private var _fadeTime:        Seconds = Tooltip.DEFAULT_FADE_TIME
  private var _appearDelayTime: Seconds = Tooltip.DEFAULT_APPEAR_DELAY_TIME

  {
    val style = if (initStyle.isDefined) initStyle.get else VisUI.getSkin.get[Tooltip.TooltipStyle]
    _target = initTarget
    _content = initContent
    _listener = new Tooltip.TooltipInputListener(this)
    _displayTask = new Tooltip.DisplayTask(this)

    setBackground(Nullable(style.background))

    _contentCell = add(initContent).padLeft(3).padRight(3).padBottom(2).asInstanceOf[Cell[Actor]]
    pack()

    if (initWidth != -1) {
      _contentCell.width(initWidth)
      pack()
    }

    if (_target.isDefined) attach()

    addListener(
      new InputListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
          toFront()
          true
        }

        override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
          if (pointer == -1) {
            clearActions()
            addAction(Actions.sequence(Actions.fadeIn(_fadeTime, Interpolation.fade)))
          }

        override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
          if (pointer == -1) doFadeOut()
      }
    )
  }

  def this()(using Sge) = this(Nullable.empty, Nullable.empty, Nullable.empty, -1)

  def this(styleName: String)(using Sge) =
    this(Nullable(VisUI.getSkin.get[Tooltip.TooltipStyle](styleName)), Nullable.empty, Nullable.empty, -1)

  def this(style: Tooltip.TooltipStyle)(using Sge) =
    this(Nullable(style), Nullable.empty, Nullable.empty, -1)

  /** Remove any attached tooltip from target actor */
  def attach(): Unit = {
    if (_target.isEmpty) return
    val listeners = _target.get.listeners
    var i         = 0
    while (i < listeners.size) {
      if (listeners(i).isInstanceOf[Tooltip.TooltipInputListener]) {
        throw new IllegalStateException("More than one tooltip cannot be added to the same target!")
      }
      i += 1
    }
    _target.get.addListener(_listener)
  }

  def detach(): Unit = {
    if (_target.isEmpty) return
    _target.get.removeListener(_listener)
  }

  /** Sets new target for this tooltip, tooltip will be automatically detached from old target. */
  def target_=(newTarget: Actor): Unit = {
    detach()
    _target = Nullable(newTarget)
    attach()
  }

  def target: Nullable[Actor] = _target

  private def doFadeOut(): Unit = {
    clearActions()
    addAction(Actions.sequence(Actions.fadeOut(_fadeTime, Interpolation.fade), Actions.removeActor()))
  }

  private def doFadeIn(): VisTable = {
    clearActions()
    color.set(1, 1, 1, 0)
    addAction(Actions.sequence(Actions.fadeIn(_fadeTime, Interpolation.fade)))
    this
  }

  def content: Nullable[Actor] = _content

  def content_=(content: Actor): Unit = {
    _content = Nullable(content)
    _contentCell.setActor(content)
    pack()
  }

  def contentCell: Cell[Actor] = _contentCell

  /** Changes text tooltip to specified text. If tooltip content is not instance of VisLabel then previous tooltip content will be replaced by VisLabel instance.
    */
  def setText(text: String): Unit = {
    if (_content.isDefined && _content.get.isInstanceOf[VisLabel]) {
      _content.get.asInstanceOf[VisLabel].setText(Nullable(text))
    } else {
      content = new VisLabel(text)
    }
    pack()
  }

  override def setPosition(x: Float, y: Float): Unit =
    super.setPosition(x.toInt.toFloat, y.toInt.toFloat)

  def appearDelayTime:                   Seconds = _appearDelayTime
  def appearDelayTime_=(value: Seconds): Unit    = _appearDelayTime = value

  def fadeTime:                   Seconds = _fadeTime
  def fadeTime_=(value: Seconds): Unit    = _fadeTime = value

  def mouseMoveFadeOut:                   Boolean = _mouseMoveFadeOut
  def mouseMoveFadeOut_=(value: Boolean): Unit    = _mouseMoveFadeOut = value
}

object Tooltip {
  var DEFAULT_FADE_TIME:         Seconds = Seconds(0.3f)
  var DEFAULT_APPEAR_DELAY_TIME: Seconds = Seconds(0.6f)

  /** Controls whether to fade out tooltip when mouse was moved. Changing this will not affect already existing tooltips.
    */
  var MOUSE_MOVED_FADEOUT: Boolean = false

  /** Remove any attached tooltip from target actor */
  def removeTooltip(target: Actor): Unit = {
    val listeners = target.listeners
    var i         = 0
    while (i < listeners.size) {
      listeners(i) match {
        case _: TooltipInputListener => target.removeListener(listeners(i))
        case _ =>
      }
      i += 1
    }
  }

  class TooltipStyle {
    var background: Drawable = scala.compiletime.uninitialized

    def this(style: TooltipStyle) = {
      this()
      this.background = style.background
    }

    def this(background: Drawable) = {
      this()
      this.background = background
    }
  }

  private class DisplayTask(tooltip: Tooltip)(using Sge) extends Timer.Task {
    override def run(): Unit = {
      if (tooltip._target.isEmpty || tooltip._target.get.stage.isEmpty) return
      tooltip._target.get.stage.get.addActor(tooltip.doFadeIn())
      tooltip.stage.foreach(s => ActorUtils.keepWithinStage(s, tooltip))
    }
  }

  private class TooltipInputListener(tooltip: Tooltip)(using Sge) extends InputListener {
    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
      tooltip._displayTask.cancel()
      tooltip.toFront()
      tooltip.doFadeOut()
      true
    }

    override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
      if (pointer == -1 && tooltip._target.isDefined) {
        val targetPos = tooltip._target.get.localToStageCoordinates(new Vector2())

        tooltip.x = targetPos.x + (tooltip._target.get.width - tooltip.width) / 2

        val tooltipY    = targetPos.y - tooltip.height - 6
        val stageHeight = tooltip._target.get.stage.get.height

        if (stageHeight - tooltipY > stageHeight) {
          tooltip.y = targetPos.y + tooltip._target.get.height + 6
        } else {
          tooltip.y = tooltipY
        }

        tooltip._displayTask.cancel()
        Timer.schedule(tooltip._displayTask, tooltip._appearDelayTime)
      }

    override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
      if (pointer == -1) {
        tooltip._displayTask.cancel()
        tooltip.doFadeOut()
      }

    override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
      if (tooltip._mouseMoveFadeOut && tooltip.visible) tooltip.doFadeOut()
      false
    }
  }

  class Builder(val content: Actor)(using Sge) {
    private var _target: Nullable[Actor]        = Nullable.empty
    private var _style:  Nullable[TooltipStyle] = Nullable.empty
    private var _width:  Float                  = -1

    def this(text: String)(using Sge) = this({
      val label = new VisLabel(text)
      label.setAlignment(Align.center)
      label
    })

    def this(text: String, textAlign: Align)(using Sge) = this({
      val label = new VisLabel(text)
      label.setAlignment(textAlign)
      label
    })

    def target(target: Actor): Builder = {
      _target = Nullable(target)
      this
    }

    def style(styleName: String): Builder = style(VisUI.getSkin.get[TooltipStyle](styleName))

    def style(style: TooltipStyle): Builder = {
      _style = Nullable(style)
      this
    }

    /** Sets tooltip width. If tooltip content is text only then calling this will automatically enable label wrapping.
      */
    def width(width: Float): Builder = {
      require(width >= 0, "width must be > 0")
      _width = width
      content match {
        case label: VisLabel => label.wrap = true
        case _ =>
      }
      this
    }

    def build(): Tooltip =
      new Tooltip(_style, _target, Nullable(content), _width)
  }
}
