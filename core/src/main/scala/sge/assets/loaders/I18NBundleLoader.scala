package sge
package assets
package loaders

import sge.files.FileHandle
import sge.utils.I18NBundle
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer
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
class I18NBundleLoader(resolver: FileHandleResolver)(using sge: Sge) extends AsynchronousAssetLoader[I18NBundle, I18NBundleLoader.I18NBundleParameter](resolver) {

  private var bundle: Nullable[I18NBundle] = Nullable.empty

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: I18NBundleLoader.I18NBundleParameter): Unit = {
    this.bundle = Nullable.empty
    val locale = parameter match {
      case null                  => Locale.getDefault()
      case p if p.locale == null => Locale.getDefault()
      case p                     => p.locale
    }
    val encoding = parameter match {
      case null => null
      case p    => p.encoding
    }

    if (encoding == null) {
      this.bundle = Nullable(I18NBundle.createBundle(file, locale))
    } else {
      this.bundle = Nullable(I18NBundle.createBundle(file, locale, encoding))
    }
  }

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: I18NBundleLoader.I18NBundleParameter): I18NBundle = {
    val result = this.bundle
    this.bundle = Nullable.empty
    result.orNull
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: I18NBundleLoader.I18NBundleParameter): ArrayBuffer[AssetDescriptor[?]] =
    ArrayBuffer.empty
}

object I18NBundleLoader {
  class I18NBundleParameter(val locale: Locale = null, val encoding: String = null) extends AssetLoaderParameters[I18NBundle] {
    def this() = this(null, null)
    def this(locale: Locale) = this(locale, null)
  }
}
