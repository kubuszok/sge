/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/SizeToAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: target.getWidth -> target.foreach; tuple destructure for (w,h)
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: SizeToAction,begin,endHeight,endWidth,setSize,startHeight,startWidth,update
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/SizeToAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

/** Moves an actor from its current size to a specific size.
  * @author
  *   Nathan Sweet
  */
class SizeToAction extends TemporalAction {
  private var startWidth:  Float = 0
  private var startHeight: Float = 0
  var endWidth:            Float = 0
  var endHeight:           Float = 0

  override protected def begin(): Unit =
    target.foreach { t =>
      startWidth = t.width
      startHeight = t.height
    }

  override protected def update(percent: Float): Unit =
    target.foreach { t =>
      val (w, h) =
        if (percent == 0) (startWidth, startHeight)
        else if (percent == 1) (endWidth, endHeight)
        else (startWidth + (endWidth - startWidth) * percent, startHeight + (endHeight - startHeight) * percent)
      t.setSize(w, h)
    }

  def setSize(width: Float, height: Float): Unit = { endWidth = width; endHeight = height }
}
