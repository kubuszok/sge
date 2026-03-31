/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package building
package utilities

/** Provides static utilities for nullable objects to avoid NullPointerExceptions.
  * @author
  *   MJ
  */
object Nullables {
  def isNull(nullable: AnyRef): Boolean    = nullable == null   // @nowarn -- Java interop boundary
  def isNotNull(nullable: AnyRef): Boolean = nullable != null   // @nowarn -- Java interop boundary

  def getOrElse[A](nullable: A, alternative: A): A = if (nullable == null) alternative else nullable // @nowarn -- Java interop boundary

  def executeIfNotNull(nullable: AnyRef, command: Runnable): Unit = {
    if (nullable != null) command.run() // @nowarn -- Java interop boundary
  }

  def areEqual(first: AnyRef, second: AnyRef): Boolean = (first eq second) || (first != null && first.equals(second)) // @nowarn -- Java interop boundary

  def isAnyNull(nullables: AnyRef*): Boolean    = nullables.exists(_ == null)   // @nowarn -- Java interop boundary
  def areAllNull(nullables: AnyRef*): Boolean   = nullables.forall(_ == null)   // @nowarn -- Java interop boundary
  def isAnyNotNull(nullables: AnyRef*): Boolean = nullables.exists(_ != null)   // @nowarn -- Java interop boundary
  def areAllNotNull(nullables: AnyRef*): Boolean = nullables.forall(_ != null)  // @nowarn -- Java interop boundary
}
