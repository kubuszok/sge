package sge
package visui

class SizesSuite extends munit.FunSuite {

  test("Sizes default values are zero") {
    val s = Sizes()
    assertEqualsFloat(s.scaleFactor, 0f, 0.001f)
    assertEqualsFloat(s.spacingTop, 0f, 0.001f)
    assertEqualsFloat(s.borderSize, 0f, 0.001f)
    assertEqualsFloat(s.spinnerButtonHeight, 0f, 0.001f)
  }

  test("Sizes copy constructor copies all fields") {
    val original = Sizes()
    original.scaleFactor = 2.0f
    original.spacingTop = 10.0f
    original.spacingBottom = 5.0f
    original.spacingRight = 8.0f
    original.spacingLeft = 8.0f
    original.buttonBarSpacing = 12.0f
    original.menuItemIconSize = 24.0f
    original.borderSize = 1.0f
    original.spinnerButtonHeight = 16.0f
    original.spinnerFieldSize = 40.0f
    original.fileChooserViewModeBigIconsSize = 64.0f
    original.fileChooserViewModeMediumIconsSize = 32.0f
    original.fileChooserViewModeSmallIconsSize = 16.0f
    original.fileChooserViewModeListWidthSize = 200.0f

    val copy = Sizes(original)
    assertEqualsFloat(copy.scaleFactor, 2.0f, 0.001f)
    assertEqualsFloat(copy.spacingTop, 10.0f, 0.001f)
    assertEqualsFloat(copy.spacingBottom, 5.0f, 0.001f)
    assertEqualsFloat(copy.spacingRight, 8.0f, 0.001f)
    assertEqualsFloat(copy.spacingLeft, 8.0f, 0.001f)
    assertEqualsFloat(copy.buttonBarSpacing, 12.0f, 0.001f)
    assertEqualsFloat(copy.menuItemIconSize, 24.0f, 0.001f)
    assertEqualsFloat(copy.borderSize, 1.0f, 0.001f)
    assertEqualsFloat(copy.spinnerButtonHeight, 16.0f, 0.001f)
    assertEqualsFloat(copy.spinnerFieldSize, 40.0f, 0.001f)
    assertEqualsFloat(copy.fileChooserViewModeBigIconsSize, 64.0f, 0.001f)
    assertEqualsFloat(copy.fileChooserViewModeMediumIconsSize, 32.0f, 0.001f)
    assertEqualsFloat(copy.fileChooserViewModeSmallIconsSize, 16.0f, 0.001f)
    assertEqualsFloat(copy.fileChooserViewModeListWidthSize, 200.0f, 0.001f)
  }

  test("Sizes copy is independent of original") {
    val original = Sizes()
    original.scaleFactor = 1.0f
    val copy = Sizes(original)
    original.scaleFactor = 3.0f
    assertEqualsFloat(copy.scaleFactor, 1.0f, 0.001f)
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float)(using munit.Location): Unit =
    assert(Math.abs(actual - expected) <= delta, s"expected $expected +/- $delta but got $actual")
}
