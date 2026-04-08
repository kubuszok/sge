/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/KnownFonts.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: LifecycleListener → deferred, OrderedSet → Set,
 *     ObjectMap → HashMap, ShaderProgram → deferred,
 *     Gdx/Application/FileHandle → deferred,
 *     TextureAtlas/Pixmap/Texture → deferred,
 *     BufferedReader → deferred
 *   Convention: All font name constants preserved; getFont() and specific
 *     font getter methods preserved. Actual font file loading deferred
 *     until rendering layer wired up.
 *   Idiom: Singleton via object; font caching with HashMap.
 *
 * Covenant: partial-port
 * Covenant-source-reference: textratypist/src/main/java/com/github/tommyettinger/textra/KnownFonts.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Actual font file loading deferred until SGE rendering layer + TextureAtlas
 *     loading + Gdx.files integration land in textra (currently the get* methods
 *     create placeholder Fonts so the API surface matches).
 */
package sge
package textra

import scala.collection.mutable.{ HashMap, LinkedHashSet }

/** Preconfigured static Font instances, with any important metric adjustments already applied. This uses a singleton to ensure each font exists at most once.
  *
  * Note: Depending on TextraTypist as a library won't include any of the preconfigured font files used by this class. Check the Javadocs for each font you want to use; they include links to all files
  * needed to use any given font in this class.
  */
object KnownFonts {

  private var initialized: Boolean               = false
  private var prefix:      String                = ""
  private val loaded:      HashMap[String, Font] = HashMap.empty

  /** Initializes the singleton. This is called automatically by every method that uses the internal singleton. */
  def initialize(): Unit =
    if (!initialized) {
      initialized = true
    }

  /** Changes the String prepended to each filename this looks up. */
  def setAssetPrefix(prefix: String): Unit = {
    initialize()
    if (prefix != null) this.prefix = prefix
  }

  /** Gets the asset prefix. */
  def getAssetPrefix: String = {
    initialize()
    prefix
  }

  // ---- Font name constants ----

  /** Base name for a fixed-width octagonal font with square dimensions. */
  val A_STARRY = "A-Starry"

  /** Base name for a variable-width serif font that supports the Ethiopic script. */
  val ABYSSINICA_SIL = "Abyssinica-SIL"

  /** Base name for a variable-width font with slight serifs and bowed edges. */
  val ASUL = "Asul"

  /** Base name for a variable-width font with odd, alien-like angles and curves. */
  val AUBREY = "Aubrey"

  /** Base name for a variable-width "sloppy" or "grungy" display font. */
  val BIRDLAND_AEROPLANE = "Birdland-Aeroplane"

  /** Base name for a variable-width thin-weight serif font. */
  val BITTER = "Bitter"

  /** Base name for a variable-width, thin, elegant handwriting font. */
  val BONHEUR_ROYALE = "Bonheur-Royale"

  /** Base name for a variable-width sans font. */
  val CANADA1500 = "Canada1500"

  /** Base name for a fixed-width programming font. */
  val CASCADIA_MONO = "Cascadia-Mono"

  /** Base name for a variable-width informal handwriting font. */
  val CAVEAT = "Caveat"

  /** Base name for a variable-width extra-heavy-weight "attention-getting" font. */
  val CHANGA_ONE = "Changa-One"

  /** Base name for a fixed-width dyslexia-friendly handwriting-like font. */
  val COMIC_MONO = "Comic-Mono"

  /** Base name for a fixed-width octagonal font, possibly usable as "college-style" lettering. */
  val COMPUTER_SAYS_NO = "Computer-Says-No"

  /** Base name for a fixed-width, tall, thin pixel font. */
  val CORDATA_16X26 = "Cordata-16x26"

  /** Base name for a variable-width, sturdy, slab-serif font with slightly rounded corners. */
  val CRETE_ROUND = "Crete-Round"

  /** Base name for a variable-width narrow sans font. */
  val DEJAVU_SANS_CONDENSED = "DejaVu-Sans-Condensed"

