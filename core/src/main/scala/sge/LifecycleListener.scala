/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/LifecycleListener.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge

/** A LifecycleListener can be added to an {@link Application} via {@link Application#addLifecycleListener(LifecycleListener)} . It will receive notification of pause, resume and dispose events. This
  * is mainly meant to be used by extensions that need to manage resources based on the life-cycle. Normal, application level development should rely on the {@link ApplicationListener} interface. </p>
  *
  * The methods will be invoked on the rendering thread. The methods will be executed before the {@link ApplicationListener} methods are executed.
  *
  * @author
  *   mzechner (original implementation)
  */
trait LifecycleListener {

  /** Called when the {@link Application} is about to pause */
  def pause(): Unit

  /** Called when the Application is about to be resumed */
  def resume(): Unit

  /** Called when the {@link Application} is about to be disposed */
  def dispose(): Unit
}
