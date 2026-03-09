/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtPermissions.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtPermissions -> BrowserPermissions
 *   Convention: Scala.js only; JSNI -> js.Dynamic + js.Promise
 *   Convention: GwtPermissionResult inner interface -> PermissionResult trait
 *   Idiom: navigator.permissions.query() via js.Dynamic
 *   Audited: 2026-03-06
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import org.scalajs.dom.window
import scala.scalajs.js

/** Wraps the [[https://w3c.github.io/permissions/ W3C Permissions API]].
  *
  * Queries the browser for the permission status of a given feature (e.g. "accelerometer", "gyroscope") and reports the result via a [[PermissionResult]] callback. Also installs an `onchange` handler
  * to track permission state changes.
  */
object BrowserPermissions {

  /** Callback for permission query results.
    * @see
    *   [[https://w3c.github.io/permissions/#status-of-a-permission status-of-a-permission]]
    */
  trait PermissionResult {

    /** The permission is granted without asking the user. */
    def granted(): Unit

    /** Accessing the feature is not allowed. */
    def denied(): Unit

    /** The user will be prompted when the feature is accessed. */
    def prompt(): Unit
  }

  /** Queries the browser for the permission status of the given API.
    *
    * @param permission
    *   the permission name (see [[https://w3c.github.io/permissions/#permission-registry W3C registry]])
    * @param result
    *   handler for the permission result
    */
  def queryPermission(permission: String, result: PermissionResult): Unit = {
    val nav = window.navigator.asInstanceOf[js.Dynamic]
    if (js.isUndefined(nav.permissions)) {
      // Permissions API not available — assume granted (permissive default)
      result.granted()
    } else {
      val query = js.Dynamic.literal(name = permission)
      try
        nav.permissions
          .query(query)
          .asInstanceOf[js.Promise[js.Dynamic]]
          .`then`(
            { (status: js.Dynamic) =>
              val state = status.state.asInstanceOf[String]
              dispatchState(state, result)
              status.onchange = { () =>
                val newState = status.state.asInstanceOf[String]
                dispatchState(newState, result)
              }: js.Function0[Unit]
            }: js.Function1[js.Dynamic, Unit],
            { (_: Any) =>
              // Browser rejected the permission name (e.g. Firefox doesn't support "accelerometer")
              result.granted() // permissive default
            }: js.Function1[Any, Unit]
          )
      catch {
        case _: Throwable => result.granted() // permissive default
      }
      ()
    }
  }

  private def dispatchState(state: String, result: PermissionResult): Unit =
    state match {
      case "granted" => result.granted()
      case "denied"  => result.denied()
      case "prompt"  => result.prompt()
      case _         => ()
    }
}
