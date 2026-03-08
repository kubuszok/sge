// SGE — Android application configuration interface
//
// Self-contained (JDK types only). Pure data — no Android SDK references.
// Can be instantiated on any JVM.

package sge
package platform
package android

/** Android application configuration. All fields are public vars with sensible defaults.
  *
  * This is a pure data class with no Android SDK dependencies. It mirrors LibGDX's AndroidApplicationConfiguration.
  */
class AndroidConfigOps {

  /** Number of bits per color channel. */
  var r: Int = 8
  var g: Int = 8
  var b: Int = 8
  var a: Int = 8

  /** Number of bits for depth and stencil buffer. */
  var depth:   Int = 16
  var stencil: Int = 0

  /** Number of samples for CSAA/MSAA. 0 means disabled. */
  var numSamples: Int = 0

  /** Whether to use the accelerometer. Default: true. */
  var useAccelerometer: Boolean = true

  /** Whether to use the gyroscope. Default: false. */
  var useGyroscope: Boolean = false

  /** Whether to use the compass. Default: true. */
  var useCompass: Boolean = true

  /** Whether to use Android's rotation vector software sensor. Default: false. */
  var useRotationVectorSensor: Boolean = false

  /** Sensor sampling rate in microseconds. Default: SENSOR_DELAY_GAME (1). */
  var sensorDelay: Int = 1

  /** Whether to keep the screen on while running. Default: false. */
  var useWakelock: Boolean = false

  /** Whether to disable audio. Default: false. */
  var disableAudio: Boolean = false

  /** Maximum number of simultaneous sound instances. */
  var maxSimultaneousSounds: Int = 16

  /** Whether to use Android 4.4 KitKat's 'Immersive mode'. Default: true. */
  var useImmersiveMode: Boolean = true

  /** Whether to enable OpenGL ES 3.0 if supported. Default: false. */
  var useGL30: Boolean = false

  /** Maximum number of threads for network requests. */
  var maxNetThreads: Int = Int.MaxValue

  /** Whether to render under the display cutout. Default: false. */
  var renderUnderCutout: Boolean = false
}
