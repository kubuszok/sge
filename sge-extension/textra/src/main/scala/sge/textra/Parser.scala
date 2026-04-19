/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/Parser.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: RegExodus Pattern/Matcher/Replacer → java.util.regex.Pattern/Matcher,
 *     Array<T> → Array[T], MathUtils.clamp → Math.min/max
 *   Convention: parseTokens() fully ported with java.util.regex replacements.
 *   Idiom: while loops for Matcher iteration; boundary/break not needed here.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 457
 * Covenant-baseline-methods: BOOLEAN_TRUE,INDEX_PARAM,INDEX_TOKEN,PATTERN_COLOR_HEX_NO_HASH,PATTERN_MARKUP_STRIP,Parser,colorMatcher,colorPattern,colorSb,compileTokenPattern,defaultValue,found,getResetReplacement,handleBracketMinusMarkup,i,markupStripped,n,params,parseRegularTokens,parseReplacements,parseTokens,preprocess,processIfToken,result,sb,sorted,stringToBoolean,stringToColor,stringToColorMarkup,stringToFloat,stringToStyleMarkup,styleMatcher,stylePattern,styleSb,text,text2,tokens,variable,variableValue
 * Covenant-source-reference: com/github/tommyettinger/textra/Parser.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import scala.compiletime.uninitialized
import scala.util.boundary
import scala.util.boundary.break
import java.util.regex.{ Matcher, Pattern }
import sge.textra.utils.{ CaseInsensitiveIntMap, Palette, StringUtils }
import scala.language.implicitConversions
import sge.utils.Nullable

/** Utility class to parse tokens from a TypingLabel; not intended for external use in most situations. */
object Parser {

  private val PATTERN_MARKUP_STRIP:      Pattern = Pattern.compile("((?<!\\[)\\[[^\\[\\]]*(\\]))")
  private val PATTERN_COLOR_HEX_NO_HASH: Pattern = Pattern.compile("[A-Fa-f0-9]{3,8}")

  private val BOOLEAN_TRUE: CaseInsensitiveIntMap =
    new CaseInsensitiveIntMap(Array("true", "yes", "t", "y", "on", "1"), new Array[Int](6))

  private val INDEX_TOKEN = 1
  private val INDEX_PARAM = 2

  @volatile private var PATTERN_TOKEN_STRIP: Pattern = uninitialized
  @volatile private var RESET_REPLACEMENT:   String  = uninitialized

  /** Replaces any square-bracket-minus markup of the form [-SOMETHING] with the curly-brace tag form {SOMETHING}. */
  def handleBracketMinusMarkup(text: String): String =
    if (text.contains("[-")) {
      val p  = Pattern.compile("((?<!\\[)\\[-([^\\[\\]]*)(?:\\]))")
      val m  = p.matcher(text)
      val sb = new StringBuffer()
      while (m.find())
        m.appendReplacement(sb, "{" + Matcher.quoteReplacement(m.group(2)) + "}")
      m.appendTail(sb)
      sb.toString
    } else {
      text
    }

  /** Replaces any style markup using square brackets with curly-brace style tags. */
  def preprocess(text: String): String = {
    var result = text
    // [ ] → {RESET}
    result = Pattern.compile("((?<!\\[)\\[ (?:\\]))").matcher(result).replaceAll("{RESET}")
    // [] → {UNDO}
    result = Pattern.compile("((?<!\\[)\\[(?:\\]))").matcher(result).replaceAll("{UNDO}")
    // Color markup → {COLOR=...}
    val colorPattern = Pattern.compile("(?<!\\[)\\[(?:(?:#([A-Fa-f0-9]{3,8}))|(?:\\|?([\\p{L}\\p{N}][^\\[\\]]*)))(\\])")
    val colorMatcher = colorPattern.matcher(result)
    val colorSb      = new StringBuffer()
    while (colorMatcher.find()) {
      val matched = if (colorMatcher.group(1) != null) colorMatcher.group(1) else colorMatcher.group(2)
      colorMatcher.appendReplacement(colorSb, Matcher.quoteReplacement("{COLOR=" + matched + "}"))
    }
    colorMatcher.appendTail(colorSb)
    result = colorSb.toString
    // General markup → {STYLE=...}
    val stylePattern = Pattern.compile("(?<!\\[)\\[([^\\[\\]\\+][^\\[\\]]*)(\\])")
    val styleMatcher = stylePattern.matcher(result)
    val styleSb      = new StringBuffer()
    while (styleMatcher.find())
      styleMatcher.appendReplacement(styleSb, Matcher.quoteReplacement("{STYLE=" + styleMatcher.group(1) + "}"))
    styleMatcher.appendTail(styleSb)
    result = styleSb.toString
    result
  }

