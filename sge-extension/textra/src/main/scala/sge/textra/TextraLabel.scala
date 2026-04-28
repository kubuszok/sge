/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraLabel.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class (no scene2d base), Batch → deferred,
 *     FloatArray/LongArray → ArrayBuffer[Float]/ArrayBuffer[Long],
 *     Skin → removed (FWSkin integration deferred), Align → Int constants,
 *     TransformDrawable → AnyRef placeholder
 *   Convention: getX()/setX() → public var where no logic.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *   Ported: draw(), layout(), getPrefWidth/getPrefHeight with background support.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 660
 * Covenant-baseline-methods: TextraLabel,_color,_height,_originX,_originY,_rotation,_scaleX,_scaleY,_width,_x,_y,act,actualWidth,adjustedWidth,align,baseX,baseY,cs,defaultToken,doLayout,draw,e,font,getAdvances,getAlignment,getColor,getDefaultToken,getEllipsis,getFont,getGlyph,getHeight,getLineHeight,getMaxLines,getOffsets,getOriginX,getOriginY,getPrefHeight,getPrefWidth,getRotation,getRotations,getScaleX,getScaleY,getSizing,getWidth,getX,getY,glyphCount,height,i,idx,index,invalidate,isWrap,layout,layoutHeight,lines,ln,n,old,originX,originY,originalHeight,prefSizeInvalid,regenerateLayout,resetShader,rot,s,sb,setAlignment,setBounds,setColor,setDefaultToken,setEllipsis,setFont,setHeight,setMaxLines,setParent,setPosition,setRotation,setScaleX,setScaleY,setSize,setStage,setSuperHeight,setSuperWidth,setText,setWidth,setWrap,setX,setY,skipToTheEnd,sn,storedText,style,substring,this,toString,useIntegerPositions,validate,widgetHeight,widgetWidth,width,wrap
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraLabel.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.scenes.scene2d.utils.Drawable
import sge.scenes.scene2d.utils.TransformDrawable
import sge.utils.Align
import sge.utils.Nullable

/** A scene2d.ui Widget that displays text using a Font rather than a libGDX BitmapFont. This supports being laid out in a Table. This permits square-bracket tag markup from Font. It does not support
  * the curly-brace token markup that its subclass TypingLabel does, nor does this handle input in the way TypingLabel can.
  */
class TextraLabel {

  var layout:         Layout = new Layout()
  protected var font: Font   = new Font()
  var align:          Align  = Align.left

  /** If true, allows text to wrap when it would go past the layout's targetWidth. */
  var wrap:                      Boolean                     = false
  var storedText:                String                      = ""
  var style:                     Nullable[Styles.LabelStyle] = Nullable.empty
  protected var prefSizeInvalid: Boolean                     = true
  protected var defaultToken:    String                      = ""

  // Widget-like fields (normally inherited from scene2d)
  private var _width:    Float = 0f
  private var _height:   Float = 0f
  private var _x:        Float = 0f
  private var _y:        Float = 0f
  private var _scaleX:   Float = 1f
  private var _scaleY:   Float = 1f
  private var _rotation: Float = 0f
  private val _originX:  Float = 0f
  private val _originY:  Float = 0f
  private val _color:    Color = new Color(Color.WHITE)

  def getX:                    Float = _x
  def getY:                    Float = _y
  def setX(x:          Float): Unit  = _x = x
  def setY(y:          Float): Unit  = _y = y
  def getScaleX:               Float = _scaleX
  def getScaleY:               Float = _scaleY
  def setScaleX(sx:    Float): Unit  = _scaleX = sx
  def setScaleY(sy:    Float): Unit  = _scaleY = sy
  def getRotation:             Float = _rotation
  def setRotation(rot: Float): Unit  = _rotation = rot
  def getOriginX:              Float = _originX
  def getOriginY:              Float = _originY
  def getColor:                Color = _color
  def setColor(c:      Color): Unit  = if (c != null) _color.set(c)

  def setColor(r: Float, g: Float, b: Float, a: Float): Unit = _color.set(r, g, b, a)

