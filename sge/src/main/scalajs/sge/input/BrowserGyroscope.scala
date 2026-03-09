/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtGyroscope.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtGyroscope -> BrowserGyroscope
 *   Convention: Scala.js only; JSNI -> js.Dynamic
 *   Convention: Static JSNI methods -> companion object
 *   Idiom: new Gyroscope() via js.Dynamic.global
 *   Audited: 2026-03-06
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import scala.scalajs.js

/** Implementation of the [[https://www.w3.org/TR/gyroscope/#gyroscope-interface Gyroscope Interface]].
  *
  * Uses the browser Generic Sensor API to read device angular velocity values.
  */
class BrowserGyroscope(underlying: js.Dynamic) extends BrowserSensor(underlying) {

  def x: Double = {
    val v = underlying.x
    if (js.isUndefined(v) || v == null) 0.0 else v.asInstanceOf[Double]
  }

  def y: Double = {
    val v = underlying.y
    if (js.isUndefined(v) || v == null) 0.0 else v.asInstanceOf[Double]
  }

  def z: Double = {
    val v = underlying.z
    if (js.isUndefined(v) || v == null) 0.0 else v.asInstanceOf[Double]
  }
}

object BrowserGyroscope {

  /** Permission string to query via the Permissions API.
    * @see
    *   [[BrowserPermissions]]
    */
  val Permission: String = "gyroscope"

  def isSupported: Boolean =
    js.typeOf(js.Dynamic.global.Gyroscope) != "undefined"

  def getInstance(): BrowserGyroscope =
    new BrowserGyroscope(js.Dynamic.newInstance(js.Dynamic.global.Gyroscope)())
}
