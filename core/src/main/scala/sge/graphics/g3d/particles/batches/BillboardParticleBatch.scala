/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/BillboardParticleBatch.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package batches

import sge.graphics.GL20
import sge.graphics.Mesh
import sge.graphics.Texture
import sge.graphics.VertexAttribute
import sge.graphics.VertexAttributes
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.Material
import sge.graphics.g3d.Renderable
import sge.graphics.g3d.Shader
import sge.graphics.g3d.attributes.BlendingAttribute
import sge.graphics.g3d.attributes.DepthTestAttribute
import sge.graphics.g3d.attributes.TextureAttribute
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ResourceData
import sge.graphics.g3d.particles.renderers.BillboardControllerRenderData
import sge.graphics.g3d.shaders.DefaultShader
import sge.graphics.glutils.ShaderProgram
import sge.math.MathUtils
import sge.math.Matrix3
import sge.math.Vector3
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.Pool

/** This class is used to render billboard particles.
  * @author
  *   Inferno
  */
class BillboardParticleBatch(
  mode:               BillboardParticleBatch.AlignMode,
  useGPU:             Boolean,
  capacity:           Int,
  blendingAttribute:  Nullable[BlendingAttribute],
  depthTestAttribute: Nullable[DepthTestAttribute]
)(using Sge)
    extends BufferedParticleBatch[BillboardControllerRenderData] {

  import BillboardParticleBatch.*

  private val renderablePool:       RenderablePool                   = new RenderablePool(this)
  private val renderables:          DynamicArray[Renderable]         = DynamicArray[Renderable]()
  private var vertices:             Array[Float]                     = scala.compiletime.uninitialized
  private var indices:              Array[Short]                     = scala.compiletime.uninitialized
  private var currentVertexSize:    Int                              = 0
  private var currentAttributes:    VertexAttributes                 = scala.compiletime.uninitialized
  protected var _useGPU:            Boolean                          = false
  protected var _mode:              BillboardParticleBatch.AlignMode = BillboardParticleBatch.AlignMode.Screen
  protected var texture:            Texture                          = scala.compiletime.uninitialized
  protected var _blendingAttribute: BlendingAttribute                =
    blendingAttribute.getOrElse(new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f))
  protected var _depthTestAttribute: DepthTestAttribute =
    depthTestAttribute.getOrElse(new DepthTestAttribute(GL20.GL_LEQUAL, false))
  var shader: Nullable[Shader] = Nullable.empty

  allocIndices()
  initRenderData()
  ensureCapacity(capacity)
  setUseGpu(useGPU)
  setAlignMode(mode)

  /** Create a new BillboardParticleBatch
    * @param mode
    * @param useGPU
    *   Allow to use GPU instead of CPU
    * @param capacity
    *   Max particle displayed
    * @param blendingAttribute
    *   Blending attribute used by the batch
    * @param depthTestAttribute
    *   DepthTest attribute used by the batch
    */
  def this(mode: BillboardParticleBatch.AlignMode, useGPU: Boolean, capacity: Int)(using Sge) =
    this(mode, useGPU, capacity, Nullable.empty, Nullable.empty)

  def this()(using Sge) =
    this(BillboardParticleBatch.AlignMode.Screen, false, 100)

  def this(capacity: Int)(using Sge) =
    this(BillboardParticleBatch.AlignMode.Screen, false, capacity)

  override def allocParticlesData(capacity: Int): Unit = {
    vertices = new Array[Float](currentVertexSize * 4 * capacity)
    allocRenderables(capacity)
  }

  protected def allocRenderable(): Renderable = {
    val renderable = new Renderable()
    renderable.meshPart.primitiveType = GL20.GL_TRIANGLES
    renderable.meshPart.offset = 0
    renderable.material = Nullable(new Material(_blendingAttribute, _depthTestAttribute, TextureAttribute.createDiffuse(texture)))
    renderable.meshPart.mesh = new Mesh(false, MAX_VERTICES_PER_MESH, MAX_PARTICLES_PER_MESH * 6, currentAttributes)
    renderable.meshPart.mesh.setIndices(indices)
    renderable.shader = shader
    renderable
  }

  private def allocIndices(): Unit = {
    val indicesCount = MAX_PARTICLES_PER_MESH * 6
    indices = new Array[Short](indicesCount)
    var i      = 0
    var vertex = 0
    while (i < indicesCount) {
      indices(i) = vertex.toShort
      indices(i + 1) = (vertex + 1).toShort
      indices(i + 2) = (vertex + 2).toShort
      indices(i + 3) = (vertex + 2).toShort
      indices(i + 4) = (vertex + 3).toShort
      indices(i + 5) = vertex.toShort
      i += 6
      vertex += 4
    }
  }

  private def allocRenderables(capacity: Int): Unit = {
    // Free old meshes
    val meshCount = MathUtils.ceil(capacity.toFloat / MAX_PARTICLES_PER_MESH)
    val free      = renderablePool.getFree
    if (free < meshCount) {
      var i    = 0
      val left = meshCount - free
      while (i < left) {
        renderablePool.free(renderablePool.newObject())
        i += 1
      }
    }
  }

  protected def getShader(renderable: Renderable): Shader = {
    // ParticleShader is not yet ported; use DefaultShader for now
    val s: Shader = new DefaultShader(renderable)
    s.init()
    s
  }

  private def allocShader(): Unit = {
    val newRenderable = allocRenderable()
    val s             = getShader(newRenderable)
    shader = Nullable(s)
    newRenderable.shader = Nullable(s)
    renderablePool.free(newRenderable)
  }

  private def clearRenderablesPool(): Unit = {
    renderables.foreach(renderablePool.free)
    var i    = 0
    val free = renderablePool.getFree
    while (i < free) {
      val renderable = renderablePool.obtain()
      renderable.meshPart.mesh.close()
      i += 1
    }
    renderables.clear()
  }

  /** Sets vertex attributes and size */
  def setVertexData(): Unit =
    if (_useGPU) {
      currentAttributes = GPU_ATTRIBUTES
      currentVertexSize = GPU_VERTEX_SIZE
      /*
       * if(mode == AlignMode.ParticleDirection){ currentAttributes = GPU_EXT_ATTRIBUTES; currentVertexSize =
       * GPU_EXT_VERTEX_SIZE; } else{ currentAttributes = GPU_ATTRIBUTES; currentVertexSize = GPU_VERTEX_SIZE; }
       */
    } else {
      currentAttributes = CPU_ATTRIBUTES
      currentVertexSize = CPU_VERTEX_SIZE
    }

  /** Allocates all the require rendering resources like Renderables,Shaders,Meshes according to the current batch configuration.
    */
  private def initRenderData(): Unit = {
    setVertexData()
    clearRenderablesPool()
    allocShader()
    resetCapacity()
  }

  /** Sets the current align mode. It will reallocate internal data, use only when necessary. */
  def setAlignMode(mode: BillboardParticleBatch.AlignMode): Unit =
    if (mode != this._mode) {
      this._mode = mode
      if (_useGPU) {
        initRenderData()
        allocRenderables(bufferedParticlesCount)
      }
    }

  def getAlignMode(): BillboardParticleBatch.AlignMode =
    _mode

  /** Sets the current align mode. It will reallocate internal data, use only when necessary. */
  def setUseGpu(useGPU: Boolean): Unit =
    if (this._useGPU != useGPU) {
      this._useGPU = useGPU
      initRenderData()
      allocRenderables(bufferedParticlesCount)
    }

  def isUseGPU(): Boolean =
    _useGPU

  def setTexture(texture: Texture): Unit = {
    renderables.foreach(renderablePool.free)
    renderables.clear()
    var i    = 0
    val free = renderablePool.getFree
    while (i < free) {
      val renderable = renderablePool.obtain()
      renderable.material.foreach { mat =>
        mat.get(TextureAttribute.Diffuse).foreach { attr =>
          attr.asInstanceOf[TextureAttribute].textureDescription.texture = Nullable(texture)
        }
      }
      i += 1
    }
    this.texture = texture
  }

  def getTexture(): Texture =
    texture

  def getBlendingAttribute(): BlendingAttribute =
    _blendingAttribute

  override def begin(): Unit = {
    super.begin()
    renderables.foreach(renderablePool.free)
    renderables.clear()
  }

  // GPU
  // Required + Color + Rotation
  private def putVertexGPU(
    vertices:    Array[Float],
    offset:      Int,
    x:           Float,
    y:           Float,
    z:           Float,
    u:           Float,
    v:           Float,
    scaleX:      Float,
    scaleY:      Float,
    cosRotation: Float,
    sinRotation: Float,
    r:           Float,
    g:           Float,
    b:           Float,
    a:           Float
  ): Unit = {
    // Position
    vertices(offset + GPU_POSITION_OFFSET) = x
    vertices(offset + GPU_POSITION_OFFSET + 1) = y
    vertices(offset + GPU_POSITION_OFFSET + 2) = z
    // UV
    vertices(offset + GPU_UV_OFFSET) = u
    vertices(offset + GPU_UV_OFFSET + 1) = v
    // Scale
    vertices(offset + GPU_SIZE_ROTATION_OFFSET) = scaleX
    vertices(offset + GPU_SIZE_ROTATION_OFFSET + 1) = scaleY
    vertices(offset + GPU_SIZE_ROTATION_OFFSET + 2) = cosRotation
    vertices(offset + GPU_SIZE_ROTATION_OFFSET + 3) = sinRotation
    // Color
    vertices(offset + GPU_COLOR_OFFSET) = r
    vertices(offset + GPU_COLOR_OFFSET + 1) = g
    vertices(offset + GPU_COLOR_OFFSET + 2) = b
    vertices(offset + GPU_COLOR_OFFSET + 3) = a
  }

  /*
   * //Required + Color + Rotation + Direction private static void putVertex( float[] vertices, int offset, float x, float y,
   * float z, float u, float v, float scaleX, float scaleY, float cosRotation, float sinRotation, float r, float g, float b,
   * float a, Vector3 direction) { //Position vertices[offset + GPU_EXT_POSITION_OFFSET] = x; vertices[offset +
   * GPU_EXT_POSITION_OFFSET+1] = y; vertices[offset + GPU_EXT_POSITION_OFFSET+2] = z; //UV vertices[offset + GPU_EXT_UV_OFFSET]
   * = u; vertices[offset + GPU_EXT_UV_OFFSET+1] = v; //Scale vertices[offset + GPU_EXT_SIZE_ROTATION_OFFSET] = scaleX;
   * vertices[offset + GPU_EXT_SIZE_ROTATION_OFFSET+1] = scaleY; vertices[offset + GPU_EXT_SIZE_ROTATION_OFFSET+2] = cosRotation;
   * vertices[offset + GPU_EXT_SIZE_ROTATION_OFFSET+3] = sinRotation; //Color vertices[offset + GPU_EXT_COLOR_OFFSET] = r;
   * vertices[offset + GPU_EXT_COLOR_OFFSET+1] = g; vertices[offset + GPU_EXT_COLOR_OFFSET+2] = b; vertices[offset +
   * GPU_EXT_COLOR_OFFSET+3] = a; //Direction vertices[offset + GPU_EXT_DIRECTION_OFFSET] = direction.x; vertices[offset +
   * GPU_EXT_DIRECTION_OFFSET +1] = direction.y; vertices[offset + GPU_EXT_DIRECTION_OFFSET +2] = direction.z; }
   */

  // CPU
  // Required
  private def putVertexCPU(vertices: Array[Float], offset: Int, p: Vector3, u: Float, v: Float, r: Float, g: Float, b: Float, a: Float): Unit = {
    // Position
    vertices(offset + CPU_POSITION_OFFSET) = p.x
    vertices(offset + CPU_POSITION_OFFSET + 1) = p.y
    vertices(offset + CPU_POSITION_OFFSET + 2) = p.z
    // UV
    vertices(offset + CPU_UV_OFFSET) = u
    vertices(offset + CPU_UV_OFFSET + 1) = v
    // Color
    vertices(offset + CPU_COLOR_OFFSET) = r
    vertices(offset + CPU_COLOR_OFFSET + 1) = g
    vertices(offset + CPU_COLOR_OFFSET + 2) = b
    vertices(offset + CPU_COLOR_OFFSET + 3) = a
  }

  private def fillVerticesGPU(particlesOffset: Array[Int]): Unit = {
    var tp = 0
    for (data <- renderData) {
      val scaleChannel    = data.scaleChannel
      val regionChannel   = data.regionChannel
      val positionChannel = data.positionChannel
      val colorChannel    = data.colorChannel
      val rotationChannel = data.rotationChannel
      var p               = 0
      val c               = data.controller.particles.size
      while (p < c) {
        var baseOffset     = particlesOffset(tp) * currentVertexSize * 4
        val scale          = scaleChannel.floatData(p * scaleChannel.strideSize)
        val regionOffset   = p * regionChannel.strideSize
        val positionOffset = p * positionChannel.strideSize
        val colorOffset    = p * colorChannel.strideSize
        val rotationOffset = p * rotationChannel.strideSize
        val px             = positionChannel.floatData(positionOffset + ParticleChannels.XOffset)
        val py             = positionChannel.floatData(positionOffset + ParticleChannels.YOffset)
        val pz             = positionChannel.floatData(positionOffset + ParticleChannels.ZOffset)
        val u              = regionChannel.floatData(regionOffset + ParticleChannels.UOffset)
        val v              = regionChannel.floatData(regionOffset + ParticleChannels.VOffset)
        val u2             = regionChannel.floatData(regionOffset + ParticleChannels.U2Offset)
        val v2             = regionChannel.floatData(regionOffset + ParticleChannels.V2Offset)
        val sx             = regionChannel.floatData(regionOffset + ParticleChannels.HalfWidthOffset) * scale
        val sy             = regionChannel.floatData(regionOffset + ParticleChannels.HalfHeightOffset) * scale
        val r              = colorChannel.floatData(colorOffset + ParticleChannels.RedOffset)
        val g              = colorChannel.floatData(colorOffset + ParticleChannels.GreenOffset)
        val b              = colorChannel.floatData(colorOffset + ParticleChannels.BlueOffset)
        val a              = colorChannel.floatData(colorOffset + ParticleChannels.AlphaOffset)
        val cosRotation    = rotationChannel.floatData(rotationOffset + ParticleChannels.CosineOffset)
        val sinRotation    = rotationChannel.floatData(rotationOffset + ParticleChannels.SineOffset)

        // bottom left, bottom right, top right, top left
        putVertexGPU(vertices, baseOffset, px, py, pz, u, v2, -sx, -sy, cosRotation, sinRotation, r, g, b, a)
        baseOffset += currentVertexSize
        putVertexGPU(vertices, baseOffset, px, py, pz, u2, v2, sx, -sy, cosRotation, sinRotation, r, g, b, a)
        baseOffset += currentVertexSize
        putVertexGPU(vertices, baseOffset, px, py, pz, u2, v, sx, sy, cosRotation, sinRotation, r, g, b, a)
        baseOffset += currentVertexSize
        putVertexGPU(vertices, baseOffset, px, py, pz, u, v, -sx, sy, cosRotation, sinRotation, r, g, b, a)
        baseOffset += currentVertexSize

        p += 1
        tp += 1
      }
    }
  }

  /*
   * private void fillVerticesToParticleDirectionGPU (int[] particlesOffset) { int tp=0; for(BillboardControllerRenderData data :
   * renderData){ FloatChannel scaleChannel = data.scaleChannel; FloatChannel regionChannel = data.regionChannel; FloatChannel
   * positionChannel = data.positionChannel; FloatChannel colorChannel = data.colorChannel; FloatChannel rotationChannel =
   * data.rotationChannel;
   *
   * for(int p=0, c = data.controller.particles.size; p < c; ++p, ++tp){ int baseOffset =
   * particlesOffset[tp]*currentVertexSize*4; float scale = scaleChannel.data[p* scaleChannel.strideSize]; int regionOffset =
   * p*regionChannel.strideSize; int positionOffset = p*positionChannel.strideSize; int colorOffset = p*colorChannel.strideSize;
   * int rotationOffset = p*rotationChannel.strideSize; int velocityOffset = p* velocityChannel.strideSize; float px =
   * positionChannel.data[positionOffset + ParticleChannels.XOffset], py = positionChannel.data[positionOffset +
   * ParticleChannels.YOffset], pz = positionChannel.data[positionOffset + ParticleChannels.ZOffset]; float u =
   * regionChannel.data[regionOffset +ParticleChannels.UOffset]; float v = regionChannel.data[regionOffset
   * +ParticleChannels.VOffset]; float u2 = regionChannel.data[regionOffset +ParticleChannels.U2Offset]; float v2 =
   * regionChannel.data[regionOffset +ParticleChannels.V2Offset]; float sx = regionChannel.data[regionOffset
   * +ParticleChannels.HalfWidthOffset] * scale, sy = regionChannel.data[regionOffset+ParticleChannels.HalfHeightOffset] * scale;
   * float r = colorChannel.data[colorOffset +ParticleChannels.RedOffset]; float g = colorChannel.data[colorOffset
   * +ParticleChannels.GreenOffset]; float b = colorChannel.data[colorOffset +ParticleChannels.BlueOffset]; float a =
   * colorChannel.data[colorOffset +ParticleChannels.AlphaOffset]; float cosRotation = rotationChannel.data[rotationOffset
   * +ParticleChannels.CosineOffset]; float sinRotation = rotationChannel.data[rotationOffset +ParticleChannels.SineOffset];
   * float vx = velocityChannel.data[velocityOffset + ParticleChannels.XOffset], vy = velocityChannel.data[velocityOffset +
   * ParticleChannels.YOffset], vz = velocityChannel.data[velocityOffset + ParticleChannels.ZOffset];
   *
   * //bottom left, bottom right, top right, top left TMP_V1.set(vx, vy, vz).nor(); putVertex(vertices, baseOffset, px, py, pz,
   * u, v2, -sx, -sy, cosRotation, sinRotation, r, g, b, a, TMP_V1); baseOffset += currentVertexSize; putVertex(vertices,
   * baseOffset, px, py, pz, u2, v2, sx, -sy, cosRotation, sinRotation, r, g, b, a, TMP_V1); baseOffset += currentVertexSize;
   * putVertex(vertices, baseOffset, px, py, pz, u2, v, sx, sy, cosRotation, sinRotation, r, g, b, a, TMP_V1); baseOffset +=
   * currentVertexSize; putVertex(vertices, baseOffset, px, py, pz, u, v, -sx, sy, cosRotation, sinRotation, r, g, b, a, TMP_V1);
   * } } }
   *
   * private void fillVerticesToParticleDirectionCPU (int[] particlesOffset) { int tp=0; for(ParticleController controller :
   * renderData){ FloatChannel scaleChannel = controller.particles.getChannel(ParticleChannels.Scale); FloatChannel regionChannel
   * = controller.particles.getChannel(ParticleChannels.TextureRegion); FloatChannel positionChannel =
   * controller.particles.getChannel(ParticleChannels.Position); FloatChannel colorChannel =
   * controller.particles.getChannel(ParticleChannels.Color); FloatChannel rotationChannel =
   * controller.particles.getChannel(ParticleChannels.Rotation2D); FloatChannel velocityChannel =
   * controller.particles.getChannel(ParticleChannels.Accelleration);
   *
   * for(int p=0, c = controller.particles.size; p < c; ++p, ++tp){ int baseOffset = particlesOffset[tp]*currentVertexSize*4;
   * float scale = scaleChannel.data[p* scaleChannel.strideSize]; int regionOffset = p*regionChannel.strideSize; int
   * positionOffset = p*positionChannel.strideSize; int colorOffset = p*colorChannel.strideSize; int rotationOffset =
   * p*rotationChannel.strideSize; int velocityOffset = p* velocityChannel.strideSize; float px =
   * positionChannel.data[positionOffset + ParticleChannels.XOffset], py = positionChannel.data[positionOffset +
   * ParticleChannels.YOffset], pz = positionChannel.data[positionOffset + ParticleChannels.ZOffset]; float u =
   * regionChannel.data[regionOffset +ParticleChannels.UOffset]; float v = regionChannel.data[regionOffset
   * +ParticleChannels.VOffset]; float u2 = regionChannel.data[regionOffset +ParticleChannels.U2Offset]; float v2 =
   * regionChannel.data[regionOffset +ParticleChannels.V2Offset]; float sx = regionChannel.data[regionOffset
   * +ParticleChannels.HalfWidthOffset] * scale, sy = regionChannel.data[regionOffset+ParticleChannels.HalfHeightOffset] * scale;
   * float r = colorChannel.data[colorOffset +ParticleChannels.RedOffset]; float g = colorChannel.data[colorOffset
   * +ParticleChannels.GreenOffset]; float b = colorChannel.data[colorOffset +ParticleChannels.BlueOffset]; float a =
   * colorChannel.data[colorOffset +ParticleChannels.AlphaOffset]; float cosRotation = rotationChannel.data[rotationOffset
   * +ParticleChannels.CosineOffset]; float sinRotation = rotationChannel.data[rotationOffset +ParticleChannels.SineOffset];
   * float vx = velocityChannel.data[velocityOffset + ParticleChannels.XOffset], vy = velocityChannel.data[velocityOffset +
   * ParticleChannels.YOffset], vz = velocityChannel.data[velocityOffset + ParticleChannels.ZOffset]; Vector3 up =
   * TMP_V1.set(vx,vy,vz).nor(), look = TMP_V3.set(camera.position).sub(px,py,pz).nor(), //normal right =
   * TMP_V2.set(up).crs(look).nor(); //tangent look.set(right).crs(up).nor(); right.scl(sx); up.scl(sy);
   *
   * if(cosRotation != 1){ TMP_M3.setToRotation(look, cosRotation, sinRotation); putVertex(vertices, baseOffset,
   * TMP_V6.set(-TMP_V1.x-TMP_V2.x, -TMP_V1.y-TMP_V2.y, -TMP_V1.z-TMP_V2.z).mul(TMP_M3).add(px, py, pz), u, v2, r, g, b, a);
   * baseOffset += currentVertexSize; putVertex(vertices, baseOffset,TMP_V6.set(TMP_V1.x-TMP_V2.x, TMP_V1.y-TMP_V2.y,
   * TMP_V1.z-TMP_V2.z).mul(TMP_M3).add(px, py, pz), u2, v2, r, g, b, a); baseOffset += currentVertexSize; putVertex(vertices,
   * baseOffset,TMP_V6.set(TMP_V1.x+TMP_V2.x, TMP_V1.y+TMP_V2.y, TMP_V1.z+TMP_V2.z).mul(TMP_M3).add(px, py, pz), u2, v, r, g, b,
   * a); baseOffset += currentVertexSize; putVertex(vertices, baseOffset, TMP_V6.set(-TMP_V1.x+TMP_V2.x, -TMP_V1.y+TMP_V2.y,
   * -TMP_V1.z+TMP_V2.z).mul(TMP_M3).add(px, py, pz), u, v, r, g, b, a); } else { putVertex(vertices,
   * baseOffset,TMP_V6.set(-TMP_V1.x-TMP_V2.x+px, -TMP_V1.y-TMP_V2.y+py, -TMP_V1.z-TMP_V2.z+pz), u, v2, r, g, b, a); baseOffset
   * += currentVertexSize; putVertex(vertices, baseOffset,TMP_V6.set(TMP_V1.x-TMP_V2.x+px, TMP_V1.y-TMP_V2.y+py,
   * TMP_V1.z-TMP_V2.z+pz), u2, v2, r, g, b, a); baseOffset += currentVertexSize; putVertex(vertices,
   * baseOffset,TMP_V6.set(TMP_V1.x+TMP_V2.x+px, TMP_V1.y+TMP_V2.y+py, TMP_V1.z+TMP_V2.z+pz), u2, v, r, g, b, a); baseOffset +=
   * currentVertexSize; putVertex(vertices, baseOffset, TMP_V6.set(-TMP_V1.x+TMP_V2.x+px, -TMP_V1.y+TMP_V2.y+py,
   * -TMP_V1.z+TMP_V2.z+pz), u, v, r, g, b, a); } } } }
   */

  private def fillVerticesToViewPointCPU(particlesOffset: Array[Int]): Unit = {
    var tp = 0
    for (data <- renderData) {
      val scaleChannel    = data.scaleChannel
      val regionChannel   = data.regionChannel
      val positionChannel = data.positionChannel
      val colorChannel    = data.colorChannel
      val rotationChannel = data.rotationChannel

      var p = 0
      val c = data.controller.particles.size
      while (p < c) {
        var baseOffset     = particlesOffset(tp) * currentVertexSize * 4
        val scale          = scaleChannel.floatData(p * scaleChannel.strideSize)
        val regionOffset   = p * regionChannel.strideSize
        val positionOffset = p * positionChannel.strideSize
        val colorOffset    = p * colorChannel.strideSize
        val rotationOffset = p * rotationChannel.strideSize
        val px             = positionChannel.floatData(positionOffset + ParticleChannels.XOffset)
        val py             = positionChannel.floatData(positionOffset + ParticleChannels.YOffset)
        val pz             = positionChannel.floatData(positionOffset + ParticleChannels.ZOffset)
        val u              = regionChannel.floatData(regionOffset + ParticleChannels.UOffset)
        val v              = regionChannel.floatData(regionOffset + ParticleChannels.VOffset)
        val u2             = regionChannel.floatData(regionOffset + ParticleChannels.U2Offset)
        val v2             = regionChannel.floatData(regionOffset + ParticleChannels.V2Offset)
        val sx             = regionChannel.floatData(regionOffset + ParticleChannels.HalfWidthOffset) * scale
        val sy             = regionChannel.floatData(regionOffset + ParticleChannels.HalfHeightOffset) * scale
        val r              = colorChannel.floatData(colorOffset + ParticleChannels.RedOffset)
        val g              = colorChannel.floatData(colorOffset + ParticleChannels.GreenOffset)
        val b              = colorChannel.floatData(colorOffset + ParticleChannels.BlueOffset)
        val a              = colorChannel.floatData(colorOffset + ParticleChannels.AlphaOffset)
        val cosRotation    = rotationChannel.floatData(rotationOffset + ParticleChannels.CosineOffset)
        val sinRotation    = rotationChannel.floatData(rotationOffset + ParticleChannels.SineOffset)
        val look           = TMP_V3.set(camera.position).sub(px, py, pz).nor() // normal
        val right          = TMP_V1.set(camera.up).crs(look).nor() // tangent
        val up             = TMP_V2.set(look).crs(right)
        right.scl(sx)
        up.scl(sy)

        if (cosRotation != 1) {
          TMP_M3.setToRotation(look, cosRotation, sinRotation)
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(-TMP_V1.x - TMP_V2.x, -TMP_V1.y - TMP_V2.y, -TMP_V1.z - TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u,
            v2,
            r,
            g,
            b,
            a
          )
          baseOffset += currentVertexSize
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(TMP_V1.x - TMP_V2.x, TMP_V1.y - TMP_V2.y, TMP_V1.z - TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u2,
            v2,
            r,
            g,
            b,
            a
          )
          baseOffset += currentVertexSize
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(TMP_V1.x + TMP_V2.x, TMP_V1.y + TMP_V2.y, TMP_V1.z + TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u2,
            v,
            r,
            g,
            b,
            a
          )
          baseOffset += currentVertexSize
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(-TMP_V1.x + TMP_V2.x, -TMP_V1.y + TMP_V2.y, -TMP_V1.z + TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u,
            v,
            r,
            g,
            b,
            a
          )
        } else {
          putVertexCPU(vertices, baseOffset, TMP_V6.set(-TMP_V1.x - TMP_V2.x + px, -TMP_V1.y - TMP_V2.y + py, -TMP_V1.z - TMP_V2.z + pz), u, v2, r, g, b, a)
          baseOffset += currentVertexSize
          putVertexCPU(vertices, baseOffset, TMP_V6.set(TMP_V1.x - TMP_V2.x + px, TMP_V1.y - TMP_V2.y + py, TMP_V1.z - TMP_V2.z + pz), u2, v2, r, g, b, a)
          baseOffset += currentVertexSize
          putVertexCPU(vertices, baseOffset, TMP_V6.set(TMP_V1.x + TMP_V2.x + px, TMP_V1.y + TMP_V2.y + py, TMP_V1.z + TMP_V2.z + pz), u2, v, r, g, b, a)
          baseOffset += currentVertexSize
          putVertexCPU(vertices, baseOffset, TMP_V6.set(-TMP_V1.x + TMP_V2.x + px, -TMP_V1.y + TMP_V2.y + py, -TMP_V1.z + TMP_V2.z + pz), u, v, r, g, b, a)
        }

        p += 1
        tp += 1
      }
    }
  }

  private def fillVerticesToScreenCPU(particlesOffset: Array[Int]): Unit = {
    val look  = TMP_V3.set(camera.direction).scl(-1) // normal
    val right = TMP_V4.set(camera.up).crs(look).nor() // tangent
    val up    = camera.up

    var tp = 0
    for (data <- renderData) {
      val scaleChannel    = data.scaleChannel
      val regionChannel   = data.regionChannel
      val positionChannel = data.positionChannel
      val colorChannel    = data.colorChannel
      val rotationChannel = data.rotationChannel

      var p = 0
      val c = data.controller.particles.size
      while (p < c) {
        var baseOffset     = particlesOffset(tp) * currentVertexSize * 4
        val scale          = scaleChannel.floatData(p * scaleChannel.strideSize)
        val regionOffset   = p * regionChannel.strideSize
        val positionOffset = p * positionChannel.strideSize
        val colorOffset    = p * colorChannel.strideSize
        val rotationOffset = p * rotationChannel.strideSize
        val px             = positionChannel.floatData(positionOffset + ParticleChannels.XOffset)
        val py             = positionChannel.floatData(positionOffset + ParticleChannels.YOffset)
        val pz             = positionChannel.floatData(positionOffset + ParticleChannels.ZOffset)
        val u              = regionChannel.floatData(regionOffset + ParticleChannels.UOffset)
        val v              = regionChannel.floatData(regionOffset + ParticleChannels.VOffset)
        val u2             = regionChannel.floatData(regionOffset + ParticleChannels.U2Offset)
        val v2             = regionChannel.floatData(regionOffset + ParticleChannels.V2Offset)
        val sx             = regionChannel.floatData(regionOffset + ParticleChannels.HalfWidthOffset) * scale
        val sy             = regionChannel.floatData(regionOffset + ParticleChannels.HalfHeightOffset) * scale
        val r              = colorChannel.floatData(colorOffset + ParticleChannels.RedOffset)
        val g              = colorChannel.floatData(colorOffset + ParticleChannels.GreenOffset)
        val b              = colorChannel.floatData(colorOffset + ParticleChannels.BlueOffset)
        val a              = colorChannel.floatData(colorOffset + ParticleChannels.AlphaOffset)
        val cosRotation    = rotationChannel.floatData(rotationOffset + ParticleChannels.CosineOffset)
        val sinRotation    = rotationChannel.floatData(rotationOffset + ParticleChannels.SineOffset)
        TMP_V1.set(right).scl(sx)
        TMP_V2.set(up).scl(sy)

        if (cosRotation != 1) {
          TMP_M3.setToRotation(look, cosRotation, sinRotation)
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(-TMP_V1.x - TMP_V2.x, -TMP_V1.y - TMP_V2.y, -TMP_V1.z - TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u,
            v2,
            r,
            g,
            b,
            a
          )
          baseOffset += currentVertexSize
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(TMP_V1.x - TMP_V2.x, TMP_V1.y - TMP_V2.y, TMP_V1.z - TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u2,
            v2,
            r,
            g,
            b,
            a
          )
          baseOffset += currentVertexSize
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(TMP_V1.x + TMP_V2.x, TMP_V1.y + TMP_V2.y, TMP_V1.z + TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u2,
            v,
            r,
            g,
            b,
            a
          )
          baseOffset += currentVertexSize
          putVertexCPU(
            vertices,
            baseOffset,
            TMP_V6.set(-TMP_V1.x + TMP_V2.x, -TMP_V1.y + TMP_V2.y, -TMP_V1.z + TMP_V2.z).mul(TMP_M3).add(px, py, pz),
            u,
            v,
            r,
            g,
            b,
            a
          )
        } else {
          putVertexCPU(vertices, baseOffset, TMP_V6.set(-TMP_V1.x - TMP_V2.x + px, -TMP_V1.y - TMP_V2.y + py, -TMP_V1.z - TMP_V2.z + pz), u, v2, r, g, b, a)
          baseOffset += currentVertexSize
          putVertexCPU(vertices, baseOffset, TMP_V6.set(TMP_V1.x - TMP_V2.x + px, TMP_V1.y - TMP_V2.y + py, TMP_V1.z - TMP_V2.z + pz), u2, v2, r, g, b, a)
          baseOffset += currentVertexSize
          putVertexCPU(vertices, baseOffset, TMP_V6.set(TMP_V1.x + TMP_V2.x + px, TMP_V1.y + TMP_V2.y + py, TMP_V1.z + TMP_V2.z + pz), u2, v, r, g, b, a)
          baseOffset += currentVertexSize
          putVertexCPU(vertices, baseOffset, TMP_V6.set(-TMP_V1.x + TMP_V2.x + px, -TMP_V1.y + TMP_V2.y + py, -TMP_V1.z + TMP_V2.z + pz), u, v, r, g, b, a)
        }

        p += 1
        tp += 1
      }
    }
  }

  override protected def flush(offsets: Array[Int]): Unit = {

    // fill vertices
    if (_useGPU) {
      // if(mode != AlignMode.ParticleDirection)
      fillVerticesGPU(offsets)
      // else
      // fillVerticesToParticleDirectionGPU(offsets);
    } else {
      if (_mode == BillboardParticleBatch.AlignMode.Screen)
        fillVerticesToScreenCPU(offsets)
      else if (_mode == BillboardParticleBatch.AlignMode.ViewPoint) fillVerticesToViewPointCPU(offsets)
      // else
      // fillVerticesToParticleDirectionCPU(offsets);
    }

    // send vertices to meshes
    var addedVertexCount = 0
    val vCount           = bufferedParticlesCount * 4
    var v                = 0
    while (v < vCount) {
      addedVertexCount = Math.min(vCount - v, MAX_VERTICES_PER_MESH)
      val renderable = renderablePool.obtain()
      renderable.meshPart.size = (addedVertexCount / 4) * 6
      renderable.meshPart.mesh.setVertices(vertices, currentVertexSize * v, currentVertexSize * addedVertexCount)
      renderable.meshPart.update()
      renderables.add(renderable)
      v += addedVertexCount
    }
  }

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    for (renderable <- this.renderables)
      renderables.add(pool.obtain().set(renderable))

  override def save(manager: _root_.sge.assets.AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.createSaveData("billboardBatch")
    data.save("cfg", new BillboardParticleBatch.Config(_useGPU, _mode))
    data.saveAsset(manager.getAssetFileName(texture), classOf[Texture])
  }

  override def load(manager: _root_.sge.assets.AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.getSaveData("billboardBatch")
    data.foreach { d =>
      d.loadAsset().foreach { asset =>
        setTexture(manager.get(asset.fileName, asset.`type`).asInstanceOf[Texture])
      }
      d.load[BillboardParticleBatch.Config]("cfg").foreach { cfg =>
        setUseGpu(cfg.useGPU)
        setAlignMode(cfg.mode)
      }
    }
  }
}

