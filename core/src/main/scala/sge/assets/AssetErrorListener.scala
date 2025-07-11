package sge
package assets

trait AssetErrorListener {
  def error(asset: AssetDescriptor[?], throwable: Throwable): Unit
}
