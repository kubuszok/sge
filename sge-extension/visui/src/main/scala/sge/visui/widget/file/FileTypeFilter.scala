/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package file

import sge.files.FileHandle
import sge.utils.DynamicArray

/** FileTypeFilter is used to limit [[FileChooser]] selection only to specified set of extensions. User can use file chooser's select box to select that extension or all types (if it is
  * allowed).
  *
  * This class is not replacement for [[FileChooser.fileFilter_=]]. While the main file chooser filter does general filtering (such as removing hidden or inaccessible files), FileTypeFilter is
  * used to limit extensions of files than user can select.
  *
  * This filter works by adding rules. Each rule has a description (showed in file chooser's filter select box) and a list of extensions that it accepts. During selection user can switch active
  * rule via select box. Additionally each FileTypeFilter can support 'all types allowed' where all files are accepted regardless of their extension.
  * @author
  *   Kotcrab
  * @since
  *   1.1.0
  */
class FileTypeFilter(private var _allTypesAllowed: Boolean) {
  private val _rules: DynamicArray[FileTypeFilter.Rule] = DynamicArray[FileTypeFilter.Rule]()

  def this(other: FileTypeFilter) = {
    this(other._allTypesAllowed)
    _rules.addAll(other._rules)
  }

  /** Adds new rule to [[FileTypeFilter]]
    * @param description
    *   rule description used in FileChooser's file type select box
    * @param extensions
    *   list of extensions without leading dot, eg. 'jpg', 'png' etc.
    */
  def addRule(description: String, extensions: String*): Unit = {
    _rules.add(new FileTypeFilter.Rule(description, extensions*))
  }

  def getRules: DynamicArray[FileTypeFilter.Rule] = _rules

  /** Controls whether to allow 'all types allowed' mode, where all file types are shown.
    * @param allTypesAllowed
    *   if true then user can choose "All types" in file chooser's filter select box where all files are shown
    */
  def allTypesAllowed_=(allTypesAllowed: Boolean): Unit = _allTypesAllowed = allTypesAllowed

  def allTypesAllowed: Boolean = _allTypesAllowed
}

object FileTypeFilter {

  /** Defines single rule for [[FileTypeFilter]]. Rule instances are immutable. */
  class Rule private (val description: String, val extensions: DynamicArray[String], val allowAll: Boolean) {

    /** Creates an allow-all rule with the given description. */
    def this(description: String) = {
      this(description, DynamicArray[String](), true)
      require(description != null, "description can't be null") // @nowarn -- Java interop boundary
    }

    /** Creates a rule with the given description and extension list. */
    def this(description: String, extensionList: String*) = {
      this(description, DynamicArray[String](), false)
      require(description != null, "description can't be null")             // @nowarn -- Java interop boundary
      require(extensionList != null && extensionList.nonEmpty, "extensionList can't be null nor empty") // @nowarn -- Java interop boundary
      extensionList.foreach { ext =>
        val cleaned = if (ext.startsWith(".")) ext.substring(1) else ext
        extensions.add(cleaned.toLowerCase)
      }
    }

    def accept(file: FileHandle): Boolean = {
      if (allowAll) true
      else {
        val ext = file.extension.toLowerCase
        extensions.contains(ext)
      }
    }

    def getDescription: String = description

    /** @return copy of extension list. */
    def getExtensions: DynamicArray[String] = {
      val copy = DynamicArray[String]()
      copy.addAll(extensions)
      copy
    }

    override def toString: String = description
  }
}
