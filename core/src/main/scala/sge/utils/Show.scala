package sge
package utils

trait Show[A] {

  extension (a: A) {
    def show: String
  }
}
