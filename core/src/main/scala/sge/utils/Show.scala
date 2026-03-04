/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   TODO: replace with FastShowPretty from kindlings; derive for case classes
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

trait Show[A] {

  extension (a: A) {
    def show: String
  }
}
