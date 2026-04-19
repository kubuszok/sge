/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 356
 * Covenant-baseline-methods: ScreenManager,addCurrentProcessors,autoCloseScreens,autoCloseTransitions,blankScreen,clearScreen,clearTransition,close,createFrameBuffer,currFBO,currScreen,currentHeight,currentProcessors,currentScreen,currentWidth,finalizeScreen,finalizeTransition,gameInputMultiplexer,hasDepth,initBuffers,initialize,initializeScreen,initializeTransition,initialized,isTransitioning,lastFBO,lastScreen,lastScreenOption,pause,pushScreen,removeCurrentProcessors,render,renderCurrScreenToTexture,renderLastScreenToTexture,resize,resume,setAutoClose,setHasDepth,transition,transitionQueue
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue

import sge.graphics.Pixmap
import sge.graphics.g2d.TextureRegion
import sge.graphics.glutils.{ FrameBuffer, HdpiUtils }
import sge.screen.transition.ScreenTransition
import sge.screen.utils.ScreenFboUtils
import sge.utils.{ Nullable, ScreenUtils, Seconds }

/** A screen manager that handles the different screens of a game and their transitions.
  *
  * Has to be {@linkplain #initialize initialized} before it can be used.
  *
  * To actually show a screen, push it via {@link #pushScreen}.
  *
  * As the screen manager is using framebuffers internally, screens and transitions have to use nestable framebuffers if they want to use framebuffers as well!
  *
  * @author
  *   damios
  */
class ScreenManager[S <: ManagedScreen, T <: ScreenTransition](using Sge) extends AutoCloseable {

  /** This framebuffer is used to store the content of the previously active screen while a transition is played.
    */
  private var lastFBO: Nullable[FrameBuffer] = Nullable.empty

  /** This framebuffer is used to store the content of the active screen while a transition is played.
    */
  private var currFBO: Nullable[FrameBuffer] = Nullable.empty

  /** The screen that was shown before the current screen. */
  protected var lastScreen: Nullable[ManagedScreen] = Nullable.empty

  /** The current screen. */
  protected var currScreen: Nullable[ManagedScreen] = Nullable.empty

  /** The input processors of the current screen. */
  private[screen] var currentProcessors: ArrayBuffer[InputProcessor] = ArrayBuffer.empty

  /** The blank screen used internally when no screen has been pushed yet. */
  private var blankScreen: BlankScreen = scala.compiletime.uninitialized

  /** The transition effect currently rendered. */
  protected var transition: Nullable[T] = Nullable.empty

  protected val transitionQueue: Queue[(() => Nullable[T], () => S)] = Queue.empty

  private[screen] var gameInputMultiplexer: InputMultiplexer = scala.compiletime.uninitialized

  protected var currentWidth:  Pixels = Pixels.zero
  protected var currentHeight: Pixels = Pixels.zero

  private var initialized: Boolean = false

  /** Needed when the framebuffers are (re)created. */
  protected var hasDepth:             Boolean = false
  protected var autoCloseScreens:     Boolean = false
  protected var autoCloseTransitions: Boolean = false

  def initialize(gameInputMultiplexer: InputMultiplexer, screenWidth: Pixels, screenHeight: Pixels, hasDepth: Boolean): Unit = {
    this.gameInputMultiplexer = gameInputMultiplexer
    this.currentWidth = screenWidth
    this.currentHeight = screenHeight
    this.hasDepth = hasDepth
    this.blankScreen = BlankScreen()
    this.currScreen = Nullable(this.blankScreen)

    initBuffers()

    this.initialized = true
  }

  protected def initBuffers(): Unit = {
    lastFBO.foreach(_.close())
    lastFBO = Nullable(createFrameBuffer())
    currFBO.foreach(_.close())
    currFBO = Nullable(createFrameBuffer())
  }

  protected def createFrameBuffer(): FrameBuffer =
    FrameBuffer(
      Pixmap.Format.RGBA8888,
      HdpiUtils.toBackBufferX(currentWidth),
      HdpiUtils.toBackBufferY(currentHeight),
      hasDepth
    )

  /** Sets the hasDepth attribute of the internal framebuffers and recreates them.
    *
    * If you want more granular control over the framebuffers used within the screen manager, just override createFrameBuffer().
    */
  def setHasDepth(hasDepth: Boolean): Unit = {
    this.hasDepth = hasDepth
    initBuffers()
  }

