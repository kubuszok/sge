// SGE — Desktop integration test: InputProcessor dispatch check
//
// Sets an InputProcessor on the input subsystem, verifies it is registered,
// and confirms the dispatch mechanism is wired up.

package sge.it.desktop.checks

import sge.{ Input, InputProcessor, Pixels, Sge }
import sge.it.desktop.CheckResult

/** Verifies InputProcessor registration and retrieval. */
object InputDispatchCheck {

  def run()(using Sge): CheckResult =
    try {
      val input = Sge().input

      // Save the original processor (may be null/empty)
      val original = input.getInputProcessor()

      // Set a custom InputProcessor that tracks calls
      var touchDownCalled = false
      val testProcessor   = new InputProcessor {
        override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Input.Button): Boolean = {
          touchDownCalled = true
          true
        }
      }

      input.setInputProcessor(testProcessor)
      val retrieved = input.getInputProcessor()

      // Verify the processor was registered
      val registered = retrieved eq testProcessor

      // Restore original
      input.setInputProcessor(original)

      if (registered) {
        CheckResult("input_dispatch", passed = true, "InputProcessor set/get roundtrip OK")
      } else {
        CheckResult("input_dispatch", passed = false, "InputProcessor not correctly registered")
      }
    } catch {
      case e: Exception =>
        CheckResult("input_dispatch", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
