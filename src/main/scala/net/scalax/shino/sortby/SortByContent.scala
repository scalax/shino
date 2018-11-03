package net.scalax.shino.umr

import net.scalax.asuna.mapper.common.{MacroColumnInfo, MacroColumnInfoImpl, RepColumnContent}
import net.scalax.shino.sortby.NullsOrdering
import slick.lifted.{Ordered => SOrdered}

trait SortByContent extends RepColumnContent[SOrdered, NullsOrdering] {
  self =>

  val key: String

  override def rep: SOrdered

  override lazy val columnInfo: MacroColumnInfo = MacroColumnInfoImpl(tableColumnSymbol = Symbol(key), modelColumnSymbols = List(Symbol(key)))

  def reverse: SortByContent = new SortByContent {
    override val key      = self.key
    override lazy val rep = new SOrdered(self.rep.columns.map(s => (s._1, s._2.reverse)))
  }

  def asc: SortByContent = new SortByContent {
    override val key      = self.key
    override lazy val rep = new SOrdered(self.rep.columns.map(s => (s._1, s._2.asc)))
  }

  def desc: SortByContent = new SortByContent {
    override val key      = self.key
    override lazy val rep = new SOrdered(self.rep.columns.map(s => (s._1, s._2.desc)))
  }

  def nullsDefault: SortByContent = new SortByContent {
    override val key      = self.key
    override lazy val rep = new SOrdered(self.rep.columns.map(s => (s._1, s._2.nullsDefault)))
  }

  def nullsFirst: SortByContent = new SortByContent {
    override val key      = self.key
    override lazy val rep = new SOrdered(self.rep.columns.map(s => (s._1, s._2.nullsFirst)))
  }

  def nullsLast: SortByContent = new SortByContent {
    override val key      = self.key
    override lazy val rep = new SOrdered(self.rep.columns.map(s => (s._1, s._2.nullsLast)))
  }

}

object SortByContent {
  def apply(columnName: String, ordered: SOrdered): SortByContent = new SortByContent {
    override val key      = columnName
    override lazy val rep = ordered
  }
}
