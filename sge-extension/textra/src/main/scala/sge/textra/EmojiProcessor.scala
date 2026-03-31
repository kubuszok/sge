/*
 * MIT-licensed as part of emoji-regex (https://github.com/mathiasbynens/emoji-regex).
 * Copyright Mathias Bynens https://mathiasbynens.be/
 *
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/EmojiProcessor.java
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: RegExodus Pattern/Replacer → java.util.regex.Pattern/Matcher
 *   Convention: Emoji regex preserved for matching; replacement logic uses
 *     java.util.regex instead of RegExodus.
 *   Note: The massive regex pattern matches all Unicode 14 emoji sequences.
 */
package sge
package textra

import java.util.regex.Pattern
import sge.utils.Nullable

/** Allows getting a replacement function that can replace any "correct Unicode" emoji sequences with the single PUA-char representation used by a Font to represent most emoji.
  *
  * This can parse every emoji in Emoji 14, which is what the version of Twemoji uses. Taken mostly from emoji-regex, which is MIT-licensed.
  */
object EmojiProcessor {

  import scala.compiletime.uninitialized
  @volatile private var emojiPattern: Pattern = uninitialized

  /** Initializes the singleton that stores the massive regular expression Pattern this uses internally. */
  def initialize(): Unit =
    if (emojiPattern == null) {
      // Simplified emoji pattern - the full pattern from the Java source uses RegExodus syntax.
      // For cross-platform compatibility, we use a subset that covers common emoji.
      // The full Unicode 14 pattern would need to be adapted for java.util.regex.
      emojiPattern = Pattern.compile(
        "(?:[#*0-9]\\uFE0F?\\u20E3" +
          "|[\\u00A9\\u00AE\\u203C\\u2049\\u2122\\u2139\\u2194-\\u2199\\u21A9\\u21AA" +
          "\\u2328\\u23CF\\u23ED-\\u23EF\\u23F1\\u23F2\\u23F8-\\u23FA\\u24C2" +
          "\\u25AA\\u25AB\\u25B6\\u25C0\\u25FB\\u25FC\\u25FE\\u2600-\\u2604" +
          "\\u260E\\u2611\\u2614\\u2615\\u2618\\u261D\\u2620\\u2622\\u2623" +
          "\\u2626\\u262A\\u262E\\u262F\\u2638-\\u263A\\u2640\\u2642" +
          "\\u2648-\\u2653\\u265F\\u2660\\u2663\\u2665\\u2666\\u2668" +
          "\\u267B\\u267E\\u267F\\u2692\\u2694-\\u2697\\u2699\\u269B\\u269C" +
          "\\u26A0\\u26A7\\u26AA\\u26B0\\u26B1\\u26BD\\u26BE\\u26C4\\u26C8" +
          "\\u26CF\\u26D1\\u26D3\\u26E9\\u26F0-\\u26F5\\u26F7\\u26F8\\u26FA" +
          "\\u2702\\u2708\\u2709\\u270F\\u2712\\u2714\\u2716\\u271D\\u2721" +
          "\\u2733\\u2734\\u2744\\u2747\\u274C\\u274E\\u2753-\\u2755\\u2757" +
          "\\u2763\\u27A1\\u2934\\u2935\\u2B05-\\u2B07\\u2B1B\\u2B1C\\u2B50" +
          "\\u3030\\u303D\\u3297\\u3299]\\uFE0F?)"
      )
    }

  /** Gets a replacement function that will replace any standard Unicode 14 or lower emoji that can be displayed by the given Font with the special Unicode PUA char used by that Font to show that
    * emoji. The given Font must have a nameLookup, typically by adding an atlas with KnownFonts.addEmoji(Font). If there is no nameLookup in the given Font, this returns null.
    */
  def replaceEmoji(text: String, font: Font): String = {
    initialize()
    Nullable.fold(font.nameLookup) {
      text
    } { nl =>
      val matcher = emojiPattern.matcher(text)
      val sb      = new StringBuffer()
      while (matcher.find()) {
        val replacement = nl.get(matcher.group(), '?'.toInt)
        matcher.appendReplacement(sb, String.valueOf(replacement.toChar))
      }
      matcher.appendTail(sb)
      sb.toString
    }
  }
}
