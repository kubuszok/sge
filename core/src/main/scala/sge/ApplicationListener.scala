/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/ApplicationListener.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge

/** <p> An <code>ApplicationListener</code> is called when the {@link Application} is created, resumed, rendering, paused or destroyed. All methods are called in a thread that has the OpenGL context
  * current. You can thus safely create and manipulate graphics resources. </p>
  *
  * <p> The <code>ApplicationListener</code> interface follows the standard Android activity life-cycle and is emulated on the desktop accordingly. </p>
  *
  * @author
  *   mzechner (original implementation)
  */
trait ApplicationListener {

  /** Called when the {@link Application} is first created. */
  def create(): Unit

  /** Called when the {@link Application} is resized. This can happen at any point during a non-paused state but will never happen before a call to {@link #create()} .
    *
    * @param width
    *   the new width in pixels
    * @param height
    *   the new height in pixels
    */
  def resize(width: Int, height: Int): Unit

  /** Called when the {@link Application} should render itself. */
  def render(): Unit

  /** Called when the {@link Application} is paused, usually when it's not active or visible on-screen. An Application is also paused before it is destroyed.
    */
  def pause(): Unit

  /** Called when the {@link Application} is resumed from a paused state, usually when it regains focus. */
  def resume(): Unit

  /** Called when the {@link Application} is destroyed. Preceded by a call to {@link #pause()}. */
  def dispose(): Unit
}
