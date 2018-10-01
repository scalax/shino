package net.scalax.shino.sortby

import net.scalax.shino.umr.SortByErrorContent
import slick.lifted.{Ordered => SOrdered}

case class Direction(allowedAsc: Boolean, allowedDesc: Boolean, allowedNothing: Boolean) {
  self =>

  def disableAsc: Direction     = self.copy(allowedAsc = false)
  def disableDesc: Direction    = self.copy(allowedDesc = false)
  def disableNothing: Direction = self.copy(allowedNothing = false)

  def enableAsc: Direction     = self.copy(allowedAsc = true)
  def enableDesc: Direction    = self.copy(allowedDesc = true)
  def enableNothing: Direction = self.copy(allowedNothing = true)

  def allNulls: NullsOrdering =
    NullsOrdering(direction = self, allowedFirst = true, allowedLast = true, allowedDefault = true, allowedNothing = true)

  def onlyNullsFirst: NullsOrdering =
    NullsOrdering(direction = self, allowedFirst = true, allowedLast = false, allowedDefault = false, allowedNothing = false)

  def onlyNullsLast: NullsOrdering =
    NullsOrdering(direction = self, allowedFirst = false, allowedLast = true, allowedDefault = false, allowedNothing = false)

  def onlyNullsDefault: NullsOrdering =
    NullsOrdering(direction = self, allowedFirst = false, allowedLast = false, allowedDefault = true, allowedNothing = false)

  def onlyNullsNothing: NullsOrdering =
    NullsOrdering(direction = self, allowedFirst = false, allowedLast = false, allowedDefault = false, allowedNothing = true)

  def inputDirection(name: String): Option[SOrdered => SOrdered] = {
    name match {
      case SortBy.ASC =>
        if (allowedAsc)
          Option({ s: SOrdered =>
            new SOrdered(s.columns.map(s => (s._1, s._2.asc)))
          })
        else
          Option.empty
      case SortBy.DESC =>
        if (allowedDesc)
          Option({ s: SOrdered =>
            new SOrdered(s.columns.map(s => (s._1, s._2.desc)))
          })
        else
          Option.empty
      case SortBy.NOTHING =>
        if (allowedNothing)
          Option(identity[SOrdered] _)
        else
          Option.empty
      case _ => Option.empty
    }
  }

  override def toString: String = s"""allowedAsc=${allowedAsc}, allowedDesc=${allowedDesc}, allowedNothing=${allowedNothing}"""

}

case class NullsOrdering(direction: Direction, allowedFirst: Boolean, allowedLast: Boolean, allowedDefault: Boolean, allowedNothing: Boolean) {
  self =>

  def disableFirst: NullsOrdering   = self.copy(allowedFirst = false)
  def disableLast: NullsOrdering    = self.copy(allowedLast = false)
  def disableDefault: NullsOrdering = self.copy(allowedDefault = false)
  def disableNothing: NullsOrdering = self.copy(allowedNothing = false)

  def enableFirst: NullsOrdering   = self.copy(allowedFirst = true)
  def enableLast: NullsOrdering    = self.copy(allowedLast = true)
  def enableDefault: NullsOrdering = self.copy(allowedDefault = true)
  def enableNothing: NullsOrdering = self.copy(allowedNothing = true)

  def inputDirection(name: String): Option[SOrdered => SOrdered] = direction.inputDirection(name)
  def inputNullsOrdering(name: String): Option[SOrdered => SOrdered] = {
    name match {
      case SortBy.NULLS_FIRST =>
        if (allowedFirst)
          Option({ s: SOrdered =>
            new SOrdered(s.columns.map(s => (s._1, s._2.nullsFirst)))
          })
        else
          Option.empty
      case SortBy.NULLS_LAST =>
        if (allowedLast)
          Option({ s: SOrdered =>
            new SOrdered(s.columns.map(s => (s._1, s._2.nullsLast)))
          })
        else
          Option.empty
      case SortBy.NULLS_DEFAULT =>
        if (allowedDefault)
          Option({ s: SOrdered =>
            new SOrdered(s.columns.map(s => (s._1, s._2.nullsDefault)))
          })
        else
          Option.empty
      case SortBy.NOTHING =>
        if (allowedNothing)
          Option(identity[SOrdered] _)
        else
          Option.empty

      case _ => Option.empty
    }
  }

  def tagged[T]: OrderingWrap[T] = new OrderingWrap[T] {
    override val content = self
  }

  override def toString: String =
    s"direction: ${direction.toString}\n" +
      s"""nulls: allowedFirst=${allowedFirst}, allowedLast=${allowedLast}, allowedDefault=${allowedDefault}, allowedNothing=${allowedNothing}"""

}

trait OrderingWrap[R] {
  val content: NullsOrdering
}

trait SortBy {

  val all: Direction         = Direction(allowedAsc = true, allowedDesc = true, allowedNothing = true)
  val default: NullsOrdering = all.allNulls

  val onlyDesc: Direction    = Direction(allowedAsc = false, allowedDesc = true, allowedNothing = false)
  val onlyAsc: Direction     = Direction(allowedAsc = true, allowedDesc = false, allowedNothing = false)
  val onlyNothing: Direction = Direction(allowedAsc = false, allowedDesc = false, allowedNothing = true)

  val ASC     = "asc"
  val DESC    = "desc"
  val NOTHING = "nothing"

  val NULLS_FIRST   = "nullsFirst"
  val NULLS_LAST    = "nullsLast"
  val NULLS_DEFAULT = "nullsDefault"

  val emptySortBy = new SOrdered(IndexedSeq.empty)

  def mutiplySort(d: Option[SOrdered]*): Option[SOrdered] = {
    d.foldLeft(Option(new SOrdered(IndexedSeq.empty))) { (orderOpt, item) =>
      for {
        order <- orderOpt
        i     <- item
      } yield new SOrdered(order.columns ++ i.columns)
    }
  }

  def strictMutiplySort(s: Either[SortByErrorContent, SOrdered]*): Either[SortByErrorContent, SOrdered] = {
    s.foldLeft(Right(new SOrdered(IndexedSeq.empty)): Either[SortByErrorContent, SOrdered]) { (orderEi, item) =>
      for {
        order <- orderEi.right
        i     <- item.right
      } yield new SOrdered(order.columns ++ i.columns)
    }
  }

}

object SortBy extends SortBy
