/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package i18n

/** A simple interface for one text line of the bundle file.
  * @author
  *   MJ
  */
trait BundleText {

  /** @return name of the bundle text in the bundle file. */
  def name: String

  /** @return text's unformatted message as it appears in the bundle. */
  def get: String

  /** @return text's formatted message without any arguments. */
  def format(): String

  /** @return text's formatted message with the passed arguments filling bundle placeholders. */
  def format(arguments: AnyRef*): String
}
