package sge
package graphics
package g3d
package loader

import sge.graphics.g3d.model.data.ModelData

// TODO: test: decode a real .g3dj file from libgdx/tests/ (e.g. cube.g3dj) end-to-end through G3dModelLoader
class G3dModelLoaderTest extends munit.FunSuite {

  /** Test subclass that exposes parseMeshes for direct testing. */
  private class TestableLoader(using Sge)
      extends G3dModelLoader(
        new sge.assets.loaders.FileHandleResolver {
          def resolve(fileName: String): sge.files.FileHandle = throw new UnsupportedOperationException
        }
      ) {
    def testParseMeshes(model: ModelData, meshes: List[G3dMeshJson]): Unit =
      parseMeshes(model, meshes)
  }

  private def makeLoader(): TestableLoader = {
    given Sge = SgeTestFixture.testSge()
    new TestableLoader()
  }

  test("parseMeshes reads mesh part id from part node, not parent mesh") {
    val loader = makeLoader()
    val model  = new ModelData()

    val meshes = List(
      G3dMeshJson(
        id = "meshId",
        attributes = List("POSITION"),
        vertices = List(0f, 0f, 0f),
        parts = List(
          G3dMeshPartJson(id = "partId", tpe = "TRIANGLES", indices = List[Short](0, 1, 2))
        )
      )
    )

    loader.testParseMeshes(model, meshes)

    assertEquals(model.meshes.size, 1)
    val parsedMesh = model.meshes(0)
    assertEquals(parsedMesh.id, "meshId")
    assertEquals(parsedMesh.parts.length, 1)
    // The critical assertion: mesh part id should come from the part node, not the mesh node
    assertEquals(parsedMesh.parts(0).id, "partId")
  }

  test("parseMeshes handles multiple parts with distinct ids") {
    val loader = makeLoader()
    val model  = new ModelData()

    val meshes = List(
      G3dMeshJson(
        id = "myMesh",
        attributes = List("POSITION"),
        vertices = List(1.0f, 1.0f, 1.0f),
        parts = (0 until 3).map { i =>
          G3dMeshPartJson(id = s"part$i", tpe = "TRIANGLES", indices = List[Short](0, 1, 2))
        }.toList
      )
    )

    loader.testParseMeshes(model, meshes)

    assertEquals(model.meshes.size, 1)
    val parsedMesh = model.meshes(0)
    assertEquals(parsedMesh.parts.length, 3)
    assertEquals(parsedMesh.parts(0).id, "part0")
    assertEquals(parsedMesh.parts(1).id, "part1")
    assertEquals(parsedMesh.parts(2).id, "part2")
  }

  test("parseMeshes rejects duplicate mesh part ids") {
    val loader = makeLoader()
    val model  = new ModelData()

    val meshes = List(
      G3dMeshJson(
        id = "dup",
        attributes = List("POSITION"),
        vertices = List(0f),
        parts = List(
          G3dMeshPartJson(id = "sameName", tpe = "TRIANGLES", indices = List[Short](0)),
          G3dMeshPartJson(id = "sameName", tpe = "TRIANGLES", indices = List[Short](0))
        )
      )
    )

    val ex = intercept[sge.utils.SgeError.InvalidInput] {
      loader.testParseMeshes(model, meshes)
    }
    assert(ex.message.contains("sameName"), s"expected message about 'sameName', got: ${ex.message}")
  }

  test("parseMeshes with empty list does nothing") {
    val loader = makeLoader()
    val model  = new ModelData()
    loader.testParseMeshes(model, Nil)
    assertEquals(model.meshes.size, 0)
  }

  test("codec round-trip: G3dModelJson serializes and deserializes") {
    import com.github.plokhotnyuk.jsoniter_scala.core.{ readFromString, writeToString }

    val original = G3dModelJson(
      version = List[Short](0, 1),
      id = "test-model",
      meshes = List(
        G3dMeshJson(
          id = "mesh1",
          attributes = List("POSITION", "NORMAL"),
          vertices = List(1f, 2f, 3f, 4f, 5f, 6f),
          parts = List(
            G3dMeshPartJson(id = "part1", tpe = "TRIANGLES", indices = List[Short](0, 1, 2))
          )
        )
      ),
      materials = List(
        G3dMaterialJson(id = "mat1", shininess = 0.5f, opacity = 0.9f)
      ),
      nodes = List(
        G3dNodeJson(id = "node1", translation = Some(List(1f, 2f, 3f)))
      )
    )

    val jsonStr  = writeToString(original)
    val restored = readFromString[G3dModelJson](jsonStr)

    assertEquals(restored.id, "test-model")
    assertEquals(restored.version, List[Short](0, 1))
    assertEquals(restored.meshes.size, 1)
    assertEquals(restored.meshes(0).id, "mesh1")
    assertEquals(restored.meshes(0).parts(0).tpe, "TRIANGLES")
    assertEquals(restored.materials.size, 1)
    assertEquals(restored.materials(0).shininess, 0.5f)
    assertEquals(restored.nodes.size, 1)
    assertEquals(restored.nodes(0).translation, Some(List(1f, 2f, 3f)))
  }
}
