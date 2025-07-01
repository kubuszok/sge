package sge
package utils

object GdxNativesLoader {
  var disaableNativesLoading = false

  private var nativesLoaded = false

  def load(): Unit = synchronized {
    if (nativesLoaded) return
    if (disaableNativesLoading) return

    // TODO
    nativesLoaded = true
  }
}
