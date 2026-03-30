package sge
package ai
package steer

import sge.ai.steer.behaviors.{Arrive, Flee, Seek}
import sge.ai.utils.Location
import sge.math.Vector2
import sge.utils.Nullable

/** A simple 2D steerable implementation for testing steering behaviors. */
class SimpleSteerable(
  private val pos: Vector2,
  private val vel: Vector2 = Vector2(0, 0),
  private var _orientation: Float = 0f,
  private var _maxLinearSpeed: Float = 10f,
  private var _maxLinearAcceleration: Float = 100f,
  private var _maxAngularSpeed: Float = 5f,
  private var _maxAngularAcceleration: Float = 10f,
  private var _tagged: Boolean = false
) extends Steerable[Vector2] {

  override def position: Vector2 = pos
  override def orientation: Float = _orientation
  override def orientation_=(orientation: Float): Unit = { _orientation = orientation }
  override def linearVelocity: Vector2 = vel
  override def angularVelocity: Float = 0f
  override def boundingRadius: Float = 1f
  override def tagged: Boolean = _tagged
  override def tagged_=(tagged: Boolean): Unit = { _tagged = tagged }

  override def zeroLinearSpeedThreshold: Float = 0.001f
  override def zeroLinearSpeedThreshold_=(value: Float): Unit = {}
  override def maxLinearSpeed: Float = _maxLinearSpeed
  override def maxLinearSpeed_=(value: Float): Unit = { _maxLinearSpeed = value }
  override def maxLinearAcceleration: Float = _maxLinearAcceleration
  override def maxLinearAcceleration_=(value: Float): Unit = { _maxLinearAcceleration = value }
  override def maxAngularSpeed: Float = _maxAngularSpeed
  override def maxAngularSpeed_=(value: Float): Unit = { _maxAngularSpeed = value }
  override def maxAngularAcceleration: Float = _maxAngularAcceleration
  override def maxAngularAcceleration_=(value: Float): Unit = { _maxAngularAcceleration = value }

  override def vectorToAngle(vector: Vector2): Float = {
    Math.atan2(vector.y, vector.x).toFloat
  }

  override def angleToVector(outVector: Vector2, angle: Float): Vector2 = {
    outVector.set(Math.cos(angle).toFloat, Math.sin(angle).toFloat)
  }

  override def newLocation(): Location[Vector2] = new SimpleLocation(Vector2(0, 0))
}

/** A simple 2D location for use as a target. */
class SimpleLocation(private val pos: Vector2) extends Location[Vector2] {
  override def position: Vector2 = pos
  override def orientation: Float = 0f
  override def orientation_=(orientation: Float): Unit = {}
  override def vectorToAngle(vector: Vector2): Float = Math.atan2(vector.y, vector.x).toFloat
  override def angleToVector(outVector: Vector2, angle: Float): Vector2 = {
    outVector.set(Math.cos(angle).toFloat, Math.sin(angle).toFloat)
  }
  override def newLocation(): Location[Vector2] = new SimpleLocation(Vector2(0, 0))
}

class SteeringSuite extends munit.FunSuite {

  private val Eps = 1e-3f

  test("Seek: output acceleration points toward target") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(10, 0))
    val seek = new Seek[Vector2](owner, Nullable(target))
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))

    seek.calculateSteering(output)

    // Acceleration should point in +x direction (toward target)
    assert(output.linear.x > 0, s"Expected positive x acceleration, got ${ output.linear.x }")
    assertEqualsFloat(output.linear.y, 0f, Eps)
    assert(!output.isZero, "Steering output should not be zero")
  }

  test("Seek: acceleration magnitude equals max linear acceleration") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(5, 5))
    val seek = new Seek[Vector2](owner, Nullable(target))
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))

    seek.calculateSteering(output)

    val mag = output.linear.length
    assertEqualsFloat(mag, 100f, Eps) // maxLinearAcceleration = 100
  }

  test("Flee: output acceleration points away from target") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(10, 0))
    val flee = new Flee[Vector2](owner, Nullable(target))
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))

    flee.calculateSteering(output)

    // Acceleration should point in -x direction (away from target)
    assert(output.linear.x < 0, s"Expected negative x acceleration, got ${ output.linear.x }")
    assertEqualsFloat(output.linear.y, 0f, Eps)
    assert(!output.isZero, "Steering output should not be zero")
  }

  test("Flee: opposite direction to Seek") {
    val target = new SimpleLocation(Vector2(5, 3))

    val seekOutput = new SteeringAcceleration[Vector2](Vector2(0, 0))
    val fleeOutput = new SteeringAcceleration[Vector2](Vector2(0, 0))

    // Need separate owners because seek/flee mutate the output linear vector
    new Seek[Vector2](new SimpleSteerable(Vector2(0, 0)), Nullable(target)).calculateSteering(seekOutput)
    new Flee[Vector2](new SimpleSteerable(Vector2(0, 0)), Nullable(target)).calculateSteering(fleeOutput)

    // Flee should be opposite direction to Seek
    assertEqualsFloat(seekOutput.linear.x, -fleeOutput.linear.x, Eps)
    assertEqualsFloat(seekOutput.linear.y, -fleeOutput.linear.y, Eps)
  }

  test("Arrive: output is zero when at target (within tolerance)") {
    val owner = new SimpleSteerable(Vector2(5, 5))
    val target = new SimpleLocation(Vector2(5, 5))
    val arrive = new Arrive[Vector2](owner, Nullable(target))
    arrive.arrivalTolerance = 0.5f
    arrive.decelerationRadius = 5f
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))

    arrive.calculateSteering(output)

    assert(output.isZero, s"Expected zero output at target, got (${ output.linear.x }, ${ output.linear.y })")
  }

  test("Arrive: output is non-zero when far from target") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(20, 0))
    val arrive = new Arrive[Vector2](owner, Nullable(target))
    arrive.arrivalTolerance = 0.5f
    arrive.decelerationRadius = 5f
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))

    arrive.calculateSteering(output)

    assert(!output.isZero, "Expected non-zero output when far from target")
    assert(output.linear.x > 0, s"Expected positive x acceleration toward target, got ${ output.linear.x }")
  }

  test("Arrive: slows down within deceleration radius") {
    val farOwner = new SimpleSteerable(Vector2(0, 0))
    val nearOwner = new SimpleSteerable(Vector2(18, 0)) // 2 units away, within decelerationRadius=5
    val target = new SimpleLocation(Vector2(20, 0))

    val farArrive = new Arrive[Vector2](farOwner, Nullable(target))
    farArrive.arrivalTolerance = 0.1f
    farArrive.decelerationRadius = 5f

    val nearArrive = new Arrive[Vector2](nearOwner, Nullable(target))
    nearArrive.arrivalTolerance = 0.1f
    nearArrive.decelerationRadius = 5f

    val farOutput = new SteeringAcceleration[Vector2](Vector2(0, 0))
    val nearOutput = new SteeringAcceleration[Vector2](Vector2(0, 0))

    farArrive.calculateSteering(farOutput)
    nearArrive.calculateSteering(nearOutput)

    // Both should point toward target (+x)
    assert(farOutput.linear.x > 0, "far output should point toward target")
    assert(nearOutput.linear.x > 0, "near output should point toward target")
  }

  test("disabled behavior returns zero") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(10, 0))
    val seek = new Seek[Vector2](owner, Nullable(target))
    seek.enabled = false
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))

    seek.calculateSteering(output)

    assert(output.isZero, "Disabled behavior should return zero acceleration")
  }
}
