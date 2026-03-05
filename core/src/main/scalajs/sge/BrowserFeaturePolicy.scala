/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtFeaturePolicy.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtFeaturePolicy -> BrowserFeaturePolicy
 *   Convention: Scala.js only; JSNI -> js.Dynamic
 *   Convention: GwtUtils.toStringArray removed (not needed in Scala.js)
 *   Idiom: Nullable[Array[String]] for null-returning methods
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import org.scalajs.dom.document
import scala.scalajs.js
import sge.utils.Nullable

/** Implementation of the [[https://w3c.github.io/webappsec-feature-policy/#featurepolicy Feature Policy Interface]].
  *
  * Wraps `document.featurePolicy` (deprecated but still supported in some browsers). When the API is not available, `allowsFeature` returns true (permissive default).
  */
object BrowserFeaturePolicy {

  private def isSupported: Boolean = {
    val doc = document.asInstanceOf[js.Dynamic]
    !js.isUndefined(doc.featurePolicy)
  }

  def allowsFeature(feature: String): Boolean =
    if (!isSupported) true
    else document.asInstanceOf[js.Dynamic].featurePolicy.allowsFeature(feature).asInstanceOf[Boolean]

  def allowsFeature(feature: String, origin: String): Boolean =
    if (!isSupported) true
    else document.asInstanceOf[js.Dynamic].featurePolicy.allowsFeature(feature, origin).asInstanceOf[Boolean]

  def features(): Nullable[Array[String]] =
    if (!isSupported) Nullable.empty
    else {
      val jsArr = document.asInstanceOf[js.Dynamic].featurePolicy.features().asInstanceOf[js.Array[String]]
      Nullable(jsArr.toArray)
    }

  def allowedFeatures(): Nullable[Array[String]] =
    if (!isSupported) Nullable.empty
    else {
      val jsArr = document.asInstanceOf[js.Dynamic].featurePolicy.allowedFeatures().asInstanceOf[js.Array[String]]
      Nullable(jsArr.toArray)
    }

  def getAllowlistForFeature(feature: String): Nullable[Array[String]] =
    if (!isSupported) Nullable.empty
    else {
      val jsArr =
        document.asInstanceOf[js.Dynamic].featurePolicy.getAllowlistForFeature(feature).asInstanceOf[js.Array[String]]
      Nullable(jsArr.toArray)
    }
}
