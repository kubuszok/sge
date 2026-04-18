/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package rgb

import sge.colorful.FloatColors
import sge.graphics.Color
import sge.graphics.Colors

import scala.collection.mutable

/** A palette of predefined colors as packed RGB floats, the kind [[ColorTools]] works with, plus a way to describe colors by combinations and adjustments. The description code revolves around
  * [[parseDescription]], which takes a color description String and returns a packed float color. The color descriptions look like "darker rich mint sage", where the order of the words doesn't
  * matter. They can optionally include lightness changes (light/dark), and saturation changes (rich/dull), and must include one or more color names that will be mixed together (repeats are allowed to
  * use a color more heavily). The changes can be suffixed with "er", "est", or "most", such as "duller", "lightest", or "richmost", to progressively increase their effect on lightness or saturation.
  *
  * The rest of this is about the same as in [[Palette]].
  *
  * You can access colors by their constant name, such as [[CACTUS]], by the [[NAMED]] map using `NAMED.getOrElse("cactus", 0f)`, or by index in the ArrayBuffer called [[LIST]]. Note that to access a
  * float color from NAMED, you need to give a default value if the name is not found; `0f` is a good default because it is used for fully-transparent black. You can access the names in a specific
  * order with [[NAMES]] (which is alphabetical), [[NAMES_BY_HUE]] (which is sorted by the hue of the matching color, from red to yellow to blue (with gray around here) to purple to red again), or
  * [[NAMES_BY_LIGHTNESS]] (which is sorted by the intensity of the matching color, from darkest to lightest). Having a name lets you look up the matching color in [[NAMED]].
  */
object SimplePalette {

  /** You can look up colors by name here; the names are lower-case, and the colors are packed floats in rgba format. */
  val NAMED: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Stores alternative names for colors in [[NAMED]], like "grey" as an alias for [[GRAY]] or "gold" as an alias for [[SAFFRON]]. Currently, the list of aliases is as follows:
    *   - "grey" maps to [[GRAY]],
    *   - "gold" maps to [[SAFFRON]],
    *   - "puce" maps to [[MAUVE]],
    *   - "sand" maps to [[TAN]],
    *   - "skin" maps to [[PEACH]],
    *   - "coral" maps to [[SALMON]],
    *   - "azure" maps to [[SKY]],
    *   - "ocean" maps to [[TEAL]], and
    *   - "sapphire" maps to [[COBALT]].
    *
    * Note that these aliases are not duplicated in [[NAMES]], [[NAMES_BY_HUE]], or [[NAMES_BY_LIGHTNESS]]; they are primarily there so blind attempts to name a color might still work.
    */
  val ALIASES: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Lists the packed float color values in this, in no particular order. Does not include duplicates from aliases. */
  val LIST: mutable.ArrayBuffer[Float] = mutable.ArrayBuffer.empty[Float]

  /** This color constant "transparent" has RGBA8888 code 00000000, R 0.0, G 0.0, B 0.0, alpha 0.0, hue 0.0, and saturation 0.0. It can be represented as a packed float with the constant 0x0.0p0F.
    */
  val TRANSPARENT: Float = java.lang.Float.intBitsToFloat(0x00000000)

  /** This color constant "black" has RGBA8888 code 000000FF, R 0.0, G 0.0, B 0.0, alpha 1.0, hue 0.0, and saturation 0.0. It can be represented as a packed float with the constant -0x1.0p125F. */
  val BLACK: Float = java.lang.Float.intBitsToFloat(0xfe000000)

  /** This color constant "gray" has RGBA8888 code 808080FF, R 0.5019608, G 0.5019608, B 0.5019608, alpha 1.0, hue 0.0, and saturation 0.0. It can be represented as a packed float with the constant
    * -0x1.0101p126F.
    */
  val GRAY: Float = java.lang.Float.intBitsToFloat(0xfe808080)

  /** This color constant "silver" has RGBA8888 code B6B6B6FF, R 0.7137255, G 0.7137255, B 0.7137255, alpha 1.0, hue 0.0, and saturation 0.0. It can be represented as a packed float with the constant
    * -0x1.6d6d6cp126F.
    */
  val SILVER: Float = java.lang.Float.intBitsToFloat(0xfeb6b6b6)

  /** This color constant "white" has RGBA8888 code FFFFFFFF, R 1.0, G 1.0, B 1.0, alpha 1.0, hue 0.0, and saturation 0.0. It can be represented as a packed float with the constant -0x1.fffffep126F.
    */
  val WHITE: Float = java.lang.Float.intBitsToFloat(0xfeffffff)

  /** This color constant "red" has RGBA8888 code FF0000FF, R 1.0, G 0.0, B 0.0, alpha 1.0, hue 0.0, and saturation 1.0. It can be represented as a packed float with the constant -0x1.0001fep125F.
    */
  val RED: Float = java.lang.Float.intBitsToFloat(0xfe0000ff)

  /** This color constant "orange" has RGBA8888 code FF7F00FF, R 1.0, G 0.49803922, B 0.0, alpha 1.0, hue 0.08300654, and saturation 1.0. It can be represented as a packed float with the constant
    * -0x1.00fffep125F.
    */
  val ORANGE: Float = java.lang.Float.intBitsToFloat(0xfe007fff)

  /** This color constant "yellow" has RGBA8888 code FFFF00FF, R 1.0, G 1.0, B 0.0, alpha 1.0, hue 0.16666667, and saturation 1.0. It can be represented as a packed float with the constant
    * -0x1.01fffep125F.
    */
  val YELLOW: Float = java.lang.Float.intBitsToFloat(0xfe00ffff)