  def setPosition(x: Float, y: Float): Unit = {
    _x = x
    _y = y
  }

  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    _x = x
    _y = y
    _width = width
    _height = height
  }

  /** Creates a TextraLabel that uses the default font with white color. */
  def this(dummy: Unit) = {
    this()
    layout = new Layout()
    font = new Font()
    style = Nullable(new Styles.LabelStyle(font, Nullable.empty))
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken
  }

  /** Creates a TextraLabel with the given text and using the given style. */
  def this(text: String, style: Styles.LabelStyle) = {
    this()
    this.font = Nullable.fold(style.font)(new Font())(identity)
    this.layout = new Layout()
    Nullable.foreach(style.fontColor)(c => layout.setBaseColor(c))
    this.style = Nullable(style)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text and style, using a replacement font. */
  def this(text: String, style: Styles.LabelStyle, replacementFont: Font) = {
    this()
    this.font = replacementFont
    this.layout = new Layout()
    Nullable.foreach(style.fontColor)(c => layout.setBaseColor(c))
    this.style = Nullable(style)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text and font. */
  def this(text: String, font: Font) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text, font, and default color. */
  def this(text: String, font: Font, color: Color) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    if (color != null) layout.setBaseColor(color)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text, font, color, and justification. */
  def this(text: String, font: Font, color: Color, justify: Justify) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    if (color != null) layout.setBaseColor(color)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
    layout.setJustification(justify)
  }

  def getWidth:  Float = _width
  def getHeight: Float = _height

  def setWidth(width: Float): Unit = {
    _width = width
    layout.setTargetWidth(width)
    font.calculateSize(layout)
  }

  def setHeight(height: Float): Unit = {
    _height = height
    font.calculateSize(layout)
  }

  def setSize(width: Float, height: Float): Unit = {
    _width = width
    _height = height
    layout.setTargetWidth(width)
    font.calculateSize(layout)
  }

  /** This only exists so code that needs to set the raw Actor width (bypassing layout recalculation) still can, even with setWidth() implemented here.
    * @param width
    *   the new width, in world units as a float
    */
  def setSuperWidth(width: Float): Unit =
    _width = width

  /** This only exists so code that needs to set the raw Actor height (bypassing layout recalculation) still can, even with setHeight() implemented here.
    * @param height
    *   the new height, in world units as a float
    */
  def setSuperHeight(height: Float): Unit =
    _height = height

  /** Called by the framework when this actor or any ascendant is added to a group that is in the stage. This is overridden as public instead of protected because most of its usage in scene2d.ui code
    * is not actually in inheriting classes, but in other classes in the same package.
    * @param stage
    *   May be null if the actor or any ascendant is no longer in a stage.
    */
  def setStage(stage: AnyRef): Unit = ()

  /** Called by the framework when an actor is added to or removed from a group. This is overridden as public instead of protected because most of its usage in scene2d.ui code is not actually in
    * inheriting classes, but in other classes in the same package.
    * @param parent
    *   May be null if the actor has been removed from the parent.
    */
  def setParent(parent: AnyRef): Unit = ()

  def getPrefWidth: Float =
    if (wrap) 0f
    else {
      if (prefSizeInvalid) validate()
      var width = layout.getWidth
      Nullable.foreach(style) { s =>
        Nullable.foreach(s.background) { bgAny =>
          bgAny match {
            case bg: Drawable =>
              width = Math.max(width + bg.leftWidth + bg.rightWidth, bg.minWidth)
            case _ => ()
          }
        }
      }
      width
    }

  def getPrefHeight: Float = {
    if (prefSizeInvalid) validate()
    var height = layout.getHeight
    Nullable.foreach(style) { s =>
      Nullable.foreach(s.background) { bgAny =>
        bgAny match {
          case bg: Drawable =>
            height = Math.max(height + bg.bottomHeight + bg.topHeight, bg.minHeight)
          case _ => ()
        }
      }
    }
    height
  }

  /** A no-op unless font is a subclass that overrides Font.handleIntegerPosition(float). */
  def useIntegerPositions(integer: Boolean): TextraLabel = {
    font.integerPosition = integer
    this
  }

  def isWrap: Boolean = wrap

  def setWrap(wrap: Boolean): TextraLabel = {
    val old = this.wrap
    this.wrap = wrap
    if (old != wrap) {
      invalidate()
      if (this.wrap) {
        doLayout()
      }
    }
    this
  }

  def getAlignment: Align = align

  def setAlignment(alignment: Align): Unit =
    align = alignment

  def getFont: Font = font

  /** Sets the font and regenerates the layout. */
  def setFont(font: Font): Unit =
    if (!this.font.eq(font)) {
      this.font = font
      regenerateLayout()
    }

  /** Sets the font; only regenerates the layout if regenerate is true. */
  def setFont(font: Font, regenerate: Boolean): Unit =
    if (!this.font.eq(font)) {
      this.font = font
      if (regenerate) regenerateLayout()
    }

  /** Re-calculates line breaks when wrapping is enabled, and always re-calculates the size. */
  def regenerateLayout(): Unit = {
    font.regenerateLayout(layout)
    font.calculateSize(layout)
  }

  /** Changes the text in this TextraLabel to the given String, parsing any markup in it. */
  def setText(markupText: String): Unit = {
    storedText = defaultToken + markupText
    if (wrap) layout.setTargetWidth(_width)
    font.markup(storedText, layout.clear())
  }

  /** By default, does nothing; this is overridden in TypingLabel to skip its text progression ahead. */
  def skipToTheEnd(): TextraLabel = this

  /** Called each frame with the time since the last frame. No-op for TextraLabel; overridden in TypingLabel. */
  def act(delta: Float): Unit = ()

  def invalidate(): Unit =
    prefSizeInvalid = true

  def validate(): Unit =
    prefSizeInvalid = false

  /** Performs layout calculations, adjusting wrapping and target width. Called by validate() or when wrap changes. */
  def doLayout(): Unit = {
    val width         = _width
    var adjustedWidth = width
    Nullable.foreach(style) { s =>
      Nullable.foreach(s.background) { bgAny =>
        bgAny match {
          case bg: Drawable =>
            adjustedWidth = adjustedWidth - (bg.leftWidth + bg.rightWidth)
          case _ => ()
        }
      }
    }
    val originalHeight = layout.getHeight
    val actualWidth    = font.calculateSize(layout)

    if (wrap) {
      if (adjustedWidth == 0 || layout.getTargetWidth != adjustedWidth || actualWidth > adjustedWidth) {
        if (adjustedWidth != 0f) {
          layout.setTargetWidth(adjustedWidth)
        }
        font.regenerateLayout(layout)
      }

      // If the call to calculateSize() changed layout's height, update height.
      val newHeight = layout.getHeight
      if (!sge.math.MathUtils.isEqual(originalHeight, newHeight)) {
        setSuperHeight(newHeight)
      }
    }
  }

  /** Draws this label using the given Batch. parentAlpha is multiplied into the label's own alpha. */
  def draw(batch: sge.graphics.g2d.Batch, parentAlpha: Float): Unit = boundary {
    validate()

    val rot     = _rotation
    val originX = _originX
    val originY = _originY
    val sn      = sge.math.MathUtils.sinDeg(rot)
    val cs      = sge.math.MathUtils.cosDeg(rot)

    val lines = layout.lineCount
    var baseX = _x
    var baseY = _y

    // These two blocks use different height measurements, so center vertical is offset once by half the layout
    // height, and once by half the widget height.
    val layoutHeight = layout.getHeight * _scaleY
    if (align.isBottom) {
      baseX -= sn * layoutHeight
      baseY += cs * layoutHeight
    } else if (align.isCenterVertical) {
      baseX -= sn * layoutHeight * 0.5f
      baseY += cs * layoutHeight * 0.5f
    }
    val widgetHeight = _height * _scaleY
    if (align.isTop) {
      baseX -= sn * widgetHeight
      baseY += cs * widgetHeight
    } else if (align.isCenterVertical) {
      baseX -= sn * widgetHeight * 0.5f
      baseY += cs * widgetHeight * 0.5f
    }

    val widgetWidth = _width * _scaleX
    if (align.isRight) {
      baseX += cs * widgetWidth
      baseY += sn * widgetWidth
    } else if (align.isCenterHorizontal) {
      baseX += cs * widgetWidth * 0.5f
      baseY += sn * widgetWidth * 0.5f
    }

    Nullable.foreach(style) { s =>
      Nullable.foreach(s.background) { bgAny =>
        bgAny match {
          case bg: Drawable =>
            if (align.isLeft) {
              baseX += cs * bg.leftWidth
              baseY += sn * bg.leftWidth
            } else if (align.isRight) {
              baseX -= cs * bg.rightWidth
              baseY -= sn * bg.rightWidth
            } else {
              baseX += cs * (bg.leftWidth - bg.rightWidth) * 0.5f
              baseY += sn * (bg.leftWidth - bg.rightWidth) * 0.5f
            }
            if (align.isBottom) {
              baseX -= sn * bg.bottomHeight
              baseY += cs * bg.bottomHeight
            } else if (align.isTop) {
              baseX += sn * bg.topHeight
              baseY -= cs * bg.topHeight
            } else {
              baseX -= sn * (bg.bottomHeight - bg.topHeight) * 0.5f
              baseY += cs * (bg.bottomHeight - bg.topHeight) * 0.5f
            }
            bg match {
              case td: TransformDrawable =>
                try
                  td.draw(batch,
                          _x,
                          _y, // position
                          originX,
                          originY, // origin
                          _width,
                          _height, // size
                          1f,
                          1f, // scale
                          rot
                  ) // rotation
                catch {
                  case _: UnsupportedOperationException | _: ClassCastException =>
                    bg.draw(batch, _x, _y, _width, _height)
                }
              case _ =>
                bg.draw(batch, _x, _y, _width, _height)
            }
          case _ => ()
        }
      }
    }

    if (layout.lines.isEmpty || parentAlpha <= 0f) break(())

    // we only change the shader or batch color if we actually are drawing something.
    val resetShader = font.getDistanceField != Font.DistanceFieldType.STANDARD &&
      Nullable.fold(font.shader)(true)(sh => batch.shader ne sh)
    if (resetShader) {
      font.enableShader(batch)
    }
    batch.color.set(_color).a *= parentAlpha
    batch.color = batch.color

    var ln = 0
    while (ln < lines) {
      val line = layout.lines(ln)

      if (line.glyphs.nonEmpty) {
        val lineWidth  = line.width * _scaleX
        val lineHeight = line.height * _scaleY

        baseX += sn * lineHeight
        baseY -= cs * lineHeight

        var x = baseX
        var y = baseY

        val worldOriginX = x + originX
        val worldOriginY = y + originY
        val fx           = -originX
        val fy           = -originY
        x = cs * fx - sn * fy + worldOriginX
        y = sn * fx + cs * fy + worldOriginY

        if (align.isCenterHorizontal) {
          x -= cs * (lineWidth * 0.5f)
          y -= sn * (lineWidth * 0.5f)
        } else if (align.isRight) {
          x -= cs * lineWidth
          y -= sn * lineWidth
        }
        x -= sn * (0.5f * lineHeight)
        y += cs * (0.5f * lineHeight)

        var xChange = 0f
        var yChange = 0f
        var f: Nullable[Font] = Nullable.empty
        var kern  = -1
        var curly = false
        val start = layout.countGlyphsBeforeLine(ln)
        var i     = 0
        val n     = line.glyphs.size
        while (i < n) {
          val glyph = line.glyphs(i)
          val ch    = (glyph & 0xffff).toChar

          var skipGlyph = false
          if (font.omitCurlyBraces) {
            if (curly) {
              if (ch == '}') {
                curly = false
                skipGlyph = true
              } else if (ch == '{') {
                curly = false
                // fall through to rendering
              } else {
                skipGlyph = true
              }
            } else if (ch == '{') {
              curly = true
              skipGlyph = true
            }
          }

          if (!skipGlyph) {
            Nullable.foreach(font.family) { fam =>
              f = Nullable(fam.connected((glyph >>> 16 & 15).toInt))
            }
            val fFont = Nullable.getOrElse(f)(font)
            val even  = (start + i) << 1
            val odd   = even | 1
            val a     = getAdvances.get(start + i) * _scaleX
            if (i == 0) {
              x -= 0.5f * fFont.cellWidth
              x += cs * 0.5f * fFont.cellWidth
              y += sn * 0.5f * fFont.cellWidth

              if (font.integerPosition) {
                x = x.toInt.toFloat
                y = y.toInt.toFloat
              }

              val reg = fFont.mapping.getOrElse(ch.toInt, null) // @nowarn — Java interop: HashMap getOrElse default
              if (reg != null && reg.offsetX < 0 && !font.isMono) {
                val ox = reg.offsetX * fFont.scaleX * a
                xChange -= cs * ox
                yChange -= sn * ox
              }
            }

            Nullable.foreach(fFont.kerning) { kernMap =>
              kern = kern << 16 | (glyph & 0xffff).toInt
              val amt = kernMap.getOrElse(kern, 0f) * fFont.scaleX * a
              xChange += cs * amt
              yChange += sn * amt
            }
            if (Nullable.isEmpty(fFont.kerning)) {
              kern = -1
            }

            var xx = x + xChange + getOffsets.get(even) * _scaleX
            var yy = y + yChange + getOffsets.get(odd) * _scaleY
            if (font.integerPosition) {
              xx = xx.toInt.toFloat
              yy = yy.toInt.toFloat
            }

            val single = fFont.drawGlyph(batch, glyph, xx, yy, getRotations.get(start + i) + rot, getSizing.get(even) * _scaleX, getSizing.get(odd) * _scaleY, 0, a)
            xChange += cs * single
            yChange += sn * single
          }
          i += 1
        }
      }
      ln += 1
    }

    if (resetShader) {
      batch.shader = Nullable.empty
    }
  }

  /** Gets a glyph from this label's layout. */
  def getGlyph(index: Int): Long = boundary {
    var idx = index
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) break(glyphs(idx))
      else idx -= glyphs.size
      i += 1
    }
    0xffffffL
  }

  /** The maximum number of Lines this label can display. */
  def getMaxLines: Int = layout.maxLines

  /** Sets the maximum number of Lines this Layout can display; this is always at least 1. */
  def setMaxLines(maxLines: Int): Unit =
    layout.setMaxLines(maxLines)

  /** Gets the ellipsis, which may be null, or may be a String placed at the end of text if its max lines are exceeded. */
  def getEllipsis: Nullable[String] = layout.ellipsis

  /** Sets the ellipsis text. */
  def setEllipsis(ellipsis: Nullable[String]): Unit =
    layout.setEllipsis(ellipsis)

  /** Gets a String from the layout, made of only the char portions of the glyphs from start (inclusive) to end (exclusive). */
  def substring(start: Int, end: Int): String = boundary {
    val s          = Math.max(0, start)
    val e          = Math.min(layout.countGlyphs, end)
    var index      = s
    val sb         = new StringBuilder(e - s)
    var glyphCount = 0
    var i          = 0
    val n          = layout.lineCount
    while (i < n && index >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (index < glyphs.size) {
        val fin = index - s - glyphCount + e
        while (index < fin && index < glyphs.size) {
          val c = (glyphs(index) & 0xffff).toChar
          if (c >= '\ue000' && c <= '\uf800') {
            Nullable.fold(font.namesByCharCode) {
              sb.append(c)
            } { nbc =>
              nbc.get(c.toInt) match {
                case Some(name) => sb.append(name)
                case None       => sb.append(c)
              }
            }
          } else {
            if (c == '\u0002') sb.append('[')
            else if (c != '\u200b') sb.append(c) // do not print zero-width space
          }
          glyphCount += 1
          index += 1
        }
        if (glyphCount == e - s) break(sb.toString)
        index = 0
      } else {
        index -= glyphs.size
      }
      i += 1
    }
    sb.toString
  }

  /** Gets the height of the Line containing the glyph at the given index. */
  def getLineHeight(index: Int): Float = boundary {
    var idx = index
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) break(layout.lines(i).height)
      else idx -= glyphs.size
      i += 1
    }
    font.cellHeight
  }

  /** Contains one float per glyph; each is a rotation in degrees to apply to that glyph (around its center). */
  def getRotations: FloatArrayHelper = new FloatArrayHelper(layout.rotations)

  /** Contains two floats per glyph; even items are x offsets, odd items are y offsets. */
  def getOffsets: FloatArrayHelper = new FloatArrayHelper(layout.offsets)

  /** Contains two floats per glyph, as size multipliers; even items apply to x, odd items apply to y. */
  def getSizing: FloatArrayHelper = new FloatArrayHelper(layout.sizing)

  /** Contains one float per glyph; each is a multiplier that affects the x-advance of that glyph. */
  def getAdvances: FloatArrayHelper = new FloatArrayHelper(layout.advances)

  /** Returns the default token being used in this label. */
  def getDefaultToken: String = defaultToken

  /** Sets the default token being used in this label. */
  def setDefaultToken(defaultToken: String): Unit =
    this.defaultToken = if (defaultToken == null) "" else defaultToken

  override def toString: String = substring(0, Int.MaxValue)
}
