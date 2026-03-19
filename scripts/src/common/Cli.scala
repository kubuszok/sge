package sgedev

/** Hand-rolled command-line argument parser. */
object Cli {

  /** Parsed command-line arguments. */
  final case class Args(
    flags: Map[String, String],
    positional: List[String]
  ) {
    def flag(name: String): Option[String] = flags.get(name)
    def hasFlag(name: String): Boolean = flags.contains(name)
    def flagOrDefault(name: String, default: String): String = flags.getOrElse(name, default)
    def requirePositional(index: Int, name: String): String = {
      if (index >= positional.length) {
        Term.err(s"Missing required argument: <$name>")
        sys.exit(1)
      }
      positional(index)
    }
  }

  /** Parse arguments into flags and positionals.
    * Supports: --flag value, --flag=value, --boolean-flag, positional args.
    * Stops parsing flags after --.
    */
  def parse(args: List[String]): Args = {
    val flags = scala.collection.mutable.Map.empty[String, String]
    val positional = scala.collection.mutable.ListBuffer.empty[String]
    var remaining = args
    var pastDashes = false

    while (remaining.nonEmpty) {
      remaining match {
        case "--" :: rest =>
          pastDashes = true
          remaining = rest
        case arg :: rest if !pastDashes && arg.startsWith("--") =>
          val keyVal = arg.drop(2)
          val eqIdx = keyVal.indexOf('=')
          if (eqIdx >= 0) {
            flags(keyVal.substring(0, eqIdx)) = keyVal.substring(eqIdx + 1)
            remaining = rest
          } else if (rest.nonEmpty && !rest.head.startsWith("--")) {
            flags(keyVal) = rest.head
            remaining = rest.tail
          } else {
            flags(keyVal) = "true"
            remaining = rest
          }
        case arg :: rest =>
          positional += arg
          remaining = rest
        case Nil =>
          remaining = Nil
      }
    }
    Args(flags.toMap, positional.toList)
  }
}