  /** This color constant "green" has RGBA8888 code 00FF00FF, R 0.0, G 1.0, B 0.0, alpha 1.0, hue 0.33333334, and saturation 1.0. It can be represented as a packed float with the constant
    * -0x1.01fep125F.
    */
  val GREEN: Float = java.lang.Float.intBitsToFloat(0xfe00ff00)

  /** This color constant "blue" has RGBA8888 code 0000FFFF, R 0.0, G 0.0, B 1.0, alpha 1.0, hue 0.6666667, and saturation 1.0. It can be represented as a packed float with the constant -0x1.fep126F.
    */
  val BLUE: Float = java.lang.Float.intBitsToFloat(0xfeff0000)

  /** This color constant "indigo" has RGBA8888 code 520FE0FF, R 0.32156864, G 0.05882353, B 0.8784314, alpha 1.0, hue 0.7200957, and saturation 0.81960785. It can be represented as a packed float
    * with the constant -0x1.c01ea4p126F.
    */
  val INDIGO: Float = java.lang.Float.intBitsToFloat(0xfee00f52)

  /** This color constant "violet" has RGBA8888 code 9040EFFF, R 0.5647059, G 0.2509804, B 0.9372549, alpha 1.0, hue 0.74285716, and saturation 0.6862745. It can be represented as a packed float with
    * the constant -0x1.de812p126F.
    */
  val VIOLET: Float = java.lang.Float.intBitsToFloat(0xfeef4090)

  /** This color constant "purple" has RGBA8888 code C000FFFF, R 0.7529412, G 0.0, B 1.0, alpha 1.0, hue 0.7921569, and saturation 1.0. It can be represented as a packed float with the constant
    * -0x1.fe018p126F.
    */
  val PURPLE: Float = java.lang.Float.intBitsToFloat(0xfeff00c0)

  /** This color constant "brown" has RGBA8888 code 8F573BFF, R 0.56078434, G 0.34117648, B 0.23137255, alpha 1.0, hue 0.055555552, and saturation 0.3294118. It can be represented as a packed float
    * with the constant -0x1.76af1ep125F.
    */
  val BROWN: Float = java.lang.Float.intBitsToFloat(0xfe3b578f)

  /** This color constant "pink" has RGBA8888 code FFA0E0FF, R 1.0, G 0.627451, B 0.8784314, alpha 1.0, hue 0.8877193, and saturation 0.372549. It can be represented as a packed float with the
    * constant -0x1.c141fep126F.
    */
  val PINK: Float = java.lang.Float.intBitsToFloat(0xfee0a0ff)

  /** This color constant "magenta" has RGBA8888 code F500F5FF, R 0.9607843, G 0.0, B 0.9607843, alpha 1.0, hue 0.8333333, and saturation 0.9607843. It can be represented as a packed float with the
    * constant -0x1.ea01eap126F.
    */
  val MAGENTA: Float = java.lang.Float.intBitsToFloat(0xfef500f5)

  /** This color constant "brick" has RGBA8888 code D5524AFF, R 0.8352941, G 0.32156864, B 0.2901961, alpha 1.0, hue 0.009592325, and saturation 0.54509807. It can be represented as a packed float
    * with the constant -0x1.94a5aap125F.
    */
  val BRICK: Float = java.lang.Float.intBitsToFloat(0xfe4a52d5)

  /** This color constant "ember" has RGBA8888 code F55A32FF, R 0.9607843, G 0.3529412, B 0.19607843, alpha 1.0, hue 0.034188036, and saturation 0.7647059. It can be represented as a packed float with
    * the constant -0x1.64b5eap125F.
    */
  val EMBER: Float = java.lang.Float.intBitsToFloat(0xfe325af5)

  /** This color constant "salmon" has RGBA8888 code FF6262FF, R 1.0, G 0.38431373, B 0.38431373, alpha 1.0, hue 0.0, and saturation 0.6156863. It can be represented as a packed float with the
    * constant -0x1.c4c5fep125F.
    */
  val SALMON: Float = java.lang.Float.intBitsToFloat(0xfe6262ff)

  /** This color constant "chocolate" has RGBA8888 code 683818FF, R 0.40784314, G 0.21960784, B 0.09411765, alpha 1.0, hue 0.066666655, and saturation 0.3137255. It can be represented as a packed
    * float with the constant -0x1.3070dp125F.
    */
  val CHOCOLATE: Float = java.lang.Float.intBitsToFloat(0xfe183868)

  /** This color constant "tan" has RGBA8888 code D2B48CFF, R 0.8235294, G 0.7058824, B 0.54901963, alpha 1.0, hue 0.0952381, and saturation 0.2745098. It can be represented as a packed float with the
    * constant -0x1.1969a4p126F.
    */
  val TAN: Float = java.lang.Float.intBitsToFloat(0xfe8cb4d2)

  /** This color constant "bronze" has RGBA8888 code CE8E31FF, R 0.80784315, G 0.5568628, B 0.19215687, alpha 1.0, hue 0.09872612, and saturation 0.6156863. It can be represented as a packed float
    * with the constant -0x1.631d9cp125F.
    */
  val BRONZE: Float = java.lang.Float.intBitsToFloat(0xfe318ece)

