/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/ModelBatch.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Disposable -> AutoCloseable (dispose -> close).
 *   - Java single 3-arg constructor with null checks -> Scala primary constructor + Nullable overloads.
 *   - FlushablePool -> Pool.Default with Pool.Flushable mixin.
 *   - RenderablePool.obtain: meshPart.set uses Nullable.empty for id (Java uses "").
 *   - camera: Nullable[Camera] (Java Camera, nullable).
 *   - All constructors match: 3-arg, context+provider, context+sorter, context, provider+sorter,
 *     sorter, provider, FileHandle pair, String pair, no-arg.
 *   - All public methods present: begin, setCamera, getCamera, ownsRenderContext, getRenderContext,
 *     getShaderProvider, getRenderableSorter, flush, end, render (8 overloads), close.
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d

import sge.files.FileHandle
import sge.graphics.PrimitiveMode
import sge.graphics.g3d.utils.DefaultRenderableSorter
import sge.graphics.g3d.utils.DefaultShaderProvider
import sge.graphics.g3d.utils.DefaultTextureBinder
import sge.graphics.g3d.utils.RenderContext
import sge.graphics.g3d.utils.RenderableSorter
import sge.graphics.g3d.utils.ShaderProvider
import sge.utils.{ DynamicArray, Nullable, Pool, SgeError }

/** Batches [[Renderable]] instances, fetches [[Shader]]s for them, sorts them and then renders them. Fetching the shaders is done using a [[ShaderProvider]], which defaults to
  * [[DefaultShaderProvider]]. Sorting the renderables is done using a [[RenderableSorter]], which default to [[DefaultRenderableSorter]].
  *
  * The OpenGL context between the [[begin]] and [[end]] call is maintained by the [[RenderContext]].
  *
  * To provide multiple [[Renderable]]s at once a [[RenderableProvider]] can be used, e.g. a ModelInstance.
  *
  * @author
  *   xoppa, badlogic
  */
