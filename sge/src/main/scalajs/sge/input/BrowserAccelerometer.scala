/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtAccelerometer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtAccelerometer -> BrowserAccelerometer
 *   Convention: Scala.js only; JSNI -> js.Dynamic
 *   Convention: Static JSNI methods -> companion object
 *   Idiom: new Accelerometer() via js.Dynamic.global
 *   Audited: 2026-03-06
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import scala.scalajs.js

/** Implementation of the [[https://www.w3.org/TR/accelerometer/#accelerometer-interface Accelerometer Interface]].
  *
  * Uses the browser Generic Sensor API to read device acceleration values.
  */
class BrowserAccelerometer(underlying: js.Dynamic) extends BrowserSensor(underlying) {

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

object BrowserAccelerometer {

  /** Permission string to query via the Permissions API.
    * @see
    *   [[BrowserPermissions]]
    */
  val Permission: String = "accelerometer"

  def isSupported: Boolean =
    !js.isUndefined(js.Dynamic.global.Accelerometer)

  def getInstance(): BrowserAccelerometer =
    new BrowserAccelerometer(js.Dynamic.newInstance(js.Dynamic.global.Accelerometer)())
}
