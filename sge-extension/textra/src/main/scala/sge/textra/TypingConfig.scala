/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingConfig.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 146
 * Covenant-baseline-methods: CHAR_LIMIT_PER_FRAME,DEFAULT_CLEAR_COLOR,DEFAULT_SPEED_PER_CHAR,DEFAULT_WAIT_VALUE,EFFECT_END_TOKENS,EFFECT_START_TOKENS,GLOBAL_VARS,INTERVAL_MULTIPLIERS_BY_CHAR,MAX_SPEED_MODIFIER,MIN_SPEED_MODIFIER,TypingConfig,b,defaultInitialText,dirtyEffectMaps,getDefaultInitialText,initializeGlobalVars,name,registerEffect,setDefaultInitialText,unregisterEffect
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingConfig.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import scala.collection.mutable.{ HashMap, LinkedHashMap }
import sge.graphics.Color

/** Configuration class that easily allows the user to fine tune the library's functionality. */
object TypingConfig {

  /** Default time in seconds that an empty WAIT token should wait for. */
  var DEFAULT_WAIT_VALUE: Float = 0.250f

  /** Time in seconds that takes for each char to appear in the default speed. */
  var DEFAULT_SPEED_PER_CHAR: Float = 0.05f

  /** Minimum value for the SPEED token. */
  var MIN_SPEED_MODIFIER: Float = 0.001f

  /** Maximum value for the SPEED token. */
  var MAX_SPEED_MODIFIER: Float = 100.0f

  /** Defines how many chars can appear per frame. Use a value less than 1 to disable. */
  var CHAR_LIMIT_PER_FRAME: Int = -1

  /** Default color for the CLEARCOLOR token. */
  var DEFAULT_CLEAR_COLOR: Color = new Color(Color.WHITE)

  /** Returns a map of characters and their respective interval multipliers. */
  val INTERVAL_MULTIPLIERS_BY_CHAR: HashMap[Int, Float] = HashMap.empty

  private var defaultInitialText: String = ""

  def getDefaultInitialText:               String = defaultInitialText
  def setDefaultInitialText(text: String): Unit   =
    defaultInitialText = if (text != null) text else ""

  /** Map of global variables that affect all TypingLabel instances at once. */
  val GLOBAL_VARS: HashMap[String, String] = HashMap.empty

  /** Defines several default variables. */
  def initializeGlobalVars(): Unit = {
    GLOBAL_VARS.put("FIRE", "{OCEAN=0.7;1.25;0.11;1.0;0.65}")
    GLOBAL_VARS.put("ENDFIRE", "{ENDOCEAN}")
    GLOBAL_VARS.put("SPUTTERINGFIRE", "{OCEAN=0.7;1.25;0.11;1.0;0.65}{SPUTTER=0.2;0.25;4;inf}")
    GLOBAL_VARS.put("ENDSPUTTERINGFIRE", "{ENDOCEAN}{ENDSPUTTER}")
    GLOBAL_VARS.put("BLIZZARD", "{GRADIENT=88ccff;eef8ff;-0.5;5}{WIND=2;4;0.25;0.1}")
    GLOBAL_VARS.put("ENDBLIZZARD", "{ENDGRADIENT}{ENDWIND}")
    GLOBAL_VARS.put("SHIVERINGBLIZZARD", "{GRADIENT=88ccff;eef8ff;-0.5;5}{WIND=2;4;0.25;0.1}{JOLT=1;0.6;inf;0.1;;}")
    GLOBAL_VARS.put("ENDSHIVERINGBLIZZARD", "{ENDGRADIENT}{ENDWIND}{ENDJOLT}")
    GLOBAL_VARS.put("ELECTRIFY", "{JOLT=1;1.2;inf;0.3;dull lavender;light butter}")
    GLOBAL_VARS.put("ENDELECTRIFY", "{ENDJOLT}")
    GLOBAL_VARS.put("ZOMBIE", "{SICK=0.4}{CROWD}{EMERGE=0.1}[dark olive sage]")
    GLOBAL_VARS.put("ENDZOMBIE", "{ENDSICK}{ENDCROWD}{ENDEMERGE}{CLEARCOLOR}")
  }

  /** Map of start tokens and their effect classes. Internal use only. */
  val EFFECT_START_TOKENS: LinkedHashMap[String, Effect.EffectBuilder] = LinkedHashMap.empty

  /** Map of end tokens and their effect classes. Internal use only. */
  val EFFECT_END_TOKENS: LinkedHashMap[String, Effect.EffectBuilder] = LinkedHashMap.empty

  /** Whether effect tokens are dirty and need to be recalculated. */
  var dirtyEffectMaps: Boolean = true

