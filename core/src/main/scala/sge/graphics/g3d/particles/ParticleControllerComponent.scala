/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleControllerComponent.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles

import sge.assets.AssetManager
import sge.math.{ Matrix3, Matrix4, Quaternion, Vector3 }

/** It's the base class of every {@link ParticleController} component. A component duty is to participate in one or some events during the simulation. (i.e it can handle the particles emission or
  * modify particle properties, etc.).
  * @author
  *   inferno
  */
abstract class ParticleControllerComponent extends AutoCloseable with ResourceData.Configurable {
  import ParticleControllerComponent.*

  protected var controller: ParticleController = scala.compiletime.uninitialized

  /** Called to initialize new emitted particles. */
  def activateParticles(startIndex: Int, count: Int): Unit = {}

  /** Called to notify which particles have been killed. */
  def killParticles(startIndex: Int, count: Int): Unit = {}

  /** Called to execute the component behavior. */
  def update(): Unit = {}

  /** Called once during intialization */
  def init(): Unit = {}

  /** Called at the start of the simulation. */
  def start(): Unit = {}

  /** Called at the end of the simulation. */
  def end(): Unit = {}

  override def close(): Unit = {}

  def copy(): ParticleControllerComponent

  /** Called during initialization to allocate additional particles channels */
  def allocateChannels(): Unit = {}

  def set(particleController: ParticleController): Unit =
    controller = particleController

  override def save(manager: AssetManager, data: ResourceData[?]): Unit = {}

  override def load(manager: AssetManager, data: ResourceData[?]): Unit = {}
}

object ParticleControllerComponent {
  private[particles] val TMP_V1: Vector3    = new Vector3()
  private[particles] val TMP_V2: Vector3    = new Vector3()
  private[particles] val TMP_V3: Vector3    = new Vector3()
  private[particles] val TMP_V4: Vector3    = new Vector3()
  private[particles] val TMP_V5: Vector3    = new Vector3()
  private[particles] val TMP_V6: Vector3    = new Vector3()
  private[particles] val TMP_Q:  Quaternion = new Quaternion()
  private[particles] val TMP_Q2: Quaternion = new Quaternion()
  private[particles] val TMP_M3: Matrix3    = new Matrix3()
  private[particles] val TMP_M4: Matrix4    = new Matrix4()
}
