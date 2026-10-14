package sge.port.ext.gltf

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** gdx-gltf's policy (`gltf/src`, 135 types: the glTF/GLB loaders and exporters, the PBR shaders and the scene3d layer) — a DEPENDENT of sge-core: it extends libGDX's 3D API throughout, so libGDX's
  * policy is inherited from the core manifest and never restated. What it adds answers the three reflective sites upstream wrote as a GWT workaround, the way sge's hand port did.
  */
object GltfPolicy {

  private val Gltf = balticporter.tir.RuleScope.Only(Set("net.mgsx.gltf"))

  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-gltf",
        governs = Set("net.mgsx.gltf"),
        packageRenames = Map("net.mgsx.gltf" -> "sge.gltf"),
        surface = List(
          // two members reach a libGDX API through reflection only so GWT would not see the reference (upstream says so); the hand
          // port makes the direct call, WebGL guard kept
          new balticporter.transform.MethodBodyTransform(
            Map(
              // `new Pixmap(bytes, offset, len)` through `getConstructor(...).newInstance(...)`
              "net.mgsx.gltf.loaders.shared.texture.PixmapBinaryLoaderHack#load" ->
                """{
                  |    if (scala.Predef.summon[sge.Sge].application.applicationType eq sge.Application.ApplicationType.WebGL) {
                  |      throw new sge.gltf.loaders.exceptions.GLTFUnsupportedException("load pixmap from bytes not supported for WebGL")
                  |    } else {
                  |      new sge.graphics.Pixmap(encodedData, offset, len)
                  |    }
                  |  }""".stripMargin,
              // `PixmapIO.writePNG(file, pixmap)` through `forName` + `getMethod` + `invoke`
              "net.mgsx.gltf.exporters.GLTFBinaryExporter#savePNG" ->
                """{
                  |    if (scala.Predef.summon[sge.Sge].application.applicationType eq sge.Application.ApplicationType.WebGL) {
                  |      throw new sge.gltf.loaders.exceptions.GLTFUnsupportedException("saving pixmap not supported for WebGL")
                  |    } else {
                  |      sge.graphics.PixmapIO.writePNG(file, pixmap)
                  |    }
                  |  }""".stripMargin
            )
          ),
          // `GLTFMaterialExporter#ext` builds a material extension from its `Class`: a table of the seven extensions upstream defines,
          // and java's own `GdxRuntimeException` (sge's `InvalidInput`) for any other
          new balticporter.transform.RegistryTransform(
            List(
              balticporter.transform.RegistryTransform.Registry(
                callee = "com.badlogic.gdx.utils.reflect.ClassReflection#newInstance",
                placement = balticporter.transform.RegistryTransform.Placement.Object(
                  "net.mgsx.gltf.data.extensions.GLTFExtensionFactories",
                  balticporter.transform.RegistryTransform.Spelling("factories", "register", "create")
                ),
                scope = Gltf,
                seeds = List(
                  "net.mgsx.gltf.data.extensions.KHRMaterialsEmissiveStrength",
                  "net.mgsx.gltf.data.extensions.KHRMaterialsIOR",
                  "net.mgsx.gltf.data.extensions.KHRMaterialsIridescence",
                  "net.mgsx.gltf.data.extensions.KHRMaterialsSpecular",
                  "net.mgsx.gltf.data.extensions.KHRMaterialsTransmission",
                  "net.mgsx.gltf.data.extensions.KHRMaterialsUnlit",
                  "net.mgsx.gltf.data.extensions.KHRMaterialsVolume"
                ),
                handles = Set("com.badlogic.gdx.utils.reflect.ReflectionException"),
                miss = balticporter.transform.RegistryTransform.Miss.Throw("sge.utils.SgeError.InvalidInput", "no factory registered for glTF material extension ")
              )
            )
          ),
          new balticporter.transform.BeanPropertyTransform(scope = Gltf, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = Gltf, derive = true),
          new balticporter.transform.NullabilityTransform(scope = Gltf, deriveMembers = true),
          // LAST, deliberately: reads what the BASE actually emitted
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        parity = reference.map(r => ParityRef(roots = List(r))),
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/gltf-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
      )
    )
}