  /** This color constant "cinnamon" has RGBA8888 code D2691DFF, R 0.8235294, G 0.4117647, B 0.11372549, alpha 1.0, hue 0.06998159, and saturation 0.70980394. It can be represented as a packed float
    * with the constant -0x1.3ad3a4p125F.
    */
  val CINNAMON: Float = java.lang.Float.intBitsToFloat(0xfe1d69d2)

  /** This color constant "apricot" has RGBA8888 code FFA828FF, R 1.0, G 0.65882355, B 0.15686275, alpha 1.0, hue 0.09922481, and saturation 0.84313726. It can be represented as a packed float with
    * the constant -0x1.5151fep125F.
    */
  val APRICOT: Float = java.lang.Float.intBitsToFloat(0xfe28a8ff)

  /** This color constant "peach" has RGBA8888 code FFBF81FF, R 1.0, G 0.7490196, B 0.5058824, alpha 1.0, hue 0.08201058, and saturation 0.49411762. It can be represented as a packed float with the
    * constant -0x1.037ffep126F.
    */
  val PEACH: Float = java.lang.Float.intBitsToFloat(0xfe81bfff)

  /** This color constant "pear" has RGBA8888 code D3E330FF, R 0.827451, G 0.8901961, B 0.1882353, alpha 1.0, hue 0.18156426, and saturation 0.7019608. It can be represented as a packed float with the
    * constant -0x1.61c7a6p125F.
    */
  val PEAR: Float = java.lang.Float.intBitsToFloat(0xfe30e3d3)

  /** This color constant "saffron" has RGBA8888 code FFD510FF, R 1.0, G 0.8352941, B 0.0627451, alpha 1.0, hue 0.13737796, and saturation 0.9372549. It can be represented as a packed float with the
    * constant -0x1.21abfep125F.
    */
  val SAFFRON: Float = java.lang.Float.intBitsToFloat(0xfe10d5ff)

  /** This color constant "butter" has RGBA8888 code FFF288FF, R 1.0, G 0.9490196, B 0.53333336, alpha 1.0, hue 0.14845939, and saturation 0.46666664. It can be represented as a packed float with the
    * constant -0x1.11e5fep126F.
    */
  val BUTTER: Float = java.lang.Float.intBitsToFloat(0xfe88f2ff)

  /** This color constant "chartreuse" has RGBA8888 code C8FF41FF, R 0.78431374, G 1.0, B 0.25490198, alpha 1.0, hue 0.21491227, and saturation 0.745098. It can be represented as a packed float with
    * the constant -0x1.83ff9p125F.
    */
  val CHARTREUSE: Float = java.lang.Float.intBitsToFloat(0xfe41ffc8)

  /** This color constant "cactus" has RGBA8888 code 30A000FF, R 0.1882353, G 0.627451, B 0.0, alpha 1.0, hue 0.28333336, and saturation 0.627451. It can be represented as a packed float with the
    * constant -0x1.01406p125F.
    */
  val CACTUS: Float = java.lang.Float.intBitsToFloat(0xfe00a030)

  /** This color constant "lime" has RGBA8888 code 93D300FF, R 0.5764706, G 0.827451, B 0.0, alpha 1.0, hue 0.21721959, and saturation 0.827451. It can be represented as a packed float with the
    * constant -0x1.01a726p125F.
    */
  val LIME: Float = java.lang.Float.intBitsToFloat(0xfe00d393)

  /** This color constant "olive" has RGBA8888 code 818000FF, R 0.5058824, G 0.5019608, B 0.0, alpha 1.0, hue 0.16537468, and saturation 0.5058824. It can be represented as a packed float with the
    * constant -0x1.010102p125F.
    */
  val OLIVE: Float = java.lang.Float.intBitsToFloat(0xfe008081)

  /** This color constant "fern" has RGBA8888 code 4E7942FF, R 0.30588236, G 0.4745098, B 0.25882354, alpha 1.0, hue 0.2969697, and saturation 0.21568626. It can be represented as a packed float with
    * the constant -0x1.84f29cp125F.
    */
  val FERN: Float = java.lang.Float.intBitsToFloat(0xfe42794e)

  /** This color constant "moss" has RGBA8888 code 204608FF, R 0.1254902, G 0.27450982, B 0.03137255, alpha 1.0, hue 0.26881722, and saturation 0.24313727. It can be represented as a packed float with
    * the constant -0x1.108c4p125F.
    */
  val MOSS: Float = java.lang.Float.intBitsToFloat(0xfe084620)

  /** This color constant "celery" has RGBA8888 code 7DFF73FF, R 0.49019608, G 1.0, B 0.4509804, alpha 1.0, hue 0.32142857, and saturation 0.5490196. It can be represented as a packed float with the
    * constant -0x1.e7fefap125F.
    */
  val CELERY: Float = java.lang.Float.intBitsToFloat(0xfe73ff7d)

  /** This color constant "sage" has RGBA8888 code ABE3C5FF, R 0.67058825, G 0.8901961, B 0.77254903, alpha 1.0, hue 0.4107143, and saturation 0.21960783. It can be represented as a packed float with
    * the constant -0x1.8bc756p126F.
    */
  val SAGE: Float = java.lang.Float.intBitsToFloat(0xfec5e3ab)

  /** This color constant "jade" has RGBA8888 code 3FBF3FFF, R 0.24705882, G 0.7490196, B 0.24705882, alpha 1.0, hue 0.33333334, and saturation 0.5019608. It can be represented as a packed float with
    * the constant -0x1.7f7e7ep125F.
    */
  val JADE: Float = java.lang.Float.intBitsToFloat(0xfe3fbf3f)