  /** Base name for a fixed-width programming font. */
  val DEJAVU_SANS_MONO = "DejaVu-Sans-Mono"

  /** Base name for a variable-width sans font. */
  val DEJAVU_SANS = "DejaVu-Sans"

  /** Base name for a variable-width narrow serif font. */
  val DEJAVU_SERIF_CONDENSED = "DejaVu-Serif-Condensed"

  /** Base name for a variable-width serif font. */
  val DEJAVU_SERIF = "DejaVu-Serif"

  /** Base name for a variable-width Latin-and-Cyrillic road-sign font, with regular weight. */
  val DINISH = "DINish"

  /** Base name for a variable-width Latin-and-Cyrillic road-sign font, with heavy weight. */
  val DINISH_HEAVY = "DINish-Heavy"

  /** Base name for a variable-width Latin-and-Cyrillic road-sign font, with light weight. */
  val DINISH_LIGHT = "DINish-Light"

  /** Base name for a narrow-width Latin-and-Cyrillic road-sign font, with regular weight. */
  val DINISH_CONDENSED = "DINish-Condensed"

  /** Base name for a narrow-width Latin-and-Cyrillic road-sign font, with heavy weight. */
  val DINISH_CONDENSED_HEAVY = "DINish-Condensed-Heavy"

  /** Base name for a narrow-width Latin-and-Cyrillic road-sign font, with light weight. */
  val DINISH_CONDENSED_LIGHT = "DINish-Condensed-Light"

  /** Base name for an extra-wide Latin-and-Cyrillic road-sign font, with regular weight. */
  val DINISH_EXPANDED = "DINish-Expanded"

  /** Base name for an extra-wide Latin-and-Cyrillic road-sign font, with heavy weight. */
  val DINISH_EXPANDED_HEAVY = "DINish-Expanded-Heavy"

  /** Base name for an extra-wide Latin-and-Cyrillic road-sign font, with light weight. */
  val DINISH_EXPANDED_LIGHT = "DINish-Expanded-Light"

  /** Base name for a variable-width Unicode-heavy serif font. */
  val GENTIUM = "Gentium"

  /** Base name for a variable-width Unicode-heavy "swashbuckling" serif font. */
  val GENTIUM_UN_ITALIC = "Gentium-Un-Italic"

  /** Base name for a variable-width geometric font. */
  val GLACIAL_INDIFFERENCE = "Glacial-Indifference"

  /** Base name for a variable-width Unicode-heavy sans font. Uses a larger Texture, 4096x4096. */
  val GO_NOTO_UNIVERSAL = "Go-Noto-Universal"

  /** Base name for a variable-width heavy-weight serif font, looking like early printing-press type. */
  val GRENZE = "Grenze"

  /** Base name for a fixed-width "traditional" pixel font. */
  val IBM_8X16 = "IBM-8x16"

  /** Base name for a fixed-width geometric/programming font. */
  val INCONSOLATA_LGC = "Inconsolata-LGC"

  /** Base name for a fixed-width Unicode-heavy sans font. */
  val IOSEVKA = "Iosevka"

  /** Base name for a fixed-width Unicode-heavy slab-serif font. */
  val IOSEVKA_SLAB = "Iosevka-Slab"

  /** Base name for a variable-width ornate medieval font. */
  val KINGTHINGS_FOUNDATION = "Kingthings-Foundation"

  /** Base name for a variable-width legible medieval font. */
  val KINGTHINGS_PETROCK = "Kingthings-Petrock"

  /** Base name for a variable-width, high-weight, very tall sans-serif font. */
  val LEAGUE_GOTHIC = "League-Gothic"

  /** Base name for a variable-width medium-weight serif font. */
  val LIBERTINUS_SERIF = "Libertinus-Serif"

  /** Base name for a variable-width heavy-weight serif font. */
  val LIBERTINUS_SERIF_SEMIBOLD = "Libertinus-Serif-Semibold"

