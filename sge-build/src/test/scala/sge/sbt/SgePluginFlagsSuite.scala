package sge.sbt

// RED test for ISS-556: SgePlugin must partition its scalacOptions into a
// LENIENT default applied to every downstream game project and an OPT-IN
// strict set. Forcing -Werror / -no-indent / -W... onto user projects breaks
// tutorial-style Scala 3, and the default never enables
// -language:implicitConversions which the Nullable assignment ergonomics need.
//
// Pre-fix this suite fails to compile: SgePlugin.strictScalacOptions and
// SgePlugin.defaultScalacOptions do not exist yet (capability-absent RED).
class SgePluginFlagsSuite extends munit.FunSuite {

  test("strictScalacOptions carries the opt-in strict flags") {
    assert(SgePlugin.strictScalacOptions.contains("-Werror"), "-Werror must be opt-in (strict)")
    assert(SgePlugin.strictScalacOptions.contains("-no-indent"), "-no-indent must be opt-in (strict)")
    assert(SgePlugin.strictScalacOptions.exists(_.startsWith("-Wunused")), "-Wunused must be strict")
  }

  test("defaultScalacOptions is lenient and enables implicitConversions") {
    assert(
      SgePlugin.defaultScalacOptions.contains("-language:implicitConversions"),
      "default must enable implicitConversions for Nullable ergonomics"
    )
    assert(!SgePlugin.defaultScalacOptions.contains("-Werror"), "default (user projects) must NOT force -Werror")
    assert(!SgePlugin.defaultScalacOptions.contains("-no-indent"), "default must NOT force -no-indent")
    assert(
      !SgePlugin.defaultScalacOptions.exists(_.startsWith("-W")),
      "default must not force -W strict warnings"
    )
  }
}
