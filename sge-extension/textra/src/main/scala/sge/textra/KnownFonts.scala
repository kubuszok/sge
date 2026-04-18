/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/KnownFonts.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: LifecycleListener → dispose only, OrderedSet → LinkedHashSet,
 *     ObjectMap → HashMap, ShaderProgram → deferred,
 *     Gdx.files.internal → Sge().files.internal,
 *     TextureAtlas(packFile, imagesDir, flip) → SGE TextureAtlas(packFile, imagesDir, flip),
 *     BitmapFont/getBitmapFont → skipped (no BitmapFont in SGE)
 *   Merged with: loadUnicodeAtlas inlined as loadEmojiAtlas (UTF-8 is default in SGE)
 *   Convention: All font name constants preserved; getFont() and specific
 *     font getter methods preserved. Actual font file loading fully ported.
 *   Idiom: Singleton via object; font caching with HashMap; (using Sge) propagation.
 *   TODOs: ShaderProgram initialization deferred — SGE manages shaders at a higher level.
 *     getBitmapFont skipped — SGE does not expose BitmapFont directly.
 *     loadUnicodeAtlas → uses standard TextureAtlas (SGE reads UTF-8 by default).
 *
 * Covenant: partial-port
 * Covenant-source-reference: textratypist/src/main/java/com/github/tommyettinger/textra/KnownFonts.java
 * Covenant-verified: 2026-04-11
 */
package sge
package textra

import scala.collection.mutable.{ HashMap, LinkedHashSet }

import sge.graphics.g2d.TextureAtlas
import sge.utils.Nullable

/** Preconfigured static Font instances, with any important metric adjustments already applied. This uses a singleton to ensure each font exists at most once.
  *
  * Note: Depending on TextraTypist as a library won't include any of the preconfigured font files used by this class. Check the Javadocs for each font you want to use; they include links to all files
  * needed to use any given font in this class.
  */
object KnownFonts {

  private var initialized: Boolean               = false
  private var prefix:      String                = ""
  private val loaded:      HashMap[String, Font] = HashMap.empty

  // Cached atlas references for emoji/icon sets
  private var twemoji:        Nullable[TextureAtlas] = Nullable.empty
  private var openMojiColor:  Nullable[TextureAtlas] = Nullable.empty
  private var openMojiWhite:  Nullable[TextureAtlas] = Nullable.empty
  private var notoEmoji:      Nullable[TextureAtlas] = Nullable.empty
  private var gameIcons:      Nullable[TextureAtlas] = Nullable.empty
  private var materialDesign: Nullable[TextureAtlas] = Nullable.empty
  private var gameIconsFont:  Nullable[Font]         = Nullable.empty

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

  // ---- Internal utilities ----

  /** Tries several extensions for a Structured JSON font file, returning the first that exists. Extensions are checked in order: .ubj.lzma, .json.lzma, .ubj, .dat, .json. Throws if none found.
    */
  private def getJsonExtension(jsonName: String)(using Sge): String = {
    val files = summon[Sge].files
    if (files.internal(jsonName + ".ubj.lzma").exists()) jsonName + ".ubj.lzma"
    else if (files.internal(jsonName + ".json.lzma").exists()) jsonName + ".json.lzma"
    else if (files.internal(jsonName + ".ubj").exists()) jsonName + ".ubj"
    else if (files.internal(jsonName + ".dat").exists()) jsonName + ".dat"
    else if (files.internal(jsonName + ".json").exists()) jsonName + ".json"
    else throw new RuntimeException("No file was found with an appropriate extension appended to " + jsonName)
  }

  // ---- Generic font retrieval ----

  /** A general way to get a copied Font from the known set of fonts, treating it as using no distance field effect (STANDARD). */
  def getFont(baseName: String)(using Sge): Font =
    getFont(baseName, Font.DistanceFieldType.STANDARD)

  /** A general way to get a copied Font from the known set of fonts. It looks up the appropriate file name, respecting asset prefix, creates the Font if necessary, then returns a copy of it. This
    * uses getJsonExtension to try a variety of file extensions for Structured JSON fonts. This also scales Structured JSON fonts to have height 32 using scaleHeightTo, but does not scale .fnt or
    * .font fonts because those are usually pixel fonts that require very specific sizes.
    */
  def getFont(baseName: String, distanceField: Font.DistanceFieldType)(using Sge): Font = {
    if (baseName == null) throw new RuntimeException("Font name cannot be null.")
    val dft = if (distanceField == null) Font.DistanceFieldType.STANDARD else distanceField
    initialize()
    val rootName = baseName + dft.filePart
    val known    = loaded.getOrElseUpdate(
      rootName,
      if (JSON_NAMES.contains(baseName) || LIMITED_JSON_NAMES.contains(baseName))
        new Font(getJsonExtension(prefix + rootName), true).scaleHeightTo(32)
      else if (FNT_NAMES.contains(baseName))
        new Font(prefix + rootName + ".fnt", dft)
      else if (dft == Font.DistanceFieldType.STANDARD && SAD_NAMES.contains(baseName))
        new Font(prefix, rootName + ".font", true)
      else
        throw new RuntimeException("Unknown font name/distance field: " + baseName + "/" + dft.toString)
    )
    new Font(known).setName(baseName + dft.namePart).setDistanceField(dft)
  }

