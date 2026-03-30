package sge
package textra

class TypingConfigSuite extends munit.FunSuite {

  test("DEFAULT_SPEED_PER_CHAR has expected default value") {
    assertEquals(TypingConfig.DEFAULT_SPEED_PER_CHAR, 0.05f)
  }

  test("DEFAULT_WAIT_VALUE has expected default value") {
    assertEquals(TypingConfig.DEFAULT_WAIT_VALUE, 0.250f)
  }

  test("DEFAULT_CLEAR_COLOR is not null and is white") {
    assert(TypingConfig.DEFAULT_CLEAR_COLOR != null, "DEFAULT_CLEAR_COLOR should not be null")
    assertEquals(TypingConfig.DEFAULT_CLEAR_COLOR.r, 1f)
    assertEquals(TypingConfig.DEFAULT_CLEAR_COLOR.g, 1f)
    assertEquals(TypingConfig.DEFAULT_CLEAR_COLOR.b, 1f)
    assertEquals(TypingConfig.DEFAULT_CLEAR_COLOR.a, 1f)
  }

  test("40 effect start tokens are registered") {
    assert(
      TypingConfig.EFFECT_START_TOKENS.size >= 40,
      s"Expected at least 40 effect tokens, got ${TypingConfig.EFFECT_START_TOKENS.size}"
    )
  }

  test("known effects are registered") {
    val knownEffects = Seq(
      "WAVE",
      "SHAKE",
      "RAINBOW",
      "FADE",
      "BLINK",
      "JOLT",
      "SPIRAL",
      "SPIN",
      "CROWD",
      "SHRINK",
      "EMERGE",
      "OCEAN"
    )
    knownEffects.foreach { name =>
      assert(TypingConfig.EFFECT_START_TOKENS.contains(name), s"Missing effect: $name")
      assert(TypingConfig.EFFECT_END_TOKENS.contains("END" + name), s"Missing end token: END$name")
    }
  }
}
