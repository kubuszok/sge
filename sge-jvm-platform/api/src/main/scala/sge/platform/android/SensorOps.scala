// SGE — Android sensor operations interface
//
// Self-contained (JDK types only). Abstracts accelerometer, gyroscope,
// compass, and rotation vector sensor access. Implemented in
// sge-jvm-platform-android using android.hardware.SensorManager.

package sge
package platform
package android

/** Sensor operations for Android. Uses only JDK types.
  *
  * Native orientation constants: 0 = landscape, 1 = portrait.
  */
trait SensorOps {

  /** Registers sensor listeners based on configuration.
    * @param config
    *   the Android configuration
    */
  def registerListeners(config: AndroidConfigOps): Unit

  /** Unregisters all sensor listeners. */
  def unregisterListeners(): Unit

  /** Current accelerometer values (x, y, z) in m/s^2. */
  def accelerometerX: Float
  def accelerometerY: Float
  def accelerometerZ: Float

  /** Current gyroscope values (x, y, z) in rad/s. */
  def gyroscopeX: Float
  def gyroscopeY: Float
  def gyroscopeZ: Float

  /** Current compass azimuth in degrees. */
  def azimuth: Float

  /** Current pitch in degrees. */
  def pitch: Float

  /** Current roll in degrees. */
  def roll: Float

  /** The 4x4 rotation matrix from the rotation vector sensor. */
  def rotationMatrix: Array[Float]

  /** Native orientation of the device. 0 = landscape, 1 = portrait.
    */
  def nativeOrientation: Int

  /** Whether the accelerometer is available. */
  def hasAccelerometer: Boolean

  /** Whether the gyroscope is available. */
  def hasGyroscope: Boolean

  /** Whether the compass is available. */
  def hasCompass: Boolean

  /** Whether the rotation vector sensor is available. */
  def hasRotationVector: Boolean
}