  /** This color constant "cyan" has RGBA8888 code 00FFFFFF, R 0.0, G 1.0, B 1.0, alpha 1.0, hue 0.5, and saturation 1.0. It can be represented as a packed float with the constant -0x1.fffep126F.
    */
  val CYAN: Float = java.lang.Float.intBitsToFloat(0xfeffff00)

  /** This color constant "mint" has RGBA8888 code 7FFFD4FF, R 0.49803922, G 1.0, B 0.83137256, alpha 1.0, hue 0.44401044, and saturation 0.50196075. It can be represented as a packed float with the
    * constant -0x1.a9fefep126F.
    */
  val MINT: Float = java.lang.Float.intBitsToFloat(0xfed4ff7f)

  /** This color constant "teal" has RGBA8888 code 007F7FFF, R 0.0, G 0.49803922, B 0.49803922, alpha 1.0, hue 0.5, and saturation 0.49803922. It can be represented as a packed float with the constant
    * -0x1.fefep125F.
    */
  val TEAL: Float = java.lang.Float.intBitsToFloat(0xfe7f7f00)

  /** This color constant "turquoise" has RGBA8888 code 2ED6C9FF, R 0.18039216, G 0.8392157, B 0.7882353, alpha 1.0, hue 0.48710316, and saturation 0.65882355. It can be represented as a packed float
    * with the constant -0x1.93ac5cp126F.
    */
  val TURQUOISE: Float = java.lang.Float.intBitsToFloat(0xfec9d62e)

  /** This color constant "sky" has RGBA8888 code 10C0E0FF, R 0.0627451, G 0.7529412, B 0.8784314, alpha 1.0, hue 0.5256411, and saturation 0.8156863. It can be represented as a packed float with the
    * constant -0x1.c1802p126F.
    */
  val SKY: Float = java.lang.Float.intBitsToFloat(0xfee0c010)

  /** This color constant "cobalt" has RGBA8888 code 0046ABFF, R 0.0, G 0.27450982, B 0.67058825, alpha 1.0, hue 0.5984406, and saturation 0.67058825. It can be represented as a packed float with the
    * constant -0x1.568cp126F.
    */
  val COBALT: Float = java.lang.Float.intBitsToFloat(0xfeab4600)

  /** This color constant "denim" has RGBA8888 code 3088B8FF, R 0.1882353, G 0.53333336, B 0.72156864, alpha 1.0, hue 0.5588235, and saturation 0.53333336. It can be represented as a packed float with
    * the constant -0x1.71106p126F.
    */
  val DENIM: Float = java.lang.Float.intBitsToFloat(0xfeb88830)

  /** This color constant "navy" has RGBA8888 code 000080FF, R 0.0, G 0.0, B 0.5019608, alpha 1.0, hue 0.6666667, and saturation 0.5019608. It can be represented as a packed float with the constant
    * -0x1.0p126F.
    */
  val NAVY: Float = java.lang.Float.intBitsToFloat(0xfe800000)

  /** This color constant "lavender" has RGBA8888 code B991FFFF, R 0.7254902, G 0.5686275, B 1.0, alpha 1.0, hue 0.72727275, and saturation 0.43137252. It can be represented as a packed float with the
    * constant -0x1.ff2372p126F.
    */
  val LAVENDER: Float = java.lang.Float.intBitsToFloat(0xfeff91b9)

  /** This color constant "plum" has RGBA8888 code BE0DC6FF, R 0.74509805, G 0.050980393, B 0.7764706, alpha 1.0, hue 0.82612616, and saturation 0.7254902. It can be represented as a packed float with
    * the constant -0x1.8c1b7cp126F.
    */
  val PLUM: Float = java.lang.Float.intBitsToFloat(0xfec60dbe)

  /** This color constant "mauve" has RGBA8888 code AB73ABFF, R 0.67058825, G 0.4509804, B 0.67058825, alpha 1.0, hue 0.8333334, and saturation 0.21960786. It can be represented as a packed float with
    * the constant -0x1.56e756p126F.
    */
  val MAUVE: Float = java.lang.Float.intBitsToFloat(0xfeab73ab)

  /** This color constant "rose" has RGBA8888 code E61E78FF, R 0.9019608, G 0.11764706, B 0.47058824, alpha 1.0, hue 0.925, and saturation 0.78431374. It can be represented as a packed float with the
    * constant -0x1.f03dccp125F.
    */
  val ROSE: Float = java.lang.Float.intBitsToFloat(0xfe781ee6)

  /** This color constant "raspberry" has RGBA8888 code 911437FF, R 0.5686275, G 0.078431375, B 0.21568628, alpha 1.0, hue 0.9533333, and saturation 0.4901961. It can be represented as a packed float
    * with the constant -0x1.6e2922p125F.
    */
  val RASPBERRY: Float = java.lang.Float.intBitsToFloat(0xfe371491)

  /** All names for colors in this palette, in alphabetical order. You can fetch the corresponding packed float color by looking up a name in [[NAMED]]. */
  val NAMES: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty[String]

  /** All names for colors in this palette, with grayscale first, then sorted by hue from red to yellow to green to blue. You can fetch the corresponding packed float color by looking up a name in
    * [[NAMED]].
    */
  val NAMES_BY_HUE: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty[String]

  val COLORS_BY_HUE: mutable.ArrayBuffer[Float] = mutable.ArrayBuffer.empty[Float]

  /** All names for colors in this palette, sorted by lightness from black to white. You can fetch the corresponding packed float color by looking up a name in [[NAMED]]. */
  val NAMES_BY_LIGHTNESS: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty[String]

