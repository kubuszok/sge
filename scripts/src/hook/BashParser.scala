package sgedev
package hook

/** Recursive descent bash command parser.
  *
  * Parses the subset of bash that Claude actually produces:
  * BashExpr = Pipeline (("&&" | "||" | ";") Pipeline)*
  * Pipeline = Command ("|" Command)*
  * Command  = SimpleCommand | Subshell
  * SimpleCommand = word+ Redirect*
  * Subshell = "$(" BashExpr ")"
  */
object BashParser {

  // AST
  enum BashExpr {
    case Simple(program: String, args: List[String], redirects: List[Redirect])
    case Pipe(left: BashExpr, right: BashExpr)
    case Chain(left: BashExpr, op: String, right: BashExpr)
    case Sub(inner: BashExpr)
    case Empty
  }

  final case class Redirect(op: String, target: String)

  // Tokens
  enum Token {
    case Word(value: String)
    case Pipe
    case And       // &&
    case Or        // ||
    case Semi      // ;
    case LParen    // (
    case RParen    // )
    case DollarParen  // $(
    case RedirectOp(op: String)  // >, >>, 2>, 2>&1, <
    case Eof
  }

  // Tokenizer
  final class Tokenizer(input: String) {
    private var pos: Int = 0

    def peek(): Token = {
      val saved = pos
      val tok = next()
      pos = saved
      tok
    }

    def next(): Token = {
      skipWhitespace()
      if (pos >= input.length) return Token.Eof

      input(pos) match {
        case ';' =>
          pos += 1
          Token.Semi
        case '|' =>
          pos += 1
          if (pos < input.length && input(pos) == '|') {
            pos += 1
            Token.Or
          } else {
            Token.Pipe
          }
        case '&' =>
          pos += 1
          if (pos < input.length && input(pos) == '&') {
            pos += 1
            Token.And
          } else {
            // Background & — treat as semi
            Token.Semi
          }
        case '(' =>
          pos += 1
          Token.LParen
        case ')' =>
          pos += 1
          Token.RParen
        case '$' if pos + 1 < input.length && input(pos + 1) == '(' =>
          pos += 2
          Token.DollarParen
        case '>' =>
          pos += 1
          if (pos < input.length && input(pos) == '>') {
            pos += 1
            Token.RedirectOp(">>")
          } else {
            Token.RedirectOp(">")
          }
        case '<' =>
          if (input.startsWith("<<", pos)) {
            // Heredoc — consume until delimiter
            pos += 2
            val delim = readHeredocDelimiter()
            val content = readHeredocBody(delim)
            Token.Word(content)
          } else {
            pos += 1
            Token.RedirectOp("<")
          }
        case '2' if pos + 1 < input.length && input(pos + 1) == '>' =>
          pos += 2
          if (pos < input.length && input(pos) == '&' && pos + 1 < input.length && input(pos + 1) == '1') {
            pos += 2
            Token.RedirectOp("2>&1")
          } else if (pos < input.length && input(pos) == '>') {
            pos += 1
            Token.RedirectOp("2>>")
          } else {
            Token.RedirectOp("2>")
          }
        case _ =>
          Token.Word(readWord())
      }
    }

    private def skipWhitespace(): Unit = {
      while (pos < input.length && (input(pos) == ' ' || input(pos) == '\t')) {
        pos += 1
      }
      // Skip line continuation
      if (pos + 1 < input.length && input(pos) == '\\' && input(pos + 1) == '\n') {
        pos += 2
        skipWhitespace()
      }
    }

    private def readWord(): String = {
      val sb = new StringBuilder
      while (pos < input.length) {
        input(pos) match {
          case ' ' | '\t' | '\n' | ';' | '|' | '(' | ')' => return sb.toString
          case '&' => return sb.toString
          case '>' if sb.isEmpty || !sb.last.isDigit => return sb.toString
          case '>' if sb.nonEmpty && sb.last == '2' =>
            // Part of 2> redirect — but we already consumed '2', put it back
            return sb.toString
          case '<' => return sb.toString
          case '\'' =>
            pos += 1
            while (pos < input.length && input(pos) != '\'') {
              sb.append(input(pos))
              pos += 1
            }
            if (pos < input.length) pos += 1 // skip closing '
          case '"' =>
            pos += 1
            while (pos < input.length && input(pos) != '"') {
              if (input(pos) == '\\' && pos + 1 < input.length) {
                pos += 1
                sb.append(input(pos))
              } else {
                sb.append(input(pos))
              }
              pos += 1
            }
            if (pos < input.length) pos += 1 // skip closing "
          case '\\' if pos + 1 < input.length =>
            pos += 1
            sb.append(input(pos))
            pos += 1
          case '$' if pos + 1 < input.length && input(pos + 1) == '(' =>
            // Subshell in word — consume matching parens
            sb.append("$(")
            pos += 2
            var depth = 1
            while (pos < input.length && depth > 0) {
              if (input(pos) == '(') depth += 1
              else if (input(pos) == ')') depth -= 1
              if (depth > 0) sb.append(input(pos))
              pos += 1
            }
            sb.append(')')
          case c =>
            sb.append(c)
            pos += 1
        }
      }
      sb.toString
    }

