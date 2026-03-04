/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/I18nBundle.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `static` -> companion object; `return` -> `boundary`/`break`; `null` -> `Nullable`
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getLocale, getSimpleFormatter/setSimpleFormatter, getExceptionOnMissingKey/setExceptionOnMissingKey
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ IOException, Reader }
import java.util.{ ArrayList, List => JList, Locale, MissingResourceException }
import scala.collection.mutable.{ LinkedHashSet, Set => MutableSet }
import scala.util.boundary
import sge.files.FileHandle
import sge.utils.{ Nullable, ObjectMap, PropertiesUtils, TextFormatter }

import scala.language.implicitConversions

/** A {@code I18NBundle} provides {@code Locale} -specific resources loaded from property files. A bundle contains a number of named resources, whose names and values are {@code Strings} . A bundle
  * may have a parent bundle, and when a resource is not found in a bundle, the parent bundle is searched for the resource. If the fallback mechanism reaches the base bundle and still can't find the
  * resource it throws a {@code MissingResourceException} .
  *
  * <ul> <li>All bundles for the same group of resources share a common base bundle. This base bundle acts as the root and is the last fallback in case none of its children was able to respond to a
  * request.</li> <li>The first level contains changes between different languages. Only the differences between a language and the language of the base bundle need to be handled by a
  * language-specific {@code I18NBundle} .</li> <li>The second level contains changes between different countries that use the same language. Only the differences between a country and the country of
  * the language bundle need to be handled by a country-specific {@code I18NBundle} .</li> <li>The third level contains changes that don't have a geographic reason (e.g. changes that where made at
  * some point in time like {@code PREEURO} where the currency of come countries changed. The country bundle would return the current currency (Euro) and the {@code PREEURO} variant bundle would
  * return the old currency (e.g. DM for Germany).</li> </ul>
  *
  * <strong>Examples</strong> <ul> <li>BaseName (base bundle) <li>BaseName_de (german language bundle) <li>BaseName_fr (french language bundle) <li>BaseName_de_DE (bundle with Germany specific
  * resources in german) <li>BaseName_de_CH (bundle with Switzerland specific resources in german) <li>BaseName_fr_CH (bundle with Switzerland specific resources in french) <li>BaseName_de_DE_PREEURO
  * (bundle with Germany specific resources in german of the time before the Euro) <li>BaseName_fr_FR_PREEURO (bundle with France specific resources in french of the time before the Euro) </ul>
  *
  * It's also possible to create variants for languages or countries. This can be done by just skipping the country or language abbreviation: BaseName_us__POSIX or BaseName__DE_PREEURO. But it's not
  * allowed to circumvent both language and country: BaseName___VARIANT is illegal.
  *
  * @see
  *   PropertiesUtils
  *
  * @author
  *   davebaol (original implementation)
  */
class I18NBundle {

  /** The parent of this {@code I18NBundle} that is used if this bundle doesn't include the requested resource. */
  private var parent: Nullable[I18NBundle] = Nullable.empty

  /** The locale for this bundle. */
  private var locale: Locale = scala.compiletime.uninitialized

  /** The properties for this bundle. */
  private var properties: ObjectMap[String, String] = scala.compiletime.uninitialized

  /** The formatter used for argument replacement. */
  private var formatter: TextFormatter = scala.compiletime.uninitialized

  /** Load the properties from the specified reader.
    *
    * @param reader
    *   the reader
    * @throws IOException
    *   if an error occurred when reading from the input stream.
    */
  @throws[IOException]
  protected def load(reader: Reader): Unit = {
    properties = ObjectMap[String, String]()
    PropertiesUtils.load(properties, reader)
  }

  /** Returns the locale of this bundle. This method can be used after a call to <code>createBundle()</code> to determine whether the resource bundle returned really corresponds to the requested
    * locale or is a fallback.
    *
    * @return
    *   the locale of this bundle
    */
  def getLocale: Locale = locale

  /** Sets the bundle locale. This method is private because a bundle can't change the locale during its life.
    *
    * @param locale
    */
  private def setLocale(locale: Locale): Unit = {
    this.locale = locale
    this.formatter = TextFormatter(locale, !I18NBundle.simpleFormatter)
  }

