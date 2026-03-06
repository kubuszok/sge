/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtSensor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtSensor -> BrowserSensor
 *   Convention: Scala.js only; JSNI -> js.Dynamic wrapper
 *   Convention: GWT JavaScriptObject -> js.Dynamic internal reference
 *   Idiom: SAM callbacks replace inner interfaces (SensorRead/SensorError/SensorActivate)
 *   Audited: 2026-03-06
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import scala.scalajs.js

/** Implementation of the [[https://www.w3.org/TR/generic-sensor/#the-sensor-interface Sensor Interface]].
  *
  * Wraps a browser Generic Sensor API object via `js.Dynamic`. Subclasses provide typed access to specific sensor data (accelerometer, gyroscope, etc.).
  *
  * @param underlying
  *   the JavaScript Sensor object
  */
class BrowserSensor(protected val underlying: js.Dynamic) {

  def activated: Boolean = {
    val v = underlying.activated
    if (js.isUndefined(v)) false else v.asInstanceOf[Boolean]
  }

  def hasReading: Boolean = {
    val v = underlying.hasReading
    if (js.isUndefined(v)) false else v.asInstanceOf[Boolean]
  }

  def timestamp: Double = {
    val v = underlying.timestamp
    if (js.isUndefined(v)) 0.0 else v.asInstanceOf[Double]
  }

  def start(): Unit = underlying.start()

  def stop(): Unit = underlying.stop()

  def setReadingListener(listener: () => Unit): Unit =
    underlying.onreading = listener: js.Function0[Unit]

  def setErrorListener(listener: () => Unit): Unit =
    underlying.onerror = listener: js.Function0[Unit]

  def setActivateListener(listener: () => Unit): Unit =
    underlying.onactivate = listener: js.Function0[Unit]
}