  /** Enables automatic closing for screens and/or transitions. If set to true, close() is called right after hide().
    *
    * @param autoCloseScreens
    *   if true, screens are automatically closed after they are hidden; false by default
    * @param autoCloseTransitions
    *   if true, transitions are automatically closed after they are hidden; false by default
    */
  def setAutoClose(autoCloseScreens: Boolean, autoCloseTransitions: Boolean): Unit = {
    this.autoCloseScreens = autoCloseScreens
    this.autoCloseTransitions = autoCloseTransitions
  }

  /** Pushes a screen to be the active screen. If there is still a transition ongoing, the pushed one is queued. If screen and transition should be instantiated lazily, use the supplier variant. This
    * is useful when the constructors need to run on the rendering thread.
    *
    * show() is called on the pushed screen and hide() is called on the previously active screen, as soon as the transition is finished. This is always done on the rendering thread.
    *
    * If the same screen is pushed twice in a row, the second call is being ignored.
    *
    * @param screen
    *   the screen to be pushed
    * @param newTransition
    *   the transition effect; can be Nullable.empty
    */
  def pushScreen(screen: S, newTransition: Nullable[T]): Unit = {
    require(screen != null, "screen cannot be null") // scalastyle:ignore null
    pushScreen(() => screen, () => newTransition)
  }

  /** Pushes a screen to be the active screen. If there is still a transition ongoing, the pushed one is queued.
    *
    * The provided suppliers are called on the rendering thread, which is useful if the screen's or transition's constructors perform OpenGL operations.
    *
    * If the same screen is pushed twice in a row, the second call is being ignored.
    *
    * @param screenSupplier
    *   a supplier for the screen to be pushed
    * @param transitionSupplier
    *   a supplier for the transition effect; the supplier may return Nullable.empty
    */
  def pushScreen(screenSupplier: () => S, transitionSupplier: () => Nullable[T]): Unit = {
    require(screenSupplier != null, "screenSupplier cannot be null") // scalastyle:ignore null
    transitionQueue.enqueue((transitionSupplier, screenSupplier))
  }

  /** Renders the screens and transitions.
    *
    * @param delta
    *   the time delta since the last render call; in seconds
    */
  def render(delta: Seconds): Unit = {
    require(initialized, "The screen manager has to be initialized first!")

    if (transition.isEmpty) {
      if (transitionQueue.nonEmpty) {
        /* Start the next queued transition */
        val (transSupplier, screenSupplier) = transitionQueue.dequeue()
        val tmp                             = screenSupplier()
        if (Nullable(tmp: ManagedScreen) == currScreen) {
          // one can't push the same screen twice in a row
          render(delta) // render again so no frame is skipped
        } else {
          this.lastScreen = currScreen
          this.currScreen = Nullable(tmp)
          val transResult = transSupplier()
          this.transition = transResult.map(_.asInstanceOf[T])

          removeCurrentProcessors()

          initializeScreen(tmp)

          if (this.transition.isDefined) {
            initializeTransition(this.transition.get)
          } else {
            // a screen was pushed without transition
            finalizeScreen(this.lastScreen.get)
            this.lastScreen = Nullable.empty

            this.currentProcessors = ArrayBuffer.from(tmp.inputProcessors)
            addCurrentProcessors()
          }

          render(delta) // render again so no frame is skipped
        }
      } else {
        /* Render the current screen; no transition is going on */
        val cs = currScreen.get
        clearScreen(cs)
        cs.render(delta)
      }
    } else {
      val trans = this.transition.get
      if (!trans.isDone) {
        /* Render the current transition */
        clearTransition(trans)
        trans.render(
          delta,
          renderLastScreenToTexture(delta),
          renderCurrScreenToTexture(delta)
        )
      } else {
        /* The current transition is finished; remove it */
        finalizeTransition(trans)
        this.transition = Nullable.empty

        finalizeScreen(this.lastScreen.get)
        this.lastScreen = Nullable.empty

        val cs = this.currScreen.get
        this.currentProcessors = ArrayBuffer.from(cs.inputProcessors)
        addCurrentProcessors()

        render(delta) // render again so no frame is skipped
      }
    }
  }

