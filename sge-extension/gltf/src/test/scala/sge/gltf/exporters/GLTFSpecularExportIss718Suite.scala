/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-718 — GLTFMaterialExporter drops the
 * TextureAttribute.Specular -> KHR_materials_specular.specularColorTexture
 * export branch, causing a round-trip data loss for materials that carry a
 * specular COLOR texture.
 *
 * Original semantics (original-src/gdx-gltf/gltf/src/net/mgsx/gltf/exporters/
 * GLTFMaterialExporter.java:145-154):
 *
 *     // Specular
 *     else if(a.type == PBRFloatAttribute.SpecularFactor){
 *         extSpecular(m).specularFactor = ((PBRFloatAttribute)a).value;
 *     }
 *     else if(a.type == PBRHDRColorAttribute.Specular){
 *         PBRHDRColorAttribute v = (PBRHDRColorAttribute)a;
 *         extSpecular(m).specularColorFactor = new float[]{v.r, v.g, v.b};
 *     }
 *     else if(a.type == PBRTextureAttribute.SpecularFactorTexture){
 *         extSpecular(m).specularTexture = texture((PBRTextureAttribute)a);
 *     }
 *     else if(a.type == PBRTextureAttribute.Specular){            // <-- line 152
 *         extSpecular(m).specularColorTexture = texture((TextureAttribute)a); // 153
 *     }                                                            //     line 154
 *
 * `PBRTextureAttribute.Specular` does not exist on PBRTextureAttribute; it
 * resolves to the INHERITED libGDX base constant TextureAttribute.Specular
 * (registered from alias "specularTexture"). The port
 * (GLTFMaterialExporter.scala:126-127) instead matches on
 * PBRTextureAttribute.SpecularColorTexture (a DISTINCT registered attribute
 * type). Since Attribute.register assigns a distinct bit per distinct alias
 * string, TextureAttribute.Specular != PBRTextureAttribute.SpecularColorTexture,
 * so a material carrying TextureAttribute.Specular never enters the branch and
 * its specular color texture is silently dropped from the export.
 *
 * The port's own loader still PRODUCES TextureAttribute.Specular:
 * PBRMaterialLoader.scala:122
 *     ext.specularGlossinessTexture.foreach { sgt =>
 *       material.set(getTexureMap(TextureAttribute.Specular, sgt))
 *     }
 * so this is a genuine load -> export round-trip data loss.
 *
 * This suite runs the material export headlessly: the exported GLTFMaterial
 * MUST carry a KHR_materials_specular extension whose specularColorTexture is
 * set. A TextureAttribute built with the single-arg constructor has an empty
 * TextureDescriptor (no live Texture), so getTexture() never touches the GL /
 * binary manager — no GL context, no filesystem.
 *
 * This test is written by the reproducer agent and MUST NOT be modified by the
 * fixer: it encodes the original Java semantics, not the port's.
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer

import sge.graphics.g3d.Material
import sge.graphics.g3d.attributes.TextureAttribute
import sge.graphics.g3d.model.{ Node, NodePart }
import sge.gltf.data.GLTF
import sge.gltf.data.extensions.KHRMaterialsSpecular
import sge.gltf.data.material.GLTFMaterial
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

final class GLTFSpecularExportIss718Suite extends munit.FunSuite {

  import GLTFSpecularExportIss718Suite.*

  private given Sge = Sge(NoopApplicationStub, new NoopGraphics(), new NoopAudio(), NoopFilesStub, new NoopInput(), NoopNetStub)

  /** Runs GLTFMaterialExporter.exportMaterials on a single material and returns the exported GLTFMaterial. */
  private def exportSingle(material: Material): GLTFMaterial = {
    val part = new NodePart()
    part.material = material
    val node = new Node()
    node.parts.add(part)
    val nodes = DynamicArray[Node]()
    nodes.add(node)

    val base = new GLTFExporter()
    base.root = new GLTF()
    new GLTFMaterialExporter(base).exportMaterials(nodes)

    val materials: Nullable[ArrayBuffer[GLTFMaterial]] = base.root.materials
    assert(materials.isDefined && materials.get.nonEmpty, "exporter must emit a glTF material")
    materials.get.head
  }

  test(
    "ISS-718: a TextureAttribute.Specular color texture is exported as KHR_materials_specular.specularColorTexture (GLTFMaterialExporter.java:152-154)"
  ) {
    // The material carries a specular COLOR texture, exactly as PBRMaterialLoader.scala:122 produces it.
    val material = new Material("iss718-specular", new TextureAttribute(TextureAttribute.Specular))

    val exported = exportSingle(material)

    val specular: Nullable[KHRMaterialsSpecular] =
      exported.extensions.flatMap(_.get(classOf[KHRMaterialsSpecular], KHRMaterialsSpecular.EXT))

    assert(
      specular.isDefined,
      "exported material must carry a KHR_materials_specular extension for a TextureAttribute.Specular (GLTFMaterialExporter.java:152-154 sets extSpecular(m).specularColorTexture)"
    )
    assert(
      specular.get.specularColorTexture.isDefined,
      "KHR_materials_specular.specularColorTexture must reference the exported specular color texture (GLTFMaterialExporter.java:153: extSpecular(m).specularColorTexture = texture((TextureAttribute)a))"
    )
  }
}

object GLTFSpecularExportIss718Suite {

  // Minimal headless Sge fixture — mirrors GltfBinaryExportPngRedSuiteBase; the
  // material-export path used here touches neither graphics, audio, input, files
  // nor net (the empty TextureDescriptor short-circuits getTexture's binary
  // export), so every accessor is an unused stub.
  private object NoopApplicationStub extends Application {
    def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                                             Graphics                    = throw new UnsupportedOperationException
    def audio:                                                Audio                       = throw new UnsupportedOperationException
    def input:                                                Input                       = throw new UnsupportedOperationException
    def files:                                                Files                       = throw new UnsupportedOperationException
    def net:                                                  Net                         = throw new UnsupportedOperationException
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = throw new UnsupportedOperationException
    def clipboard:                                            sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object NoopFilesStub extends Files {
    def getFileHandle(path: String, fileType: sge.files.FileType): sge.files.FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                                       String               = ""
    def isExternalStorageAvailable:                                Boolean              = false
    def localStoragePath:                                          String               = ""
    def isLocalStorageAvailable:                                   Boolean              = false
  }

  private object NoopNetStub extends Net {
    import Net.*
    def httpClient:                                                                                         sge.net.SgeHttpClient = sge.net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       sge.net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean               = false
  }
}
