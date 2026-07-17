/*
 * SGE Gauntlet — JVM Tiled fixture builders (mirrors TiledMapFileLoadingSuite, ISS-796).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.maps.tiled.{ TiledMap, TiledMapTileLayer }

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files => JFiles, Path }
import java.util.zip.{ DeflaterOutputStream, GZIPOutputStream }

import scala.collection.mutable.ListBuffer

/** Builds real .tmx/.tmj/.tsx/.tsj fixtures plus a generated PNG tileset on disk, exactly the way `sge.maps.tiled.TiledMapFileLoadingSuite` does — the gauntlet cannot depend on test sources, so the
  * small builders are replicated here. JVM-only: `java.util.zip` gzip/deflate and the `javax.imageio` PNG encode path do not exist on the JS/Native linkers, which is why the Tiled load probes live in
  * `scalajvm` alongside the net probes.
  */
object TiledFixtures {

  /** Row-major 4x4 gids; each of tileset gids 1..4 appears exactly four times, so every cell is filled. */
  val Gids: Array[Int] = Array(1, 2, 3, 4, 4, 3, 2, 1, 1, 1, 2, 2, 3, 3, 4, 4)

  /** The sorted multiset every populated layer must decode to (invariant under the loader's row Y-flip). */
  val ExpectedSorted: List[Int] = Gids.toList.sorted

  /** Little-endian 4-byte-per-gid packing, exactly what the base64 layer decoders read back. */
  private def pack(ids: Array[Int]): Array[Byte] = {
    val out = new Array[Byte](ids.length * 4)
    var i   = 0
    while (i < ids.length) {
      val g = ids(i)
      out(i * 4) = (g & 0xff).toByte
      out(i * 4 + 1) = ((g >> 8) & 0xff).toByte
      out(i * 4 + 2) = ((g >> 16) & 0xff).toByte
      out(i * 4 + 3) = ((g >> 24) & 0xff).toByte
      i += 1
    }
    out
  }

  def gzipBase64(ids: Array[Int]): String = {
    val bos = new ByteArrayOutputStream()
    val gz  = new GZIPOutputStream(bos)
    gz.write(pack(ids))
    gz.close()
    java.util.Base64.getEncoder.encodeToString(bos.toByteArray)
  }

  def zlibBase64(ids: Array[Int]): String = {
    val bos = new ByteArrayOutputStream()
    val df  = new DeflaterOutputStream(bos)
    df.write(pack(ids))
    df.close()
    java.util.Base64.getEncoder.encodeToString(bos.toByteArray)
  }

  /** 32x32 RGBA PNG (2x2 grid of 16px tiles) written to `dir/tiles.png` via ImageIO. */
  def writeTilesetPng(dir: Path): Unit = {
    val img = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    var y   = 0
    while (y < 32) {
      var x = 0
      while (x < 32) {
        img.setRGB(x, y, 0xff000000 | ((x * 8) << 16) | ((y * 8) << 8))
        x += 1
      }
      y += 1
    }
    val out = dir.resolve("tiles.png").toFile
    val _   = javax.imageio.ImageIO.write(img, "png", out)
  }

  def write(dir: Path, name: String, content: String): Path = {
    val p = dir.resolve(name)
    JFiles.write(p, content.getBytes(StandardCharsets.UTF_8))
    p
  }

  /** Collects the sorted gids of every populated cell — invariant under the loader's Y-flip, which only reorders rows. */
  def layerGids(layer: TiledMapTileLayer): List[Int] = {
    val buf = ListBuffer.empty[Int]
    var y   = 0
    while (y < layer.height) {
      var x = 0
      while (x < layer.width) {
        layer.getCell(x, y).foreach(cell => cell.tile.foreach(t => buf += t.id))
        x += 1
      }
      y += 1
    }
    buf.toList.sorted
  }

  /** Resolves a named layer as a [[TiledMapTileLayer]], or empty if missing / wrong kind. */
  def tileLayer(map: TiledMap, name: String): Option[TiledMapTileLayer] =
    map.layers.get(name).fold(Option.empty[TiledMapTileLayer]) {
      case l: TiledMapTileLayer => Some(l)
      case _ => None
    }
}