  /** Base name for a variable-width geometric font. */
  val NOW_ALT = "Now-Alt"

  /** Base name for a variable-width brush-stroke font with support for many CJK glyphs. */
  val MA_SHAN_ZHENG = "Ma-Shan-Zheng"

  /** Base name for a fixed-width sans-serif font with support for many CJK glyphs (mostly at double width). */
  val MAPLE_MONO = "Maple-Mono"

  /** Base name for a variable-width, sweeping, legible handwriting font. */
  val MOON_DANCE = "Moon-Dance"

  /** Base name for a variable-width, legible, modern-style Fraktur font. */
  val NUGOTHIC = "Nugothic"

  /** Base name for a variable-width sans font. */
  val OPEN_SANS = "Open-Sans"

  /** Base name for a variable-width all-caps geometric sans font. */
  val OSTRICH_BLACK = "Ostrich-Black"

  /** Base name for a variable-width "flowy" sans font. */
  val OVERLOCK = "Overlock"

  /** Base name for a variable-width "especially flowy" sans font. */
  val OVERLOCK_UN_ITALIC = "Overlock-Un-Italic"

  /** Base name for a variable-width "sci-fi" display font. */
  val OXANIUM = "Oxanium"

  /** Base name for a variable-width "crayon-drawn" display font supporting LGC glyphs. */
  val PANGOLIN = "Pangolin"

  /** Base name for a variable-width brush-stroke font with a paint-like texture. */
  val PROTEST_REVOLUTION = "Protest-Revolution"

  /** Base name for a variable-width narrow sans font. */
  val ROBOTO_CONDENSED = "Roboto-Condensed"

  /** Base name for a variable-width "Wild West" display font. */
  val SANCREEK = "Sancreek"

  /** Base name for a variable-width sans-serif font. */
  val SELAWIK = "Selawik"

  /** Base name for a variable-width bold sans-serif font. */
  val SELAWIK_BOLD = "Selawik-Bold"

  /** Base name for a variable-width, child-like, "blobby" display font. */
  val SOUR_GUMMY = "Sour-Gummy"

  /** Base name for a fixed-width distressed typewriter font. */
  val SPECIAL_ELITE = "Special-Elite"

  /** Base name for a variable-width formal script font. */
  val TANGERINE = "Tangerine"

  /** Base name for a variable-width partial-serif font with bowed edges and some Devanagari script support. */
  val TILLANA = "Tillana"

  /** Base name for a variable-width humanist sans font. */
  val YANONE_KAFFEESATZ = "Yanone-Kaffeesatz"

  /** Base name for a variable-width "dark fantasy" display font. */
  val YATAGHAN = "Yataghan"

  /** Base name for a fixed-width pixel font. */
  val COZETTE = "Cozette"

  /** Base name for a fixed-width CJK-heavy serif font. */
  val HANAZONO = "Hanazono"

  /** Base name for a variable-width Unicode-heavy pixel font. */
  val LANAPIXEL = "LanaPixel"

  /** Base name for a fixed-width pan-European-script pixel font. */
  val MONOGRAM = "Monogram"

  /** Base name for a fixed-width true-italic pan-European-script pixel font. */
  val MONOGRAM_ITALIC = "Monogram-Italic"

  /** Base name for a tiny variable-width Unicode-heavy pixel font. */
  val QUANPIXEL = "QuanPixel"

  /** Base name for a fixed-width "traditional" pixel font using SadConsole's format. */
  val IBM_8X16_SAD = "IBM-8x16-Sad"

  // ---- Font name sets ----

