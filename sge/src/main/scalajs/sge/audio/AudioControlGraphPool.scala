/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../webaudio/AudioControlGraphPool.java
 * Original authors: barkholt
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AudioControlGraphPool -> AudioControlGraphPool (same name)
 *   Convention: Scala.js only; extends SGE Pool trait
 *   Convention: JavaScriptObject -> js.Dynamic
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import scala.scalajs.js

/** Pool for [[AudioControlGraph]] instances to minimize object creation during sound playback.
  *
  * @param audioContext
  *   the Web Audio AudioContext
  * @param destinationNode
  *   the destination AudioNode for each graph
  */
class AudioControlGraphPool(
  val audioContext:    js.Dynamic,
  val destinationNode: js.Dynamic
) extends utils.Pool[AudioControlGraph] {

  override protected val max:             Int = Int.MaxValue
  override protected val initialCapacity: Int = 4

  override protected def newObject(): AudioControlGraph =
    AudioControlGraph(audioContext, destinationNode)
}