  // ---- Convenience getters for specific fonts ----
  // These all delegate to getFont(NAME, DFT) and exist for API compatibility.

  def getAStarry()(using Sge):                                Font = getFont(A_STARRY)
  def getAStarry(dft: Font.DistanceFieldType)(using Sge):     Font = getFont(A_STARRY, dft)
  def getAStarryMSDF()(using Sge):                            Font = getFont(A_STARRY, Font.DistanceFieldType.MSDF)
  def getAStarryTall()(using Sge):                            Font = getAStarryTall(Font.DistanceFieldType.STANDARD)
  def getAStarryTall(dft: Font.DistanceFieldType)(using Sge): Font =
    getFont(A_STARRY, dft).scale(0.5f, 1f).setName(A_STARRY + "-Tall" + dft.namePart)
  def getAbyssinicaSIL()(using Sge):                                      Font = getFont(ABYSSINICA_SIL)
  def getAbyssinicaSIL(dft:           Font.DistanceFieldType)(using Sge): Font = getFont(ABYSSINICA_SIL, dft)
  def getAsul()(using Sge):                                               Font = getFont(ASUL)
  def getAsul(dft:                    Font.DistanceFieldType)(using Sge): Font = getFont(ASUL, dft)
  def getAubrey()(using Sge):                                             Font = getFont(AUBREY)
  def getAubrey(dft:                  Font.DistanceFieldType)(using Sge): Font = getFont(AUBREY, dft)
  def getBirdlandAeroplane()(using Sge):                                  Font = getFont(BIRDLAND_AEROPLANE)
  def getBirdlandAeroplane(dft:       Font.DistanceFieldType)(using Sge): Font = getFont(BIRDLAND_AEROPLANE, dft)
  def getBitter()(using Sge):                                             Font = getFont(BITTER)
  def getBitter(dft:                  Font.DistanceFieldType)(using Sge): Font = getFont(BITTER, dft)
  def getBonheurRoyale()(using Sge):                                      Font = getFont(BONHEUR_ROYALE)
  def getBonheurRoyale(dft:           Font.DistanceFieldType)(using Sge): Font = getFont(BONHEUR_ROYALE, dft)
  def getCanada1500()(using Sge):                                         Font = getFont(CANADA1500)
  def getCanada1500(dft:              Font.DistanceFieldType)(using Sge): Font = getFont(CANADA1500, dft)
  def getCascadiaMono()(using Sge):                                       Font = getFont(CASCADIA_MONO)
  def getCascadiaMono(dft:            Font.DistanceFieldType)(using Sge): Font = getFont(CASCADIA_MONO, dft)
  def getCascadiaMonoMSDF()(using Sge):                                   Font = getFont(CASCADIA_MONO, Font.DistanceFieldType.MSDF)
  def getCaveat()(using Sge):                                             Font = getFont(CAVEAT)
  def getCaveat(dft:                  Font.DistanceFieldType)(using Sge): Font = getFont(CAVEAT, dft)
  def getChangaOne()(using Sge):                                          Font = getFont(CHANGA_ONE)
  def getChangaOne(dft:               Font.DistanceFieldType)(using Sge): Font = getFont(CHANGA_ONE, dft)
  def getComicMono()(using Sge):                                          Font = getFont(COMIC_MONO)
  def getComicMono(dft:               Font.DistanceFieldType)(using Sge): Font = getFont(COMIC_MONO, dft)
  def getComputerSaysNo()(using Sge):                                     Font = getFont(COMPUTER_SAYS_NO)
  def getComputerSaysNo(dft:          Font.DistanceFieldType)(using Sge): Font = getFont(COMPUTER_SAYS_NO, dft)
  def getCordata16x26()(using Sge):                                       Font = getFont(CORDATA_16X26)
  def getCozette()(using Sge):                                            Font = getFont(COZETTE)
  def getCreteRound()(using Sge):                                         Font = getFont(CRETE_ROUND)
  def getCreteRound(dft:              Font.DistanceFieldType)(using Sge): Font = getFont(CRETE_ROUND, dft)
  def getDejaVuSans()(using Sge):                                         Font = getFont(DEJAVU_SANS)
  def getDejaVuSans(dft:              Font.DistanceFieldType)(using Sge): Font = getFont(DEJAVU_SANS, dft)
  def getDejaVuSansCondensed()(using Sge):                                Font = getFont(DEJAVU_SANS_CONDENSED)
  def getDejaVuSansCondensed(dft:     Font.DistanceFieldType)(using Sge): Font = getFont(DEJAVU_SANS_CONDENSED, dft)
  def getDejaVuSansMono()(using Sge):                                     Font = getFont(DEJAVU_SANS_MONO)
  def getDejaVuSansMono(dft:          Font.DistanceFieldType)(using Sge): Font = getFont(DEJAVU_SANS_MONO, dft)
  def getDejaVuSerif()(using Sge):                                        Font = getFont(DEJAVU_SERIF)
  def getDejaVuSerif(dft:             Font.DistanceFieldType)(using Sge): Font = getFont(DEJAVU_SERIF, dft)
  def getDejaVuSerifCondensed()(using Sge):                               Font = getFont(DEJAVU_SERIF_CONDENSED)
  def getDejaVuSerifCondensed(dft:    Font.DistanceFieldType)(using Sge): Font = getFont(DEJAVU_SERIF_CONDENSED, dft)
  def getDINish()(using Sge):                                             Font = getFont(DINISH)
  def getDINish(dft:                  Font.DistanceFieldType)(using Sge): Font = getFont(DINISH, dft)
  def getDINishHeavy()(using Sge):                                        Font = getFont(DINISH_HEAVY)
  def getDINishHeavy(dft:             Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_HEAVY, dft)
  def getDINishLight()(using Sge):                                        Font = getFont(DINISH_LIGHT)
  def getDINishLight(dft:             Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_LIGHT, dft)
  def getDINishCondensed()(using Sge):                                    Font = getFont(DINISH_CONDENSED)
  def getDINishCondensed(dft:         Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_CONDENSED, dft)
  def getDINishCondensedHeavy()(using Sge):                               Font = getFont(DINISH_CONDENSED_HEAVY)
  def getDINishCondensedHeavy(dft:    Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_CONDENSED_HEAVY, dft)
  def getDINishCondensedLight()(using Sge):                               Font = getFont(DINISH_CONDENSED_LIGHT)
  def getDINishCondensedLight(dft:    Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_CONDENSED_LIGHT, dft)
  def getDINishExpanded()(using Sge):                                     Font = getFont(DINISH_EXPANDED)
  def getDINishExpanded(dft:          Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_EXPANDED, dft)
  def getDINishExpandedHeavy()(using Sge):                                Font = getFont(DINISH_EXPANDED_HEAVY)
  def getDINishExpandedHeavy(dft:     Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_EXPANDED_HEAVY, dft)
  def getDINishExpandedLight()(using Sge):                                Font = getFont(DINISH_EXPANDED_LIGHT)
  def getDINishExpandedLight(dft:     Font.DistanceFieldType)(using Sge): Font = getFont(DINISH_EXPANDED_LIGHT, dft)
  def getGentium()(using Sge):                                            Font = getFont(GENTIUM)
  def getGentium(dft:                 Font.DistanceFieldType)(using Sge): Font = getFont(GENTIUM, dft)
  def getGentiumMSDF()(using Sge):                                        Font = getFont(GENTIUM, Font.DistanceFieldType.MSDF)
  def getGentiumSDF()(using Sge):                                         Font = getFont(GENTIUM, Font.DistanceFieldType.SDF)
  def getGentiumUnItalic()(using Sge):                                    Font = getFont(GENTIUM_UN_ITALIC)
  def getGentiumUnItalic(dft:         Font.DistanceFieldType)(using Sge): Font = getFont(GENTIUM_UN_ITALIC, dft)
  def getGlacialIndifference()(using Sge):                                Font = getFont(GLACIAL_INDIFFERENCE)
  def getGlacialIndifference(dft:     Font.DistanceFieldType)(using Sge): Font = getFont(GLACIAL_INDIFFERENCE, dft)
  def getGoNotoUniversal()(using Sge):                                    Font = getFont(GO_NOTO_UNIVERSAL)
  def getGoNotoUniversal(dft:         Font.DistanceFieldType)(using Sge): Font = getFont(GO_NOTO_UNIVERSAL, dft)
  def getGoNotoUniversalSDF()(using Sge):                                 Font = getFont(GO_NOTO_UNIVERSAL, Font.DistanceFieldType.SDF)
  def getGrenze()(using Sge):                                             Font = getFont(GRENZE)
  def getGrenze(dft:                  Font.DistanceFieldType)(using Sge): Font = getFont(GRENZE, dft)
  def getHanazono()(using Sge):                                           Font = getFont(HANAZONO)
  def getIBM8x16()(using Sge):                                            Font = getFont(IBM_8X16)
  def getIBM8x16Sad()(using Sge):                                         Font = getFont(IBM_8X16_SAD)
  def getInconsolata()(using Sge):                                        Font = getFont(INCONSOLATA_LGC)
  def getInconsolata(dft:             Font.DistanceFieldType)(using Sge): Font = getFont(INCONSOLATA_LGC, dft)
  def getInconsolataMSDF()(using Sge):                                    Font = getFont(INCONSOLATA_LGC, Font.DistanceFieldType.MSDF)
  def getInconsolataLGC()(using Sge):                                     Font = getFont(INCONSOLATA_LGC)
  def getIosevka()(using Sge):                                            Font = getFont(IOSEVKA)
  def getIosevka(dft:                 Font.DistanceFieldType)(using Sge): Font = getFont(IOSEVKA, dft)
  def getIosevkaMSDF()(using Sge):                                        Font = getFont(IOSEVKA, Font.DistanceFieldType.MSDF)
  def getIosevkaSDF()(using Sge):                                         Font = getFont(IOSEVKA, Font.DistanceFieldType.SDF)
  def getIosevkaSlab()(using Sge):                                        Font = getFont(IOSEVKA_SLAB)
  def getIosevkaSlab(dft:             Font.DistanceFieldType)(using Sge): Font = getFont(IOSEVKA_SLAB, dft)
  def getIosevkaSlabMSDF()(using Sge):                                    Font = getFont(IOSEVKA_SLAB, Font.DistanceFieldType.MSDF)
  def getIosevkaSlabSDF()(using Sge):                                     Font = getFont(IOSEVKA_SLAB, Font.DistanceFieldType.SDF)
  def getKingthingsFoundation()(using Sge):                               Font = getFont(KINGTHINGS_FOUNDATION)
  def getKingthingsFoundation(dft:    Font.DistanceFieldType)(using Sge): Font = getFont(KINGTHINGS_FOUNDATION, dft)
  def getKingthingsPetrock()(using Sge):                                  Font = getFont(KINGTHINGS_PETROCK)
  def getKingthingsPetrock(dft:       Font.DistanceFieldType)(using Sge): Font = getFont(KINGTHINGS_PETROCK, dft)
  def getLanaPixel()(using Sge):                                          Font = getFont(LANAPIXEL)
  def getLeagueGothic()(using Sge):                                       Font = getFont(LEAGUE_GOTHIC)
  def getLeagueGothic(dft:            Font.DistanceFieldType)(using Sge): Font = getFont(LEAGUE_GOTHIC, dft)
  def getLibertinusSerif()(using Sge):                                    Font = getFont(LIBERTINUS_SERIF)
  def getLibertinusSerif(dft:         Font.DistanceFieldType)(using Sge): Font = getFont(LIBERTINUS_SERIF, dft)
  def getLibertinusSerifSemibold()(using Sge):                            Font = getFont(LIBERTINUS_SERIF_SEMIBOLD)
  def getLibertinusSerifSemibold(dft: Font.DistanceFieldType)(using Sge): Font = getFont(LIBERTINUS_SERIF_SEMIBOLD, dft)
  def getMaShanZheng()(using Sge):                                        Font = getFont(MA_SHAN_ZHENG)
  def getMaShanZheng(dft:             Font.DistanceFieldType)(using Sge): Font = getFont(MA_SHAN_ZHENG, dft)
  def getMapleMono()(using Sge):                                          Font = getFont(MAPLE_MONO)
  def getMapleMono(dft:               Font.DistanceFieldType)(using Sge): Font = getFont(MAPLE_MONO, dft)
  def getMonogram()(using Sge):                                           Font = getFont(MONOGRAM)
  def getMonogramItalic()(using Sge):                                     Font = getFont(MONOGRAM_ITALIC)
  def getMoonDance()(using Sge):                                          Font = getFont(MOON_DANCE)
  def getMoonDance(dft:               Font.DistanceFieldType)(using Sge): Font = getFont(MOON_DANCE, dft)
  def getNowAlt()(using Sge):                                             Font = getFont(NOW_ALT)
  def getNowAlt(dft:                  Font.DistanceFieldType)(using Sge): Font = getFont(NOW_ALT, dft)
  def getNugothic()(using Sge):                                           Font = getFont(NUGOTHIC)
  def getNugothic(dft:                Font.DistanceFieldType)(using Sge): Font = getFont(NUGOTHIC, dft)
  def getOpenSans()(using Sge):                                           Font = getFont(OPEN_SANS)
  def getOpenSans(dft:                Font.DistanceFieldType)(using Sge): Font = getFont(OPEN_SANS, dft)
  def getOstrichBlack()(using Sge):                                       Font = getFont(OSTRICH_BLACK)
  def getOstrichBlack(dft:            Font.DistanceFieldType)(using Sge): Font = getFont(OSTRICH_BLACK, dft)
  def getOverlock()(using Sge):                                           Font = getFont(OVERLOCK)
  def getOverlock(dft:                Font.DistanceFieldType)(using Sge): Font = getFont(OVERLOCK, dft)
  def getOverlockUnItalic()(using Sge):                                   Font = getFont(OVERLOCK_UN_ITALIC)
  def getOverlockUnItalic(dft:        Font.DistanceFieldType)(using Sge): Font = getFont(OVERLOCK_UN_ITALIC, dft)
  def getOxanium()(using Sge):                                            Font = getFont(OXANIUM)
  def getOxanium(dft:                 Font.DistanceFieldType)(using Sge): Font = getFont(OXANIUM, dft)
  def getPangolin()(using Sge):                                           Font = getFont(PANGOLIN)
  def getPangolin(dft:                Font.DistanceFieldType)(using Sge): Font = getFont(PANGOLIN, dft)
  def getProtestRevolution()(using Sge):                                  Font = getFont(PROTEST_REVOLUTION)
  def getProtestRevolution(dft:       Font.DistanceFieldType)(using Sge): Font = getFont(PROTEST_REVOLUTION, dft)
  def getQuanPixel()(using Sge):                                          Font = getFont(QUANPIXEL)
  def getRobotoCondensed()(using Sge):                                    Font = getFont(ROBOTO_CONDENSED)
  def getRobotoCondensed(dft:         Font.DistanceFieldType)(using Sge): Font = getFont(ROBOTO_CONDENSED, dft)
  def getSancreek()(using Sge):                                           Font = getFont(SANCREEK)
  def getSancreek(dft:                Font.DistanceFieldType)(using Sge): Font = getFont(SANCREEK, dft)
  def getSelawik()(using Sge):                                            Font = getFont(SELAWIK)
  def getSelawik(dft:                 Font.DistanceFieldType)(using Sge): Font = getFont(SELAWIK, dft)
  def getSelawikBold()(using Sge):                                        Font = getFont(SELAWIK_BOLD)
  def getSelawikBold(dft:             Font.DistanceFieldType)(using Sge): Font = getFont(SELAWIK_BOLD, dft)
  def getSourGummy()(using Sge):                                          Font = getFont(SOUR_GUMMY)
  def getSourGummy(dft:               Font.DistanceFieldType)(using Sge): Font = getFont(SOUR_GUMMY, dft)
  def getSpecialElite()(using Sge):                                       Font = getFont(SPECIAL_ELITE)
  def getSpecialElite(dft:            Font.DistanceFieldType)(using Sge): Font = getFont(SPECIAL_ELITE, dft)
  def getTangerine()(using Sge):                                          Font = getFont(TANGERINE)
  def getTangerine(dft:               Font.DistanceFieldType)(using Sge): Font = getFont(TANGERINE, dft)
  def getTangerineSDF()(using Sge):                                       Font = getFont(TANGERINE, Font.DistanceFieldType.SDF)
  def getTillana()(using Sge):                                            Font = getFont(TILLANA)
  def getTillana(dft:                 Font.DistanceFieldType)(using Sge): Font = getFont(TILLANA, dft)
  def getYanoneKaffeesatz()(using Sge):                                   Font = getFont(YANONE_KAFFEESATZ)
  def getYanoneKaffeesatz(dft:        Font.DistanceFieldType)(using Sge): Font = getFont(YANONE_KAFFEESATZ, dft)
  def getYanoneKaffeesatzMSDF()(using Sge):                               Font = getFont(YANONE_KAFFEESATZ, Font.DistanceFieldType.MSDF)
  def getYataghan()(using Sge):                                           Font = getFont(YATAGHAN)
  def getYataghan(dft:                Font.DistanceFieldType)(using Sge): Font = getFont(YATAGHAN, dft)
  def getYataghanMSDF()(using Sge):                                       Font = getFont(YATAGHAN, Font.DistanceFieldType.MSDF)