  private val namesByHue:  mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty[String]
  private val colorsByHue: mutable.ArrayBuffer[Float]  = mutable.ArrayBuffer.empty[Float]

  private val lightAdjectives: Array[String] =
    Array("darkmost ", "darkest ", "darker ", "dark ", "", "light ", "lighter ", "lightest ", "lightmost ")
  private val satAdjectives: Array[String] =
    Array("dullmost ", "dullest ", "duller ", "dull ", "", "rich ", "richer ", "richest ", "richmost ")
  private val combinedAdjectives: Array[String] = new Array[String](81)

  // Register all colors
  locally {
    val colors: Array[(String, Float)] = Array(
      "transparent" -> TRANSPARENT,
      "black" -> BLACK,
      "gray" -> GRAY,
      "silver" -> SILVER,
      "white" -> WHITE,
      "red" -> RED,
      "orange" -> ORANGE,
      "yellow" -> YELLOW,
      "green" -> GREEN,
      "blue" -> BLUE,
      "indigo" -> INDIGO,
      "violet" -> VIOLET,
      "purple" -> PURPLE,
      "brown" -> BROWN,
      "pink" -> PINK,
      "magenta" -> MAGENTA,
      "brick" -> BRICK,
      "ember" -> EMBER,
      "salmon" -> SALMON,
      "chocolate" -> CHOCOLATE,
      "tan" -> TAN,
      "bronze" -> BRONZE,
      "cinnamon" -> CINNAMON,
      "apricot" -> APRICOT,
      "peach" -> PEACH,
      "pear" -> PEAR,
      "saffron" -> SAFFRON,
      "butter" -> BUTTER,
      "chartreuse" -> CHARTREUSE,
      "cactus" -> CACTUS,
      "lime" -> LIME,
      "olive" -> OLIVE,
      "fern" -> FERN,
      "moss" -> MOSS,
      "celery" -> CELERY,
      "sage" -> SAGE,
      "jade" -> JADE,
      "cyan" -> CYAN,
      "mint" -> MINT,
      "teal" -> TEAL,
      "turquoise" -> TURQUOISE,
      "sky" -> SKY,
      "cobalt" -> COBALT,
      "denim" -> DENIM,
      "navy" -> NAVY,
      "lavender" -> LAVENDER,
      "plum" -> PLUM,
      "mauve" -> MAUVE,
      "rose" -> ROSE,
      "raspberry" -> RASPBERRY
    )
    for ((name, color) <- colors) {
      NAMED.put(name, color)
      LIST += color
    }

    // Build NAMES (alphabetical)
    NAMES ++= NAMED.keys
    NAMES.sortInPlace()

    // Build NAMES_BY_HUE
    NAMES_BY_HUE ++= NAMES
    NAMES_BY_HUE.sortInPlace()(using
      Ordering.fromLessThan[String] { (o1, o2) =>
        val c1 = NAMED.getOrElse(o1, TRANSPARENT)
        val c2 = NAMED.getOrElse(o2, TRANSPARENT)
        val s1 = ColorTools.saturation(c1)
        val s2 = ColorTools.saturation(c2)
        // a packed float color with a sign bit of 0 (a non-negative number) is mostly transparent.
        // this also considers 0x80000000 transparent, but it's almost at the threshold.
        val cmp =
          if (c1 >= 0f) -10000
          else if (c2 >= 0f) 10000
          else if (s1 <= 0.05f && s2 > 0.05f) -1000
          else if (s1 > 0.05f && s2 <= 0.05f) 1000
          else if (s1 <= 0.05f && s2 <= 0.05f)
            Math.signum(ColorTools.lightness(c1) - ColorTools.lightness(c2)).toInt
          else
            2 * Math.signum(ColorTools.hue(c1) - ColorTools.hue(c2)).toInt +
              Math.signum(ColorTools.lightness(c1) - ColorTools.lightness(c2)).toInt
        cmp < 0
      }
    )
    for (name <- NAMES_BY_HUE)
      COLORS_BY_HUE += NAMED.getOrElse(name, TRANSPARENT)

    // Build NAMES_BY_LIGHTNESS
    NAMES_BY_LIGHTNESS ++= NAMES
    NAMES_BY_LIGHTNESS.sortInPlace()(using
      Ordering.fromLessThan[String] { (o1, o2) =>
        java.lang.Float.compare(
          ColorTools.lightness(NAMED.getOrElse(o1, TRANSPARENT)),
          ColorTools.lightness(NAMED.getOrElse(o2, TRANSPARENT))
        ) < 0
      }
    )

    // Build namesByHue and colorsByHue (private, for bestMatch; excludes "transparent")
    namesByHue ++= NAMES_BY_HUE
    colorsByHue ++= COLORS_BY_HUE
    val trn = namesByHue.indexOf("transparent")
    if (trn >= 0) {
      namesByHue.remove(trn)
      colorsByHue.remove(trn)
    }

    // Register aliases
    ALIASES.put("grey", GRAY)
    ALIASES.put("gold", SAFFRON)
    ALIASES.put("puce", MAUVE)
    ALIASES.put("sand", TAN)
    ALIASES.put("skin", PEACH) // Yes, I am aware that there is more than one skin color, but this can only map to one.
    ALIASES.put("coral", SALMON)
    ALIASES.put("azure", SKY)
    ALIASES.put("ocean", TEAL)
    ALIASES.put("sapphire", COBALT)
    for ((name, color) <- ALIASES)
      NAMED.put(name, color)

    // Build combinedAdjectives
    var idx = 0
    for (sat <- 0 until 9)
      for (lit <- 0 until 9) {
        val s = sat - 4
        val l = lit - 4
        if (s != l && s != -l)
          combinedAdjectives(idx) = lightAdjectives(lit) + satAdjectives(sat)
        idx += 1
      }

    // Special cases for multiple-effect adjectives:
    combinedAdjectives(0 * 9 + 0) = "weakmost "
    combinedAdjectives(1 * 9 + 1) = "weakest "
    combinedAdjectives(2 * 9 + 2) = "weaker "
    combinedAdjectives(3 * 9 + 3) = "weak "

    combinedAdjectives(0 * 9 + 8) = "palemost "
    combinedAdjectives(1 * 9 + 7) = "palest "
    combinedAdjectives(2 * 9 + 6) = "paler "
    combinedAdjectives(3 * 9 + 5) = "pale "

    combinedAdjectives(8 * 9 + 0) = "deepmost "
    combinedAdjectives(7 * 9 + 1) = "deepest "
    combinedAdjectives(6 * 9 + 2) = "deeper "
    combinedAdjectives(5 * 9 + 3) = "deep "

    combinedAdjectives(8 * 9 + 8) = "brightmost "
    combinedAdjectives(7 * 9 + 7) = "brightest "
    combinedAdjectives(6 * 9 + 6) = "brighter "
    combinedAdjectives(5 * 9 + 5) = "bright "

    combinedAdjectives(4 * 9 + 4) = ""
  }

