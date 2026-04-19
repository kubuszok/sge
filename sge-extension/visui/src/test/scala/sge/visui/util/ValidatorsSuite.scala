package sge
package visui
package util

class ValidatorsSuite extends munit.FunSuite {

  // ---------- IntegerValidator ----------

  test("IntegerValidator accepts valid integers") {
    val v = Validators.IntegerValidator()
    assert(v.validateInput("42"))
    assert(v.validateInput("-7"))
    assert(v.validateInput("0"))
    assert(v.validateInput("2147483647"))
  }

  test("IntegerValidator rejects non-integers") {
    val v = Validators.IntegerValidator()
    assert(!v.validateInput("3.14"))
    assert(!v.validateInput("abc"))
    assert(!v.validateInput(""))
    assert(!v.validateInput("12 34"))
  }

  test("IntegerValidator accepts negative int min") {
    val v = Validators.IntegerValidator()
    assert(v.validateInput("-2147483648"))
  }

  test("IntegerValidator rejects overflow") {
    val v = Validators.IntegerValidator()
    // Just beyond Int.MaxValue
    assert(!v.validateInput("2147483648"))
    // Just beyond Int.MinValue
    assert(!v.validateInput("-2147483649"))
  }

  test("IntegerValidator rejects leading/trailing spaces") {
    val v = Validators.IntegerValidator()
    assert(!v.validateInput(" 42"))
    assert(!v.validateInput("42 "))
  }

  test("IntegerValidator rejects hex notation") {
    val v = Validators.IntegerValidator()
    assert(!v.validateInput("0xFF"))
    assert(!v.validateInput("0x10"))
  }

  // ---------- FloatValidator ----------

  test("FloatValidator accepts valid floats") {
    val v = Validators.FloatValidator()
    assert(v.validateInput("3.14"))
    assert(v.validateInput("-0.5"))
    assert(v.validateInput("42"))
    assert(v.validateInput("0"))
    assert(v.validateInput("1e10"))
  }

  test("FloatValidator rejects non-floats") {
    val v = Validators.FloatValidator()
    assert(!v.validateInput("abc"))
    assert(!v.validateInput(""))
    assert(!v.validateInput("12.34.56"))
  }

  test("FloatValidator accepts scientific notation") {
    val v = Validators.FloatValidator()
    assert(v.validateInput("1.5e3"))
    assert(v.validateInput("-2.5E-4"))
    assert(v.validateInput("1E10"))
  }

  test("FloatValidator accepts special values") {
    val v = Validators.FloatValidator()
    assert(v.validateInput("NaN"))
    assert(v.validateInput("Infinity"))
    assert(v.validateInput("-Infinity"))
  }

  test("FloatValidator accepts strings with leading/trailing whitespace") {
    // Java's Float.parseFloat trims whitespace, so these are valid
    val v = Validators.FloatValidator()
    assert(v.validateInput(" 3.14"))
    assert(v.validateInput("3.14 "))
  }

  // ---------- GreaterThanValidator ----------

  test("GreaterThanValidator without equals") {
    val v = Validators.GreaterThanValidator(10.0f)
    assert(v.validateInput("11"))
    assert(v.validateInput("100.5"))
    assert(!v.validateInput("10"))
    assert(!v.validateInput("9"))
    assert(!v.validateInput("abc"))
  }

  test("GreaterThanValidator with equals") {
    val v = Validators.GreaterThanValidator(10.0f, useEquals = true)
    assert(v.validateInput("10"))
    assert(v.validateInput("11"))
    assert(!v.validateInput("9.99"))
  }

  test("GreaterThanValidator with zero threshold") {
    val v = Validators.GreaterThanValidator(0.0f)
    assert(v.validateInput("0.001"))
    assert(!v.validateInput("0"))
    assert(!v.validateInput("-1"))
  }

  test("GreaterThanValidator with negative threshold") {
    val v = Validators.GreaterThanValidator(-5.0f)
    assert(v.validateInput("-4"))
    assert(v.validateInput("0"))
    assert(!v.validateInput("-5"))
    assert(!v.validateInput("-6"))
  }

  test("GreaterThanValidator threshold can be changed") {
    val v = Validators.GreaterThanValidator(10.0f)
    assert(!v.validateInput("5"))
    v.greaterThan = 3.0f
    assert(v.validateInput("5"))
  }