    private def readHeredocDelimiter(): String = {
      skipWhitespace()
      val sb = new StringBuilder
      // Strip quotes from delimiter
      var quoting = false
      while (pos < input.length && input(pos) != '\n') {
        input(pos) match {
          case '\'' | '"' =>
            quoting = !quoting
            pos += 1
          case c =>
            sb.append(c)
            pos += 1
        }
      }
      if (pos < input.length) pos += 1 // skip newline
      sb.toString.trim
    }

    private def readHeredocBody(delim: String): String = {
      val sb = new StringBuilder
      while (pos < input.length) {
        val lineStart = pos
        // Read to end of line
        while (pos < input.length && input(pos) != '\n') pos += 1
        val line = input.substring(lineStart, pos).trim
        if (pos < input.length) pos += 1 // skip newline
        if (line == delim) return sb.toString
        sb.append(line)
        sb.append('\n')
      }
      sb.toString
    }
  }

  // Parser
  def parse(input: String): BashExpr = {
    if (input.trim.isEmpty) return BashExpr.Empty
    val tokenizer = new Tokenizer(input)
    val result = parseExpr(tokenizer)
    result
  }

  private def parseExpr(t: Tokenizer): BashExpr = {
    var left = parsePipeline(t)
    var continue = true
    while (continue) {
      t.peek() match {
        case Token.And =>
          t.next()
          val right = parsePipeline(t)
          left = BashExpr.Chain(left, "&&", right)
        case Token.Or =>
          t.next()
          val right = parsePipeline(t)
          left = BashExpr.Chain(left, "||", right)
        case Token.Semi =>
          t.next()
          if (t.peek() != Token.Eof && t.peek() != Token.RParen) {
            val right = parsePipeline(t)
            left = BashExpr.Chain(left, ";", right)
          }
        case _ =>
          continue = false
      }
    }
    left
  }

  private def parsePipeline(t: Tokenizer): BashExpr = {
    var left = parseCommand(t)
    while (t.peek() == Token.Pipe) {
      t.next()
      val right = parseCommand(t)
      left = BashExpr.Pipe(left, right)
    }
    left
  }

  private def parseCommand(t: Tokenizer): BashExpr = {
    t.peek() match {
      case Token.DollarParen =>
        t.next()
        val inner = parseExpr(t)
        t.peek() match {
          case Token.RParen => t.next()
          case _ => // missing closing paren, be lenient
        }
        BashExpr.Sub(inner)
      case Token.LParen =>
        t.next()
        val inner = parseExpr(t)
        t.peek() match {
          case Token.RParen => t.next()
          case _ => // missing closing paren
        }
        BashExpr.Sub(inner)
      case _ =>
        parseSimple(t)
    }
  }

  private def parseSimple(t: Tokenizer): BashExpr = {
    val words = scala.collection.mutable.ListBuffer.empty[String]
    val redirects = scala.collection.mutable.ListBuffer.empty[Redirect]

    var continue = true
    while (continue) {
      t.peek() match {
        case Token.Word(w) =>
          t.next()
          words += w
        case Token.RedirectOp(op) =>
          t.next()
          t.peek() match {
            case Token.Word(target) =>
              t.next()
              redirects += Redirect(op, target)
            case _ =>
              redirects += Redirect(op, "")
          }
        case _ =>
          continue = false
      }
    }

    if (words.isEmpty) {
      BashExpr.Empty
    } else {
      BashExpr.Simple(words.head, words.tail.toList, redirects.toList)
    }
  }

  /** Extract all Simple commands from an AST. */
  def allCommands(expr: BashExpr): List[BashExpr.Simple] = {
    expr match {
      case s: BashExpr.Simple => List(s)
      case BashExpr.Pipe(l, r) => allCommands(l) ++ allCommands(r)
      case BashExpr.Chain(l, _, r) => allCommands(l) ++ allCommands(r)
      case BashExpr.Sub(inner) => allCommands(inner)
      case BashExpr.Empty => Nil
    }
  }
}
