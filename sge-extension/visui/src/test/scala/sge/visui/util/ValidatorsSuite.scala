package sge
package visui
package util

class ValidatorsSuite extends munit.FunSuite {

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

  test("shared INTEGERS and FLOATS instances work") {
    assert(Validators.INTEGERS.validateInput("42"))
    assert(!Validators.INTEGERS.validateInput("3.14"))
    assert(Validators.FLOATS.validateInput("3.14"))
    assert(!Validators.FLOATS.validateInput("abc"))
  }

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
}