  test("GreaterThanValidator useEquals can be toggled") {
    val v = Validators.GreaterThanValidator(10.0f, useEquals = false)
    assert(!v.validateInput("10"))
    v.useEquals = true
    assert(v.validateInput("10"))
  }

  // ---------- LesserThanValidator ----------

  test("LesserThanValidator without equals") {
    val v = Validators.LesserThanValidator(10.0f)
    assert(v.validateInput("9"))
    assert(v.validateInput("-5"))
    assert(!v.validateInput("10"))
    assert(!v.validateInput("11"))
    assert(!v.validateInput("abc"))
  }

  test("LesserThanValidator with equals") {
    val v = Validators.LesserThanValidator(10.0f, useEquals = true)
    assert(v.validateInput("10"))
    assert(v.validateInput("9"))
    assert(!v.validateInput("10.01"))
  }

  test("LesserThanValidator with zero threshold") {
    val v = Validators.LesserThanValidator(0.0f)
    assert(v.validateInput("-0.001"))
    assert(!v.validateInput("0"))
    assert(!v.validateInput("1"))
  }

  test("LesserThanValidator with negative threshold") {
    val v = Validators.LesserThanValidator(-5.0f)
    assert(v.validateInput("-6"))
    assert(v.validateInput("-100"))
    assert(!v.validateInput("-5"))
    assert(!v.validateInput("0"))
  }

  test("LesserThanValidator threshold can be changed") {
    val v = Validators.LesserThanValidator(10.0f)
    assert(v.validateInput("5"))
    v.lesserThan = 3.0f
    assert(!v.validateInput("5"))
  }

  test("LesserThanValidator useEquals can be toggled") {
    val v = Validators.LesserThanValidator(10.0f, useEquals = false)
    assert(!v.validateInput("10"))
    v.useEquals = true
    assert(v.validateInput("10"))
  }

  // ---------- Shared instances ----------

  test("shared INTEGERS and FLOATS instances work") {
    assert(Validators.INTEGERS.validateInput("42"))
    assert(!Validators.INTEGERS.validateInput("3.14"))
    assert(Validators.FLOATS.validateInput("3.14"))
    assert(!Validators.FLOATS.validateInput("abc"))
  }

  test("INTEGERS is an IntegerValidator instance") {
    assert(Validators.INTEGERS.isInstanceOf[Validators.IntegerValidator])
  }

  test("FLOATS is a FloatValidator instance") {
    assert(Validators.FLOATS.isInstanceOf[Validators.FloatValidator])
  }

  // ---------- Combined validators ----------

  test("GreaterThan and LesserThan combined define a range") {
    val lower   = Validators.GreaterThanValidator(0.0f, useEquals = true)
    val upper   = Validators.LesserThanValidator(100.0f, useEquals = true)
    val inRange = (s: String) => lower.validateInput(s) && upper.validateInput(s)
    assert(inRange("0"))
    assert(inRange("50"))
    assert(inRange("100"))
    assert(!inRange("-1"))
    assert(!inRange("101"))
  }

  test("Exclusive range rejects boundaries") {
    val lower   = Validators.GreaterThanValidator(0.0f, useEquals = false)
    val upper   = Validators.LesserThanValidator(1.0f, useEquals = false)
    val inRange = (s: String) => lower.validateInput(s) && upper.validateInput(s)
    assert(inRange("0.5"))
    assert(!inRange("0"))
    assert(!inRange("1"))
  }

  // ---------- InputValidator interface ----------

  test("All validators implement InputValidator") {
    val validators: List[InputValidator] = List(
      Validators.IntegerValidator(),
      Validators.FloatValidator(),
      Validators.GreaterThanValidator(0f),
      Validators.LesserThanValidator(100f)
    )
    // All should reject empty string
    validators.foreach { v =>
      assert(!v.validateInput(""), s"${v.getClass.getSimpleName} should reject empty string")
    }
  }

  test("All validators reject non-numeric garbage") {
    val validators: List[InputValidator] = List(
      Validators.IntegerValidator(),
      Validators.FloatValidator(),
      Validators.GreaterThanValidator(0f),
      Validators.LesserThanValidator(100f)
    )
    val garbage = List("abc", "!@#", "one", "null", "true")
    for {
      v <- validators
      g <- garbage
    } assert(!v.validateInput(g), s"${v.getClass.getSimpleName} should reject '$g'")
  }
}