  protected def initializeScreen(newScreen: ManagedScreen): Unit = {
    newScreen.show()
    newScreen.resize(currentWidth, currentHeight)
  }

  protected def initializeTransition(newTransition: T): Unit = {
    newTransition.show()
    newTransition.resize(currentWidth, currentHeight)
  }

  protected def finalizeScreen(oldScreen: ManagedScreen): Unit = {
    oldScreen.hide()
    if (autoCloseScreens) oldScreen.close()
  }

  protected def finalizeTransition(oldTransition: T): Unit = {
    oldTransition.hide()
    if (autoCloseTransitions) oldTransition.close()
  }

  /** Clears the screen with the given color. Separated for testability. */
  protected def clearScreen(screen: ManagedScreen): Unit =
    screen.clearColor.foreach { color =>
      ScreenUtils.clear(color, true)
    }

  /** Clears the screen for a transition. Separated for testability. */
  protected def clearTransition(trans: T): Unit =
    trans.clearColor.foreach { color =>
      ScreenUtils.clear(color, true)
    }

  /** Renders the last screen into a texture region for use during transition. Separated for testability. */
  protected def renderLastScreenToTexture(delta: Seconds): TextureRegion =
    ScreenFboUtils.screenToTexture(this.lastScreen.get, this.lastFBO.get, delta)

  /** Renders the current screen into a texture region for use during transition. Separated for testability. */
  protected def renderCurrScreenToTexture(delta: Seconds): TextureRegion =
    ScreenFboUtils.screenToTexture(this.currScreen.get, this.currFBO.get, delta)

  private[screen] def removeCurrentProcessors(): Unit =
    currentProcessors.foreach { p =>
      gameInputMultiplexer.removeProcessor(p)
    }

  private[screen] def addCurrentProcessors(): Unit =
    currentProcessors.foreach { p =>
      gameInputMultiplexer.addProcessor(p)
    }

  def resize(width: Pixels, height: Pixels): Unit = {
    require(initialized, "The screen manager has to be initialized first!")

    if (currentWidth != width || currentHeight != height) {
      currentWidth = width
      currentHeight = height

      // Resize screens & transitions
      currScreen.foreach(_.resize(width, height))
      lastScreen.foreach(_.resize(width, height))
      transition.foreach(_.resize(width, height))

      // Recreate buffers
      initBuffers()
    }
  }

  def pause(): Unit = {
    require(initialized, "The screen manager has to be initialized first!")
    lastScreen.foreach(_.pause())
    currScreen.foreach(_.pause())
  }

  def resume(): Unit = {
    require(initialized, "The screen manager has to be initialized first!")
    lastScreen.foreach(_.resume())
    currScreen.foreach(_.resume())
  }

  /** Closes the screen manager as well as any screens and transitions, which were pushed but not yet hidden, regardless of whether they already started being rendered.
    */
  override def close(): Unit = {
    // Current screens & transitions
    lastScreen.foreach { s =>
      s.close()
    }
    lastScreen = Nullable.empty

    currScreen.foreach { s =>
      s.close()
    }
    currScreen = Nullable.empty

    transition.foreach { t =>
      t.close()
    }
    transition = Nullable.empty

    // Queued screens & transitions
    transitionQueue.foreach { case (transSupplier, screenSupplier) =>
      screenSupplier().close()
      transSupplier().foreach(_.close())
    }
    transitionQueue.clear()

    // FBOs
    lastFBO.foreach(_.close())
    lastFBO = Nullable.empty

    currFBO.foreach(_.close())
    currFBO = Nullable.empty
  }

  /** @return
    *   empty if no transition is going on; otherwise returns the previous screen that is still rendered as part of the transition
    */
  def lastScreenOption: Nullable[S] =
    lastScreen.flatMap { s =>
      if (s eq blankScreen) Nullable.empty[S]
      else Nullable(s.asInstanceOf[S])
    }

  /** @return the current screen; is empty before the first screen was pushed */
  def currentScreen: Nullable[S] =
    currScreen.flatMap { s =>
      if (s eq blankScreen) Nullable.empty[S]
      else Nullable(s.asInstanceOf[S])
    }

  /** @return true when a transition is currently rendered */
  def isTransitioning: Boolean = transition.isDefined
}