  /** Alias for getCanada1500(). Returns a Font with Canada1500 STANDARD distance field type. */
  def getCanada()(using Sge): Font = getFont(CANADA1500)

  /** Alias for getCanada1500(dft). Returns a Font with Canada1500 using the given distance field type. */
  def getCanada(dft: Font.DistanceFieldType)(using Sge): Font = getFont(CANADA1500, dft)

  // ---- Bulk font retrieval ----

  /** Returns a new array of Font instances, calling each getXyz() method that returns any Font. This will only function if all font assets are present and loadable. You should store the result rather
    * than calling often, because each call copies many Fonts. Returns a mix of standard, SDF, and MSDF fonts.
    */
  def getAll()(using Sge): Array[Font] = Array(
    getAStarry(),
    getAStarryMSDF(),
    getAStarryTall(),
    getAbyssinicaSIL(),
    getAsul(),
    getAubrey(),
    getBirdlandAeroplane(),
    getBitter(),
    getBonheurRoyale(),
    getCanada(),
    getCascadiaMono(),
    getCascadiaMonoMSDF(),
    getCaveat(),
    getChangaOne(),
    getComicMono(),
    getComputerSaysNo(),
    getCordata16x26(),
    getCozette(),
    getCreteRound(),
    getDejaVuSans(),
    getDejaVuSansCondensed(),
    getDejaVuSansMono(),
    getDejaVuSerif(),
    getDejaVuSerifCondensed(),
    getGentium(),
    getGentiumMSDF(),
    getGentiumSDF(),
    getGentiumUnItalic(),
    getGlacialIndifference(),
    getGoNotoUniversal(),
    getGoNotoUniversalSDF(),
    getGrenze(),
    getHanazono(),
    getIBM8x16(),
    getIBM8x16Sad(),
    getInconsolata(),
    getInconsolataMSDF(),
    getIosevka(),
    getIosevkaMSDF(),
    getIosevkaSDF(),
    getIosevkaSlab(),
    getIosevkaSlabMSDF(),
    getIosevkaSlabSDF(),
    getKingthingsFoundation(),
    getKingthingsPetrock(),
    getLanaPixel(),
    getLeagueGothic(),
    getLibertinusSerif(),
    getLibertinusSerifSemibold(),
    getMaShanZheng(),
    getMapleMono(),
    getMonogram(),
    getMonogramItalic(),
    getMoonDance(),
    getNowAlt(),
    getNugothic(),
    getOpenSans(),
    getOstrichBlack(),
    getOverlock(),
    getOverlockUnItalic(),
    getOxanium(),
    getPangolin(),
    getProtestRevolution(),
    getQuanPixel(),
    getRobotoCondensed(),
    getSancreek(),
    getSelawik(),
    getSelawikBold(),
    getSourGummy(),
    getSpecialElite(),
    getTangerine(),
    getTangerineSDF(),
    getTillana(),
    getYanoneKaffeesatz(),
    getYanoneKaffeesatzMSDF(),
    getYataghan(),
    getYataghanMSDF()
  )

