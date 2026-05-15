/*
 * SGE-original file, no LibGDX counterpart.
 * Bridge utilities for creating DynamicArray instances with reference-type
 * elements when no concrete MkArray is available at compile time.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import lowlevel.MkArray
import lowlevel.util.DynamicArray

extension (da: DynamicArray.type) {

  inline def createRef[A](capacity: Int = 16, preserveOrder: Boolean = true): DynamicArray[A] = {
    @annotation.nowarn("msg=unused local definition") // consumed by summonInline at inline expansion sites
    given MkArray[A] = MkArray.anyRef[AnyRef].asInstanceOf[MkArray[A]]
    DynamicArray[A](preserveOrder, capacity)
  }
}
