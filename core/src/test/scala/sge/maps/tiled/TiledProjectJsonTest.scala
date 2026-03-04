package sge
package maps
package tiled

import com.github.plokhotnyuk.jsoniter_scala.core.{ readFromString, writeToString }

class TiledProjectJsonTest extends munit.FunSuite {

  test("round-trip: parse then serialize then parse again") {
    val jsonStr =
      """{
        |  "propertyTypes": [{
        |    "name": "Enemy",
        |    "type": "class",
        |    "members": [
        |      {"name": "health", "type": "int", "value": 100},
        |      {"name": "speed", "type": "float", "value": 1.5},
        |      {"name": "hostile", "type": "bool", "value": true},
        |      {"name": "sprite", "type": "string", "value": "goblin.png"}
        |    ]
        |  }]
        |}""".stripMargin

    val first        = readFromString[TiledProjectJson](jsonStr)
    val reserialized = writeToString(first)
    val second       = readFromString[TiledProjectJson](reserialized)
    assertEquals(second, first)
    assertEquals(second.propertyTypes.head.name, "Enemy")
    assertEquals(second.propertyTypes.head.members.size, 4)
  }

  test("parse: project file with class definitions") {
    val json =
      """{
        |  "propertyTypes": [
        |    {
        |      "name": "PlayerStats",
        |      "type": "class",
        |      "members": [
        |        {"name": "health", "type": "int", "value": 100},
        |        {"name": "speed", "type": "float", "value": 3.5},
        |        {"name": "name", "type": "string", "value": "Player"},
        |        {"name": "invincible", "type": "bool", "value": false}
        |      ]
        |    },
        |    {
        |      "name": "LootTable",
        |      "type": "class",
        |      "members": [
        |        {"name": "dropChance", "type": "float", "value": 0.5},
        |        {"name": "itemType", "type": "string", "propertyType": "ItemCategory"}
        |      ]
        |    }
        |  ]
        |}""".stripMargin

    val proj = readFromString[TiledProjectJson](json)
    assertEquals(proj.propertyTypes.size, 2)

    val stats = proj.propertyTypes(0)
    assertEquals(stats.name, "PlayerStats")
    assertEquals(stats.tpe, "class")
    assertEquals(stats.members.size, 4)
    assertEquals(stats.members(0).name, "health")
    assertEquals(stats.members(0).tpe, "int")
    assertEquals(stats.members(1).name, "speed")
    assertEquals(stats.members(1).tpe, "float")

    val loot = proj.propertyTypes(1)
    assertEquals(loot.members(1).propertyType, Some("ItemCategory"))
  }

  test("parse: enum property types are preserved (type field)") {
    // The loader currently only uses class types, but enum types should parse without error
    val json =
      """{
        |  "propertyTypes": [
        |    {
        |      "name": "Direction",
        |      "type": "enum"
        |    },
        |    {
        |      "name": "Weapon",
        |      "type": "class",
        |      "members": [
        |        {"name": "damage", "type": "int", "value": 10}
        |      ]
        |    }
        |  ]
        |}""".stripMargin

    val proj = readFromString[TiledProjectJson](json)
    assertEquals(proj.propertyTypes.size, 2)
    assertEquals(proj.propertyTypes(0).name, "Direction")
    assertEquals(proj.propertyTypes(0).tpe, "enum")
    assertEquals(proj.propertyTypes(0).members, Nil)
    assertEquals(proj.propertyTypes(1).tpe, "class")
  }

  test("parse: unknown top-level fields are silently ignored") {
    val json =
      """{
        |  "automappingRulesFile": "",
        |  "commands": [],
        |  "compatibilityVersion": 1100,
        |  "extensionsPath": "extensions",
        |  "folders": ["maps"],
        |  "propertyTypes": [
        |    {"name": "Foo", "type": "class", "members": []}
        |  ]
        |}""".stripMargin

    val proj = readFromString[TiledProjectJson](json)
    assertEquals(proj.propertyTypes.size, 1)
    assertEquals(proj.propertyTypes.head.name, "Foo")
  }

  test("parse: unknown property type fields are silently ignored") {
    // Real .tiled-project files have id, color, drawFill, useAs, etc.
    val json =
      """{
        |  "propertyTypes": [{
        |    "id": 42,
        |    "name": "NPC",
        |    "type": "class",
        |    "color": "#ff00ff00",
        |    "drawFill": true,
        |    "useAs": ["property", "map"],
        |    "members": [
        |      {"name": "dialog", "type": "string", "value": "hello"}
        |    ]
        |  }]
        |}""".stripMargin

    val proj = readFromString[TiledProjectJson](json)
    assertEquals(proj.propertyTypes.head.name, "NPC")
    assertEquals(proj.propertyTypes.head.members.size, 1)
  }

  test("parse: member with nested object value") {
    val json =
      """{
        |  "propertyTypes": [{
        |    "name": "Config",
        |    "type": "class",
        |    "members": [{
        |      "name": "defaults",
        |      "type": "class",
        |      "propertyType": "PlayerStats",
        |      "value": {"health": 100, "speed": 3.5}
        |    }]
        |  }]
        |}""".stripMargin

    val member = readFromString[TiledProjectJson](json).propertyTypes.head.members.head
    assertEquals(member.name, "defaults")
    assertEquals(member.tpe, "class")
    assertEquals(member.propertyType, Some("PlayerStats"))
    assert(member.value.isDefined)
  }

  test("parse: empty project file") {
    val json = """{}"""
    val proj = readFromString[TiledProjectJson](json)
    assertEquals(proj.propertyTypes, Nil)
  }

  test("parse: member with no value field") {
    val json =
      """{
        |  "propertyTypes": [{
        |    "name": "Simple",
        |    "type": "class",
        |    "members": [
        |      {"name": "label", "type": "string"}
        |    ]
        |  }]
        |}""".stripMargin

    val member = readFromString[TiledProjectJson](json).propertyTypes.head.members.head
    assertEquals(member.name, "label")
    assertEquals(member.value, None)
    assertEquals(member.propertyType, None)
  }
}