  val JSON_NAMES: LinkedHashSet[String] = LinkedHashSet(
    A_STARRY,
    ABYSSINICA_SIL,
    ASUL,
    AUBREY,
    BIRDLAND_AEROPLANE,
    BITTER,
    BONHEUR_ROYALE,
    CANADA1500,
    CASCADIA_MONO,
    CAVEAT,
    CHANGA_ONE,
    COMIC_MONO,
    COMPUTER_SAYS_NO,
    CRETE_ROUND,
    DEJAVU_SANS_CONDENSED,
    DEJAVU_SANS_MONO,
    DEJAVU_SANS,
    DEJAVU_SERIF_CONDENSED,
    DEJAVU_SERIF,
    DINISH,
    DINISH_LIGHT,
    DINISH_HEAVY,
    DINISH_CONDENSED,
    DINISH_CONDENSED_LIGHT,
    DINISH_CONDENSED_HEAVY,
    DINISH_EXPANDED,
    DINISH_EXPANDED_LIGHT,
    DINISH_EXPANDED_HEAVY,
    GENTIUM,
    GENTIUM_UN_ITALIC,
    GLACIAL_INDIFFERENCE,
    GO_NOTO_UNIVERSAL,
    GRENZE,
    INCONSOLATA_LGC,
    IOSEVKA,
    IOSEVKA_SLAB,
    KINGTHINGS_FOUNDATION,
    KINGTHINGS_PETROCK,
    LEAGUE_GOTHIC,
    LIBERTINUS_SERIF,
    LIBERTINUS_SERIF_SEMIBOLD,
    MA_SHAN_ZHENG,
    MAPLE_MONO,
    MOON_DANCE,
    NOW_ALT,
    NUGOTHIC,
    OPEN_SANS,
    OSTRICH_BLACK,
    OVERLOCK,
    OVERLOCK_UN_ITALIC,
    OXANIUM,
    PANGOLIN,
    PROTEST_REVOLUTION,
    ROBOTO_CONDENSED,
    SANCREEK,
    SELAWIK,
    SELAWIK_BOLD,
    SOUR_GUMMY,
    SPECIAL_ELITE,
    TANGERINE,
    TILLANA,
    YANONE_KAFFEESATZ,
    YATAGHAN
  )

  val FNT_NAMES: LinkedHashSet[String] = LinkedHashSet(
    COZETTE,
    HANAZONO,
    LANAPIXEL,
    MONOGRAM,
    MONOGRAM_ITALIC,
    QUANPIXEL
  )

  val SAD_NAMES: LinkedHashSet[String] = LinkedHashSet(IBM_8X16_SAD)

  val LIMITED_JSON_NAMES: LinkedHashSet[String] = LinkedHashSet(CORDATA_16X26, IBM_8X16)

  val STANDARD_NAMES: LinkedHashSet[String] = {
    val s = LinkedHashSet[String]()
    s ++= JSON_NAMES
    s ++= LIMITED_JSON_NAMES
    s ++= FNT_NAMES
    s ++= SAD_NAMES
    s
  }

  val SDF_NAMES:  LinkedHashSet[String] = LinkedHashSet.from(JSON_NAMES)
  val MSDF_NAMES: LinkedHashSet[String] = LinkedHashSet.from(JSON_NAMES)

  // ---- Generic font retrieval ----

  /** A general way to get a copied Font from the known set of fonts, treating it as using no distance field effect (STANDARD). */
  def getFont(baseName: String): Font =
    getFont(baseName, Font.DistanceFieldType.STANDARD)

  /** A general way to get a copied Font from the known set of fonts. It looks up the appropriate file name, respecting asset prefix, creates the Font if necessary, then returns a copy of it.
    *
    * Note: Actual font file loading is deferred until the rendering layer is wired up. This method provides the API surface for future integration.
    */
  def getFont(baseName: String, distanceField: Font.DistanceFieldType): Font = {
    if (baseName == null) throw new RuntimeException("Font name cannot be null.")
    val dft = if (distanceField == null) Font.DistanceFieldType.STANDARD else distanceField
    initialize()
    val rootName = baseName + dft.filePart
    loaded.getOrElseUpdate(
      rootName, {
        // Actual font loading deferred until rendering layer is wired up.
        // For now, create a placeholder Font with the correct name.
        val f = new Font()
        f.setName(baseName + dft.namePart)
        f.setDistanceField(dft)
        f
      }
    )
    val cached = loaded(rootName)
    new Font(cached).setName(baseName + dft.namePart).setDistanceField(dft)
  }