  /** Registers a new effect to TypingLabel. */
  def registerEffect(startTokenName: String, builder: Effect.EffectBuilder): Unit = {
    val name = startTokenName.toUpperCase
    val b: Effect.EffectBuilder = (label: TypingLabel, params: Array[String]) => builder.produce(label, params).assignTokenName(name)
    EFFECT_START_TOKENS.put(name, b)
    EFFECT_END_TOKENS.put("END" + name, b)
    dirtyEffectMaps = true
  }

  /** Unregisters an effect from TypingLabel. */
  def unregisterEffect(startTokenName: String): Unit = {
    val name = startTokenName.toUpperCase
    EFFECT_START_TOKENS.remove(name)
    EFFECT_END_TOKENS.remove("END" + name)
    dirtyEffectMaps = true
  }

  // Static initialization
  locally {
    // Generate default char intervals
    INTERVAL_MULTIPLIERS_BY_CHAR.put(' ', 0.0f)
    INTERVAL_MULTIPLIERS_BY_CHAR.put(',', 2.0f)
    INTERVAL_MULTIPLIERS_BY_CHAR.put(';', 2.5f)
    INTERVAL_MULTIPLIERS_BY_CHAR.put(':', 2.5f)
    INTERVAL_MULTIPLIERS_BY_CHAR.put('.', 3.0f)
    INTERVAL_MULTIPLIERS_BY_CHAR.put('!', 5.0f)
    INTERVAL_MULTIPLIERS_BY_CHAR.put('?', 5.0f)
    INTERVAL_MULTIPLIERS_BY_CHAR.put('\n', 2.5f)

    // Register default tokens
    import sge.textra.effects._
    registerEffect("EASE", (l, p) => new EaseEffect(l, p))
    registerEffect("HANG", (l, p) => new HangEffect(l, p))
    registerEffect("JUMP", (l, p) => new JumpEffect(l, p))
    registerEffect("SHAKE", (l, p) => new ShakeEffect(l, p))
    registerEffect("SICK", (l, p) => new SickEffect(l, p))
    registerEffect("SLIDE", (l, p) => new SlideEffect(l, p))
    registerEffect("WAVE", (l, p) => new WaveEffect(l, p))
    registerEffect("WIND", (l, p) => new WindEffect(l, p))
    registerEffect("RAINBOW", (l, p) => new RainbowEffect(l, p))
    registerEffect("GRADIENT", (l, p) => new GradientEffect(l, p))
    registerEffect("FADE", (l, p) => new FadeEffect(l, p))
    registerEffect("BLINK", (l, p) => new BlinkEffect(l, p))
    registerEffect("JOLT", (l, p) => new JoltEffect(l, p))
    registerEffect("SPIRAL", (l, p) => new SpiralEffect(l, p))
    registerEffect("SPIN", (l, p) => new SpinEffect(l, p))
    registerEffect("CROWD", (l, p) => new CrowdEffect(l, p))
    registerEffect("SHRINK", (l, p) => new ShrinkEffect(l, p))
    registerEffect("EMERGE", (l, p) => new EmergeEffect(l, p))
    registerEffect("HEARTBEAT", (l, p) => new HeartbeatEffect(l, p))
    registerEffect("CAROUSEL", (l, p) => new CarouselEffect(l, p))
    registerEffect("SQUASH", (l, p) => new SquashEffect(l, p))
    registerEffect("SCALE", (l, p) => new ScaleEffect(l, p))
    registerEffect("ROTATE", (l, p) => new RotateEffect(l, p))
    registerEffect("HIGHLIGHT", (l, p) => new HighlightEffect(l, p))
    registerEffect("LINK", (l, p) => new LinkEffect(l, p))
    registerEffect("TRIGGER", (l, p) => new TriggerEffect(l, p))
    registerEffect("ATTENTION", (l, p) => new AttentionEffect(l, p))
    registerEffect("STYLIST", (l, p) => new StylistEffect(l, p))
    registerEffect("CANNON", (l, p) => new CannonEffect(l, p))
    registerEffect("OCEAN", (l, p) => new OceanEffect(l, p))
    registerEffect("SPUTTER", (l, p) => new SputterEffect(l, p))
    registerEffect("INSTANT", (l, p) => new InstantEffect(l, p))
    registerEffect("SLAM", (l, p) => new SlamEffect(l, p))
    registerEffect("MEET", (l, p) => new MeetEffect(l, p))
    registerEffect("ZIPPER", (l, p) => new ZipperEffect(l, p))
    registerEffect("SLIP", (l, p) => new SlipEffect(l, p))
    registerEffect("THINKING", (l, p) => new ThinkingEffect(l, p))
    registerEffect("THROB", (l, p) => new ThrobEffect(l, p))
    registerEffect("PINCH", (l, p) => new PinchEffect(l, p))
    registerEffect("SHOOT", (l, p) => new ShootEffect(l, p))

    initializeGlobalVars()
  }
}
