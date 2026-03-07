/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../webaudio/AudioControlGraph.java
 * Original authors: barkholt
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AudioControlGraph -> AudioControlGraph (same name)
 *   Convention: Scala.js only; JSNI native methods replaced with js.Dynamic calls
 *   Convention: JavaScriptObject -> js.Dynamic for Web Audio nodes
 *   Idiom: GainNode + StereoPannerNode for volume/pan control
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import scala.scalajs.js

/** Web Audio API control graph for managing volume and pan for a single sound or music instance.
  *
  * Creates a GainNode and (optionally) a StereoPannerNode, connecting them to the destination. Sources are routed through this graph for volume/pan control.
  *
  * @param audioContext
  *   the Web Audio AudioContext
  * @param destinationNode
  *   the destination AudioNode (typically a global GainNode)
  */
class AudioControlGraph(audioContext: js.Dynamic, destinationNode: js.Dynamic) {

  private val gainNode: js.Dynamic = {
    val node =
      if (!js.isUndefined(audioContext.createGain)) audioContext.createGain()
      else audioContext.createGainNode() // old WebKit/iOS fallback
    node.gain.value = 1.0
    node.connect(destinationNode)
    node
  }

  private val panNode: js.Dynamic =
    if (!js.isUndefined(audioContext.createStereoPanner)) {
      val node = audioContext.createStereoPanner()
      node.pan.value = 0
      node.connect(gainNode)
      node
    } else js.undefined.asInstanceOf[js.Dynamic]

  /** Connect a source AudioNode to this control graph. */
  def setSource(sourceNode: js.Dynamic): Unit =
    if (!js.isUndefined(panNode)) sourceNode.connect(panNode)
    else sourceNode.connect(gainNode)

  /** Set the volume (gain). */
  def setVolume(volume: Float): Unit =
    gainNode.gain.value = volume.toDouble

  /** Get the current volume. */
  def getVolume(): Float =
    gainNode.gain.value.asInstanceOf[Double].toFloat

  /** Set the stereo pan (-1 to 1). */
  def setPan(pan: Float): Unit =
    if (!js.isUndefined(panNode)) panNode.pan.value = pan.toDouble

  /** Get the current pan value. */
  def getPan(): Float =
    if (!js.isUndefined(panNode)) panNode.pan.value.asInstanceOf[Double].toFloat
    else 0f
}
