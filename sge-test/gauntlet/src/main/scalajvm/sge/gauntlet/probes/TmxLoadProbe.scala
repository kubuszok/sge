/*
 * SGE Gauntlet — maps: real .tmx file loading end-to-end (ISS-796, mirrors ISS-767 suite).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.assets.loaders.FileHandleResolver
import sge.maps.tiled.TmxMapLoader

import java.nio.file.{ Files => JFiles }
import scala.collection.mutable.ListBuffer

/** Drives the REAL [[TmxMapLoader]] over a hand-written .tmx fixture: an external .tsx tileset (generated PNG image), a gzip+base64 layer, a CSV layer, and an object layer with a typed custom
  * property. Asserts the decoded map/layer/tile/property content — the file-loading path that had zero coverage before ISS-767. Non-GPU: Texture creation runs through NoopGraphics headlessly (the
  * desktop image decode + java.util.zip gunzip are JVM-only, so this probe lives in scalajvm alongside the net probes).
  */
object TmxLoadProbe extends FeatureProbe {

  override def id: String = "maps/tmx-load"

  override def area: String = "maps"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    given Sge = ctx.sgeCtx
    val dir   = JFiles.createTempDirectory("sge-gauntlet-tmx")
    TiledFixtures.writeTilesetPng(dir)

    TiledFixtures.write(
      dir,
      "tileset.tsx",
      """<?xml version="1.0" encoding="UTF-8"?>
        |<tileset version="1.10" name="tiles" tilewidth="16" tileheight="16" tilecount="4" columns="2">
        | <image source="tiles.png" width="32" height="32"/>
        |</tileset>""".stripMargin
    )

    val mapFile = TiledFixtures.write(
      dir,
      "map.tmx",
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<map version="1.10" orientation="orthogonal" renderorder="right-down" width="4" height="4" tilewidth="16" tileheight="16" infinite="0">
         | <properties>
         |  <property name="author" value="sge-gauntlet"/>
         | </properties>
         | <tileset firstgid="1" source="tileset.tsx"/>
         | <layer id="1" name="gzip_layer" width="4" height="4">
         |  <data encoding="base64" compression="gzip">${TiledFixtures.gzipBase64(TiledFixtures.Gids)}</data>
         | </layer>
         | <layer id="2" name="csv_layer" width="4" height="4">
         |  <data encoding="csv">
         |${TiledFixtures.Gids.mkString(",")}
         |</data>
         | </layer>
         | <objectgroup id="3" name="objects">
         |  <object id="1" name="spawn" type="marker" x="16" y="32" width="8" height="8">
         |   <properties>
         |    <property name="hp" type="int" value="100"/>
         |   </properties>
         |  </object>
         | </objectgroup>
         |</map>""".stripMargin
    )

    val loader = TmxMapLoader(FileHandleResolver.Absolute())
    val map    = loader.load(mapFile.toAbsolutePath.toString)
    try {
      // Map-level properties parsed from attributes and the <properties> block.
      checks += Check.eq("map-width", 4, map.properties.getAs[Integer]("width").fold(-1)(_.intValue()))
      checks += Check.eq("map-tilewidth", 16, map.properties.getAs[Integer]("tilewidth").fold(-1)(_.intValue()))
      checks += Check.eq("map-author-property", "sge-gauntlet", map.properties.getAs[String]("author").getOrElse("<missing>"))

      // External .tsx tileset parsed and sliced into 4 tiles (gids 1..4).
      checks += Check.cond("external-tileset-gid1", map.tileSets.getTile(1).isDefined, "gid 1 present", map.tileSets.getTile(1).isDefined.toString)
      checks += Check.cond("external-tileset-gid4", map.tileSets.getTile(4).isDefined, "gid 4 present", map.tileSets.getTile(4).isDefined.toString)

      // gzip+base64 and CSV layers both decode to the same gid multiset.
      val gzip = TiledFixtures.tileLayer(map, "gzip_layer").fold[List[Int]](Nil)(TiledFixtures.layerGids)
      checks += Check.eq("gzip-layer-gids", TiledFixtures.ExpectedSorted, gzip)
      val csv = TiledFixtures.tileLayer(map, "csv_layer").fold[List[Int]](Nil)(TiledFixtures.layerGids)
      checks += Check.eq("csv-layer-gids", TiledFixtures.ExpectedSorted, csv)

      // Object layer + typed custom property.
      val objects = map.layers.get("objects").fold(0)(_.objects.count)
      checks += Check.eq("object-count", 1, objects)
      val hp = map.layers.get("objects").fold(-1)(l => l.objects.get("spawn").fold(-1)(_.properties.getAs[Integer]("hp").fold(-1)(_.intValue())))
      checks += Check.eq("object-typed-property-hp", 100, hp)
    } finally map.close()
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
