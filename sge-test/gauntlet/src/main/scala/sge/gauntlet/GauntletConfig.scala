/*
 * SGE Gauntlet — CLI configuration.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** Parsed CLI configuration for a gauntlet run.
  *
  * @param headless
  *   run under HeadlessApplication and skip `requiresGpu` probes (reported as `skipped_gpu`)
  * @param interactive
  *   windowed run that stays open after the probes finish, with a navigable results grid
  * @param onlyPrefix
  *   when set, only probes whose id starts with this prefix run
  * @param areaFilter
  *   when set, only probes of this area run
  * @param reportDir
  *   absolute path of the report directory (report.json, report.md, screenshots/, tmp/)
  */
final case class GauntletConfig(
  headless:    Boolean = false,
  interactive: Boolean = false,
  onlyPrefix:  Option[String] = None,
  areaFilter:  Option[String] = None,
  reportDir:   String = "target/gauntlet"
) {

  /** Applies `--only` / `--area` filtering to a probe list. */
  def select(probes: List[FeatureProbe]): List[FeatureProbe] =
    probes
      .filter(p => onlyPrefix.forall(p.id.startsWith))
      .filter(p => areaFilter.forall(_ == p.area))
}

object GauntletConfig {

  val usage: String =
    """Usage: gauntlet [--all] [--only <id-prefix>] [--area <area>] [--headless] [--interactive] [--report <dir>]
      |  --all           run every probe (default)
      |  --only <p>      run only probes whose id starts with <p>
      |  --area <a>      run only probes of area <a> (g2d, g3d, files, net, audio, assets, utils, scene2d, ext/...)
      |  --headless      no window, no GL: requiresGpu probes are reported as skipped_gpu
      |  --interactive   windowed; stays open with a navigable results grid after the run
      |  --report <dir>  report output directory (default target/gauntlet)""".stripMargin

  /** Parses CLI args; Left(message) on unknown/incomplete arguments. */
  def parse(args: Array[String]): Either[String, GauntletConfig] = {
    def loop(rest: List[String], acc: GauntletConfig): Either[String, GauntletConfig] =
      rest match {
        case Nil                      => Right(acc)
        case "--all" :: tail          => loop(tail, acc.copy(onlyPrefix = None, areaFilter = None))
        case "--only" :: p :: tail    => loop(tail, acc.copy(onlyPrefix = Some(p)))
        case "--area" :: a :: tail    => loop(tail, acc.copy(areaFilter = Some(a)))
        case "--headless" :: tail     => loop(tail, acc.copy(headless = true))
        case "--interactive" :: tail  => loop(tail, acc.copy(interactive = true))
        case "--report" :: d :: tail  => loop(tail, acc.copy(reportDir = d))
        case ("--only" | "--area" | "--report") :: Nil => Left(s"missing value for ${rest.head}\n$usage")
        case other :: _               => Left(s"unknown argument: $other\n$usage")
      }
    loop(args.toList, GauntletConfig()).flatMap { cfg =>
      if (cfg.headless && cfg.interactive) Left("--headless and --interactive are mutually exclusive")
      else Right(cfg)
    }
  }
}
