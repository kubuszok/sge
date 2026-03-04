/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/I18NBundleLoader.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODOs: test: I18NBundleLoader loadAsync/loadSync with locale and encoding parameters
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.utils.{ DynamicArray, I18NBundle, Nullable, SgeError }
import java.util.Locale

/** {@link AssetLoader} for {@link I18NBundle} instances. The I18NBundle is loaded asynchronously. <p> Notice that you can't load two bundles with the same base name and different locale or encoding
  * using the same {@link AssetManager} . For example, if you try to load the 2 bundles below
  *
  * <pre> manager.load(&quot;i18n/message&quot;, I18NBundle.class, new I18NBundleParameter(Locale.ITALIAN)); manager.load(&quot;i18n/message&quot;, I18NBundle.class, new
  * I18NBundleParameter(Locale.ENGLISH)); </pre>
  *
  * the English bundle won't be loaded because the asset manager thinks they are the same bundle since they have the same name. There are 2 use cases: <ul> <li>If you want to load the English bundle
  * so to replace the Italian bundle you have to unload the Italian bundle first. <li>If you want to load the English bundle without replacing the Italian bundle you should use another asset manager.
  * </ul>
  * @author
  *   davebaol (original implementation)
  */
class I18NBundleLoader(resolver: FileHandleResolver)(using Sge) extends AsynchronousAssetLoader[I18NBundle, I18NBundleLoader.I18NBundleParameter](resolver) {

  private var bundle: Nullable[I18NBundle] = Nullable.empty

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: I18NBundleLoader.I18NBundleParameter): Unit = {
    this.bundle = Nullable.empty
    val param    = Nullable(parameter)
    val locale   = param.fold(Locale.getDefault())(_.locale.getOrElse(Locale.getDefault()))
    val encoding = param.fold(Nullable.empty[String])(_.encoding)

    encoding.fold {
      this.bundle = Nullable(I18NBundle.createBundle(file, locale))
    } { enc =>
      this.bundle = Nullable(I18NBundle.createBundle(file, locale, enc))
    }
  }

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: I18NBundleLoader.I18NBundleParameter): I18NBundle = {
    val result = this.bundle
    this.bundle = Nullable.empty
    result.getOrElse(throw SgeError.SerializationError("I18NBundle not loaded"))
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: I18NBundleLoader.I18NBundleParameter): DynamicArray[AssetDescriptor[?]] =
    DynamicArray[AssetDescriptor[?]]()
}

object I18NBundleLoader {
  class I18NBundleParameter(val locale: Nullable[Locale] = Nullable.empty, val encoding: Nullable[String] = Nullable.empty) extends AssetLoaderParameters[I18NBundle] {
    def this() = this(Nullable.empty, Nullable.empty)
    def this(locale: Locale) = this(Nullable(locale), Nullable.empty)
  }
}