  /** Parses all tokens from the given TypingLabel. */
  def parseTokens(label: TypingLabel): Unit = {
    // Compile patterns if necessary
    if (PATTERN_TOKEN_STRIP == null || TypingConfig.dirtyEffectMaps) {
      PATTERN_TOKEN_STRIP = compileTokenPattern()
    }
    if (RESET_REPLACEMENT == null || TypingConfig.dirtyEffectMaps) {
      RESET_REPLACEMENT = getResetReplacement
    }

    // Remove any previous entries
    label.tokenEntries.clear()

    // Parse all tokens with text replacements, namely color and var.
    parseReplacements(label)

    // Parse all regular tokens and properly register them
    parseRegularTokens(label)

    label.setText(label.getIntermediateText.toString, modifyOriginalText = false, restart = false)

    // Sort token entries
    val sorted = label.tokenEntries.sortWith((a, b) => a.compareTo(b) < 0)
    label.tokenEntries.clear()
    label.tokenEntries ++= sorted
  }

  /** Parse tokens that only replace text, such as colors and variables. */
  private def parseReplacements(label: TypingLabel): Unit = {
    var text: CharSequence = label.layout.appendIntoDirect(new StringBuilder)

    if (label.getFont.omitCurlyBraces && label.getFont.enableSquareBrackets) {
      var matcherIndexOffset = 0

      var continue_ = true
      while (continue_) {
        val m2 = PATTERN_TOKEN_STRIP.matcher(text)
        if (!m2.find(matcherIndexOffset)) {
          continue_ = false
        } else {
          val internalTokenOpt = InternalToken.fromName(m2.group(INDEX_TOKEN))
          val param            = if (m2.groupCount() >= INDEX_PARAM) m2.group(INDEX_PARAM) else null

          if (Nullable.isEmpty(internalTokenOpt)) {
            matcherIndexOffset += 1
          } else {
            val internalToken = Nullable.fold(internalTokenOpt)(null: InternalToken)(identity) // @nowarn — guarded by isEmpty check above
            var replacement: String = null
            var skip = false

            internalToken match {
              case InternalToken.COLOR =>
                replacement = stringToColorMarkup(param)
              case InternalToken.STYLE | InternalToken.SIZE =>
                replacement = stringToStyleMarkup(param)
              case InternalToken.FONT =>
                replacement = "[@" + param + ']'
              case InternalToken.ENDCOLOR | InternalToken.CLEARCOLOR =>
                replacement = "[#" + label.getClearColor.toString + ']'
              case InternalToken.CLEARSIZE =>
                replacement = "[%]"
              case InternalToken.CLEARFONT =>
                replacement = "[@]"
              case InternalToken.VAR =>
                replacement = null
                Nullable.foreach(label.getTypingListener) { l =>
                  val rv = l.replaceVariable(param)
                  Nullable.foreach(rv)(v => replacement = v)
                }
                if (replacement == null) replacement = label.getVariables.getOrElse(param.toUpperCase, null)
                if (replacement == null) replacement = TypingConfig.GLOBAL_VARS.getOrElse(param.toUpperCase, null)
                if (replacement == null) replacement = param.toUpperCase
              case InternalToken.IF =>
                replacement = processIfToken(label, param)
                if (replacement == null) replacement = param.toUpperCase
              case InternalToken.RESET =>
                replacement = RESET_REPLACEMENT + label.getDefaultToken
              case InternalToken.UNDO =>
                replacement = "[]"
              case _ =>
                matcherIndexOffset += 1
                skip = true
            }

            if (!skip) {
              // Replace the matched token with the replacement
              val sb = new StringBuilder()
              sb.append(text, 0, m2.start())
              sb.append(replacement)
              sb.append(text, m2.end(), text.length())
              text = sb
              matcherIndexOffset = m2.start() + replacement.length
            }
          }
        }
      }
    }
    label.setIntermediateText(text, false, false)
  }