  /** Gets a string for the given key from this bundle or one of its parents.
    *
    * @param key
    *   the key for the desired string
    * @exception
    *   NullPointerException if <code>key</code> is <code>null</code>
    * @exception
    *   MissingResourceException if no string for the given key can be found and {@link #getExceptionOnMissingKey()} returns {@code true}
    * @return
    *   the string for the given key or the key surrounded by {@code ???} if it cannot be found and {@link #getExceptionOnMissingKey()} returns {@code false}
    */
  def get(key: String): String = boundary {
    properties.get(key).foreach(r => boundary.break(r))
    parent.foreach(p => boundary.break(p.get(key)))
    if (I18NBundle.exceptionOnMissingKey)
      throw new MissingResourceException("Can't find bundle key " + key, this.getClass.getName, key)
    else
      "???" + key + "???"
  }

  /** Gets a key set of loaded properties. Keys will be copied into a new set and returned.
    *
    * @return
    *   a key set of loaded properties. Never null, might be an empty set
    */
  def keys(): MutableSet[String] = {
    val result = new LinkedHashSet[String]()
    properties.foreachKey(key => result.add(key))
    result
  }

  /** Gets the string with the specified key from this bundle or one of its parent after replacing the given arguments if they occur.
    *
    * @param key
    *   the key for the desired string
    * @param args
    *   the arguments to be replaced in the string associated to the given key.
    * @exception
    *   NullPointerException if <code>key</code> is <code>null</code>
    * @exception
    *   MissingResourceException if no string for the given key can be found
    * @return
    *   the string for the given key formatted with the given arguments
    */
  def format(key: String, args: AnyRef*): String =
    formatter.format(get(key), args*)

  /** Sets the value of all localized strings to String placeholder so hardcoded, unlocalized values can be easily spotted. The I18NBundle won't be able to reset values after calling debug and should
    * only be using during testing.
    *
    * @param placeholder
    */
  def debug(placeholder: String): Unit =
    properties.foreachKey(key => properties.put(key, placeholder))
}

object I18NBundle {
  private val DEFAULT_ENCODING = "UTF-8"

  private var simpleFormatter       = false
  private var exceptionOnMissingKey = true

  /** Returns the flag indicating whether to use the simplified message pattern syntax (default is false). This flag is always assumed to be true on GWT backend.
    */
  def getSimpleFormatter: Boolean = simpleFormatter

  /** Sets the flag indicating whether to use the simplified message pattern. The flag must be set before calling the factory methods {@code createBundle} . Notice that this method has no effect on
    * the GWT backend where it's always assumed to be true.
    */
  def setSimpleFormatter(enabled: Boolean): Unit =
    simpleFormatter = enabled

  /** Returns the flag indicating whether to throw a {@link MissingResourceException} from the {@link #get(String) get(key)} method if no string for the given key can be found. If this flag is
    * {@code false} the missing key surrounded by {@code ???} is returned.
    */
  def getExceptionOnMissingKey: Boolean = exceptionOnMissingKey

  /** Sets the flag indicating whether to throw a {@link MissingResourceException} from the {@link #get(String) get(key)} method if no string for the given key can be found. If this flag is
    * {@code false} the missing key surrounded by {@code ???} is returned.
    */
  def setExceptionOnMissingKey(enabled: Boolean): Unit =
    exceptionOnMissingKey = enabled

  /** Creates a new bundle using the specified <code>baseFileHandle</code>, the default locale and the default encoding "UTF-8".
    *
    * @param baseFileHandle
    *   the file handle to the base of the bundle
    * @exception
    *   NullPointerException if <code>baseFileHandle</code> is <code>null</code>
    * @exception
    *   MissingResourceException if no bundle for the specified base file handle can be found
    * @return
    *   a bundle for the given base file handle and the default locale
    */
  def createBundle(baseFileHandle: FileHandle): I18NBundle =
    createBundleImpl(baseFileHandle, Locale.getDefault(), DEFAULT_ENCODING)

  /** Creates a new bundle using the specified <code>baseFileHandle</code> and <code>locale</code>; the default encoding "UTF-8" is used.
    *
    * @param baseFileHandle
    *   the file handle to the base of the bundle
    * @param locale
    *   the locale for which a bundle is desired
    * @return
    *   a bundle for the given base file handle and locale
    * @exception
    *   NullPointerException if <code>baseFileHandle</code> or <code>locale</code> is <code>null</code>
    * @exception
    *   MissingResourceException if no bundle for the specified base file handle can be found
    */
  def createBundle(baseFileHandle: FileHandle, locale: Locale): I18NBundle =
    createBundleImpl(baseFileHandle, locale, DEFAULT_ENCODING)

