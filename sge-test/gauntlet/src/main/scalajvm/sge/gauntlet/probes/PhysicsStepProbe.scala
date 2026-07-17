/*
 * SGE Gauntlet — ext/physics: Rapier2D deterministic gravity step. ISS-796.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.physics.{ BodyType, PhysicsWorld, Shape }

import scala.collection.mutable.ListBuffer

/** Steps a dynamic body under gravity through a fixed number of fixed-dt Rapier2D steps and asserts it reaches the analytically expected fall distance (semi-implicit Euler, g=-10, 60 steps of 1/60s →
  * ~5.08m drop) within tolerance, that its velocity approaches terminal, and — the key property — that two independently-built worlds run to bit-close identical results (deterministic simulation).
  * Non-GPU: this drives the JVM Panama physics backend directly (the same seam the physics integration suite uses), no display required.
  */
object PhysicsStepProbe extends FeatureProbe {

  override def id: String = "ext/physics-step"

  override def area: String = "ext/physics"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val Steps  = 60
  private val Dt     = 1f / 60f
  private val StartY = 100f

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  /** Runs a fresh world, returns (finalY, finalVy, midY after half the steps). */
  private def simulate(): (Float, Float, Float) = {
    val world = new PhysicsWorld(0f, -10f)
    try {
      val body = world.createBody(BodyType.Dynamic, x = 0f, y = StartY)
      body.attachCollider(Shape.Circle(0.5f))
      var midY = StartY
      var i    = 0
      while (i < Steps) {
        world.step(Dt)
        if (i == Steps / 2 - 1) midY = body.position._2
        i += 1
      }
      val (_, finalY) = body.position
      val (_, finalV) = body.linearVelocity
      (finalY, finalV, midY)
    } finally world.close()
  }

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    // Gravity vector round-trips.
    val probe = new PhysicsWorld(0f, -10f)
    try {
      val (gx, gy) = probe.gravity
      checks += Check.cond("gravity-vector", Math.abs(gx) <= 0.001f && Math.abs(gy + 10f) <= 0.001f, "gravity (0,-10)", s"($gx,$gy)")
    } finally probe.close()

    val (finalY, finalVy, midY) = simulate()
    ctx.log(s"fall: startY=$StartY midY=$midY finalY=$finalY finalVy=$finalVy")

    val drop = StartY - finalY
    checks += Check.cond("body-falls-under-gravity", finalY < StartY - 4f, "finalY < 96 (fell > 4m)", finalY.toString)
    // Analytic semi-implicit Euler drop over 1s: g*dt^2*sum(1..60) = 5.083m.
    checks += Check.cond("fall-distance-analytic", Math.abs(drop - 5.083f) <= 1.0f, "drop ~ 5.08m +-1", drop.toString)
    checks += Check.cond("velocity-approaches-terminal", Math.abs(finalVy + 10f) <= 1.0f, "vy ~ -10 +-1", finalVy.toString)
    checks += Check.cond("monotonic-descent", midY < StartY && finalY < midY, s"finalY < midY($midY) < startY", finalY.toString)

    // Determinism: a second independent world with identical setup reaches a bit-close result.
    val (finalY2, finalVy2, _) = simulate()
    checks += Check.cond(
      "deterministic-across-runs",
      Math.abs(finalY - finalY2) <= 1e-4f && Math.abs(finalVy - finalVy2) <= 1e-4f,
      s"identical finalY/finalVy across runs",
      s"finalY Δ=${Math.abs(finalY - finalY2)}, finalVy Δ=${Math.abs(finalVy - finalVy2)}"
    )
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