  private def processIfToken(label: TypingLabel, paramsString: String): String = boundary {
    val params   = if (paramsString == null) Array.empty[String] else paramsString.split(";")
    val variable = if (params.length > 0) params(0) else null

    if (params.length <= 1 || variable == null) break(null) // @nowarn — matches original API

    var variableValue: String = null
    Nullable.foreach(label.getTypingListener) { l =>
      val rv = l.replaceVariable(variable)
      Nullable.foreach(rv)(v => variableValue = v)
    }
    if (variableValue == null) variableValue = label.getVariables.getOrElse(variable.toUpperCase, null)
    if (variableValue == null) variableValue = TypingConfig.GLOBAL_VARS.getOrElse(variable.toUpperCase, null)
    if (variableValue == null) variableValue = ""

    var defaultValue: String = null
    var i     = 1
    val n     = params.length
    var found = false
    while (i < n && !found) {
      val subParams  = params(i).split("=", 2)
      val key        = subParams(0)
      val value      = subParams(subParams.length - 1)
      val isKeyValid = subParams.length > 1 && key.nonEmpty

      if (!isKeyValid) {
        defaultValue = value
        found = true
      } else if (variableValue.equalsIgnoreCase(key)) {
        break(value)
      }
      i += 1
    }

    if (defaultValue != null) defaultValue
    else variable
  }