  /** Creates a new bundle using the specified <code>baseFileHandle</code> and <code>encoding</code>; the default locale is used.
    *
    * @param baseFileHandle
    *   the file handle to the base of the bundle
    * @param encoding
    *   the character encoding
    * @return
    *   a bundle for the given base file handle and locale
    * @exception
    *   NullPointerException if <code>baseFileHandle</code> or <code>encoding</code> is <code>null</code>
    * @exception
    *   MissingResourceException if no bundle for the specified base file handle can be found
    */
  def createBundle(baseFileHandle: FileHandle, encoding: String): I18NBundle =
    createBundleImpl(baseFileHandle, Locale.getDefault(), encoding)

  /** Creates a new bundle using the specified <code>baseFileHandle</code>, <code>locale</code> and <code>encoding</code>.
    *
    * @param baseFileHandle
    *   the file handle to the base of the bundle
    * @param locale
    *   the locale for which a bundle is desired
    * @param encoding
    *   the character encoding
    * @return
    *   a bundle for the given base file handle and locale
    * @exception
    *   NullPointerException if <code>baseFileHandle</code>, <code>locale</code> or <code>encoding</code> is <code>null</code>
    * @exception
    *   MissingResourceException if no bundle for the specified base file handle can be found
    */
  def createBundle(baseFileHandle: FileHandle, locale: Locale, encoding: String): I18NBundle =
    createBundleImpl(baseFileHandle, locale, encoding)

  private def createBundleImpl(baseFileHandle: FileHandle, locale: Locale, encoding: String): I18NBundle = boundary {
    var bundle:          Nullable[I18NBundle] = Nullable.empty
    var baseBundle:      Nullable[I18NBundle] = Nullable.empty
    var targetLocaleOpt: Nullable[Locale]     = Nullable(locale)
    while (targetLocaleOpt.isDefined) {
      val targetLocale = targetLocaleOpt.getOrElse(throw new AssertionError("unreachable"))
      // Create the candidate locales
      val candidateLocales = getCandidateLocales(targetLocale)

      // Load the bundle and its parents recursively
      bundle = loadBundleChain(baseFileHandle, encoding, candidateLocales, 0, baseBundle)

      // Check the loaded bundle (if any)
      bundle.foreach { b =>
        val bundleLocale = b.getLocale // WTH? GWT can't access bundle.locale directly
        val isBaseBundle = bundleLocale.equals(Locale.ROOT)

        if (!isBaseBundle || bundleLocale.equals(locale)) {
          // Found the bundle for the requested locale
          boundary.break(b)
        }

        if (baseBundle.isEmpty) {
          // Store the base bundle for later use
          baseBundle = bundle
        }
      }

      // Try the fallback locale
      targetLocaleOpt = getFallbackLocale(targetLocale)
    }

    bundle.getOrElse {
      baseBundle.getOrElse {
        throw new MissingResourceException("Can't find bundle for base file handle " + baseFileHandle, "", "")
      }
    }
  }

  /** Returns a <code>List</code> of <code>Locale</code>s as candidate locales for <code>locale</code>. This method is equivalent to the implementation of {@link
    * java.util.ResourceBundle.Control#getCandidateLocales(String, Locale) ResourceBundle.Control.getCandidateLocales(String, Locale)}.
    *
    * @param locale
    *   the locale for which a resource bundle is desired
    * @return
    *   a <code>List</code> of candidate <code>Locale</code>s for the given <code>locale</code>
    * @exception
    *   NullPointerException if <code>locale</code> is <code>null</code>
    */
  private def getCandidateLocales(locale: Locale): JList[Locale] = {
    val language = locale.getLanguage()
    val country  = locale.getCountry()
    val variant  = locale.getVariant()

    val locales = new ArrayList[Locale](4)
    if (!variant.isEmpty()) {
      locales.add(locale)
    }
    if (!country.isEmpty()) {
      locales.add(if (locales.isEmpty()) locale else Locale.of(language, country))
    }
    if (!language.isEmpty()) {
      locales.add(if (locales.isEmpty()) locale else Locale.of(language))
    }
    locales.add(Locale.ROOT)
    locales
  }

  /** Returns a <code>Locale</code> to be used as a fallback locale for further bundle searches by the <code>createBundle</code> factory method. This method is called from the factory method every
    * time when no resulting bundle has been found for <code>baseFileHandler</code> and <code>locale</code>, where locale is either the parameter for <code>createBundle</code> or the previous fallback
    * locale returned by this method.
    *
    * <p> This method returns the {@linkplain Locale#getDefault() default <code>Locale</code>} if the given <code>locale</code> isn't the default one. Otherwise, <code>null</code> is returned.
    *
    * @param locale
    *   the <code>Locale</code> for which <code>createBundle</code> has been unable to find any resource bundles (except for the base bundle)
    * @return
    *   a <code>Locale</code> for the fallback search, or <code>null</code> if no further fallback search is needed.
    */
  private def getFallbackLocale(locale: Locale): Nullable[Locale] = {
    val defaultLocale = Locale.getDefault()
    if (locale.equals(defaultLocale)) Nullable.empty else Nullable(defaultLocale)
  }

