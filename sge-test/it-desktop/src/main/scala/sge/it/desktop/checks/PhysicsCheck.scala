// SGE — Desktop integration test: Physics extension check
//
// Creates a PhysicsWorld (backed by Rapier2D), which triggers
// native lib loading (sge_physics). A successful create + close = pass.

package sge.it.desktop.checks

import sge.Sge
import sge.physics.PhysicsWorld
import sge.it.desktop.CheckResult

/** Verifies physics native library loading and world creation. */
object PhysicsCheck {

  def run()(using Sge): CheckResult =
    try {
      val world = new PhysicsWorld()
      world.close()
      CheckResult("physics_load", passed = true, "PhysicsWorld create + close OK")
    } catch {
      case e: UnsatisfiedLinkError =>
        CheckResult("physics_load", passed = false, s"Native lib missing: ${e.getMessage}")
      case e: Exception =>
        CheckResult("physics_load", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
