package sge
package utils
package compression

trait ICodeProgress {
  def SetProgress (inSize: Long, outSize: Long): Unit
}
