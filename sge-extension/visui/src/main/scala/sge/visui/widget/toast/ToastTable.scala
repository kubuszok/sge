/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: ToastTable,fadeOut,getToast,setToast,toast
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/toast/ToastTable.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package toast

import sge.utils.Nullable

/** Base class for all toast content tables. Note that using this class is not required ([[VisTable]] can be used directly) however it's preferred because it provides access to [[Toast]] instance and
  * [[fadeOut]] method. Using ToastTable also allows to reuse [[Toast]] instance instead of creating new one every time you want to show toast.
  * @author
  *   Kotcrab
  * @since 1.1.0
  */
class ToastTable(setVisDefaults: Boolean = false)(using Sge) extends VisTable(setVisDefaults) {
  protected var toast: Nullable[Toast] = Nullable.empty

  def fadeOut(): Unit = {
    if (toast.isEmpty) {
      throw new IllegalStateException("fadeOut can't be called before toast was shown by ToastManager")
    }
    toast.get.fadeOut()
  }

  /** Called by framework when this ToastTable was assigned to its toast container. */
  def setToast(toast: Toast): Unit = this.toast = Nullable(toast)

  /** @return toast that this table belongs to or null if none */
  def getToast: Nullable[Toast] = toast
}