class ModelBatch(
  /** the [[RenderContext]] * */
  val context:            RenderContext,
  private val ownContext: Boolean,
  /** the [[ShaderProvider]], provides [[Shader]] instances for Renderables * */
  val shaderProvider: ShaderProvider,
  /** the [[RenderableSorter]] * */
  val sorter: RenderableSorter
) extends AutoCloseable {

  protected var camera:          Nullable[Camera]          = Nullable.empty
  protected val renderablesPool: ModelBatch.RenderablePool = ModelBatch.RenderablePool()

  /** list of Renderables to be rendered in the current batch * */
  protected val renderables: DynamicArray[Renderable] = DynamicArray[Renderable]()

  /** Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
    * @param context
    *   The [[RenderContext]] to use.
    * @param shaderProvider
    *   The [[ShaderProvider]] to use, will be disposed when this ModelBatch is disposed.
    * @param sorter
    *   The [[RenderableSorter]] to use.
    */
  def this(
    context:        Nullable[RenderContext],
    shaderProvider: Nullable[ShaderProvider],
    sorter:         Nullable[RenderableSorter]
  )(using Sge) = {
    this(
      context.getOrElse(RenderContext(DefaultTextureBinder(DefaultTextureBinder.LRU, 1))),
      context.isEmpty,
      shaderProvider.getOrElse(DefaultShaderProvider()),
      sorter.getOrElse(DefaultRenderableSorter())
    )
  }

  /** Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
    * @param context
    *   The [[RenderContext]] to use.
    * @param shaderProvider
    *   The [[ShaderProvider]] to use, will be disposed when this ModelBatch is disposed.
    */
  def this(context: RenderContext, shaderProvider: ShaderProvider)(using Sge) = {
    this(Nullable(context), Nullable(shaderProvider), Nullable.empty)
  }

  /** Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
    * @param context
    *   The [[RenderContext]] to use.
    * @param sorter
    *   The [[RenderableSorter]] to use.
    */
  def this(context: RenderContext, sorter: RenderableSorter)(using Sge) = {
    this(Nullable(context), Nullable.empty, Nullable(sorter))
  }

  /** Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
    * @param context
    *   The [[RenderContext]] to use.
    */
  def this(context: RenderContext)(using Sge) = {
    this(Nullable(context), Nullable.empty, Nullable.empty)
  }

  /** Construct a ModelBatch
    * @param shaderProvider
    *   The [[ShaderProvider]] to use, will be disposed when this ModelBatch is disposed.
    * @param sorter
    *   The [[RenderableSorter]] to use.
    */
  def this(shaderProvider: ShaderProvider, sorter: RenderableSorter)(using Sge) = {
    this(Nullable.empty, Nullable(shaderProvider), Nullable(sorter))
  }

  /** Construct a ModelBatch
    * @param sorter
    *   The [[RenderableSorter]] to use.
    */
  def this(sorter: RenderableSorter)(using Sge) = {
    this(Nullable.empty, Nullable.empty, Nullable(sorter))
  }

  /** Construct a ModelBatch
    * @param shaderProvider
    *   The [[ShaderProvider]] to use, will be disposed when this ModelBatch is disposed.
    */
  def this(shaderProvider: ShaderProvider)(using Sge) = {
    this(Nullable.empty, Nullable(shaderProvider), Nullable.empty)
  }

  /** Construct a ModelBatch with the default implementation and the specified ubershader. See [[DefaultShader]] for more information about using a custom ubershader. Requires OpenGL ES 2.0.
    * @param vertexShader
    *   The [[FileHandle]] of the vertex shader to use.
    * @param fragmentShader
    *   The [[FileHandle]] of the fragment shader to use.
    */
  def this(vertexShader: FileHandle, fragmentShader: FileHandle)(using Sge) = {
    this(Nullable.empty, Nullable(DefaultShaderProvider(vertexShader, fragmentShader)), Nullable.empty)
  }

  /** Construct a ModelBatch with the default implementation and the specified ubershader. See [[DefaultShader]] for more information about using a custom ubershader. Requires OpenGL ES 2.0.
    * @param vertexShader
    *   The vertex shader to use.
    * @param fragmentShader
    *   The fragment shader to use.
    */
  def this(vertexShader: String, fragmentShader: String)(using Sge) = {
    this(Nullable.empty, Nullable(DefaultShaderProvider(vertexShader, fragmentShader)), Nullable.empty)
  }

  /** Construct a ModelBatch with the default implementation */
  def this()(using Sge) = {
    this(Nullable.empty, Nullable.empty, Nullable.empty)
  }

  /** Start rendering one or more [[Renderable]]s. Use one of the render() methods to provide the renderables. Must be followed by a call to [[end]]. The OpenGL context must not be altered between
    * [[begin]] and [[end]].
    * @param cam
    *   The [[Camera]] to be used when rendering and sorting.
    */
  def begin(cam: Camera): Unit = {
    if (camera.isDefined) throw SgeError.InvalidInput("Call end() first.")
    camera = Nullable(cam)
    if (ownContext) context.begin()
  }

  /** Change the camera in between [[begin]] and [[end]]. This causes the batch to be flushed. Can only be called after the call to [[begin]] and before the call to [[end]].
    * @param cam
    *   The new camera to use.
    */
  def setCamera(cam: Camera): Unit = {
    if (camera.isEmpty) throw SgeError.InvalidInput("Call begin() first.")
    if (renderables.nonEmpty) flush()
    camera = Nullable(cam)
  }

  /** Provides access to the current camera in between [[begin]] and [[end]]. Do not change the camera's values. Use [[setCamera]], if you need to change the camera.
    * @return
    *   The current camera being used or Nullable.empty if called outside [[begin]] and [[end]].
    */
  def getCamera: Nullable[Camera] = camera

  /** Checks whether the [[RenderContext]] returned by [[getRenderContext]] is owned and managed by this ModelBatch. When the RenderContext isn't owned by the ModelBatch, you are responsible for
    * calling the [[RenderContext.begin]] and [[RenderContext.end]] methods yourself, as well as disposing the RenderContext.
    * @return
    *   True if this ModelBatch owns the RenderContext, false otherwise.
    */
  def ownsRenderContext: Boolean = ownContext

  /** @return the [[RenderContext]] used by this ModelBatch. */
  def getRenderContext: RenderContext = context

  /** @return the [[ShaderProvider]] used by this ModelBatch. */
  def getShaderProvider: ShaderProvider = shaderProvider

  /** @return the [[RenderableSorter]] used by this ModelBatch. */
  def getRenderableSorter: RenderableSorter = sorter

  /** Flushes the batch, causing all [[Renderable]]s in the batch to be rendered. Can only be called after the call to [[begin]] and before the call to [[end]].
    */
  def flush(): Unit = {
    camera.foreach { cam =>
      sorter.sort(cam, renderables)
      var currentShader: Nullable[Shader] = Nullable.empty
      var i = 0
      while (i < renderables.size) {
        val renderable = renderables(i)
        if (currentShader != renderable.shader) {
          currentShader.foreach(_.end())
          currentShader = renderable.shader
          currentShader.foreach(_.begin(cam, context))
        }
        currentShader.foreach(_.render(renderable))
        i += 1
      }
      currentShader.foreach(_.end())
    }
    renderablesPool.flush()
    renderables.clear()
  }

  /** End rendering one or more [[Renderable]]s. Must be called after a call to [[begin]]. This will flush the batch, causing any renderables provided using one of the render() methods to be rendered.
    * After a call to this method the OpenGL context can be altered again.
    */
  def end(): Unit = {
    flush()
    if (ownContext) context.end()
    camera = Nullable.empty
  }

  /** Add a single [[Renderable]] to the batch. The [[ShaderProvider]] will be used to fetch a suitable [[Shader]]. Can only be called after a call to [[begin]] and before a call to [[end]].
    * @param renderable
    *   The [[Renderable]] to be added.
    */
  def render(renderable: Renderable): Unit = {
    renderable.shader = Nullable(shaderProvider.getShader(renderable))
    renderables.add(renderable)
  }

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Can only be called after a call to [[begin]] and before a call to
    * [[end]].
    * @param renderableProvider
    *   the renderable provider
    */
  def render(renderableProvider: RenderableProvider): Unit = {
    val offset = renderables.size
    renderableProvider.getRenderables(renderables, renderablesPool)
    var i = offset
    while (i < renderables.size) {
      val renderable = renderables(i)
      renderable.shader = Nullable(shaderProvider.getShader(renderable))
      i += 1
    }
  }

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Can only be called after a call to [[begin]] and before a call to
    * [[end]].
    * @param renderableProviders
    *   one or more renderable providers
    */
  def render[T <: RenderableProvider](renderableProviders: Iterable[T]): Unit =
    for (renderableProvider <- renderableProviders)
      render(renderableProvider)

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Any environment set on the returned renderables will be replaced
    * with the given environment. Can only be called after a call to [[begin]] and before a call to [[end]].
    * @param renderableProvider
    *   the renderable provider
    * @param environment
    *   the [[Environment]] to use for the renderables
    */
  def render(renderableProvider: RenderableProvider, environment: Environment): Unit = {
    val offset = renderables.size
    renderableProvider.getRenderables(renderables, renderablesPool)
    var i = offset
    while (i < renderables.size) {
      val renderable = renderables(i)
      renderable.environment = Nullable(environment)
      renderable.shader = Nullable(shaderProvider.getShader(renderable))
      i += 1
    }
  }

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Any environment set on the returned renderables will be replaced
    * with the given environment. Can only be called after a call to [[begin]] and before a call to [[end]].
    * @param renderableProviders
    *   one or more renderable providers
    * @param environment
    *   the [[Environment]] to use for the renderables
    */
  def render[T <: RenderableProvider](renderableProviders: Iterable[T], environment: Environment): Unit =
    for (renderableProvider <- renderableProviders)
      render(renderableProvider, environment)

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Any shaders set on the returned renderables will be replaced with
    * the given [[Shader]]. Can only be called after a call to [[begin]] and before a call to [[end]].
    * @param renderableProvider
    *   the renderable provider
    * @param shader
    *   the shader to use for the renderables
    */
  def render(renderableProvider: RenderableProvider, shader: Shader): Unit = {
    val offset = renderables.size
    renderableProvider.getRenderables(renderables, renderablesPool)
    var i = offset
    while (i < renderables.size) {
      val renderable = renderables(i)
      renderable.shader = Nullable(shader)
      renderable.shader = Nullable(shaderProvider.getShader(renderable))
      i += 1
    }
  }

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Any shaders set on the returned renderables will be replaced with
    * the given [[Shader]]. Can only be called after a call to [[begin]] and before a call to [[end]].
    * @param renderableProviders
    *   one or more renderable providers
    * @param shader
    *   the shader to use for the renderables
    */
  def render[T <: RenderableProvider](renderableProviders: Iterable[T], shader: Shader): Unit =
    for (renderableProvider <- renderableProviders)
      render(renderableProvider, shader)

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Any environment set on the returned renderables will be replaced
    * with the given environment. Any shaders set on the returned renderables will be replaced with the given [[Shader]]. Can only be called after a call to [[begin]] and before a call to [[end]].
    * @param renderableProvider
    *   the renderable provider
    * @param environment
    *   the [[Environment]] to use for the renderables
    * @param shader
    *   the shader to use for the renderables
    */
  def render(renderableProvider: RenderableProvider, environment: Environment, shader: Shader): Unit = {
    val offset = renderables.size
    renderableProvider.getRenderables(renderables, renderablesPool)
    var i = offset
    while (i < renderables.size) {
      val renderable = renderables(i)
      renderable.environment = Nullable(environment)
      renderable.shader = Nullable(shader)
      renderable.shader = Nullable(shaderProvider.getShader(renderable))
      i += 1
    }
  }

  /** Calls [[RenderableProvider.getRenderables]] and adds all returned [[Renderable]] instances to the current batch to be rendered. Any environment set on the returned renderables will be replaced
    * with the given environment. Any shaders set on the returned renderables will be replaced with the given [[Shader]]. Can only be called after a call to [[begin]] and before a call to [[end]].
    * @param renderableProviders
    *   one or more renderable providers
    * @param environment
    *   the [[Environment]] to use for the renderables
    * @param shader
    *   the shader to use for the renderables
    */
  def render[T <: RenderableProvider](
    renderableProviders: Iterable[T],
    environment:         Environment,
    shader:              Shader
  ): Unit =
    for (renderableProvider <- renderableProviders)
      render(renderableProvider, environment, shader)

  override def close(): Unit =
    shaderProvider.close()
}

object ModelBatch {

  protected class RenderablePool extends Pool.Default[Renderable](() => Renderable()) with Pool.Flushable[Renderable] {

    override def obtain(): Renderable = {
      val renderable = super.obtain()
      renderable.environment = Nullable.empty
      renderable.material = Nullable.empty
      renderable.meshPart.set(Nullable.empty, null, 0, 0, PrimitiveMode(0))
      renderable.shader = Nullable.empty
      renderable.userData = Nullable.empty
      renderable
    }
  }
}
