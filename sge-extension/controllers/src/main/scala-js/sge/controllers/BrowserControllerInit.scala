/*
 * Scala.js initializer for BrowserControllerBackend.
 * Wires the Gamepad API implementation into the shared stub.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

/** Call [[init]] once at startup on Scala.js to wire the BrowserControllerBackend
  * to the real Web Gamepad API implementation.
  */
object BrowserControllerInit {

  def init(): Unit = {
    BrowserControllerBackend.pollControllerImpl = BrowserControllerImpl.pollController
  }
}