  /** Parses a color description and returns the approximate color it describes, as a packed RGBA float color. Color descriptions consist of one or more alphabetical words, separated by
    * non-alphanumeric characters (typically spaces and/or hyphens, though the underscore is treated as a letter). Any word that is the name of a color in this palette will be looked up in [[NAMED]]
    * and tracked; if there is more than one of these color name words, the colors will be mixed using [[FloatColors.unevenMix]], or if there is just one color name word, then the corresponding color
    * will be used. A number can be present after a color name (separated by any non-alphanumeric character(s) other than the underscore); if so, it acts as a positive weight for that color name when
    * mixed with other named colors. The recommended separator between a color name and its weight is the char '^', but other punctuation like ':' is equally valid. You can also repeat a color name to
    * increase its weight. You may use a decimal point in weights to make them floats.
    *
    * The special adjectives "light" and "dark" change the lightness of the described color; likewise, "rich" and "dull" change the saturation (how different the color is from grayscale). All of these
    * adjectives can have "-er" or "-est" appended to make their effect twice or three times as strong. Technically, the chars appended to an adjective don't matter, only their count, so "lightaa" is
    * the same as "lighter" and "richcat" is the same as "richest". There's an unofficial fourth level as well, used when any 4 characters are appended to an adjective (as in "darkmost"); it has four
    * times the effect of the original adjective. There are also the adjectives "bright" (equivalent to "light rich"), "pale" ("light dull"), "deep" ("dark rich"), and "weak" ("dark dull"). These can
    * be amplified like the other four, except that "pale" goes to "paler", "palest", and then to "palemax" or (its equivalent) "palemost", where only the word length is checked. The case of
    * adjectives doesn't matter here; they can be all-caps, all lower-case, or mixed-case without issues. The names of colors, however, are case-sensitive, because you can combine other named color
    * palettes with the one here, and at least in one common situation (merging libGDX Colors with the palette here), the other palette uses all-caps names only.
    *
    * If part of a color name or adjective is invalid, it is not considered; if the description is empty or fully invalid, this returns the float color `0f`, or fully transparent black.
    *
    * Examples of valid descriptions include "blue", "dark green", "duller red", "peach pink", "indigo purple mauve", "lightest richer apricot-olive", "bright magenta", "palest cyan blue", "deep fern
    * black", "weakmost celery", "red^3 orange", and "dark deep blue^7 cyan^3".
    *
    * This overload always reads the whole String provided.
    *
    * @param description
    *   a color description, as a String matching the above format
    * @return
    *   a packed RGBA float color as described
    */
  def parseDescription(description: String): Float =
    parseDescription(description, 0, description.length)

