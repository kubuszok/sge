/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: LinkEffect,link,onApply
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/LinkEffect.java
 * Covenant-verified: 2026-06-12
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.textra.utils.StringUtils

/** Allows clicking the affected text to open a URL in the browser.
  *
  * Upstream resolves the global `Gdx.net` at click time (LinkEffect.java:58). SGE has no global; instead every [[TextraLabel]] (and therefore every [[TypingLabel]]) captures the per-application
  * [[Sge]] context its constructor was given. `onApply` opens the URL through that context's `Sge.net.openURI`, exactly as upstream does through `Gdx.net.openURI` — so a `{LINK=url}` click can never
  * silently no-op.
  */
class LinkEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val link: String = if (params.length > 0) StringUtils.join(";", params*) else "https://libgdx.com"
  label.trackingInput = true

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit =
    if (label.lastTouchedIndex == globalIndex) {
      label.lastTouchedIndex = -1
      val _ = label.sgeContext.net.openURI(link)
    }
}
