// SGE — Android haptics operations interface
//
// Self-contained (JDK types only). Provides vibration/haptic feedback.
// Implemented in sge-jvm-platform-android using android.os.Vibrator.

package sge
package platform
package android

/** Haptic feedback operations for Android. Uses only JDK types.
  *
  * Vibration types: 0 = LIGHT, 1 = MEDIUM, 2 = HEAVY.
  */
trait HapticsOps {

  /** Vibrates for the given duration in milliseconds. */
  def vibrate(milliseconds: Int): Unit

  /** Vibrates with a predefined haptic effect.
    * @param vibrationType
    *   0 = LIGHT, 1 = MEDIUM, 2 = HEAVY
    */
  def vibrateHaptic(vibrationType: Int): Unit

  /** Vibrates with a specific intensity.
    * @param milliseconds
    *   duration
    * @param intensity
    *   amplitude 0-255
    * @param fallback
    *   if true, falls back to simple vibration when amplitude control is unavailable
    */
  def vibrateWithIntensity(milliseconds: Int, intensity: Int, fallback: Boolean): Unit

  /** Whether the device has a vibrator. */
  def hasVibratorAvailable: Boolean

  /** Whether the device supports haptic feedback with amplitude control. */
  def hasHapticsSupport: Boolean
}
