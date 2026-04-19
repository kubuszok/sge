/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/DynamicsModifier.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Inner classes moved to companion object (Java static classes).
 * - TMP_V1/V2/V3/TMP_Q moved to companion object (Java protected static fields).
 * - PolarAcceleration/TangentialAcceleration: TMP_V3.mul(TMP_Q) replaced with
 *   TMP_Q.transform(TMP_V3) — Vector3.mul(Quaternion) not available in SGE.
 * - write/read(Json) in base, Strength, Angular omitted (Json serialization not ported).
 * - All 8 inner classes faithfully ported: FaceDirection, Strength, Angular,
 *   Rotational2D, Rotational3D, CentripetalAcceleration, PolarAcceleration,
 *   TangentialAcceleration, BrownianAcceleration.
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 531
 * Covenant-baseline-methods: Angular,BrownianAcceleration,CentripetalAcceleration,DynamicsModifier,FaceDirection,PolarAcceleration,Rotational2D,Rotational3D,Strength,TMP_Q,TMP_V1,TMP_V2,TMP_V3,TangentialAcceleration,accelerationChannel,accellerationChannel,activateParticles,allocateChannels,angularChannel,copy,directionalVelocityChannel,isGlobal,lifeChannel,phiValue,positionChannel,rotationChannel,rotationalForceChannel,rotationalVelocity2dChannel,strengthChannel,strengthValue,thetaValue,this,update
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/influencers/DynamicsModifier.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleControllerComponent
import sge.graphics.g3d.particles.values.ScaledNumericValue
import sge.math.{ MathUtils, Matrix4, Quaternion, Vector3 }
import scala.annotation.nowarn

/** It's the base class for any kind of influencer which operates on angular velocity and acceleration of the particles. All the classes that will inherit this base class can and should be used only
  * as sub-influencer of an instance of {@link DynamicsInfluencer} .
  * @author
  *   Inferno
  */
abstract class DynamicsModifier extends Influencer {

  var isGlobal:              Boolean      = false
  protected var lifeChannel: FloatChannel = scala.compiletime.uninitialized

  def this(modifier: DynamicsModifier) = {
    this()
    this.isGlobal = modifier.isGlobal
  }

  override def allocateChannels(): Unit =
    lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
}

object DynamicsModifier {

  private val TMP_V1: Vector3    = Vector3()
  private val TMP_V2: Vector3    = Vector3()
  private val TMP_V3: Vector3    = Vector3()
  private val TMP_Q:  Quaternion = Quaternion()

  final class FaceDirection extends DynamicsModifier {
    private var rotationChannel:      FloatChannel = scala.compiletime.uninitialized
    private var accellerationChannel: FloatChannel = scala.compiletime.uninitialized

    def this(rotation: FaceDirection) = {
      this()
      this.isGlobal = rotation.isGlobal
    }

    override def allocateChannels(): Unit = {
      rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D)
      accellerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
    }

    override def update(): Unit = {
      var i           = 0
      var accelOffset = 0
      val c           = controller.particles.size * rotationChannel.strideSize
      while (i < c) {
        val axisZ = TMP_V1
          .set(
            accellerationChannel.floatData(accelOffset + ParticleChannels.XOffset),
            accellerationChannel.floatData(accelOffset + ParticleChannels.YOffset),
            accellerationChannel.floatData(accelOffset + ParticleChannels.ZOffset)
          )
          .nor()
        val axisY = TMP_V2.set(TMP_V1).crs(Vector3.Y).nor().crs(TMP_V1).nor()
        val axisX = TMP_V3.set(axisY).crs(axisZ).nor()
        TMP_Q.setFromAxes(false, axisX.x, axisY.x, axisZ.x, axisX.y, axisY.y, axisZ.y, axisX.z, axisY.z, axisZ.z)
        rotationChannel.floatData(i + ParticleChannels.XOffset) = TMP_Q.x
        rotationChannel.floatData(i + ParticleChannels.YOffset) = TMP_Q.y
        rotationChannel.floatData(i + ParticleChannels.ZOffset) = TMP_Q.z
        rotationChannel.floatData(i + ParticleChannels.WOffset) = TMP_Q.w
        i += rotationChannel.strideSize
        accelOffset += accellerationChannel.strideSize
      }
    }

