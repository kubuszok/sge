// SGE Native Ops — Scala.js image decoder stub
//
// Browser image decoding is inherently async (HTMLImageElement, createImageBitmap),
// which doesn't fit the synchronous Gdx2DPixmap API. Image loading on JS should
// go through the browser's texture loading path instead.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: stub for JS — browser texture loading bypasses Gdx2DPixmap
//   Idiom: split packages
//   Audited: 2026-03-10

package sge
package platform

private[platform] object Gdx2dOpsJs extends Gdx2dOps {

  override def decodeImage(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] =
    None

  override def failureReason: String = "Image decoding not supported on Scala.js — use browser APIs"
}