  private def loadBundleChain(baseFileHandle: FileHandle, encoding: String, candidateLocales: JList[Locale], candidateIndex: Int, baseBundle: Nullable[I18NBundle]): Nullable[I18NBundle] = boundary {
    val targetLocale = candidateLocales.get(candidateIndex)
    var parent: Nullable[I18NBundle] = Nullable.empty
    if (candidateIndex != candidateLocales.size() - 1) {
      // Load recursively the parent having the next candidate locale
      parent = loadBundleChain(baseFileHandle, encoding, candidateLocales, candidateIndex + 1, baseBundle)
    } else if (baseBundle.isDefined && targetLocale.equals(Locale.ROOT)) {
      boundary.break(baseBundle)
    }

    // Load the bundle
    val bundle = loadBundle(baseFileHandle, encoding, targetLocale)
    bundle.foreach { b =>
      b.parent = parent
      boundary.break(bundle)
    }

    parent
  }

  // Tries to load the bundle for the given locale.
  private def loadBundle(baseFileHandle: FileHandle, encoding: String, targetLocale: Locale): Nullable[I18NBundle] = {
    var bundle: Nullable[I18NBundle] = Nullable.empty
    var reader: Nullable[Reader]     = Nullable.empty
    try {
      val fileHandle = toFileHandle(baseFileHandle, targetLocale)
      if (checkFileExistence(fileHandle)) {
        // Instantiate the bundle
        val b = new I18NBundle()

        // Load bundle properties from the stream with the specified encoding
        val r = fileHandle.reader(encoding)
        reader = Nullable(r)
        b.load(r)
        bundle = Nullable(b)
      }
    } catch {
      case e: IOException =>
        throw SgeError.FileReadError(baseFileHandle, "Error loading I18N bundle", Some(e))
    } finally
      reader.foreach { r =>
        try
          r.close()
        catch {
          case _: IOException =>
        }
      }
    bundle.foreach(_.setLocale(targetLocale))

    bundle
  }

  // On Android this is much faster than fh.exists(), see https://github.com/libgdx/libgdx/issues/2342
  // Also this should fix a weird problem on iOS, see https://github.com/libgdx/libgdx/issues/2345
  private def checkFileExistence(fh: FileHandle): Boolean =
    try {
      fh.read().close()
      true
    } catch {
      case _: Exception => false
    }

  /** Converts the given <code>baseFileHandle</code> and <code>locale</code> to the corresponding file handle.
    *
    * <p> This implementation returns the <code>baseFileHandle</code>'s sibling with following value:
    *
    * <pre> baseFileHandle.name() + &quot;_&quot; + language + &quot;_&quot; + country + &quot;_&quot; + variant + &quot;.properties&quot; </pre>
    *
    * where <code>language</code>, <code>country</code> and <code>variant</code> are the language, country and variant values of <code>locale</code>, respectively. Final component values that are
    * empty Strings are omitted along with the preceding '_'. If all of the values are empty strings, then <code>baseFileHandle.name()</code> is returned with ".properties" appended.
    *
    * @param baseFileHandle
    *   the file handle to the base of the bundle
    * @param locale
    *   the locale for which a resource bundle should be loaded
    * @return
    *   the file handle for the bundle
    * @exception
    *   NullPointerException if <code>baseFileHandle</code> or <code>locale</code> is <code>null</code>
    */
  private def toFileHandle(baseFileHandle: FileHandle, locale: Locale): FileHandle = {
    val sb = new StringBuilder(baseFileHandle.name())
    if (!locale.equals(Locale.ROOT)) {
      val language = locale.getLanguage()
      val country  = locale.getCountry()
      val variant  = locale.getVariant()

      if (!(language.isEmpty() && country.isEmpty() && variant.isEmpty())) {
        sb.append('_')
        if (!variant.isEmpty()) {
          sb.append(language).append('_').append(country).append('_').append(variant)
        } else if (!country.isEmpty()) {
          sb.append(language).append('_').append(country)
        } else {
          sb.append(language)
        }
      }
    }
    sb.append(".properties")
    baseFileHandle.sibling(sb.toString())
  }
}
