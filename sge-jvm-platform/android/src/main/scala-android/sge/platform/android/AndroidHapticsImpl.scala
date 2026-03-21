// SGE — Android haptics implementation
//
// Uses android.os.Vibrator / VibratorManager for haptic feedback.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidHaptics
//   Renames: AndroidHaptics → AndroidHapticsImpl
//   Convention: ops interface pattern; _root_.android.* imports; Build.VERSION checks
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.content.Context
import _root_.android.os.{ Build, VibrationEffect, Vibrator }

class AndroidHapticsImpl(context: Context) extends HapticsOps {

  private val vibrator: Vibrator =
    if (Build.VERSION.SDK_INT >= 31) { // Build.VERSION_CODES.S
      val manager = context.getSystemService(classOf[_root_.android.os.VibratorManager])
      manager.getDefaultVibrator
    } else {
      context.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]
    }

  val hasVibratorAvailable: Boolean =
    vibrator != null && vibrator.hasVibrator

  val hasHapticsSupport: Boolean =
    hasVibratorAvailable && Build.VERSION.SDK_INT >= 29 && vibrator.hasAmplitudeControl

  override def vibrate(milliseconds: Int): Unit =
    if (hasVibratorAvailable) {
      if (Build.VERSION.SDK_INT >= 26) // Build.VERSION_CODES.O
        vibrator.vibrate(VibrationEffect.createOneShot(milliseconds.toLong, VibrationEffect.DEFAULT_AMPLITUDE))
      else
        vibrator.vibrate(milliseconds.toLong)
    }

  override def vibrateHaptic(vibrationType: Int): Unit =
    if (hasHapticsSupport && Build.VERSION.SDK_INT >= 29) { // Build.VERSION_CODES.Q
      val effect = vibrationType match {
        case 0 => VibrationEffect.EFFECT_TICK // LIGHT
        case 1 => VibrationEffect.EFFECT_CLICK // MEDIUM
        case 2 => VibrationEffect.EFFECT_HEAVY_CLICK // HEAVY
        case _ => throw new IllegalArgumentException(s"Unknown vibrationType: $vibrationType")
      }
      if (Build.VERSION.SDK_INT >= 33) { // Build.VERSION_CODES.TIRAMISU
        val attrs = new _root_.android.os.VibrationAttributes.Builder().setUsage(_root_.android.os.VibrationAttributes.USAGE_MEDIA).build()
        vibrator.vibrate(VibrationEffect.createPredefined(effect), attrs)
      } else {
        val attrs = new _root_.android.media.AudioAttributes.Builder()
          .setContentType(_root_.android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .setUsage(_root_.android.media.AudioAttributes.USAGE_GAME)
          .build()
        vibrator.vibrate(VibrationEffect.createPredefined(effect), attrs)
      }
    }

  override def vibrateWithIntensity(milliseconds: Int, intensity: Int, fallback: Boolean): Unit =
    if (hasHapticsSupport && Build.VERSION.SDK_INT >= 26) {
      val clamped = Math.max(0, Math.min(255, intensity))
      vibrator.vibrate(VibrationEffect.createOneShot(milliseconds.toLong, clamped))
    } else if (fallback) {
      vibrate(milliseconds)
    }
}
