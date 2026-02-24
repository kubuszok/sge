/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

trait Show[A] {

  extension (a: A) {
    def show: String
  }
}
