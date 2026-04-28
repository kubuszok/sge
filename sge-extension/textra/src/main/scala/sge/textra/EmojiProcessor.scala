/*
 * MIT-licensed as part of emoji-regex (https://github.com/mathiasbynens/emoji-regex).
 * Copyright Mathias Bynens https://mathiasbynens.be/
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/EmojiProcessor.java
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: RegExodus Pattern/Replacer -> java.util.regex.Pattern/Matcher
 *   Convention: Emoji regex preserved for matching; replacement logic uses
 *     java.util.regex instead of RegExodus.
 *   Note: The massive regex pattern matches all Unicode 14 emoji sequences
 *     including skin tones, ZWJ sequences, regional indicator flags, and
 *     all Emoji 12-14 additions.
 *
 * Covenant: full-port
 * Covenant-source-reference: com/github/tommyettinger/textra/EmojiProcessor.java
 *   Renames: RegExodus Pattern/Replacer -> java.util.regex.Pattern/Matcher
 *   Convention: Emoji regex preserved for matching; replacement logic uses
 *   Note: The massive regex pattern matches all Unicode 14 emoji sequences
 * Covenant: full-port
 * Covenant-verified: 2026-04-11
 * Covenant-verified: 2026-04-11
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import java.util.regex.Pattern
import sge.utils.Nullable

/** Allows getting a replacement function that can replace any "correct Unicode" emoji sequences with the single PUA-char representation used by a Font to represent most emoji.
  *
  * This can parse every emoji in Emoji 14, which is what our version of Twemoji uses. It probably can't parse every emoji in Noto Color Emoji at this point, and might not handle all of OpenMoji,
  * either.
  *
  * Taken mostly from [[https://github.com/mathiasbynens/emoji-regex/ emoji-regex]], which is MIT-licensed.
  */
object EmojiProcessor {

  import scala.compiletime.uninitialized
  @volatile private var emojiPattern: Pattern = uninitialized

