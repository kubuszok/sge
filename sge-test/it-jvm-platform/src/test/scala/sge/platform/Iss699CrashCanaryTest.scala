package sge
package platform

// ISS-699 crash canary (DORMANT by default).
//
// Proves the CI forked-JVM-crash guard (the ISS-690 guard generalized to every
// forked `testFull` job in .github/workflows/ci.yml) trips RED on an unclean
// death of the forked test JVM. When the environment variable
// `SGE_IT_CRASH_CANARY` is set to "1" this test deliberately kills the forked
// JVM with `Runtime.halt` — an immediate exit that runs no shutdown hooks and
// no munit teardown, so sbt never prints its per-run munit summary line. The CI
// guard then fails the job on either (a) sbt's own non-zero/hung exit code or
// (b) the absent `Passed: Total [1-9]` summary — exactly the ISS-690 mode a
// forked native crash produces.
//
// Unset (the CI default) it is an ordinary passing test, so normal runs are
// completely unaffected. It is only ever armed by a throwaway canary CI run that
// exports SGE_IT_CRASH_CANARY=1 on one forked job to demonstrate the guard.
class Iss699CrashCanaryTest extends munit.FunSuite {

  test("forked-JVM crash guard canary (armed only by SGE_IT_CRASH_CANARY=1)") {
    if (System.getenv("SGE_IT_CRASH_CANARY") == "1") {
      // Unclean death of the forked test JVM: no afterAll, no munit summary.
      // 139 mirrors a SIGSEGV exit code for readability in the CI log.
      Runtime.getRuntime.halt(139)
      fail("unreachable: the canary must have halted the JVM")
    } else {
      assert(true)
    }
  }
}