  /** Parses regular tokens that don't need replacement and register their indexes in the TypingLabel. */
  private def parseRegularTokens(label: TypingLabel): Unit = {
    val markupStripped = PATTERN_MARKUP_STRIP.matcher(label.getIntermediateText).replaceAll("")
    var text2: CharSequence = label.getIntermediateText

    if (label.getFont.omitCurlyBraces) {
      var matcherIndexOffset = 0
      var m2IndexOffset      = 0

      var continue_ = true
      while (continue_) {
        val m  = PATTERN_TOKEN_STRIP.matcher(markupStripped)
        val m2 = PATTERN_TOKEN_STRIP.matcher(text2)

        if (!m.find(matcherIndexOffset)) {
          continue_ = false
        } else {
          m2.find(m2IndexOffset)

          val tokenName = m.group(INDEX_TOKEN).toUpperCase
          var tokenCategory: TokenCategory = null
          val tmpTokenOpt = InternalToken.fromName(tokenName)
          if (Nullable.isEmpty(tmpTokenOpt)) {
            if (TypingConfig.EFFECT_START_TOKENS.contains(tokenName)) {
              tokenCategory = TokenCategory.EFFECT_START
            } else if (TypingConfig.EFFECT_END_TOKENS.contains(tokenName)) {
              tokenCategory = TokenCategory.EFFECT_END
            }
          } else {
            tokenCategory = Nullable.fold(tmpTokenOpt)(null: TokenCategory)(_.category)
          }

          val groupCount   = m.groupCount()
          val paramsString = if (groupCount >= INDEX_PARAM) m.group(INDEX_PARAM) else null
          val params       = if (paramsString == null) Array.empty[String] else paramsString.split(";")
          val firstParam   = if (params.length > 0) params(0) else null
          val index        = m.start(0)

          if (tokenCategory == null) {
            matcherIndexOffset += 1
          } else {
            var floatValue = 0f
            var stringValue: String = null
            var effect:      Effect = null

            tokenCategory match {
              case TokenCategory.WAIT =>
                floatValue = stringToFloat(firstParam, TypingConfig.DEFAULT_WAIT_VALUE)
              case TokenCategory.EVENT =>
                stringValue = paramsString
              case TokenCategory.SPEED =>
                tokenName match {
                  case "SPEED" =>
                    val minMod   = TypingConfig.MIN_SPEED_MODIFIER
                    val maxMod   = TypingConfig.MAX_SPEED_MODIFIER
                    val modifier = Math.max(minMod, Math.min(maxMod, stringToFloat(firstParam, 1f)))
                    floatValue = TypingConfig.DEFAULT_SPEED_PER_CHAR / modifier
                  case "SLOWER" =>
                    floatValue = TypingConfig.DEFAULT_SPEED_PER_CHAR * 2f
                  case "SLOW" =>
                    floatValue = TypingConfig.DEFAULT_SPEED_PER_CHAR * 1.5f
                  case "NORMAL" =>
                    floatValue = TypingConfig.DEFAULT_SPEED_PER_CHAR
                  case "FAST" =>
                    floatValue = TypingConfig.DEFAULT_SPEED_PER_CHAR * 0.5f
                  case "FASTER" =>
                    floatValue = TypingConfig.DEFAULT_SPEED_PER_CHAR * 0.25f
                  case "NATURAL" =>
                    val minMod   = TypingConfig.MIN_SPEED_MODIFIER
                    val maxMod   = TypingConfig.MAX_SPEED_MODIFIER
                    val modifier = Math.max(minMod, Math.min(maxMod, stringToFloat(firstParam, 1f)))
                    floatValue = -TypingConfig.DEFAULT_SPEED_PER_CHAR / modifier
                  case _ => // unknown speed token
                }
              case TokenCategory.EFFECT_START =>
                TypingConfig.EFFECT_START_TOKENS.get(tokenName.toUpperCase).foreach { eb =>
                  effect = eb.produce(label, params)
                }
              case TokenCategory.EFFECT_END => // nothing to do
              case _                        => // skip
            }

            // Register token
            val entry = new TokenEntry(tokenName, tokenCategory, index, m.end(0), floatValue, stringValue)
            entry.effect = Nullable(effect)
            label.tokenEntries += entry

            // Set new text without tokens
            matcherIndexOffset = m.end()
            val replacedSb = new StringBuilder()
            replacedSb.append(text2, 0, m2.start())
            replacedSb.append(text2, m2.end(), text2.length())
            text2 = replacedSb
            m2IndexOffset = m2.start()
          }
        }
      }
    }
    label.setIntermediateText(text2, false, false)
  }

  /** Returns a float value parsed from the given String, or the default value if the string couldn't be parsed. */
  def stringToFloat(str: String, defaultValue: Float): Float =
    if (str != null) {
      try {
        val cleaned = str.replaceAll("[^\\d.\\-+]", "")
        if (cleaned.nonEmpty) java.lang.Float.parseFloat(cleaned) else defaultValue
      } catch {
        case _: Exception => defaultValue
      }
    } else {
      defaultValue
    }

  /** Returns a boolean value parsed from the given String, or false if the string couldn't be parsed. */
  def stringToBoolean(str: String): Boolean =
    if (str != null) BOOLEAN_TRUE.containsKey(str) else false

  /** Parses a color from the given string. Returns 256 if the color couldn't be parsed. */
  def stringToColor(label: TypingLabel, str: String): Int = boundary {
    if (str != null) {
      val lookup = label.getFont.colorLookup
      if (lookup != null) {
        val namedColor = lookup.getRgba(str)
        if (namedColor != 256) break(namedColor)
      }
      if (str.length >= 3) {
        if (str.startsWith("#")) {
          if (str.length >= 9) break(StringUtils.intFromHex(str, 1, 9))
          if (str.length >= 7) break(StringUtils.intFromHex(str, 1, 7) << 8 | 0xff)
          if (str.length >= 4) {
            val rgb = StringUtils.intFromHex(str, 1, 4)
            break(
              (rgb << 20 & 0xf0000000) | (rgb << 16 & 0x0f000000) |
                (rgb << 16 & 0x00f00000) | (rgb << 12 & 0x000f0000) |
                (rgb << 12 & 0x0000f000) | (rgb << 8 & 0x00000f00) | 0xff
            )
          }
        } else {
          if (str.length >= 8) break(StringUtils.intFromHex(str, 0, 8))
          if (str.length >= 6) break(StringUtils.intFromHex(str, 0, 6) << 8 | 0xff)
          val rgb = StringUtils.intFromHex(str, 0, 3)
          break(
            (rgb << 20 & 0xf0000000) | (rgb << 16 & 0x0f000000) |
              (rgb << 16 & 0x00f00000) | (rgb << 12 & 0x000f0000) |
              (rgb << 12 & 0x0000f000) | (rgb << 8 & 0x00000f00) | 0xff
          )
        }
      }
    }
    256
  }

