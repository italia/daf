package daf.dataset.query.jdbc

import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import cats.instances.try_.catsStdInstancesForTry
import cats.free.Free
import cats.free.Free.catsFreeMonadForFree
import daf.dataset.query._
import doobie.util.fragment.Fragment

object WhereFragment {

  private def writeColumn(column: Column): Trampoline[String] = column match {
    case ValueColumn(value: String) => Free.pure { s"'$value'" }
    case ValueColumn(value)         => Free.pure { value.toString }
    case NamedColumn(name)          => Free.pure { name }
    case _                          => recursionError[String] { new IllegalArgumentException("Invalid operand encountered: columns or constants only are allowed") }
  }

  private def _writeComparison(left: Column, right: Column)(f: (String, String) => String): Trampoline[String] = for {
    l <- writeColumn(left)
    r <- writeColumn(right)
  } yield f(l, r)

  private def writeComparisonOp(op: ComparisonOperator): Trampoline[String] = op match {
    case Gt(left, right)  => _writeComparison(left, right) { (l, r) => s"$l > $r" }
    case Gte(left, right) => _writeComparison(left, right) { (l, r) => s"$l >= $r" }
    case Lt(left, right)  => _writeComparison(left, right) { (l, r) => s"$l < $r" }
    case Lte(left, right) => _writeComparison(left, right) { (l, r) => s"$l <= $r" }
    case Eq(left, right)  => _writeComparison(left, right) { (l, r) => s"$l = $r" }
    case Neq(left, right) => _writeComparison(left, right) { (l, r) => s"$l <> $r" }
  }

  private def writeLogicalOp(op: LogicalOperator): Trampoline[String] = op match {
    case And(inner) => inner.toList.traverse[Trampoline, String] { writeFilterOp }.map { _.mkString("(", " AND ", ")") }
    case Or(inner)  => inner.toList.traverse[Trampoline, String] { writeFilterOp }.map { _.mkString("(", " OR ", ")") }
    case Not(inner) => writeFilterOp(inner).map { s => s"NOT($s)" }
  }

  private def writeFilterOp(op: FilterOperator): Trampoline[String] = op match {
    case logical: LogicalOperator       => Free.defer { writeLogicalOp(logical) }
    case comparison: ComparisonOperator => Free.defer { writeComparisonOp(comparison) }
  }

  def writer(whereClause: WhereClause) = QueryFragmentWriter.ask {
    writeFilterOp { whereClause.filter }.runTailRec.map { s => Fragment.const(s"WHERE $s") }
  }

}