  /** Initializes the singleton that stores the massive regular expression Pattern this uses internally. */
  def initialize(): Unit =
    if (emojiPattern == null) {
      // Full Unicode 14 emoji regex ported from the Java/RegExodus original.
      // Covers: skin tone modifiers (\uD83C[\uDFFB-\uDFFF]), ZWJ sequences (\u200D),
      // regional indicator flags (\uDDE6-\uDDFF pairs), keycap sequences (#*0-9\uFE0F?\u20E3),
      // and all supplementary-plane emoji via surrogate pairs.
      emojiPattern = Pattern.compile(
        "(?:" +
          "[#*0-9]\ufe0f?\u20e3" +
          "|[\u00a9\u00ae\u203c\u2049\u2122\u2139\u2194-\u2199\u21a9\u21aa" +
          "\u2328\u23cf\u23ed-\u23ef\u23f1\u23f2\u23f8-\u23fa\u24c2" +
          "\u25aa\u25ab\u25b6\u25c0\u25fb\u25fc\u25fe\u2600-\u2604" +
          "\u260e\u2611\u2614\u2615\u2618\u2620\u2622\u2623" +
          "\u2626\u262a\u262e\u262f\u2638-\u263a\u2640\u2642" +
          "\u2648-\u2653\u265f\u2660\u2663\u2665\u2666\u2668" +
          "\u267b\u267e\u267f\u2692\u2694-\u2697\u2699\u269b\u269c" +
          "\u26a0\u26a7\u26aa\u26b0\u26b1\u26bd\u26be\u26c4\u26c8" +
          "\u26cf\u26d1\u26d3\u26e9\u26f0-\u26f5\u26f7\u26f8\u26fa" +
          "\u2702\u2708\u2709\u270f\u2712\u2714\u2716\u271d\u2721" +
          "\u2733\u2734\u2744\u2747\u274c\u274e\u2753-\u2755\u2757" +
          "\u2763\u27a1\u2934\u2935\u2b05-\u2b07\u2b1b\u2b1c\u2b50" +
          "\u3030\u303d\u3297\u3299]\ufe0f?" +
          "|[\u261d\u270c\u270d](?:\ufe0f|\ud83c[\udffb-\udfff])?" +
          "|[\u270a\u270b](?:\ud83c[\udffb-\udfff])?" +
          "|[\u23e9-\u23ec\u23f0\u23f3\u25fd\u2693\u26a1\u26ab\u26c5\u26ce\u26d4\u26ea\u26fd\u2705\u2728\u274c\u274e\u2753-\u2755\u2795-\u2797\u27b0\u27bf\u2b50]" +
          "|\u26f9(?:\ufe0f|\ud83c[\udffb-\udfff])?(?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|\u2764\ufe0f?(?:\u200d(?:\ud83d\udd25|\ud83e\ude79))?" +
          "|\ud83c(?:" +
          "[\udc04\udd70\udd71\udd7e\udd7f\ude02\ude37\udf21\udf24-\udf2c\udf36\udf7d\udf96\udf97\udf99-\udf9b\udf9e\udf9f\udfcd\udfce\udfd4-\udfdf\udff5\udff7]\ufe0f?" +
          "|[\udf85\udfc2\udfc7](?:\ud83c[\udffb-\udfff])?" +
          "|[\udfc3\udfc4\udfca](?:\ud83c[\udffb-\udfff])?(?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|[\udfcb\udfcc](?:\ufe0f|\ud83c[\udffb-\udfff])?(?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|[\udccf\udd8e\udd91-\udd9a\ude01\ude1a\ude2f\ude32-\ude36\ude38-\ude3a\ude50\ude51" +
          "\udf00-\udf20\udf2d-\udf35\udf37-\udf7c\udf7e-\udf84\udf86-\udf93" +
          "\udfa0-\udfc1\udfc5\udfc6\udfc8\udfc9\udfcf-\udfd3\udfe0-\udff0\udff8-\udfff]" +
          // Regional indicator flag sequences
          "|\udde6\ud83c[\udde8-\uddec\uddee\uddf1\uddf2\uddf4\uddf6-\uddfa\uddfc\uddfd\uddff]" +
          "|\udde7\ud83c[\udde6\udde7\udde9-\uddef\uddf1-\uddf4\uddf6-\uddf9\uddfb\uddfc\uddfe\uddff]" +
          "|\udde8\ud83c[\udde6\udde8\udde9\uddeb-\uddee\uddf0-\uddf5\uddf7\uddfa-\uddff]" +
          "|\udde9\ud83c[\uddea\uddec\uddef\uddf0\uddf2\uddf4\uddff]" +
          "|\uddea\ud83c[\udde6\udde8\uddea\uddec\udded\uddf7-\uddfa]" +
          "|\uddeb\ud83c[\uddee-\uddf0\uddf2\uddf4\uddf7]" +
          "|\uddec\ud83c[\udde6\udde7\udde9-\uddee\uddf1-\uddf3\uddf5-\uddfa\uddfc\uddfe]" +
          "|\udded\ud83c[\uddf0\uddf2\uddf3\uddf7\uddf9\uddfa]" +
          "|\uddee\ud83c[\udde8-\uddea\uddf1-\uddf4\uddf6-\uddf9]" +
          "|\uddef\ud83c[\uddea\uddf2\uddf4\uddf5]" +
          "|\uddf0\ud83c[\uddea\uddec-\uddee\uddf2\uddf3\uddf5\uddf7\uddfc\uddfe\uddff]" +
          "|\uddf1\ud83c[\udde6-\udde8\uddee\uddf0\uddf7-\uddfb\uddfe]" +
          "|\uddf2\ud83c[\udde6\udde8-\udded\uddf0-\uddff]" +
          "|\uddf3\ud83c[\udde6\udde8\uddea-\uddec\uddee\uddf1\uddf4\uddf5\uddf7\uddfa\uddff]" +
          "|\uddf4\ud83c\uddf2" +
          "|\uddf5\ud83c[\udde6\uddea-\udded\uddf0-\uddf3\uddf7-\uddf9\uddfc\uddfe]" +
          "|\uddf6\ud83c\udde6" +
          "|\uddf7\ud83c[\uddea\uddf4\uddf8\uddfa\uddfc]" +
          "|\uddf8\ud83c[\udde6-\uddea\uddec-\uddf4\uddf7-\uddf9\uddfb\uddfd-\uddff]" +
          "|\uddf9\ud83c[\udde6\udde8\udde9\uddeb-\udded\uddef-\uddf4\uddf7\uddf9\uddfb\uddfc\uddff]" +
          "|\uddfa\ud83c[\udde6\uddec\uddf2\uddf3\uddf8\uddfe\uddff]" +
          "|\uddfb\ud83c[\udde6\udde8\uddea\uddec\uddee\uddf3\uddfa]" +
          "|\uddfc\ud83c[\uddeb\uddf8]" +
          "|\uddfd\ud83c\uddf0" +
          "|\uddfe\ud83c[\uddea\uddf9]" +
          "|\uddff\ud83c[\udde6\uddf2\uddfc]" +
          "|\udff3\ufe0f?(?:\u200d(?:\u26a7\ufe0f?|\ud83c\udf08))?" +
          "|\udff4(?:\u200d\u2620\ufe0f?|\udb40\udc67\udb40\udc62\udb40(?:\udc65\udb40\udc6e\udb40\udc67|\udc73\udb40\udc63\udb40\udc74|\udc77\udb40\udc6c\udb40\udc73)\udb40\udc7f)?" +
          ")" +
          "|\ud83d(?:" +
          "[\udc3f\udcfd\udd49\udd4a\udd6f\udd70\udd73\udd76-\udd79\udd87\udd8a-\udd8d" +
          "\udda5\udda8\uddb1\uddb2\uddbc\uddc2-\uddc4\uddd1-\uddd3\udddc-\uddde" +
          "\udde1\udde3\udde8\uddef\uddf3\uddfa" +
          "\udecb\udecd-\udecf\udee0-\udee5\udee9\udef0\udef3]\ufe0f?" +
          "|[\udc42\udc43\udc46-\udc50\udc66\udc67\udc6b-\udc6d\udc72\udc74-\udc76\udc78\udc7c" +
          "\udc83\udc85\udc8f\udc91\udcaa\udd7a\udd95\udd96\ude4c\ude4f\udec0\udecc](?:\ud83c[\udffb-\udfff])?" +
          "|[\udc6e\udc70\udc71\udc73\udc77\udc81\udc82\udc86\udc87" +
          "\ude45-\ude47\ude4b\ude4d\ude4e\udea3\udeb4-\udeb6](?:\ud83c[\udffb-\udfff])?(?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|[\udd74\udd90](?:\ufe0f|\ud83c[\udffb-\udfff])?" +
          "|[\udc00-\udc07\udc09-\udc14\udc16-\udc3a\udc3c-\udc3e\udc40\udc44\udc45" +
          "\udc51-\udc65\udc6a\udc79-\udc7b\udc7d-\udc80\udc84\udc88-\udc8e\udc90" +
          "\udc92-\udca9\udcab-\udcfc\udcff-\udd3d\udd4b-\udd4e\udd50-\udd67\udda4" +
          "\uddfb-\ude2d\ude2f-\ude34\ude37-\ude44\ude48-\ude4a" +
          "\ude80-\udea2\udea4-\udeb3\udeb7-\udebf\udec1-\udec5" +
          "\uded0-\uded2\uded5-\uded7\udedd-\udedf\udeeb\udeec\udef4-\udefc" +
          "\udfe0-\udfeb\udff0]" +
          "|\udc08(?:\u200d\u2b1b)?" +
          "|\udc15(?:\u200d\ud83e\uddba)?" +
          "|\udc3b(?:\u200d\u2744\ufe0f?)?" +
          "|\udc41\ufe0f?(?:\u200d\ud83d\udde8\ufe0f?)?" +
          "|\udc68(?:" +
          "\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:\udc8b\u200d\ud83d)?\udc68" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d(?:" +
          "[\udc68\udc69]\u200d\ud83d(?:\udc66(?:\u200d\ud83d\udc66)?|\udc67(?:\u200d\ud83d[\udc66\udc67])?)" +
          "|[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\udc66(?:\u200d\ud83d\udc66)?" +
          "|\udc67(?:\u200d\ud83d[\udc66\udc67])?" +
          ")" +
          "|\ud83e[\uddaf-\uddb3\uddbc\uddbd]" +
          ")" +
          "|\ud83c(?:" +
          "\udffb(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:\udc8b\u200d\ud83d)?\udc68\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d\udc68\ud83c[\udffc-\udfff])" +
          "))?" +
          "|\udffc(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:\udc8b\u200d\ud83d)?\udc68\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d\udc68\ud83c[\udffb\udffd-\udfff])" +
          "))?" +
          "|\udffd(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:\udc8b\u200d\ud83d)?\udc68\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d\udc68\ud83c[\udffb\udffc\udffe\udfff])" +
          "))?" +
          "|\udffe(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:\udc8b\u200d\ud83d)?\udc68\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d\udc68\ud83c[\udffb-\udffd\udfff])" +
          "))?" +
          "|\udfff(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:\udc8b\u200d\ud83d)?\udc68\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d\udc68\ud83c[\udffb-\udffe])" +
          "))?" +
          ")?" +
          ")" +
          "|\udc69(?:" +
          "\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:\udc8b\u200d\ud83d)?[\udc68\udc69]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d(?:" +
          "[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\udc66(?:\u200d\ud83d\udc66)?" +
          "|\udc67(?:\u200d\ud83d[\udc66\udc67])?" +
          "|\udc69\u200d\ud83d(?:\udc66(?:\u200d\ud83d\udc66)?|\udc67(?:\u200d\ud83d[\udc66\udc67])?)" +
          ")" +
          "|\ud83e[\uddaf-\uddb3\uddbc\uddbd]" +
          ")" +
          "|\ud83c(?:" +
          "\udffb(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:[\udc68\udc69]|\udc8b\u200d\ud83d[\udc68\udc69])\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d[\udc68\udc69]\ud83c[\udffc-\udfff])" +
          "))?" +
          "|\udffc(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:[\udc68\udc69]|\udc8b\u200d\ud83d[\udc68\udc69])\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d[\udc68\udc69]\ud83c[\udffb\udffd-\udfff])" +
          "))?" +
          "|\udffd(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:[\udc68\udc69]|\udc8b\u200d\ud83d[\udc68\udc69])\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d[\udc68\udc69]\ud83c[\udffb\udffc\udffe\udfff])" +
          "))?" +
          "|\udffe(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:[\udc68\udc69]|\udc8b\u200d\ud83d[\udc68\udc69])\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d[\udc68\udc69]\ud83c[\udffb-\udffd\udfff])" +
          "))?" +
          "|\udfff(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?\u200d\ud83d(?:[\udc68\udc69]|\udc8b\u200d\ud83d[\udc68\udc69])\ud83c[\udffb-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83d[\udc68\udc69]\ud83c[\udffb-\udffe])" +
          "))?" +
          ")?" +
          ")" +
          "|\udc6f(?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|\udd75(?:\ufe0f|\ud83c[\udffb-\udfff])?(?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|\ude2e(?:\u200d\ud83d\udca8)?" +
          "|\ude35(?:\u200d\ud83d\udcab)?" +
          "|\ude36(?:\u200d\ud83c\udf2b\ufe0f?)?" +
          ")" +
          "|\ud83e(?:" +
          "[\udd0c\udd0f\udd18-\udd1f\udd30-\udd34\udd36\udd77\uddb5\uddb6\uddbb\uddd2\uddd3\uddd5" +
          "\udec3-\udec5\udef0\udef2-\udef6](?:\ud83c[\udffb-\udfff])?" +
          "|[\udd26\udd35\udd37-\udd39\udd3d\udd3e\uddb8\uddb9\uddcd-\uddcf\uddd4" +
          "\uddd6-\udddd](?:\ud83c[\udffb-\udfff])?(?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|[\uddde\udddf](?:\u200d[\u2640\u2642]\ufe0f?)?" +
          "|[\udd0d\udd0e\udd10-\udd17\udd20-\udd25\udd27-\udd2f\udd3a\udd3f-\udd45\udd47-\udd76" +
          "\udd78-\uddb4\uddb7\uddba\uddbc-\uddcc\uddd0" +
          "\udde0-\uddff\ude70-\ude74\ude78-\ude7c\ude80-\ude86" +
          "\ude90-\udeac\udeb0-\udeba\udec0-\udec2\uded0-\uded9\udee0-\udee7]" +
          "|\udd3c(?:\u200d[\u2640\u2642]\ufe0f?|\ud83c[\udffb-\udfff])?" +
          "|\uddd1(?:" +
          "\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\ud83c[\udf3e\udf73\udf7c\udf84\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83e\uddd1)" +
          ")" +
          "|\ud83c(?:" +
          "\udffb(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?(?:\u200d\ud83d\udc8b)?\u200d\ud83e\uddd1\ud83c[\udffc-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf84\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83e\uddd1\ud83c[\udffb-\udfff])" +
          "))?" +
          "|\udffc(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?(?:\u200d\ud83d\udc8b)?\u200d\ud83e\uddd1\ud83c[\udffb\udffd-\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf84\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83e\uddd1\ud83c[\udffb-\udfff])" +
          "))?" +
          "|\udffd(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?(?:\u200d\ud83d\udc8b)?\u200d\ud83e\uddd1\ud83c[\udffb\udffc\udffe\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf84\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83e\uddd1\ud83c[\udffb-\udfff])" +
          "))?" +
          "|\udffe(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?(?:\u200d\ud83d\udc8b)?\u200d\ud83e\uddd1\ud83c[\udffb-\udffd\udfff]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf84\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83e\uddd1\ud83c[\udffb-\udfff])" +
          "))?" +
          "|\udfff(?:\u200d(?:" +
          "[\u2695\u2696\u2708]\ufe0f?" +
          "|\u2764\ufe0f?(?:\u200d\ud83d\udc8b)?\u200d\ud83e\uddd1\ud83c[\udffb-\udffe]" +
          "|\ud83c[\udf3e\udf73\udf7c\udf84\udf93\udfa4\udfa8\udfeb\udfed]" +
          "|\ud83d[\udcbb\udcbc\udd27\udd2c\ude80\ude92]" +
          "|\ud83e(?:[\uddaf-\uddb3\uddbc\uddbd]|\udd1d\u200d\ud83e\uddd1\ud83c[\udffb-\udfff])" +
          "))?" +
          ")?" +
          ")" +
          "|\udef1(?:\ud83c(?:" +
          "\udffb(?:\u200d\ud83e\udef2\ud83c[\udffc-\udfff])?" +
          "|\udffc(?:\u200d\ud83e\udef2\ud83c[\udffb\udffd-\udfff])?" +
          "|\udffd(?:\u200d\ud83e\udef2\ud83c[\udffb\udffc\udffe\udfff])?" +
          "|\udffe(?:\u200d\ud83e\udef2\ud83c[\udffb-\udffd\udfff])?" +
          "|\udfff(?:\u200d\ud83e\udef2\ud83c[\udffb-\udffe])?" +
          "))?" +
          ")" +
          ")"
      )
    }

  /** Gets a replacement function that will replace any standard Unicode 14 or lower emoji that can be displayed by the given Font with the special Unicode PUA char used by that Font to show that
    * emoji. The given Font must have a nameLookup, typically by adding an atlas with KnownFonts.addEmoji(Font). If there is no nameLookup in the given Font, this returns the original text unchanged.
    *
    * @param text
    *   the text to process for emoji replacement
    * @param font
    *   a Font that already has emoji added to it, typically Twemoji via KnownFonts.addEmoji(Font)
    * @return
    *   the text with any readable Unicode emoji replaced by their Font-specific char representations
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
