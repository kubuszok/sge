package sge.port.ext.simplegraphs

import balticporter.core.PortManifest
import balticporter.runner.ClasspathCache

import java.nio.file.Path

/** simple-graphs' policy (`src/main/java`, 29 types, a dependency-free graph library). A BASE port: it imports nothing of libGDX's and resolves against no other module. */
object SimpleGraphsPolicy {

  private val Graphs = balticporter.tir.RuleScope.Only(Set("space.earlygrey.simplegraphs"))

  def core(sgeRoot: Path): PortManifest =
    PortManifest(
      name = "sge-graphs",
      governs = Set("space.earlygrey.simplegraphs"),
      packageRenames = Map("space.earlygrey.simplegraphs" -> "sge.graphs"),
      // sge keeps the library's internal structures apart, under sge's names
      subPackages = Map(
        "space.earlygrey.simplegraphs.BinaryHeap" -> "internal",
        "space.earlygrey.simplegraphs.NodeMap" -> "internal"
      ),
      typeRenames = Map("space.earlygrey.simplegraphs.Array" -> "space.earlygrey.simplegraphs.internal.InternalArray"),
      // the three move out of the package that reaches their package-private members; sge makes the split
      allowPackageSplit = Set(
        "space.earlygrey.simplegraphs.Array",
        "space.earlygrey.simplegraphs.BinaryHeap",
        "space.earlygrey.simplegraphs.NodeMap"
      ),
      surface = List(
        new balticporter.transform.CollectionsTransform(scope = Graphs),
        new balticporter.transform.MutableParamsTransform,
        new balticporter.transform.BeanPropertyTransform(scope = Graphs, derive = true),
        new balticporter.transform.NullaryArityTransform(scope = Graphs, derive = true),
        new balticporter.transform.NullabilityTransform(
          target = balticporter.transform.NullabilityTransform.Target.Named("lowlevel.Nullable"),
          scope = Graphs,
          deriveMembers = true
        )
      ),
      // javac's choices where the candidate set spans a resolution phase, each examined in the corpus port
      resolutions = Map(
        "space.earlygrey.simplegraphs.Graph#addVertices(V[])" -> "acknowledge",
        "space.earlygrey.simplegraphs.Array#remove(Object)" -> "ascribe-javac-choice",
        "space.earlygrey.simplegraphs.Node#removeEdge(Node)" -> "accept-risk"
      ),
      // derived from the hand port this generation replaced, while it existed
      frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/graphs-derived-policy.tsv"))
    )

  /** simple-graphs' own JUnit suite, converted to MUnit, as a dependent of [[core]] whose port map is under `reportRoots`. */
  def test(sgeRoot: Path, reportRoots: List[Path]): PortManifest =
    core(sgeRoot)
      .copy(baseReports = reportRoots)
      .extendedBy(
        PortManifest(
          name = "sge-graphs-test",
          surface = List(
            new balticporter.transform.TestFrameworkTransform(),
            // LAST, deliberately: reads what the main port actually emitted
            balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-graphs")
          )
        )
      )

  /** the test-scope classpath the frontend resolves the suite against: JUnit 4.12, simple-graphs' own `build.gradle` pin. */
  def testClasspath(sgeRoot: Path): List[Path] =
    ClasspathCache.entries(
      sge.port.ClasspathCacheDir.file(sgeRoot, "simplegraphs-test-classpath.txt"),
      "simple-graphs-test",
      List("junit:junit:4.12")
    )
}