  /** Encloses the given string in brackets to work as a regular color markup tag. */
  def stringToColorMarkup(str: String): String = {
    if (str != null) {
      if (str.length >= 3 && !Palette.NAMED.contains(str) && PATTERN_COLOR_HEX_NO_HASH.matcher(str).matches()) {
        return "[#" + str + "]"
      }
    }
    "[" + str + "]"
  }

  /** Matches style names to syntax and encloses the given string in brackets. */
  def stringToStyleMarkup(str: String): String = {
    if (str == null) return ""
    if (str.isEmpty || str.equalsIgnoreCase("UNDO")) return "[]"
    if (str == " ") return "[ ]"
    if (str == "*" || str.equalsIgnoreCase("B") || str.equalsIgnoreCase("BOLD") || str.equalsIgnoreCase("STRONG")) return "[*]"
    if (str == "/" || str.equalsIgnoreCase("I") || str.equalsIgnoreCase("OBLIQUE") || str.equalsIgnoreCase("ITALIC") || str.equalsIgnoreCase("EM")) return "[/]"
    if (str == "_" || str.equalsIgnoreCase("U") || str.equalsIgnoreCase("UNDER") || str.equalsIgnoreCase("UNDERLINE")) return "[_]"
    if (str == "~" || str.equalsIgnoreCase("STRIKE") || str.equalsIgnoreCase("STRIKETHROUGH") || str.equalsIgnoreCase("CROSSED")) return "[~]"
    if (str == "." || str.equalsIgnoreCase("SUB") || str.equalsIgnoreCase("SUBSCRIPT")) return "[.]"
    if (str == "=" || str.equalsIgnoreCase("MID") || str.equalsIgnoreCase("MIDSCRIPT")) return "[=]"
    if (str == "^" || str.equalsIgnoreCase("SUPER") || str.equalsIgnoreCase("SUPERSCRIPT")) return "[^]"
    if (str == "!" || str.equalsIgnoreCase("UP") || str.equalsIgnoreCase("UPPER")) return "[!]"
    if (str == "," || str.equalsIgnoreCase("LOW") || str.equalsIgnoreCase("LOWER")) return "[,]"
    if (str == ";" || str.equalsIgnoreCase("EACH") || str.equalsIgnoreCase("TITLE")) return "[;]"
    if (str == "@" || str.equalsIgnoreCase("NOFONT") || str.equalsIgnoreCase("ENDFONT")) return "[@]"
    if (str == "#" || str.equalsIgnoreCase("OUTLINE") || str.equalsIgnoreCase("BLACK OUTLINE") || str.equalsIgnoreCase("BLACKEN")) return "[#]"
    if (str.equalsIgnoreCase("JOSTLE") || str.equalsIgnoreCase("WOBBLE") || str.equalsIgnoreCase("SCATTER")) return "[?jostle]"
    if (str.equalsIgnoreCase("SMALLCAPS") || str.equalsIgnoreCase("SMALL CAPS")) return "[?small caps]"
    if (str.equalsIgnoreCase("BLUE OUTLINE") || str.equalsIgnoreCase("BLUEN")) return "[?blue outline]"
    if (str.equalsIgnoreCase("RED OUTLINE") || str.equalsIgnoreCase("REDDEN")) return "[?red outline]"
    if (str.equalsIgnoreCase("YELLOW OUTLINE") || str.equalsIgnoreCase("YELLOWEN")) return "[?yellow outline]"
    if (str.equalsIgnoreCase("WHITE OUTLINE") || str.equalsIgnoreCase("WHITEN")) return "[?white outline]"
    if (str.equalsIgnoreCase("SHINY") || str.equalsIgnoreCase("SHINE") || str.equalsIgnoreCase("GLOSSY")) return "[?shiny]"
    if (str.equalsIgnoreCase("NEON") || str.equalsIgnoreCase("GLOW")) return "[?neon]"
    if (str.equalsIgnoreCase("HALO") || str.equalsIgnoreCase("SURROUND") || str.equalsIgnoreCase("CLOAK")) return "[?halo]"
    if (str.equalsIgnoreCase("SHADOW") || str.equalsIgnoreCase("DROPSHADOW") || str.equalsIgnoreCase("DROP SHADOW")) return "[shadow]"
    if (str.equalsIgnoreCase("ERROR") || str.equalsIgnoreCase("REDLINE") || str.equalsIgnoreCase("RED LINE")) return "[?error]"
    if (str.equalsIgnoreCase("CONTEXT") || str.equalsIgnoreCase("GRAMMAR") || str.equalsIgnoreCase("GREENLINE") || str.equalsIgnoreCase("GREEN LINE")) return "[?context]"
    if (str.equalsIgnoreCase("WARN") || str.equalsIgnoreCase("YELLOWLINE") || str.equalsIgnoreCase("YELLOW LINE")) return "[?warn]"
    if (str.equalsIgnoreCase("SUGGEST") || str.equalsIgnoreCase("GRAYLINE") || str.equalsIgnoreCase("GRAY LINE") || str.equalsIgnoreCase("GREYLINE") || str.equalsIgnoreCase("GREY LINE"))
      return "[?suggest]"
    if (str.equalsIgnoreCase("NOTE") || str.equalsIgnoreCase("INFO") || str.equalsIgnoreCase("BLUELINE") || str.equalsIgnoreCase("BLUE LINE")) return "[?note]"
    if (str == "?" || str == "%?" || str == "%^" || str.equalsIgnoreCase("NOMODE") || str.equalsIgnoreCase("ENDMODE")) return "[?]"
    if (str == "%" || str.equalsIgnoreCase("NOSCALE") || str.equalsIgnoreCase("ENDSCALE")) return "[%]"
    if (str.startsWith("@")) return "[" + str + "]"
    if (str.endsWith("%")) return "[%" + str.substring(0, str.length - 1) + "]"
    if (str.startsWith("%")) return "[" + str + "]"
    if (str.startsWith("?")) return "[" + str + "]"
    if (str.startsWith("(")) return "[" + str + "]"
    if (str.startsWith(" ")) return "[" + str + "]"
    if (Palette.NAMED.contains(str)) return "[" + str + "]"
    if (str.length >= 3 && PATTERN_COLOR_HEX_NO_HASH.matcher(str).matches()) return "[#" + str + "]"
    ""
  }

  /** Compiles the token-matching pattern from all registered effect tokens. */
  private def compileTokenPattern(): Pattern = {
    val sb = new StringBuilder
    sb.append("(?<!\\{)\\{(")
    val tokens = scala.collection.mutable.ArrayBuffer[String]()
    tokens ++= TypingConfig.EFFECT_START_TOKENS.keys
    tokens ++= TypingConfig.EFFECT_END_TOKENS.keys
    for (token <- InternalToken.values)
      tokens += token.tokenName
    var i = 0
    while (i < tokens.size) {
      sb.append(Pattern.quote(tokens(i)))
      if (i + 1 < tokens.size) sb.append('|')
      i += 1
    }
    sb.append(")(?:\\=([^\\{\\}]+))?\\}")
    Pattern.compile(sb.toString, Pattern.CASE_INSENSITIVE)
  }

  /** Returns the replacement string intended to be used on {RESET} tokens. */
  private def getResetReplacement: String = {
    val sb = new StringBuilder("[ ]")
    for (token <- TypingConfig.EFFECT_END_TOKENS.keys)
      sb.append('{').append(token).append('}')
    sb.append("{NORMAL}")
    TypingConfig.dirtyEffectMaps = false
    sb.toString
  }
}
