/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: JS stub — throws UnsupportedOperationException
 *   Idiom: split packages
 */
package sge
package platform

/** Scala.js stub for [[PhysicsOps3d]].
  *
  * All methods throw [[UnsupportedOperationException]]. A future implementation may integrate with the Rapier3D WASM build via JavaScript interop.
  */
private[platform] object PhysicsOpsJs3d extends PhysicsOps3d {

  private def unsupported: Nothing =
    throw new UnsupportedOperationException("PhysicsOps3d is not yet available on Scala.js")

  // World lifecycle
  override def createWorld(gravityX: Float, gravityY: Float, gravityZ: Float): Long = unsupported
  override def destroyWorld(world: Long): Unit = unsupported
  override def worldStep(world: Long, dt: Float): Unit = unsupported
  override def worldSetGravity(world: Long, gx: Float, gy: Float, gz: Float): Unit = unsupported
  override def worldGetGravity(world: Long, out: Array[Float]): Unit = unsupported

  // Rigid body
  override def createDynamicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long = unsupported
  override def createStaticBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long = unsupported
  override def createKinematicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long = unsupported
  override def destroyBody(world: Long, body: Long): Unit = unsupported
  override def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit = unsupported
  override def bodyGetRotation(world: Long, body: Long, out: Array[Float]): Unit = unsupported
  override def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit = unsupported
  override def bodyGetAngularVelocity(world: Long, body: Long, out: Array[Float]): Unit = unsupported
  override def bodySetPosition(world: Long, body: Long, x: Float, y: Float, z: Float): Unit = unsupported
  override def bodySetRotation(world: Long, body: Long, qx: Float, qy: Float, qz: Float, qw: Float): Unit = unsupported
  override def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float, vz: Float): Unit = unsupported
  override def bodySetAngularVelocity(world: Long, body: Long, wx: Float, wy: Float, wz: Float): Unit = unsupported
  override def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float, fz: Float): Unit = unsupported
  override def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float, iz: Float): Unit = unsupported
  override def bodyApplyTorque(world: Long, body: Long, tx: Float, ty: Float, tz: Float): Unit = unsupported
  override def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, fz: Float, px: Float, py: Float, pz: Float): Unit = unsupported
  override def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, iz: Float, px: Float, py: Float, pz: Float): Unit = unsupported
  override def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit = unsupported
  override def bodyGetLinearDamping(world: Long, body: Long): Float = unsupported
  override def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit = unsupported
  override def bodyGetAngularDamping(world: Long, body: Long): Float = unsupported
  override def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit = unsupported
  override def bodyGetGravityScale(world: Long, body: Long): Float = unsupported
  override def bodyIsAwake(world: Long, body: Long): Boolean = unsupported
  override def bodyWakeUp(world: Long, body: Long): Unit = unsupported
  override def bodySleep(world: Long, body: Long): Unit = unsupported
  override def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit = unsupported
  override def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit = unsupported
  override def bodyIsCcdEnabled(world: Long, body: Long): Boolean = unsupported
  override def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit = unsupported
  override def bodyIsEnabled(world: Long, body: Long): Boolean = unsupported
  override def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit = unsupported
  override def bodyGetDominanceGroup(world: Long, body: Long): Int = unsupported
  override def bodyGetMass(world: Long, body: Long): Float = unsupported
  override def bodyRecomputeMassProperties(world: Long, body: Long): Unit = unsupported

  // Collider
  override def createSphereCollider(world: Long, body: Long, radius: Float): Long = unsupported
  override def createBoxCollider(world: Long, body: Long, hx: Float, hy: Float, hz: Float): Long = unsupported
  override def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long = unsupported
  override def createCylinderCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long = unsupported
  override def createConeCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long = unsupported
  override def createConvexHullCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long = unsupported
  override def createTriMeshCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int, indices: Array[Int], indexCount: Int): Long = unsupported
  override def destroyCollider(world: Long, collider: Long): Unit = unsupported
  override def colliderSetDensity(world: Long, collider: Long, density: Float): Unit = unsupported
  override def colliderSetFriction(world: Long, collider: Long, friction: Float): Unit = unsupported
  override def colliderSetRestitution(world: Long, collider: Long, restitution: Float): Unit = unsupported
  override def colliderSetSensor(world: Long, collider: Long, sensor: Boolean): Unit = unsupported

  // Joints
  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long = unsupported
  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long = unsupported
  override def destroyJoint(world: Long, joint: Long): Unit = unsupported

  // Queries
  override def rayCast(world: Long, originX: Float, originY: Float, originZ: Float, dirX: Float, dirY: Float, dirZ: Float, maxDist: Float, out: Array[Float]): Boolean = unsupported

  // Contact events
  override def pollContactStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported
  override def pollContactStopEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported
}
