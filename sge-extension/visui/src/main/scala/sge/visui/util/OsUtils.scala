/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab, Simon Gerst
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 85
 * Covenant-baseline-methods: MAC,OS,OsUtils,UNIX,WINDOWS,altKey,builder,ctrlKey,getAndroidApiLevel,getShortcutFor,isAndroid,isIos,isMac,isUnix,isWindows,separatorString,shiftKey
 * Covenant-source-reference: com/kotcrab/vis/ui/util/OsUtils.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util

import sge.Application
import sge.Input.{ Key, Keys }

/** Operating system related utils.
  * @author
  *   Kotcrab
  * @author
  *   Simon Gerst
  */
object OsUtils {
  private val OS:      String  = System.getProperty("os.name", "").toLowerCase
  private val WINDOWS: Boolean = OS.contains("win")
  private val MAC:     Boolean = OS.contains("mac")
  private val UNIX:    Boolean = OS.contains("nix") || OS.contains("nux") || OS.contains("aix")

  /** @return `true` if the current OS is Windows */
  def isWindows: Boolean = WINDOWS

  /** @return `true` if the current OS is Mac */
  def isMac: Boolean = MAC

  /** @return `true` if the current OS is Unix */
  def isUnix: Boolean = UNIX

  /** @return `true` if the current OS is iOS */
  def isIos(using Sge): Boolean = Sge().application.applicationType == Application.ApplicationType.iOS

  /** @return `true` if the current OS is Android */
  def isAndroid(using Sge): Boolean = Sge().application.applicationType == Application.ApplicationType.Android

  /** Returns the Android API level it's basically the same as android.os.Build.VERSION.SDK_INT
    * @return
    *   the API level. Returns 0 if the current OS isn't Android
    */
  def getAndroidApiLevel(using Sge): Int =
    if (isAndroid) Sge().application.version
    else 0

  /** Creates platform dependant shortcut text. Converts int keycodes to String text. Eg. Keys.CONTROL_LEFT, Keys.SHIFT_LEFT, Keys.F5 will be converted to Ctrl+Shift+F5 on Windows and Linux, and to
    * command+shift+F5 on Mac.
    *
    * CONTROL_LEFT and CONTROL_RIGHT and SYM are mapped to Ctrl. The same goes for Alt (ALT_LEFT, ALT_RIGHT) and Shift (SHIFT_LEFT, SHIFT_RIGHT).
    *
    * Keycodes equal to `Int.MinValue` will be ignored.
    * @param keycodes
    *   keycodes from [[Key]] that are used to create shortcut text
    * @return
    *   the platform dependent shortcut text
    */
  def getShortcutFor(keycodes: Key*): String = {
    val builder = new StringBuilder

    val separatorString = if (isMac) "" else "+"
    val ctrlKey         = if (isMac) "\u2318" else "Ctrl"
    val altKey          = if (isMac) "\u2325" else "Alt"
    val shiftKey        = if (isMac) "\u21E7" else "Shift"

    for (i <- keycodes.indices)
      if (keycodes(i).toInt != Int.MinValue) {
        keycodes(i) match {
          case k if k == Keys.CONTROL_LEFT || k == Keys.CONTROL_RIGHT || k == Keys.SYM => builder.append(ctrlKey)
          case k if k == Keys.SHIFT_LEFT || k == Keys.SHIFT_RIGHT                      => builder.append(shiftKey)
          case k if k == Keys.ALT_LEFT || k == Keys.ALT_RIGHT                          => builder.append(altKey)
          case keycode                                                                 => builder.append(Keys.toString(keycode))
        }

        if (i < keycodes.length - 1) {
          builder.append(separatorString)
        }
      }

    builder.toString()
  }
}