  /** Parses a color description and returns the approximate color it describes, as a packed RGBA float color. Color descriptions consist of one or more alphabetical words, separated by
    * non-alphanumeric characters (typically spaces and/or hyphens, though the underscore is treated as a letter). Any word that is the name of a color in this palette will be looked up in [[NAMED]]
    * and tracked; if there is more than one of these color name words, the colors will be mixed using [[FloatColors.unevenMix]], or if there is just one color name word, then the corresponding color
    * will be used. A number can be present after a color name (separated by any non-alphanumeric character(s) other than the underscore); if so, it acts as a positive weight for that color name when
    * mixed with other named colors. The recommended separator between a color name and its weight is the char '^', but other punctuation like ':' is equally valid. You can also repeat a color name to
    * increase its weight. You may use a decimal point in weights to make them floats.
    *
    * The special adjectives "light" and "dark" change the lightness of the described color; likewise, "rich" and "dull" change the saturation (how different the color is from grayscale). All of these
    * adjectives can have "-er" or "-est" appended to make their effect twice or three times as strong. Technically, the chars appended to an adjective don't matter, only their count, so "lightaa" is
    * the same as "lighter" and "richcat" is the same as "richest". There's an unofficial fourth level as well, used when any 4 characters are appended to an adjective (as in "darkmost"); it has four
    * times the effect of the original adjective. There are also the adjectives "bright" (equivalent to "light rich"), "pale" ("light dull"), "deep" ("dark rich"), and "weak" ("dark dull"). These can
    * be amplified like the other four, except that "pale" goes to "paler", "palest", and then to "palemax" or (its equivalent) "palemost", where only the word length is checked. The case of
    * adjectives doesn't matter here; they can be all-caps, all lower-case, or mixed-case without issues. The names of colors, however, are case-sensitive, because you can combine other named color
    * palettes with the one here, and at least in one common situation (merging libGDX Colors with the palette here), the other palette uses all-caps names only.
    *
    * If part of a color name or adjective is invalid, it is not considered; if the description is empty or fully invalid, this returns the float color `0f`, or fully transparent black.
    *
    * Examples of valid descriptions include "blue", "dark green", "duller red", "peach pink", "indigo purple mauve", "lightest richer apricot-olive", "bright magenta", "palest cyan blue", "deep fern
    * black", "weakmost celery", "red^3 orange", and "dark deep blue^7 cyan^3".
    *
    * This overload lets you specify a starting index in `description` to read from and a maximum `length` to read before stopping. If `length` is negative, this reads the rest of `description` after
    * `start`.
    *
    * @param description
    *   a color description, as a String matching the above format
    * @param start
    *   the first character index of the description to read from
    * @param length
    *   how much of description to attempt to parse; if negative, this parses until the end
    * @return
    *   a packed RGBA float color as described
    */
  def parseDescription(description: String, start: Int, length: Int): Float = {
    var lightness  = 0f
    var saturation = 0f
    val end        =
      if (length < 0) description.length - start
      else Math.min(description.length, start + length)
    val terms  = description.substring(start, end).split("[^a-zA-Z0-9_.]+")
    val mixing = mutable.ArrayBuffer.empty[Float]

    for (term <- terms if term != null && term.nonEmpty) {
      val len     = term.length
      var handled = false

      term.charAt(0) match {
        case 'L' | 'l' =>
          if (len > 2 && (term.charAt(2) == 'g' || term.charAt(2) == 'G')) { // light
            // Java fall-through: case 9 falls to 8, 8 to 7, 7 to 5
            len match {
              case 9 => lightness += 0.20f * 4; handled = true // lightmost
              case 8 => lightness += 0.20f * 3; handled = true // lightest
              case 7 => lightness += 0.20f * 2; handled = true // lighter
              case 5 => lightness += 0.20f; handled = true // light
              case _ => // not a modifier
            }
          }
        case 'B' | 'b' =>
          if (len > 3 && (term.charAt(3) == 'g' || term.charAt(3) == 'G')) { // bright
            len match {
              case 10 => lightness += 0.20f * 4; saturation += 0.20f * 4; handled = true // brightmost
              case 9  => lightness += 0.20f * 3; saturation += 0.20f * 3; handled = true // brightest
              case 8  => lightness += 0.20f * 2; saturation += 0.20f * 2; handled = true // brighter
              case 6  => lightness += 0.20f; saturation += 0.20f; handled = true // bright
              case _  =>
            }
          }
        case 'P' | 'p' =>
          if (len > 2 && (term.charAt(2) == 'l' || term.charAt(2) == 'L')) { // pale
            len match {
              case 8 => lightness += 0.20f * 4; saturation -= 0.20f * 4; handled = true // palemost
              case 7 => lightness += 0.20f * 4; saturation -= 0.20f * 4; handled = true // palerer (same as 8, 4 extra chars)
              case 6 => lightness += 0.20f * 3; saturation -= 0.20f * 3; handled = true // palest
              case 5 => lightness += 0.20f * 2; saturation -= 0.20f * 2; handled = true // paler
              case 4 => lightness += 0.20f; saturation -= 0.20f; handled = true // pale
              case _ =>
            }
          }
        case 'W' | 'w' =>
          if (len > 3 && (term.charAt(3) == 'k' || term.charAt(3) == 'K')) { // weak
            len match {
              case 8 => lightness -= 0.20f * 4; saturation -= 0.20f * 4; handled = true // weakmost
              case 7 => lightness -= 0.20f * 3; saturation -= 0.20f * 3; handled = true // weakest
              case 6 => lightness -= 0.20f * 2; saturation -= 0.20f * 2; handled = true // weaker
              case 4 => lightness -= 0.20f; saturation -= 0.20f; handled = true // weak
              case _ =>
            }
          }
        case 'R' | 'r' =>
          if (len > 1 && (term.charAt(1) == 'i' || term.charAt(1) == 'I')) { // rich
            len match {
              case 8 => saturation += 0.20f * 4; handled = true // richmost
              case 7 => saturation += 0.20f * 3; handled = true // richest
              case 6 => saturation += 0.20f * 2; handled = true // richer
              case 4 => saturation += 0.20f; handled = true // rich
              case _ =>
            }
          }
        case 'D' | 'd' =>
          if (len > 1 && (term.charAt(1) == 'a' || term.charAt(1) == 'A')) { // dark
            len match {
              case 8 => lightness -= 0.20f * 4; handled = true // darkmost
              case 7 => lightness -= 0.20f * 3; handled = true // darkest
              case 6 => lightness -= 0.20f * 2; handled = true // darker
              case 4 => lightness -= 0.20f; handled = true // dark
              case _ =>
            }
          } else if (len > 1 && (term.charAt(1) == 'u' || term.charAt(1) == 'U')) { // dull
            len match {
              case 8 => saturation -= 0.20f * 4; handled = true // dullmost
              case 7 => saturation -= 0.20f * 3; handled = true // dullest
              case 6 => saturation -= 0.20f * 2; handled = true // duller
              case 4 => saturation -= 0.20f; handled = true // dull
              case _ =>
            }
          } else if (len > 3 && (term.charAt(3) == 'p' || term.charAt(3) == 'P')) { // deep
            len match {
              case 8 => lightness -= 0.20f * 4; saturation += 0.20f * 4; handled = true // deepmost
              case 7 => lightness -= 0.20f * 3; saturation += 0.20f * 3; handled = true // deepest
              case 6 => lightness -= 0.20f * 2; saturation += 0.20f * 2; handled = true // deeper
              case 4 => lightness -= 0.20f; saturation += 0.20f; handled = true // deep
              case _ =>
            }
          }
        case c if c >= '0' && c <= '9' =>
          if (mixing.size >= 2) {
            var num = 1f
            try
              num = term.toFloat
            catch {
              case _: NumberFormatException => // ignored
            }
            mixing((mixing.size & -2) - 1) = num
          }
          handled = true
        case _ => // fall through to color lookup
      }

      if (!handled) {
        mixing += NAMED.getOrElse(term, 0f)
        mixing += 1f
      }
    }

    if (mixing.size < 2) 0f
    else {
      var result = FloatColors.unevenMix(mixing.toArray, 0, mixing.size)
      if (result == 0f) result
      else {
        if (lightness > 0) result = ColorTools.lighten(result, lightness)
        else if (lightness < 0) result = ColorTools.darken(result, -lightness)

        if (saturation > 0) result = ColorTools.enrich(result, saturation)
        else if (saturation < 0) result = ColorTools.dullen(result, -saturation)

        result
      }
    }
  }