  /** Returns a new array of Font instances for all standard (non-distance-field) fonts. Uses DistanceFieldType.STANDARD for JSON fonts, and specific getters for .fnt/.font fonts.
    */
  def getAllStandard()(using Sge): Array[Font] = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[Font]
    // Structured JSON format
    for (name <- JSON_NAMES) buf += getFont(name, Font.DistanceFieldType.STANDARD)
    // special JSON config
    buf += getAStarryTall()
    buf += getCordata16x26()
    buf += getIBM8x16()
    // AngelCode BMFont format
    buf += getCozette()
    buf += getHanazono()
    buf += getLanaPixel()
    buf += getMonogram()
    buf += getMonogramItalic()
    buf += getQuanPixel()
    // SadConsole format
    buf += getIBM8x16Sad()
    buf.toArray
  }

  /** Returns a new array of Font instances for all SDF fonts. */
  def getAllSDF()(using Sge): Array[Font] = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[Font]
    for (name <- SDF_NAMES) buf += getFont(name, Font.DistanceFieldType.SDF)
    buf += getAStarryTall(Font.DistanceFieldType.SDF)
    buf.toArray
  }

  /** Returns a new array of Font instances for all MSDF fonts. */
  def getAllMSDF()(using Sge): Array[Font] = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[Font]
    for (name <- MSDF_NAMES) buf += getFont(name, Font.DistanceFieldType.MSDF)
    buf += getAStarryTall(Font.DistanceFieldType.MSDF)
    buf.toArray
  }

  // ---- Emoji and icon atlas methods ----

  /** Adds Twemoji emoji images to a Font so they can be used with the [+name] syntax. Requires the Twemoji atlas files in the assets folder. */
  def addEmoji(font: Font)(using Sge): Font =
    addEmoji(font, 2f, -1f, 4f)

  /** Adds Twemoji emoji images to a Font with customizable offsets and x-advance. */
  def addEmoji(font: Font, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font =
    addEmoji(font, "", "", offsetXChange, offsetYChange, xAdvanceChange)

  /** Adds Twemoji emoji images to a Font with customizable prepend/append strings and metric adjustments. */
  def addEmoji(font: Font, prepend: String, append: String, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font = {
    initialize()
    if (Nullable.isEmpty(twemoji)) {
      try {
        val atlas = summon[Sge].files.internal(prefix + "Twemoji.atlas")
        if (summon[Sge].files.internal(prefix + "Twemoji.png").exists())
          twemoji = Nullable(new TextureAtlas(atlas, atlas.parent(), false))
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    twemoji.fold(throw new RuntimeException("Assets 'Twemoji.atlas' and 'Twemoji.png' not found.")) { ta =>
      font.addAtlas(ta, prepend, append, offsetXChange, offsetYChange, xAdvanceChange)
    }
  }

  /** Adds Noto Color Emoji images to a Font so they can be used with the [+name] syntax. Requires the Noto Emoji atlas files in the assets folder. */
  def addNotoEmoji(font: Font)(using Sge): Font =
    addNotoEmoji(font, 2f, -1f, 4f)

  /** Adds Noto Color Emoji images to a Font with customizable offsets and x-advance. */
  def addNotoEmoji(font: Font, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font =
    addNotoEmoji(font, "", "", offsetXChange, offsetYChange, xAdvanceChange)

  /** Adds Noto Color Emoji images to a Font with customizable prepend/append strings and metric adjustments. */
  def addNotoEmoji(font: Font, prepend: String, append: String, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font = {
    initialize()
    if (Nullable.isEmpty(notoEmoji)) {
      try {
        val atlas = summon[Sge].files.internal(prefix + "Noto-Emoji.atlas")
        if (summon[Sge].files.internal(prefix + "Noto-Emoji.png").exists())
          notoEmoji = Nullable(new TextureAtlas(atlas, atlas.parent(), false))
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    notoEmoji.fold(throw new RuntimeException("Assets 'Noto-Emoji.atlas' and 'Noto-Emoji.png' not found.")) { ta =>
      font.addAtlas(ta, prepend, append, offsetXChange, offsetYChange, xAdvanceChange)
    }
  }

  /** Adds OpenMoji emoji images to a Font so they can be used with the [+name] syntax. Requires the OpenMoji atlas files in the assets folder.
    * @param color
    *   if true, uses the full-color set; if false, uses the white-line set
    */
  def addOpenMoji(font: Font, color: Boolean)(using Sge): Font =
    addOpenMoji(font, color, 0f, -1f, 0f)

  /** Adds OpenMoji emoji images to a Font with customizable offsets and x-advance. */
  def addOpenMoji(font: Font, color: Boolean, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font =
    addOpenMoji(font, color, "", "", offsetXChange, offsetYChange, xAdvanceChange)

  /** Adds OpenMoji emoji images to a Font with customizable prepend/append strings and metric adjustments. */
  def addOpenMoji(font: Font, color: Boolean, prepend: String, append: String, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font = {
    initialize()
    if (color) {
      val baseName = "OpenMoji-color"
      if (Nullable.isEmpty(openMojiColor)) {
        try {
          val atlas = summon[Sge].files.internal(prefix + baseName + ".atlas")
          if (summon[Sge].files.internal(prefix + baseName + ".png").exists())
            openMojiColor = Nullable(new TextureAtlas(atlas, atlas.parent(), false))
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }
      openMojiColor.fold(throw new RuntimeException("Assets '" + baseName + ".atlas' and '" + baseName + ".png' not found.")) { ta =>
        font.addAtlas(ta, prepend, append, offsetXChange, offsetYChange, xAdvanceChange)
      }
    } else {
      val baseName = "OpenMoji-white"
      if (Nullable.isEmpty(openMojiWhite)) {
        try {
          val atlas = summon[Sge].files.internal(prefix + baseName + ".atlas")
          if (summon[Sge].files.internal(prefix + baseName + ".png").exists())
            openMojiWhite = Nullable(new TextureAtlas(atlas, atlas.parent(), false))
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }
      openMojiWhite.fold(throw new RuntimeException("Assets '" + baseName + ".atlas' and '" + baseName + ".png' not found.")) { ta =>
        font.addAtlas(ta, prepend, append, offsetXChange, offsetYChange, xAdvanceChange)
      }
    }
  }

  /** Adds game-icons.net icon images to a Font. Requires the game-icons atlas files in the assets folder. */
  def addGameIcons(font: Font)(using Sge): Font =
    addGameIcons(font, 0f, 0f, 0f)

  /** Adds game-icons.net icon images to a Font with customizable offsets and x-advance. */
  def addGameIcons(font: Font, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font =
    addGameIcons(font, "", "", offsetXChange, offsetYChange, xAdvanceChange)

  /** Adds game-icons.net icon images to a Font with customizable prepend/append strings and metric adjustments. */
  def addGameIcons(font: Font, prepend: String, append: String, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font = {
    initialize()
    if (Nullable.isEmpty(gameIcons)) {
      try {
        val atlas = summon[Sge].files.internal(prefix + "Game-Icons.atlas")
        if (summon[Sge].files.internal(prefix + "Game-Icons.png").exists())
          gameIcons = Nullable(new TextureAtlas(atlas, atlas.parent(), false))
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    gameIcons.fold(throw new RuntimeException("Assets 'Game-Icons.atlas' and 'Game-Icons.png' not found.")) { ta =>
      font.addAtlas(ta, prepend, append, offsetXChange, offsetYChange, xAdvanceChange)
    }
  }

  /** Gets a typically-square Font meant to be used in a FontFamily, allowing switching to a Font with the many game-icons.net icons. The base Font this uses is getAStarry(), because it is perfectly
    * square by default. The name this will use in a FontFamily is "Icons".
    */
  def getGameIconsFont(width: Float, height: Float)(using Sge): Font = {
    initialize()
    if (Nullable.isEmpty(gameIconsFont)) {
      try
        gameIconsFont = Nullable(addGameIcons(getAStarry().scaleTo(width, height).setName("Icons")))
      catch {
        case e: Exception => e.printStackTrace()
      }
    }
    gameIconsFont.fold(throw new RuntimeException("Assets for getGameIconsFont() not found.")) { gif =>
      new Font(gif)
    }
  }

  /** Adds Material Design icon images to a Font. Requires the Material Design atlas files in the assets folder. */
  def addMaterialDesignIcons(font: Font)(using Sge): Font =
    addMaterialDesignIcons(font, 0f, 0f, 0f)

  /** Adds Material Design icon images to a Font with customizable offsets and x-advance. */
  def addMaterialDesignIcons(font: Font, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font =
    addMaterialDesignIcons(font, "", "", offsetXChange, offsetYChange, xAdvanceChange)

  /** Adds Material Design icon images to a Font with customizable prepend/append strings and metric adjustments. */
  def addMaterialDesignIcons(font: Font, prepend: String, append: String, offsetXChange: Float, offsetYChange: Float, xAdvanceChange: Float)(using Sge): Font = {
    initialize()
    if (Nullable.isEmpty(materialDesign)) {
      try {
        val atlas = summon[Sge].files.internal(prefix + "Material-Design.atlas")
        if (summon[Sge].files.internal(prefix + "Material-Design.png").exists())
          materialDesign = Nullable(new TextureAtlas(atlas, atlas.parent(), false))
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    materialDesign.fold(throw new RuntimeException("Assets 'Material-Design.atlas' and 'Material-Design.png' not found.")) { ta =>
      font.addAtlas(ta, prepend, append, offsetXChange, offsetYChange, xAdvanceChange)
    }
  }

  // ---- FontFamily convenience ----

  /** Gets a FontFamily that has been pre-populated with many different fonts, accessible by name such as [@Sans]. Requires font assets in the assets folder.
    *
    * The names this supports are: Serif (Gentium), Sans (OpenSans), Mono (Inconsolata), Condensed (RobotoCondensed), Humanist (YanoneKaffeesatz), Retro (IBM8x16), Slab (IosevkaSlab), Handwriting
    * (Caveat), Dark (Grenze), Cozette, Iosevka, Medieval (KingthingsFoundation), Future (Oxanium), Console (AStarryTall), Code (CascadiaMono), Geometric (NowAlt). Bitter is an alias for Gentium;
    * Canada is an alias for NowAlt.
    */
  def getStandardFamily()(using Sge): Font = {
    val family = new Font.FontFamily(
      Array(
        "Serif",
        "Sans",
        "Mono",
        "Condensed",
        "Humanist",
        "Retro",
        "Slab",
        "Handwriting",
        "Dark",
        "Cozette",
        "Iosevka",
        "Medieval",
        "Future",
        "Console",
        "Code",
        "Geometric"
      ),
      Array(
        getGentium(),
        getOpenSans(),
        getInconsolata(),
        getRobotoCondensed(),
        getYanoneKaffeesatz(),
        getIBM8x16(),
        getIosevkaSlab(),
        getCaveat(),
        getGrenze(),
        getCozette(),
        getIosevka(),
        getKingthingsFoundation(),
        getOxanium(),
        getAStarryTall(),
        getCascadiaMono(),
        getNowAlt()
      )
    )
    family.fontAliases.put("Bitter", 0) // for compatibility; Bitter and Gentium look nearly identical anyway...
    family.fontAliases.put("Canada", 15) // Canada1500 is... sort-of close... to Now Alt...
    family.connected(0).setFamily(family)
  }

  /** Gets a FontFamily using the given DistanceFieldType. The names are: Serif (Gentium), Sans (OpenSans), Mono (Inconsolata), Condensed (RobotoCondensed), Humanist (YanoneKaffeesatz), Fantasy
    * (GentiumUnItalic), Slab (IosevkaSlab), Handwriting (Caveat), Dark (Grenze), Script (Tangerine), Iosevka, Medieval (KingthingsFoundation), Future (Oxanium), Console (AStarryTall), Code
    * (CascadiaMono), Geometric (NowAlt).
    */
  def getFamily(dft: Font.DistanceFieldType)(using Sge): Font = {
    val family = new Font.FontFamily(
      Array(
        "Serif",
        "Sans",
        "Mono",
        "Condensed",
        "Humanist",
        "Fantasy",
        "Slab",
        "Handwriting",
        "Dark",
        "Script",
        "Iosevka",
        "Medieval",
        "Future",
        "Console",
        "Code",
        "Geometric"
      ),
      Array(
        getGentium(dft),
        getOpenSans(dft),
        getInconsolata(dft),
        getRobotoCondensed(dft),
        getYanoneKaffeesatz(dft),
        getGentiumUnItalic(dft),
        getIosevkaSlab(dft),
        getCaveat(dft),
        getGrenze(dft),
        getTangerine(dft),
        getIosevka(dft),
        getKingthingsFoundation(dft),
        getOxanium(dft),
        getAStarryTall(dft),
        getCascadiaMono(dft),
        getNowAlt(dft)
      )
    )
    family.fontAliases.put("Bitter", 0) // for compatibility; Bitter and Gentium look nearly identical anyway...
    family.fontAliases.put("Canada", 15) // Canada1500 is... sort-of close... to Now Alt...
    family.fontAliases.put("Retro", 13) // A Starry Tall is similar to IBM 8x16.
    family.fontAliases.put("Cozette", 13) // A Starry Tall is similar to Cozette.
    family.connected(0).setFamily(family)
  }

  /** Gets a FontFamily containing Monogram (regular) and Monogram Italic, with aliases "r", "i", "em". */
  def getMonogramFamily()(using Sge): Font = {
    val family = new Font.FontFamily(
      Array("Regular", "Italic"),
      Array(getMonogram(), getMonogramItalic())
    )
    family.fontAliases.put("r", 0) // regular
    family.fontAliases.put("i", 1) // italic
    family.fontAliases.put("em", 1) // emphasis
    family.connected(0).setFamily(family)
  }

  // ---- Lifecycle ----

  /** Called when the application is paused (LifecycleListener). No-op for KnownFonts. */
  def pause(): Unit = {}

  /** Called when the application is resumed (LifecycleListener). No-op for KnownFonts. */
  def resume(): Unit = {}

  /** Disposes all cached Font instances and atlas resources. */
  def dispose(): Unit = {
    for ((_, font) <- loaded)
      font.close()
    loaded.clear()

    Nullable.foreach(twemoji)(_.close())
    twemoji = Nullable.empty
    Nullable.foreach(openMojiColor)(_.close())
    openMojiColor = Nullable.empty
    Nullable.foreach(openMojiWhite)(_.close())
    openMojiWhite = Nullable.empty
    Nullable.foreach(notoEmoji)(_.close())
    notoEmoji = Nullable.empty
    Nullable.foreach(gameIcons)(_.close())
    gameIcons = Nullable.empty
    Nullable.foreach(materialDesign)(_.close())
    materialDesign = Nullable.empty
    Nullable.foreach(gameIconsFont)(_.close())
    gameIconsFont = Nullable.empty

    initialized = false
  }
}
