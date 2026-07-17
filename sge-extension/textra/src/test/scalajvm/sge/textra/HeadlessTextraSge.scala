/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Shared headless-Sge test support for the textra wave 2026-07-17-E reproducer
 * suites (ISS-823 exact-jostle, ISS-824 Skin styleName resolution, ISS-825
 * TextraLabel replacement-font ctor size tail). Mirrors the proven GL-free
 * fixture already inlined in FontDrawGlyphWaveDRedSuite / TextraLabelWidgetRedSuite:
 * a NoopGraphics/NoopGL20 Sge plus a Custom-type TextureData so `new Texture(...)`
 * needs no GL, and a RecordingBatch that captures the vertex geometry drawGlyph
 * emits. Lives under the `sge` package so Batch's `private[sge]` begin/end are
 * overridable.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer

import sge.graphics.{ Color, Pixmap, Texture, TextureData, TextureTarget }
import sge.graphics.g2d.{ Batch, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, Matrix4 }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.files.{ FileHandle, FileType }
import lowlevel.Nullable

object HeadlessTextraSge {

  private object StubApplication extends Application {
    def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                                             Graphics                    = throw new UnsupportedOperationException
    def audio:                                                Audio                       = throw new UnsupportedOperationException
    def input:                                                Input                       = throw new UnsupportedOperationException
    def files:                                                Files                       = throw new UnsupportedOperationException
    def net:                                                  Net                         = throw new UnsupportedOperationException
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = throw new UnsupportedOperationException
    def clipboard:                                            sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object NoNet extends Net {
    import Net.*
    def httpClient:                                                                                         net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean           = false
  }

  private object NoFiles extends Files {
    def getFileHandle(path: String, fileType: FileType): FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                     FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                     FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                             String     = ""
    def isExternalStorageAvailable:                      Boolean    = false
    def localStoragePath:                                String     = ""
    def isLocalStorageAvailable:                         Boolean    = false
  }

  /** Builds a headless Sge (NoopGraphics + NoopGL20). */
  def make(bbWidth: Int = 960, bbHeight: Int = 540): Sge =
    Sge(
      StubApplication,
      new NoopGraphics(noopWidth = bbWidth, noopHeight = bbHeight),
      new NoopAudio(),
      NoFiles,
      new NoopInput(),
      NoNet
    )

  /** Custom-type TextureData so `new Texture(...)` needs no GL (only width/height + a no-op consume). */
  final class DummyTextureData(w: Int, h: Int) extends TextureData {
    def dataType:                                 TextureData.TextureDataType = TextureData.TextureDataType.Custom
    def isPrepared:                               Boolean                     = true
    def prepare():                                Unit                        = ()
    def consumePixmap():                          Pixmap                      = throw new UnsupportedOperationException("dummy texture has no pixmap")
    def disposePixmap:                            Boolean                     = false
    def consumeCustomData(target: TextureTarget): Unit                        = ()
    def width:                                    Int                         = w
    def height:                                   Int                         = h
    def getFormat:                                Pixmap.Format               = Pixmap.Format.RGBA8888
    def useMipMaps:                               Boolean                     = false
    def isManaged:                                Boolean                     = false
  }

  def newTexture()(using Sge): Texture = new Texture(new DummyTextureData(64, 64))

  /** A single recorded quad: which texture it targeted and a copy of the 20 vertex floats. */
  final case class Quad(texture: Texture, verts: Array[Float])

  /** Batch that records the vertex geometry drawGlyph emits. All other Batch surface is inert. */
  final class RecordingBatch extends Batch {
    val quads:           ArrayBuffer[Quad] = ArrayBuffer.empty
    private val _color:  Color             = new Color(1f, 1f, 1f, 1f)
    private val _projMx: Matrix4           = new Matrix4() // identity: values(0) == values(5) == 1f
    private val _tranMx: Matrix4           = new Matrix4()

    @scala.annotation.publicInBinary
    private[sge] def begin(): Unit = ()
    @scala.annotation.publicInBinary
    private[sge] def end(): Unit = ()

    def color_=(tint:              Color):                               Unit  = ()
    def setColor(r:                Float, g: Float, b: Float, a: Float): Unit  = ()
    def color:                                                           Color = _color
    def packedColor_=(packedColor: Float):                               Unit  = ()
    def packedColor:                                                     Float = 0f

    def draw(
      texture:   Texture,
      x:         Float,
      y:         Float,
      originX:   Float,
      originY:   Float,
      width:     Float,
      height:    Float,
      scaleX:    Float,
      scaleY:    Float,
      rotation:  Float,
      srcX:      Int,
      srcY:      Int,
      srcWidth:  Int,
      srcHeight: Int,
      flipX:     Boolean,
      flipY:     Boolean
    ):                                                                                                                                                                       Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX:   Int, srcY:      Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = ()
    def draw(texture: Texture, x: Float, y: Float, srcX:  Int, srcY:     Int, srcWidth: Int, srcHeight: Int):                                                                Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u:      Float, v:       Float, u2:     Float, v2:      Float):                               Unit = ()
    def draw(texture: Texture, x: Float, y: Float):                                                                                                                          Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float):                                                                                             Unit = ()
    def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int):                                                                                       Unit =
      quads += Quad(texture, spriteVertices.slice(offset, offset + count))
    def draw(region: TextureRegion, x: Float, y: Float):                                                                                                                                 Unit = ()
    def draw(region: TextureRegion, x: Float, y: Float, width:   Float, height:  Float):                                                                                                 Unit = ()
    def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float):                     Unit = ()
    def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = ()
    def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = ()

    def flush():           Unit = ()
    def disableBlending(): Unit = ()
    def enableBlending():  Unit = ()

    def setBlendFunction(srcFunc:              Int, dstFunc:      Int):                                       Unit = ()
    def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit = ()
    def blendSrcFunc:                                                                                         Int  = 0
    def blendDstFunc:                                                                                         Int  = 0
    def blendSrcFuncAlpha:                                                                                    Int  = 0
    def blendDstFuncAlpha:                                                                                    Int  = 0

    def projectionMatrix:                                        Matrix4       = _projMx
    def transformMatrix:                                         Matrix4       = _tranMx
    def projectionMatrix_=(projection: Matrix4):                 Unit          = ()
    def transformMatrix_=(transform:   Matrix4):                 Unit          = ()
    def shader_=(shader:               Nullable[ShaderProgram]): Unit          = ()
    def shader:                                                  ShaderProgram = throw new IllegalStateException("RecordingBatch.shader must not be read by the drawGlyph paths under test")
    def blendingEnabled:                                         Boolean       = true
    def drawing:                                                 Boolean       = false

    def close(): Unit = ()
  }

  /** Builds a texture-backed GlyphRegion (offsetX not NaN so it takes the normal glyph path). */
  def texturedGlyph(tex: Texture, w: Int, h: Int, xAdvance: Float): Font.GlyphRegion = {
    val gr = new Font.GlyphRegion(0f, 0f, xAdvance)
    gr.texture = tex
    gr.setRegion(0, 0, w, h)
    gr
  }
}