  /** Given a color as a packed RGBA float, this finds the closest description it can to match the given color while using at most `mixCount` colors to mix in. You should only use small numbers for
    * mixCount, like 1 to 3; this can take quite a while to run otherwise. This returns a String description that can be passed to [[parseDescription]]. It is likely that this will use very
    * contrasting colors if mixCount is 2 or greater and the color to match is desaturated or brownish.
    * @param rgb
    *   a packed RGBA float color to attempt to match
    * @param mixCount
    *   how many color names this will use in the returned description
    * @return
    *   a description that can be fed to [[parseDescription]] to get a similar color
    */
  def bestMatch(rgb: Float, mixCount: Int): String = {
    val mc           = Math.max(1, mixCount)
    var oklab        = sge.colorful.oklab.ColorTools.fromRGBA(rgb)
    var bestDistance = Float.PositiveInfinity
    val paletteSize  = namesByHue.size
    val colorTries   = Math.pow(paletteSize, mc).toInt
    val totalTries   = colorTries * 81
    val targetL      = sge.colorful.oklab.ColorTools.channelL(oklab)
    val targetA      = sge.colorful.oklab.ColorTools.channelA(oklab)
    val targetB      = sge.colorful.oklab.ColorTools.channelB(oklab)
    val mixingArr    = new Array[Float](mc)
    for (i <- 0 until mc)
      mixingArr(i) = colorsByHue(0)
    var bestCode = 0
    var c        = 0
    while (c < totalTries) {
      var i = 0
      var e = 1
      while (i < mc) {
        mixingArr(i) = colorsByHue((c / e) % paletteSize)
        i += 1
        e *= paletteSize
      }
      val idxL = (c / colorTries) % 9 - 4
      val idxS = c / (colorTries * 9) - 4

      var result = FloatColors.mix(mixingArr, 0, mc)
      if (idxL > 0) result = ColorTools.lighten(result, 0.20f * idxL)
      else if (idxL < 0) result = ColorTools.darken(result, -0.20f * idxL)

      if (idxS > 0) result = ColorTools.enrich(result, idxS * 0.200f)
      else if (idxS < 0) result = ColorTools.dullen(result, idxS * -0.200f)

      oklab = sge.colorful.oklab.ColorTools.fromRGBA(result)

      val dL       = sge.colorful.oklab.ColorTools.channelL(oklab) - targetL
      val dA       = sge.colorful.oklab.ColorTools.channelA(oklab) - targetA
      val dB       = sge.colorful.oklab.ColorTools.channelB(oklab) - targetB
      val distance = dL * dL + dA * dA + dB * dB
      if (distance < bestDistance) {
        bestDistance = distance
        bestCode = c
      }
      c += 1
    }

    val description = new StringBuilder(combinedAdjectives(bestCode / colorTries))
    var i           = 0
    var e           = 1
    while (i < mc) {
      description.append(namesByHue((bestCode / e) % paletteSize))
      i += 1
      if (i < mc)
        description.append(' ')
      e *= paletteSize
    }
    description.toString()
  }

  /** Appends standard RGBA Color instances to the map in [[Colors]], using the names in [[NAMES]] (which are "lower cased" instead of "ALL UPPER CASE"). This doesn't need any changes to be made to
    * Colors in order for it to be compatible; just remember that the colors originally in Colors use "UPPER CASE" and these use "lower case". This does append aliases as well, so some color values
    * will be duplicates.
    *
    * This can be used alongside the method with the same name in Palette, since that uses "Title Cased" names.
    */
  def appendToKnownColors(): Unit =
    for ((key, value) <- NAMED)
      Colors.put(key, ColorTools.toColor(new Color(), value))
}
