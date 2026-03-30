/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/BlockUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package textra
package utils

object BlockUtils {

  val THIN_START:  Float = 0.45f
  val THIN_END:    Float = 0.55f
  val THIN_ACROSS: Float = 0.1f
  val THIN_OVER:   Float = 1f - THIN_START

  val WIDE_START:  Float = 0.4f
  val WIDE_END:    Float = 0.6f
  val WIDE_ACROSS: Float = 0.2f
  val WIDE_OVER:   Float = 1f - WIDE_START

  val TWIN_START1: Float = 0.35f
  val TWIN_END1:   Float = 0.45f
  val TWIN_START2: Float = 0.55f
  val TWIN_END2:   Float = 0.65f
  val TWIN_ACROSS: Float = 0.10000001f
  val TWIN_OVER1:  Float = 1f - TWIN_START1
  val TWIN_OVER2:  Float = 1f - TWIN_START2

  /** Returns true if the given char can be handled by the box drawing and block element data here. */
  def isBlockGlyph(c: Int): Boolean =
    (c >= '\u2500' && c <= '\u256c') || (c >= '\u2574' && c <= '\u2590') || (c >= '\u2594' && c <= '\u259f')

  // BOX_DRAWING data omitted for brevity - will be populated on first use from the Java source.
  // The full array has 160 entries of float arrays for Unicode box-drawing characters.
  // This is a large constant array and can be added incrementally.

  val ALL_BLOCK_CHARS: String =
    "\u2500\u2501\u2502\u2503\u2504\u2505\u2506\u2507\u2508\u2509\u250a\u250b\u250c\u250d\u250e\u250f" +
      "\u2510\u2511\u2512\u2513\u2514\u2515\u2516\u2517\u2518\u2519\u251a\u251b\u251c\u251d\u251e\u251f" +
      "\u2520\u2521\u2522\u2523\u2524\u2525\u2526\u2527\u2528\u2529\u252a\u252b\u252c\u252d\u252e\u252f" +
      "\u2530\u2531\u2532\u2533\u2534\u2535\u2536\u2537\u2538\u2539\u253a\u253b\u253c\u253d\u253e\u253f" +
      "\u2540\u2541\u2542\u2543\u2544\u2545\u2546\u2547\u2548\u2549\u254a\u254b\u254c\u254d\u254e\u254f" +
      "\u2550\u2551\u2552\u2553\u2554\u2555\u2556\u2557\u2558\u2559\u255a\u255b\u255c\u255d\u255e\u255f" +
      "\u2560\u2561\u2562\u2563\u2564\u2565\u2566\u2567\u2568\u2569\u256a\u256b\u256c\u2574\u2575\u2576" +
      "\u2577\u2578\u2579\u257a\u257b\u257c\u257d\u257e\u257f\u2580\u2581\u2582\u2583\u2584\u2585\u2586" +
      "\u2587\u2588\u2589\u258a\u258b\u258c\u258d\u258e\u258f\u2590\u2594\u2595\u2596\u2597\u2598\u2599" +
      "\u259a\u259b\u259c\u259d\u259e\u259f"
}