object BillboardParticleBatch {

  protected val TMP_V1: Vector3 = new Vector3()
  protected val TMP_V2: Vector3 = new Vector3()
  protected val TMP_V3: Vector3 = new Vector3()
  protected val TMP_V4: Vector3 = new Vector3()
  protected val TMP_V5: Vector3 = new Vector3()
  protected val TMP_V6: Vector3 = new Vector3()
  protected val TMP_M3: Matrix3 = new Matrix3()

  // Attributes
  protected val sizeAndRotationUsage: Int = 1 << 9
  protected val directionUsage:       Int = 1 << 10

  private val GPU_ATTRIBUTES: VertexAttributes = new VertexAttributes(
    new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
    new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
    new VertexAttribute(Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
    new VertexAttribute(sizeAndRotationUsage, 4, "a_sizeAndRotation")
  )
  /*
   * GPU_EXT_ATTRIBUTES = new VertexAttributes(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE), new
   * VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE+"0"), new VertexAttribute(Usage.Color, 4,
   * ShaderProgram.COLOR_ATTRIBUTE), new VertexAttribute(sizeAndRotationUsage, 4, "a_sizeAndRotation"), new
   * VertexAttribute(directionUsage, 3, "a_direction")),
   */
  private val CPU_ATTRIBUTES: VertexAttributes = new VertexAttributes(
    new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
    new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
    new VertexAttribute(Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE)
  )

  // Offsets
  private val GPU_POSITION_OFFSET:      Int = GPU_ATTRIBUTES.getOffset(Usage.Position)
  private val GPU_UV_OFFSET:            Int = GPU_ATTRIBUTES.getOffset(Usage.TextureCoordinates)
  private val GPU_SIZE_ROTATION_OFFSET: Int = GPU_ATTRIBUTES.getOffset(sizeAndRotationUsage)
  private val GPU_COLOR_OFFSET:         Int = GPU_ATTRIBUTES.getOffset(Usage.ColorUnpacked)
  private val GPU_VERTEX_SIZE:          Int = GPU_ATTRIBUTES.vertexSize / 4

  // Ext
  /*
   * GPU_EXT_POSITION_OFFSET = (short)(GPU_EXT_ATTRIBUTES.findByUsage(Usage.Position).offset/4), GPU_EXT_UV_OFFSET =
   * (short)(GPU_EXT_ATTRIBUTES.findByUsage(Usage.TextureCoordinates).offset/4), GPU_EXT_SIZE_ROTATION_OFFSET =
   * (short)(GPU_EXT_ATTRIBUTES.findByUsage(sizeAndRotationUsage).offset/4), GPU_EXT_COLOR_OFFSET =
   * (short)(GPU_EXT_ATTRIBUTES.findByUsage(Usage.Color).offset/4), GPU_EXT_DIRECTION_OFFSET =
   * (short)(GPU_EXT_ATTRIBUTES.findByUsage(directionUsage).offset/4), GPU_EXT_VERTEX_SIZE = GPU_EXT_ATTRIBUTES.vertexSize/4,
   */

  // Cpu
  private val CPU_POSITION_OFFSET: Int = CPU_ATTRIBUTES.getOffset(Usage.Position)
  private val CPU_UV_OFFSET:       Int = CPU_ATTRIBUTES.getOffset(Usage.TextureCoordinates)
  private val CPU_COLOR_OFFSET:    Int = CPU_ATTRIBUTES.getOffset(Usage.ColorUnpacked)
  private val CPU_VERTEX_SIZE:     Int = CPU_ATTRIBUTES.vertexSize / 4

  private val MAX_PARTICLES_PER_MESH: Int = Short.MaxValue / 4
  private val MAX_VERTICES_PER_MESH:  Int = MAX_PARTICLES_PER_MESH * 4

  private class RenderablePool(batch: BillboardParticleBatch)(using Sge) extends Pool[Renderable] {
    override protected val max:             Int = Int.MaxValue
    override protected val initialCapacity: Int = 16

    override def newObject(): Renderable =
      batch.allocRenderable()
  }

  class Config(
    var useGPU: Boolean,
    var mode:   AlignMode
  ) {
    def this() = this(false, AlignMode.Screen)
  }

  enum AlignMode {
    case Screen, ViewPoint // , ParticleDirection
  }
}