  // ---- Convenience getters for specific fonts ----
  // These all delegate to getFont(NAME, DFT) and exist for API compatibility.

  def getAStarry():                            Font = getFont(A_STARRY)
  def getAStarry(dft: Font.DistanceFieldType): Font = getFont(A_STARRY, dft)
  def getAbyssinicaSIL():                      Font = getFont(ABYSSINICA_SIL)
  def getAsul():                               Font = getFont(ASUL)
  def getAubrey():                             Font = getFont(AUBREY)
  def getBitter():                             Font = getFont(BITTER)
  def getBonheurRoyale():                      Font = getFont(BONHEUR_ROYALE)
  def getCanada1500():                         Font = getFont(CANADA1500)
  def getCascadiaMono():                       Font = getFont(CASCADIA_MONO)
  def getCaveat():                             Font = getFont(CAVEAT)
  def getChangaOne():                          Font = getFont(CHANGA_ONE)
  def getComicMono():                          Font = getFont(COMIC_MONO)
  def getComputerSaysNo():                     Font = getFont(COMPUTER_SAYS_NO)
  def getCozette():                            Font = getFont(COZETTE)
  def getCreteRound():                         Font = getFont(CRETE_ROUND)
  def getDejaVuSans():                         Font = getFont(DEJAVU_SANS)
  def getDejaVuSansCondensed():                Font = getFont(DEJAVU_SANS_CONDENSED)
  def getDejaVuSansMono():                     Font = getFont(DEJAVU_SANS_MONO)
  def getDejaVuSerif():                        Font = getFont(DEJAVU_SERIF)
  def getDejaVuSerifCondensed():               Font = getFont(DEJAVU_SERIF_CONDENSED)
  def getDINish():                             Font = getFont(DINISH)
  def getDINishHeavy():                        Font = getFont(DINISH_HEAVY)
  def getDINishLight():                        Font = getFont(DINISH_LIGHT)
  def getDINishCondensed():                    Font = getFont(DINISH_CONDENSED)
  def getDINishCondensedHeavy():               Font = getFont(DINISH_CONDENSED_HEAVY)
  def getDINishCondensedLight():               Font = getFont(DINISH_CONDENSED_LIGHT)
  def getDINishExpanded():                     Font = getFont(DINISH_EXPANDED)
  def getDINishExpandedHeavy():                Font = getFont(DINISH_EXPANDED_HEAVY)
  def getDINishExpandedLight():                Font = getFont(DINISH_EXPANDED_LIGHT)
  def getGentium():                            Font = getFont(GENTIUM)
  def getGentiumUnItalic():                    Font = getFont(GENTIUM_UN_ITALIC)
  def getGentiumSDF():                         Font = getFont(GENTIUM, Font.DistanceFieldType.SDF)
  def getGlacialIndifference():                Font = getFont(GLACIAL_INDIFFERENCE)
  def getGoNotoUniversal():                    Font = getFont(GO_NOTO_UNIVERSAL)
  def getGrenze():                             Font = getFont(GRENZE)
  def getHanazono():                           Font = getFont(HANAZONO)
  def getIBM8x16():                            Font = getFont(IBM_8X16)
  def getInconsolataLGC():                     Font = getFont(INCONSOLATA_LGC)
  def getIosevka():                            Font = getFont(IOSEVKA)
  def getIosevkaSlab():                        Font = getFont(IOSEVKA_SLAB)
  def getKingthingsFoundation():               Font = getFont(KINGTHINGS_FOUNDATION)
  def getKingthingsPetrock():                  Font = getFont(KINGTHINGS_PETROCK)
  def getLanaPixel():                          Font = getFont(LANAPIXEL)
  def getLeagueGothic():                       Font = getFont(LEAGUE_GOTHIC)
  def getLibertinusSerif():                    Font = getFont(LIBERTINUS_SERIF)
  def getLibertinusSerifSemibold():            Font = getFont(LIBERTINUS_SERIF_SEMIBOLD)
  def getMaShanZheng():                        Font = getFont(MA_SHAN_ZHENG)
  def getMapleMono():                          Font = getFont(MAPLE_MONO)
  def getMonogram():                           Font = getFont(MONOGRAM)
  def getMonogramItalic():                     Font = getFont(MONOGRAM_ITALIC)
  def getMoonDance():                          Font = getFont(MOON_DANCE)
  def getNowAlt():                             Font = getFont(NOW_ALT)
  def getNugothic():                           Font = getFont(NUGOTHIC)
  def getOpenSans():                           Font = getFont(OPEN_SANS)
  def getOstrichBlack():                       Font = getFont(OSTRICH_BLACK)
  def getOverlock():                           Font = getFont(OVERLOCK)
  def getOverlockUnItalic():                   Font = getFont(OVERLOCK_UN_ITALIC)
  def getOxanium():                            Font = getFont(OXANIUM)
  def getPangolin():                           Font = getFont(PANGOLIN)
  def getProtestRevolution():                  Font = getFont(PROTEST_REVOLUTION)
  def getQuanPixel():                          Font = getFont(QUANPIXEL)
  def getRobotoCondensed():                    Font = getFont(ROBOTO_CONDENSED)
  def getSancreek():                           Font = getFont(SANCREEK)
  def getSelawik():                            Font = getFont(SELAWIK)
  def getSelawikBold():                        Font = getFont(SELAWIK_BOLD)
  def getSourGummy():                          Font = getFont(SOUR_GUMMY)
  def getSpecialElite():                       Font = getFont(SPECIAL_ELITE)
  def getTangerine():                          Font = getFont(TANGERINE)
  def getTillana():                            Font = getFont(TILLANA)
  def getYanoneKaffeesatz():                   Font = getFont(YANONE_KAFFEESATZ)
  def getYataghan():                           Font = getFont(YATAGHAN)

