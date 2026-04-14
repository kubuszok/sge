import sbt._
import sbt.Keys._

/** Procedural asset generation for the asset-showcase demo.
  *
  * Generates PNG textures and WAV audio files at compile time so the demo
  * can exercise AssetManager without shipping binary blobs in the repo.
  */
object AssetGenerator {

  /** sbt settings that register a resource generator producing test assets. */
  val settings: Seq[Setting[_]] = Seq(
    Compile / resourceGenerators += Def.task {
      val outDir   = (Compile / resourceManaged).value
      val texDir   = outDir / "textures"
      val audioDir = outDir / "audio"
      IO.createDirectories(Seq(texDir, audioDir))

      Seq(
        checkerboard(texDir / "checkerboard.png"),
        gradient(texDir / "gradient.png"),
        sineTone(audioDir / "tone.wav", freqHz = 440.0, durationSec = 1.0f, amplitude = 0.7),
        sineTone(audioDir / "click.wav", freqHz = 880.0, durationSec = 0.1f, amplitude = 0.5)
      )
    }
  )

  // ── Texture generators ──────────────────────────────────────────────

  /** 64x64 checkerboard PNG (8x8 grid, blue tones). */
  private def checkerboard(file: File): File = {
    if (!file.exists()) {
      val size     = 64
      val cellSize = size / 8
      val img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
      var y = 0
      while (y < size) {
        var x = 0
        while (x < size) {
          val light = ((x / cellSize) + (y / cellSize)) % 2 == 0
          img.setRGB(x, y, if (light) 0xFF4488CC else 0xFF224466)
          x += 1
        }
        y += 1
      }
      javax.imageio.ImageIO.write(img, "png", file)
    }
    file
  }

  /** 64x64 vertical gradient PNG (blue-to-cyan). */
  private def gradient(file: File): File = {
    if (!file.exists()) {
      val size = 64
      val img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
      var y = 0
      while (y < size) {
        val t   = y.toFloat / (size - 1).toFloat
        val r   = (0.1f + 0.2f * t)
        val g   = (0.3f + 0.4f * t)
        val b   = (0.6f + 0.3f * t)
        val rgb = 0xFF000000 | ((r * 255).toInt << 16) | ((g * 255).toInt << 8) | (b * 255).toInt
        var x = 0
        while (x < size) {
          img.setRGB(x, y, rgb)
          x += 1
        }
        y += 1
      }
      javax.imageio.ImageIO.write(img, "png", file)
    }
    file
  }

  // ── Audio generators ────────────────────────────────────────────────

  /** Sine-wave WAV file with linear fade-out envelope (16-bit mono, 22050 Hz). */
  private def sineTone(file: File, freqHz: Double, durationSec: Float, amplitude: Double): File = {
    if (!file.exists()) {
      val sampleRate    = 22050
      val numSamples    = (sampleRate * durationSec).toInt
      val bitsPerSample = 16
      val numChannels   = 1
      val byteRate      = sampleRate * numChannels * bitsPerSample / 8
      val blockAlign    = numChannels * bitsPerSample / 8
      val dataSize      = numSamples * blockAlign

      val buf = java.nio.ByteBuffer.allocate(44 + dataSize)
      buf.order(java.nio.ByteOrder.LITTLE_ENDIAN)

      // RIFF header
      buf.put("RIFF".getBytes("US-ASCII"))
      buf.putInt(36 + dataSize)
      buf.put("WAVE".getBytes("US-ASCII"))

      // fmt sub-chunk
      buf.put("fmt ".getBytes("US-ASCII"))
      buf.putInt(16)
      buf.putShort(1.toShort) // PCM
      buf.putShort(numChannels.toShort)
      buf.putInt(sampleRate)
      buf.putInt(byteRate)
      buf.putShort(blockAlign.toShort)
      buf.putShort(bitsPerSample.toShort)

      // data sub-chunk
      buf.put("data".getBytes("US-ASCII"))
      buf.putInt(dataSize)
      var i = 0
      while (i < numSamples) {
        val t        = i.toFloat / sampleRate.toFloat
        val envelope = 1.0f - (t / durationSec)
        val sample   = (math.sin(2.0 * math.Pi * freqHz * t) * amplitude * envelope * 32767.0).toInt
        buf.putShort(math.max(-32768, math.min(32767, sample)).toShort)
        i += 1
      }

      IO.write(file, buf.array())
    }
    file
  }
}
