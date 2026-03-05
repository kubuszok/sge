/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtApplicationConfiguration.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtApplicationConfiguration -> BrowserApplicationConfig
 *   Convention: GWT Panel/TextArea fields dropped (DOM element access via canvasId instead)
 *   Convention: OrientationLockType enum extracted as companion object member
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.utils.Nullable

/** Configuration for browser (Scala.js) applications.
  *
  * @param width
  *   the width of the drawing area in pixels, or 0 for using the available space
  * @param height
  *   the height of the drawing area in pixels, or 0 for using the available space
  * @param usePhysicalPixels
  *   whether to use physical device pixels or CSS pixels for scaling the canvas. Makes a difference on mobile devices and HDPI and Retina displays. Set to true for resizable and fullscreen games on
  *   mobile devices and for Desktops if you want to use the full resolution of HDPI/Retina displays. Setting to false mostly makes sense for fixed-size games or non-mobile games expecting performance
  *   issues on huge resolutions.
  */
class BrowserApplicationConfig(
  val width:             Int = 0,
  val height:            Int = 0,
  val usePhysicalPixels: Boolean = false
) {

  /** Padding to use for resizing the game content in the browser window, for resizable applications only. Defaults to
    *   10. The padding is necessary to prevent the browser from showing scrollbars. This can happen if the game content is of the same size as the browser window. The padding is given in logical
    *       pixels, not affected by [[usePhysicalPixels]].
    */
  var padHorizontal: Int = 10
  var padVertical:   Int = 10

  /** whether to use a stencil buffer */
  var stencil: Boolean = false

  /** whether to enable antialiasing */
  var antialiasing: Boolean = false

  /** the id of a canvas element to be used as the drawing area, can be null in which case a canvas is added automatically to the body element of the DOM
    */
  var canvasId: Nullable[String] = Nullable.empty

  /** whether to use debugging mode for OpenGL calls. Errors will result in a RuntimeException being thrown. */
  var useDebugGL: Boolean = false

  /** Whether to enable OpenGL ES 3.0 (aka WebGL2) if supported. If not supported it will fall-back to OpenGL ES 2.0.
    */
  var useGL30: Boolean = false

  /** preserve the back buffer, needed if you fetch a screenshot via canvas#toDataUrl, may have performance impact */
  var preserveDrawingBuffer: Boolean = false

  /** whether to include an alpha channel in the color buffer to combine the color buffer with the rest of the webpage effectively allows transparent backgrounds, at a performance cost.
    */
  var alpha: Boolean = false

  /** whether to use premultiplied alpha, may have performance impact */
  var premultipliedAlpha: Boolean = false

  /** screen-orientation to attempt locking as the application enters full-screen-mode. Note that on mobile browsers, full-screen mode can typically only be entered on a user gesture (click, tap,
    * key-stroke)
    */
  var fullscreenOrientation: Nullable[BrowserApplicationConfig.OrientationLockType] = Nullable.empty

  /** Whether openURI will open page in new tab. By default it will, however it may be blocked by popup blocker. To prevent the page from being blocked you can redirect to the new page. However this
    * will exit your game.
    */
  var openURLInNewWindow: Boolean = true

  /** whether to use the accelerometer. default: true */
  var useAccelerometer: Boolean = true

  /** whether to use the gyroscope. default: false */
  var useGyroscope: Boolean = false

  /** whether to make the webgl context compatible with WebXR, may have positive performance impact */
  var xrCompatible: Boolean = false

  /** Whether to fetch available output devices. It asks the user automatically for permission */
  var fetchAvailableOutputDevices: Boolean = false

  /** If true, audio backend will not be used. */
  var disableAudio: Boolean = false

  /** Returns true if this is a fixed-size application (both width and height are non-zero). */
  def isFixedSizeApplication: Boolean = width != 0 && height != 0
}

object BrowserApplicationConfig {

  /** Screen orientation lock types for fullscreen mode.
    *
    * Values from http://www.w3.org/TR/screen-orientation. Filtered based on what browsers actually support.
    */
  enum OrientationLockType(val orientationName: String) extends java.lang.Enum[OrientationLockType] {
    case Landscape extends OrientationLockType("landscape")
    case Portrait extends OrientationLockType("portrait")
    case PortraitPrimary extends OrientationLockType("portrait-primary")
    case PortraitSecondary extends OrientationLockType("portrait-secondary")
    case LandscapePrimary extends OrientationLockType("landscape-primary")
    case LandscapeSecondary extends OrientationLockType("landscape-secondary")
  }
}