  // ---- Emoji and icon atlas methods ----

  /** Adds Twemoji emoji images to a Font so they can be used with the [+name] syntax. Requires the Twemoji atlas files in the assets folder. */
  def addEmoji(font: Font): Font =
    // Deferred: requires TextureAtlas loading and Gdx.files integration
    font

  /** Adds Noto Color Emoji images to a Font so they can be used with the [+name] syntax. Requires the Noto Emoji atlas files in the assets folder. */
  def addNotoEmoji(font: Font): Font =
    // Deferred: requires TextureAtlas loading and Gdx.files integration
    font

  /** Adds OpenMoji emoji images to a Font so they can be used with the [+name] syntax. Requires the OpenMoji atlas files in the assets folder. */
  def addOpenMoji(font: Font, color: Boolean): Font =
    // Deferred: requires TextureAtlas loading and Gdx.files integration
    font

  /** Adds game-icons.net icon images to a Font. Requires the game-icons atlas files in the assets folder. */
  def addGameIcons(font: Font): Font =
    // Deferred: requires TextureAtlas loading and Gdx.files integration
    font

  /** Adds Material Design icon images to a Font. Requires the Material Design atlas files in the assets folder. */
  def addMaterialDesignIcons(font: Font): Font =
    // Deferred: requires TextureAtlas loading and Gdx.files integration
    font

  // ---- FontFamily convenience ----

  /** Gets a FontFamily that has been pre-populated with many different fonts, accessible by name such as [@Sans]. Requires font assets in the assets folder. */
  def getStandardFamily(): Font.FontFamily =
    // Deferred: requires actual font loading
    new Font.FontFamily()

  // ---- Lifecycle ----

  /** Disposes all cached Font instances. */
  def dispose(): Unit = {
    for ((_, font) <- loaded)
      font.close()
    loaded.clear()
    initialized = false
  }
}
