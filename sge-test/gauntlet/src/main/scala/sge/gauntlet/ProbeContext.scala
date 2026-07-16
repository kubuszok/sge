/*
 * SGE Gauntlet — per-probe execution context.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import sge.files.FileHandle
import sge.graphics.Color
import sge.graphics.g2d.SpriteBatch
import sge.graphics.glutils.ShapeRenderer
import sge.graphics.OrthographicCamera
import sge.Input.Button

import scala.collection.mutable.ArrayBuffer

/** Per-probe execution context handed to every [[FeatureProbe]] callback.
  *
  * Determinism helpers: fixed 1280x720 target size, a fixed timestep ([[fixedDelta]]) and a per-probe seeded RNG ([[rng]]). GPU accessors ([[batch]], [[shapes]], [[camera]], [[pixel]], [[clear]])
  * are backed by the shared [[GpuHarness]] and only valid for `requiresGpu` probes in a windowed run; touching them in headless mode throws.
  *
  * Synthetic input is injected by dispatching through the [[sge.InputProcessor]] the probe registered with [[setInputTarget]] — the same interface the real backends dispatch through, so scene2d
  * Stages observe the events exactly as they would real ones.
  */
final class ProbeContext(
  val probeId:  String,
  val headless: Boolean,
  reportDir:    String,
  gpu:          Option[GpuHarness]
)(using val sgeCtx: Sge) {

  /** Fixed probe target width/height (the FBO size for GPU probes). */
  val width:  Int = GpuHarness.Width
  val height: Int = GpuHarness.Height

  /** Fixed timestep — probes must use this instead of the wall-clock delta. */
  val fixedDelta: Float = 1f / 60f

  /** Deterministic per-probe RNG. */
  val rng: java.util.Random = new java.util.Random(0xc0ffee ^ probeId.hashCode.toLong)

  private val logBuffer = ArrayBuffer.empty[String]

  /** Appends a line to the probe's log; the lines land in the report. */
  def log(line: String): Unit =
    logBuffer += line

  def logLines: List[String] = logBuffer.toList

  /** Per-probe scratch directory under the report dir; created on first access, cleaned by the runner between runs. */
  lazy val tempDir: FileHandle = {
    val safe = probeId.replace('/', '-')
    val dir  = sgeCtx.files.absolute(s"$reportDir/tmp/$safe")
    dir.mkdirs()
    dir
  }

  // ── GPU access (windowed runs only) ─────────────────────────────────

  private def gpuOrFail: GpuHarness =
    gpu.getOrElse(
      throw new IllegalStateException(s"probe $probeId touched GPU context in headless mode — mark it requiresGpu")
    )

  /** Shared SpriteBatch, projection preset to the pixel-perfect probe camera. */
  def batch: SpriteBatch = gpuOrFail.batch

  /** Shared ShapeRenderer, projection preset to the pixel-perfect probe camera. */
  def shapes: ShapeRenderer = gpuOrFail.shapes

  /** Pixel-perfect orthographic camera over the 1280x720 target (y up). */
  def camera: OrthographicCamera = gpuOrFail.camera

  /** Clears color+depth of the probe's render target. */
  def clear(r: Float, g: Float, b: Float, a: Float): Unit =
    gpuOrFail.clear(r, g, b, a)

  /** Reads back one pixel from the probe's render target as a [[Color]]. Coordinates are world/GL coordinates (y up), matching [[camera]]. */
  def pixel(x: Int, y: Int): Color = {
    val (r, g, b, a) = gpuOrFail.readPixel(x, y)
    new Color(r / 255f, g / 255f, b / 255f, a / 255f)
  }

  /** Convenience RGBA-tuple readback (0..255 per channel), same coordinates as [[pixel]]. */
  def pixelRgba(x: Int, y: Int): (Int, Int, Int, Int) =
    gpuOrFail.readPixel(x, y)

  // ── Synthetic input injection ───────────────────────────────────────

  private var inputTarget: Option[InputProcessor] = None

  /** Registers the processor synthetic events are dispatched to (e.g. a scene2d Stage). */
  def setInputTarget(processor: InputProcessor): Unit =
    inputTarget = Some(processor)

  private def target: InputProcessor =
    inputTarget.getOrElse(
      throw new IllegalStateException(s"probe $probeId injected input without calling setInputTarget first")
    )

  /** Screen coordinates: origin top-left, y down — the convention InputProcessor implementations expect. */
  def injectTouchDown(screenX: Int, screenY: Int, pointer: Int = 0, button: Button = Input.Buttons.LEFT): Boolean =
    target.touchDown(Pixels(screenX), Pixels(screenY), pointer, button)

  def injectTouchUp(screenX: Int, screenY: Int, pointer: Int = 0, button: Button = Input.Buttons.LEFT): Boolean =
    target.touchUp(Pixels(screenX), Pixels(screenY), pointer, button)

  def injectMouseMoved(screenX: Int, screenY: Int): Boolean =
    target.mouseMoved(Pixels(screenX), Pixels(screenY))

  def injectKeyDown(keycode: Input.Key): Boolean =
    target.keyDown(keycode)
}
