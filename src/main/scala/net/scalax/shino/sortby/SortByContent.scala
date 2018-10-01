package net.scalax.shino.umr

import slick.lifted.{Ordered => SOrdered}

trait SortByContent {
  self =>

  val key: String
  val orderPro: SOrdered

  def reverse: SortByContent = new SortByContent {
    override val key: String = self.key
    override val orderPro    = new SOrdered(self.orderPro.columns.map(s => (s._1, s._2.reverse)))
  }

  def asc: SortByContent = new SortByContent {
    override val key: String = self.key
    override val orderPro    = new SOrdered(self.orderPro.columns.map(s => (s._1, s._2.asc)))
  }

  def desc: SortByContent = new SortByContent {
    override val key: String = self.key
    override val orderPro    = new SOrdered(self.orderPro.columns.map(s => (s._1, s._2.desc)))
  }

  def nullsDefault: SortByContent = new SortByContent {
    override val key: String = self.key
    override val orderPro    = new SOrdered(self.orderPro.columns.map(s => (s._1, s._2.nullsDefault)))
  }

  def nullsFirst: SortByContent = new SortByContent {
    override val key: String = self.key
    override val orderPro    = new SOrdered(self.orderPro.columns.map(s => (s._1, s._2.nullsFirst)))
  }

  def nullsLast: SortByContent = new SortByContent {
    override val key: String = self.key
    override val orderPro    = new SOrdered(self.orderPro.columns.map(s => (s._1, s._2.nullsLast)))
  }

}

object SortByContent {
  def apply(columnName: String, ordered: SOrdered): SortByContent = new SortByContent {
    override val key      = columnName
    override val orderPro = ordered
  }
}
