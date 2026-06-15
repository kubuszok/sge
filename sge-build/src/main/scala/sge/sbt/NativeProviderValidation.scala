package sge.sbt

/** Pure, dependency-free native-provider validation logic.
  *
  * Kept completely free of sbt (and any JSON-library) imports so the validation
  * decision can be exercised in isolation without an sbt build — the sbt task in
  * [[SgeNativeLibs]] is only the thin glue that reads the resolved provider JAR
  * and feeds this object.
  *
  * The contract this enforces ("manifest-strict, per binary", ISS-484): for every
  * native binary a provider's `pnm-provider.json` promises for a required desktop
  * platform, that binary must actually exist in the provider JAR and be a real
  * shared library rather than an undersized non-functional binary. The native libraries are
  * delivered to users via the resolved `pnm-provider-sge-*` dependency (declared
  * in the published POM), so validating that dependency — not files extracted into
  * the sge JAR — is what guarantees users actually receive working natives.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 86
 * Covenant-baseline-methods: NativeProviderValidation,PromiseRx,binaryProblems,classifiersInManifest,emptyManifest,missingPlatforms,promised,promisedBinaries,violations
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-15
  */
object NativeProviderValidation {

  /** Matches every `"<platform-classifier>": { "binary": "<file>" ... }` entry in a
    * `pnm-provider.json`. Anchoring on the `{ "binary"` object distinguishes the
    * per-platform binary promises from the scalar keys (`provider-name`,
    * `config-name`, `provider-schema-version`), which map to plain strings. The
    * match is tolerant of extra keys after `"binary"` (it does not require the
    * object to close immediately) so the schema can grow without breaking parsing.
    */
  private val PromiseRx =
    """"([a-z0-9][a-z0-9_-]*)"\s*:\s*\{\s*"binary"\s*:\s*"([^"]+)"""".r

  /** Every `(platformClassifier, binaryFileName)` the manifest promises, in
    * declaration order (duplicates across configs preserved — each is its own
    * promise to honour).
    */
  def promisedBinaries(manifestJson: String): Seq[(String, String)] =
    PromiseRx.findAllMatchIn(manifestJson).map(m => m.group(1) -> m.group(2)).toList

  /** Manifest-strict, per-binary coverage check of a single resolved provider JAR.
    *
    * @param providerName        provider JAR name, used only in messages
    * @param manifestJson         the provider JAR's `pnm-provider.json` content
    *                             (empty string if the JAR had no manifest)
    * @param entrySizes           map of every `native/...` JAR entry path to its
    *                             uncompressed size in bytes
    * @param requiredClassifiers  platform classifiers a release must cover
    * @param minLibBytes          minimum size for a binary to count as a real
    *                             shared library rather than an undersized one
    * @return human-readable violations; empty == valid
    */
  def violations(
    providerName: String,
    manifestJson: String,
    entrySizes: Map[String, Long],
    requiredClassifiers: Set[String],
    minLibBytes: Long
  ): Seq[String] = {
    val promised              = promisedBinaries(manifestJson)
    val classifiersInManifest = promised.map(_._1).toSet

    val emptyManifest =
      if (promised.isEmpty)
        Seq(s"$providerName: pnm-provider.json declares no native binaries (manifest missing, empty, or unreadable)")
      else Seq.empty

    val missingPlatforms =
      requiredClassifiers.toList.sorted
        .filterNot(classifiersInManifest.contains)
        .map(c => s"$providerName: manifest declares no native binaries for required platform '$c'")

    val binaryProblems =
      promised
        .filter { case (classifier, _) => requiredClassifiers.contains(classifier) }
        .flatMap { case (classifier, binary) =>
          val path = s"native/$classifier/$binary"
          entrySizes.get(path) match {
            case None =>
              Some(s"$providerName: manifest promises '$path' but it is absent from the provider JAR")
            case Some(sz) if sz < minLibBytes =>
              Some(s"$providerName: '$path' is only $sz B (< $minLibBytes B minimum) — too small to be a real shared library")
            case Some(_) =>
              None
          }
        }

    emptyManifest ++ missingPlatforms ++ binaryProblems
  }
}
