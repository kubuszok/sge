package fix

import scalafix.v1._
import scala.meta._

/** Scalafix rule that reports `== null` and `!= null` comparisons.
  *
  * This is a diagnostic-only (linting) rule — it does NOT auto-fix because the correct
  * replacement depends on context (fold, foreach, getOrElse, isDefined, isEmpty, etc.).
  * The developer must choose the right Nullable method for each case.
  *
  * Usage: `sbt "core / scalafix NullToNullable"`
  *
  * To auto-suppress after manual review: `sbt "core / scalafix NullToNullable --auto-suppress-linter-errors"`
  */
class NullToNullable extends SyntacticRule("NullToNullable") {

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      // x == null
      case t @ Term.ApplyInfix.After_4_6_0(lhs, Term.Name("=="), _, Term.ArgClause(List(Lit.Null()), _)) =>
        Patch.lint(NullCheckDiagnostic(t, lhs.syntax, isEqNull = true))

      // x != null
      case t @ Term.ApplyInfix.After_4_6_0(lhs, Term.Name("!="), _, Term.ArgClause(List(Lit.Null()), _)) =>
        Patch.lint(NullCheckDiagnostic(t, lhs.syntax, isEqNull = false))

      // null == x
      case t @ Term.ApplyInfix.After_4_6_0(Lit.Null(), Term.Name("=="), _, Term.ArgClause(List(rhs), _)) =>
        Patch.lint(NullCheckDiagnostic(t, rhs.syntax, isEqNull = true))

      // null != x
      case t @ Term.ApplyInfix.After_4_6_0(Lit.Null(), Term.Name("!="), _, Term.ArgClause(List(rhs), _)) =>
        Patch.lint(NullCheckDiagnostic(t, rhs.syntax, isEqNull = false))
    }.asPatch
  }
}

final case class NullCheckDiagnostic(
    tree: Tree,
    varName: String,
    isEqNull: Boolean
) extends Diagnostic {

  override def position: Position = tree.pos

  override def message: String = {
    val check = if (isEqNull) "== null" else "!= null"
    val suggestions = if (isEqNull) {
      s"""|`$varName $check` — use Nullable instead:
          |  • Nullable($varName).isEmpty         (boolean check)
          |  • Nullable($varName).fold(onEmpty)(onPresent)  (branch)
          |  • Nullable($varName).getOrElse(default)        (default value)""".stripMargin
    } else {
      s"""|`$varName $check` — use Nullable instead:
          |  • Nullable($varName).isDefined        (boolean check)
          |  • Nullable($varName).foreach(a => ...) (non-null action)
          |  • Nullable($varName).fold(onEmpty)(onPresent)  (branch)""".stripMargin
    }
    suggestions
  }
}