    override def copy(): ParticleControllerComponent =
      FaceDirection(this)
  }

  abstract class Strength extends DynamicsModifier {
    protected var strengthChannel: FloatChannel       = scala.compiletime.uninitialized
    var strengthValue:             ScaledNumericValue = ScaledNumericValue()

    def this(rotation: Strength) = {
      this()
      this.isGlobal = rotation.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(rotation.strengthValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      ParticleChannels.Interpolation.id = controller.particleChannels.newId()
      strengthChannel = controller.particles.addChannel(ParticleChannels.Interpolation)
    }

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex * strengthChannel.strideSize
      val c = i + count * strengthChannel.strideSize
      while (i < c) {
        val start = strengthValue.newLowValue()
        var diff  = strengthValue.newHighValue()
        if (!strengthValue.relative) diff -= start
        strengthChannel.floatData(i + ParticleChannels.VelocityStrengthStartOffset) = start
        strengthChannel.floatData(i + ParticleChannels.VelocityStrengthDiffOffset) = diff
        i += strengthChannel.strideSize
      }
    }
  }

  abstract class Angular extends Strength {
    protected var angularChannel: FloatChannel = scala.compiletime.uninitialized

    /** Polar angle, XZ plane */
    var thetaValue: ScaledNumericValue = ScaledNumericValue()

    /** Azimuth, Y */
    var phiValue: ScaledNumericValue = ScaledNumericValue()

    def this(value: Angular) = {
      this()
      this.isGlobal = value.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(value.strengthValue)
      thetaValue = ScaledNumericValue()
      phiValue = ScaledNumericValue()
      thetaValue.load(value.thetaValue)
      phiValue.load(value.phiValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      ParticleChannels.Interpolation4.id = controller.particleChannels.newId()
      angularChannel = controller.particles.addChannel(ParticleChannels.Interpolation4)
    }

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      super.activateParticles(startIndex, count)
      var i = startIndex * angularChannel.strideSize
      val c = i + count * angularChannel.strideSize
      while (i < c) {
        // Theta
        var start = thetaValue.newLowValue()
        var diff  = thetaValue.newHighValue()
        if (!thetaValue.relative) diff -= start
        angularChannel.floatData(i + ParticleChannels.VelocityThetaStartOffset) = start
        angularChannel.floatData(i + ParticleChannels.VelocityThetaDiffOffset) = diff

        // Phi
        start = phiValue.newLowValue()
        diff = phiValue.newHighValue()
        if (!phiValue.relative) diff -= start
        angularChannel.floatData(i + ParticleChannels.VelocityPhiStartOffset) = start
        angularChannel.floatData(i + ParticleChannels.VelocityPhiDiffOffset) = diff
        i += angularChannel.strideSize
      }
    }
  }

  final class Rotational2D extends Strength {
    private var rotationalVelocity2dChannel: FloatChannel = scala.compiletime.uninitialized

    def this(rotation: Rotational2D) = {
      this()
      this.isGlobal = rotation.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(rotation.strengthValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      rotationalVelocity2dChannel = controller.particles.addChannel(ParticleChannels.AngularVelocity2D)
    }

    override def update(): Unit = {
      var i = 0
      var l = ParticleChannels.LifePercentOffset
      var s = 0
      val c = controller.particles.size * rotationalVelocity2dChannel.strideSize
      while (i < c) {
        rotationalVelocity2dChannel.floatData(i) += strengthChannel.floatData(s + ParticleChannels.VelocityStrengthStartOffset) +
          strengthChannel.floatData(s + ParticleChannels.VelocityStrengthDiffOffset) *
          strengthValue.getScale(lifeChannel.floatData(l))
        s += strengthChannel.strideSize
        i += rotationalVelocity2dChannel.strideSize
        l += lifeChannel.strideSize
      }
    }

    override def copy(): Rotational2D =
      Rotational2D(this)
  }

  final class Rotational3D extends Angular {
    @nowarn("msg=not read") // set in allocateChannels, will be read in update
    private var rotationChannel:        FloatChannel = scala.compiletime.uninitialized
    private var rotationalForceChannel: FloatChannel = scala.compiletime.uninitialized

    def this(rotation: Rotational3D) = {
      this()
      this.isGlobal = rotation.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(rotation.strengthValue)
      thetaValue = ScaledNumericValue()
      phiValue = ScaledNumericValue()
      thetaValue.load(rotation.thetaValue)
      phiValue.load(rotation.phiValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D)
      rotationalForceChannel = controller.particles.addChannel(ParticleChannels.AngularVelocity3D)
    }

    override def update(): Unit = {

      // Matrix3 I_t = defined by the shape, it's the inertia tensor
      // Vector3 r = position vector
      // Vector3 L = r.cross(v.mul(m)), It's the angular momentum, where mv it's the linear momentum
      // Inverse(I_t) = a diagonal matrix where the diagonal is IyIz, IxIz, IxIy
      // Vector3 w = L/I_t = inverse(I_t)*L, It's the angular velocity
      // Quaternion spin = 0.5f*Quaternion(w, 0)*currentRotation
      // currentRotation += spin*dt
      // normalize(currentRotation)

      // Algorithm 1
      // Consider a simple channel which represent an angular velocity w
      // Sum each w for each rotation
      // Update rotation

      // Algorithm 2
      // Consider a channel which represent a sort of angular momentum L (r, v)
      // Sum each L for each rotation
      // Multiply sum by constant quantity k = m*I_to(-1) , m could be optional while I is constant and can be calculated at
      // start
      // Update rotation

      // Algorithm 3
      // Consider a channel which represent a simple angular momentum L
      // Proceed as Algorithm 2

      var i = 0
      var l = ParticleChannels.LifePercentOffset
      var s = 0
      var a = 0
      val c = controller.particles.size * rotationalForceChannel.strideSize
      while (i < c) {
        val lifePercent = lifeChannel.floatData(l)
        val strength    = strengthChannel.floatData(s + ParticleChannels.VelocityStrengthStartOffset) +
          strengthChannel.floatData(s + ParticleChannels.VelocityStrengthDiffOffset) * strengthValue.getScale(lifePercent)
        val phi = angularChannel.floatData(a + ParticleChannels.VelocityPhiStartOffset) +
          angularChannel.floatData(a + ParticleChannels.VelocityPhiDiffOffset) * phiValue.getScale(lifePercent)
        val theta = angularChannel.floatData(a + ParticleChannels.VelocityThetaStartOffset) +
          angularChannel.floatData(a + ParticleChannels.VelocityThetaDiffOffset) * thetaValue.getScale(lifePercent)

        val cosTheta = MathUtils.cosDeg(theta)
        val sinTheta = MathUtils.sinDeg(theta)
        val cosPhi   = MathUtils.cosDeg(phi)
        val sinPhi   = MathUtils.sinDeg(phi)

        TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi)
        TMP_V3.scl(strength * MathUtils.degreesToRadians)

        rotationalForceChannel.floatData(i + ParticleChannels.XOffset) += TMP_V3.x
        rotationalForceChannel.floatData(i + ParticleChannels.YOffset) += TMP_V3.y
        rotationalForceChannel.floatData(i + ParticleChannels.ZOffset) += TMP_V3.z

        s += strengthChannel.strideSize
        i += rotationalForceChannel.strideSize
        a += angularChannel.strideSize
        l += lifeChannel.strideSize
      }
    }

    override def copy(): Rotational3D =
      Rotational3D(this)
  }

  final class CentripetalAcceleration extends Strength {
    private var accelerationChannel: FloatChannel = scala.compiletime.uninitialized
    private var positionChannel:     FloatChannel = scala.compiletime.uninitialized

    def this(rotation: CentripetalAcceleration) = {
      this()
      this.isGlobal = rotation.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(rotation.strengthValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      accelerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
      positionChannel = controller.particles.addChannel(ParticleChannels.Position)
    }

    override def update(): Unit = {
      var cx = 0f
      var cy = 0f
      var cz = 0f
      if (!isGlobal) {
        val values = controller.transform.values
        cx = values(Matrix4.M03)
        cy = values(Matrix4.M13)
        cz = values(Matrix4.M23)
      }

      var lifeOffset     = ParticleChannels.LifePercentOffset
      var strengthOffset = 0
      var positionOffset = 0
      var forceOffset    = 0
      var i              = 0
      val c              = controller.particles.size
      while (i < c) {
        val strength = strengthChannel.floatData(strengthOffset + ParticleChannels.VelocityStrengthStartOffset) +
          strengthChannel.floatData(strengthOffset + ParticleChannels.VelocityStrengthDiffOffset) *
          strengthValue.getScale(lifeChannel.floatData(lifeOffset))
        TMP_V3
          .set(
            positionChannel.floatData(positionOffset + ParticleChannels.XOffset) - cx,
            positionChannel.floatData(positionOffset + ParticleChannels.YOffset) - cy,
            positionChannel.floatData(positionOffset + ParticleChannels.ZOffset) - cz
          )
          .nor()
          .scl(strength)
        accelerationChannel.floatData(forceOffset + ParticleChannels.XOffset) += TMP_V3.x
        accelerationChannel.floatData(forceOffset + ParticleChannels.YOffset) += TMP_V3.y
        accelerationChannel.floatData(forceOffset + ParticleChannels.ZOffset) += TMP_V3.z
        i += 1
        positionOffset += positionChannel.strideSize
        strengthOffset += strengthChannel.strideSize
        forceOffset += accelerationChannel.strideSize
        lifeOffset += lifeChannel.strideSize
      }
    }

    override def copy(): CentripetalAcceleration =
      CentripetalAcceleration(this)
  }

  final class PolarAcceleration extends Angular {
    private var directionalVelocityChannel: FloatChannel = scala.compiletime.uninitialized

    def this(rotation: PolarAcceleration) = {
      this()
      this.isGlobal = rotation.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(rotation.strengthValue)
      thetaValue = ScaledNumericValue()
      phiValue = ScaledNumericValue()
      thetaValue.load(rotation.thetaValue)
      phiValue.load(rotation.phiValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      directionalVelocityChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
    }

    override def update(): Unit = {
      var i = 0
      var l = ParticleChannels.LifePercentOffset
      var s = 0
      var a = 0
      val c = controller.particles.size * directionalVelocityChannel.strideSize
      while (i < c) {
        val lifePercent = lifeChannel.floatData(l)
        val strength    = strengthChannel.floatData(s + ParticleChannels.VelocityStrengthStartOffset) +
          strengthChannel.floatData(s + ParticleChannels.VelocityStrengthDiffOffset) * strengthValue.getScale(lifePercent)
        val phi = angularChannel.floatData(a + ParticleChannels.VelocityPhiStartOffset) +
          angularChannel.floatData(a + ParticleChannels.VelocityPhiDiffOffset) * phiValue.getScale(lifePercent)
        val theta = angularChannel.floatData(a + ParticleChannels.VelocityThetaStartOffset) +
          angularChannel.floatData(a + ParticleChannels.VelocityThetaDiffOffset) * thetaValue.getScale(lifePercent)

        val cosTheta = MathUtils.cosDeg(theta)
        val sinTheta = MathUtils.sinDeg(theta)
        val cosPhi   = MathUtils.cosDeg(phi)
        val sinPhi   = MathUtils.sinDeg(phi)
        TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi).nor().scl(strength)

        if (!isGlobal) {
          controller.transform.rotation(TMP_Q, true)
          TMP_Q.transform(TMP_V3)
        }

        directionalVelocityChannel.floatData(i + ParticleChannels.XOffset) += TMP_V3.x
        directionalVelocityChannel.floatData(i + ParticleChannels.YOffset) += TMP_V3.y
        directionalVelocityChannel.floatData(i + ParticleChannels.ZOffset) += TMP_V3.z

        s += strengthChannel.strideSize
        i += directionalVelocityChannel.strideSize
        a += angularChannel.strideSize
        l += lifeChannel.strideSize
      }
    }

    override def copy(): PolarAcceleration =
      PolarAcceleration(this)
  }

  final class TangentialAcceleration extends Angular {
    private var directionalVelocityChannel: FloatChannel = scala.compiletime.uninitialized
    private var positionChannel:            FloatChannel = scala.compiletime.uninitialized

    def this(rotation: TangentialAcceleration) = {
      this()
      this.isGlobal = rotation.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(rotation.strengthValue)
      thetaValue = ScaledNumericValue()
      phiValue = ScaledNumericValue()
      thetaValue.load(rotation.thetaValue)
      phiValue.load(rotation.phiValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      directionalVelocityChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
      positionChannel = controller.particles.addChannel(ParticleChannels.Position)
    }

    override def update(): Unit = {
      var i              = 0
      var l              = ParticleChannels.LifePercentOffset
      var s              = 0
      var a              = 0
      var positionOffset = 0
      val c              = controller.particles.size * directionalVelocityChannel.strideSize
      while (i < c) {
        val lifePercent = lifeChannel.floatData(l)
        val strength    = strengthChannel.floatData(s + ParticleChannels.VelocityStrengthStartOffset) +
          strengthChannel.floatData(s + ParticleChannels.VelocityStrengthDiffOffset) * strengthValue.getScale(lifePercent)
        val phi = angularChannel.floatData(a + ParticleChannels.VelocityPhiStartOffset) +
          angularChannel.floatData(a + ParticleChannels.VelocityPhiDiffOffset) * phiValue.getScale(lifePercent)
        val theta = angularChannel.floatData(a + ParticleChannels.VelocityThetaStartOffset) +
          angularChannel.floatData(a + ParticleChannels.VelocityThetaDiffOffset) * thetaValue.getScale(lifePercent)

        val cosTheta = MathUtils.cosDeg(theta)
        val sinTheta = MathUtils.sinDeg(theta)
        val cosPhi   = MathUtils.cosDeg(phi)
        val sinPhi   = MathUtils.sinDeg(phi)
        TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi)
        TMP_V1.set(
          positionChannel.floatData(positionOffset + ParticleChannels.XOffset),
          positionChannel.floatData(positionOffset + ParticleChannels.YOffset),
          positionChannel.floatData(positionOffset + ParticleChannels.ZOffset)
        )
        if (!isGlobal) {
          controller.transform.translation(TMP_V2)
          TMP_V1.sub(TMP_V2)
          controller.transform.rotation(TMP_Q, true)
          TMP_Q.transform(TMP_V3)
        }
        TMP_V3.crs(TMP_V1).nor().scl(strength)
        directionalVelocityChannel.floatData(i + ParticleChannels.XOffset) += TMP_V3.x
        directionalVelocityChannel.floatData(i + ParticleChannels.YOffset) += TMP_V3.y
        directionalVelocityChannel.floatData(i + ParticleChannels.ZOffset) += TMP_V3.z

        s += strengthChannel.strideSize
        i += directionalVelocityChannel.strideSize
        a += angularChannel.strideSize
        l += lifeChannel.strideSize
        positionOffset += positionChannel.strideSize
      }
    }

    override def copy(): TangentialAcceleration =
      TangentialAcceleration(this)
  }

  final class BrownianAcceleration extends Strength {
    private var accelerationChannel: FloatChannel = scala.compiletime.uninitialized

    def this(rotation: BrownianAcceleration) = {
      this()
      this.isGlobal = rotation.isGlobal
      strengthValue = ScaledNumericValue()
      strengthValue.load(rotation.strengthValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      accelerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
    }

    override def update(): Unit = {
      var lifeOffset     = ParticleChannels.LifePercentOffset
      var strengthOffset = 0
      var forceOffset    = 0
      var i              = 0
      val c              = controller.particles.size
      while (i < c) {
        val strength = strengthChannel.floatData(strengthOffset + ParticleChannels.VelocityStrengthStartOffset) +
          strengthChannel.floatData(strengthOffset + ParticleChannels.VelocityStrengthDiffOffset) *
          strengthValue.getScale(lifeChannel.floatData(lifeOffset))
        TMP_V3.set(MathUtils.random(-1, 1f), MathUtils.random(-1, 1f), MathUtils.random(-1, 1f)).nor().scl(strength)
        accelerationChannel.floatData(forceOffset + ParticleChannels.XOffset) += TMP_V3.x
        accelerationChannel.floatData(forceOffset + ParticleChannels.YOffset) += TMP_V3.y
        accelerationChannel.floatData(forceOffset + ParticleChannels.ZOffset) += TMP_V3.z
        i += 1
        strengthOffset += strengthChannel.strideSize
        forceOffset += accelerationChannel.strideSize
        lifeOffset += lifeChannel.strideSize
      }
    }

    override def copy(): BrownianAcceleration =
      BrownianAcceleration(this)
  }
}
