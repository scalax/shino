package net.scalax.shino.umr

import net.scalax.asuna.core.encoder.EncoderShape
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.asuna.mapper.encoder.{EncoderContent, EncoderWrapperHelper}
import net.scalax.shino.sortby.{NullsOrdering, SortBy}
import slick.lifted.{Ordered => SOrdered}

trait SortByParameter {
  val emptySortBy = new SOrdered(IndexedSeq.empty)
  def allowMap: Map[String, NullsOrdering]
  def singleSort(columnName: String, direction: String, nullsOrdering: String = SortBy.NOTHING): Option[SOrdered]
}

trait SortByWrapper[RepOut, DataType] extends EncoderContent[RepOut, DataType] {
  def inputData(data: DataType): SortByParameter
}

trait SortByMapper {

  object sortby extends EncoderWrapperHelper[Map[String, SOrdered], Map[String, (SOrdered, NullsOrdering)], SortByWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: EncoderShape.Aux[Rep, D, Out, Map[String, SOrdered], Map[String, (SOrdered, NullsOrdering)]]): SortByWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      new SortByWrapper[Out, D] {
        override def inputData(data: D): SortByParameter = {
          val sortByMap: Map[String, (SOrdered, NullsOrdering)] = shape1.buildData(data, wrapCol, Map.empty)
          new SortByParameter {
            override def singleSort(columnName: String, direction: String, nullsOrdering: String): Option[SOrdered] = {
              sortByMap.get(columnName).flatMap {
                case (ordered, nullsOrderingOpt) =>
                  val dOpt = nullsOrderingOpt.inputDirection(direction)
                  val nOpt = nullsOrderingOpt.inputNullsOrdering(nullsOrdering)
                  for {
                    d <- dOpt
                    n <- nOpt
                  } yield n(d(ordered))
              }
            }
            override def allowMap: Map[String, NullsOrdering] = sortByMap.map { case (key, value) => (key, value._2) }
          }
        }
      }
    }
  }

  implicit def sortbyColumnRepImplicit1[D](
      implicit orderMap: D => SOrdered
  ): EncoderShape.Aux[RepColumnContent[D, NullsOrdering], NullsOrdering, SortByContent, Map[String, SOrdered], Map[String, (SOrdered, NullsOrdering)]] = {
    new EncoderShape[RepColumnContent[D, NullsOrdering], Map[String, SOrdered], Map[String, (SOrdered, NullsOrdering)]] {
      override type Target = SortByContent
      override type Data   = NullsOrdering
      override def wrapRep(base: RepColumnContent[D, NullsOrdering]): SortByContent                    = SortByContent(base.columnInfo.modelColumnName, orderMap(base.rep))
      override def toLawRep(base: SortByContent, oldRep: Map[String, SOrdered]): Map[String, SOrdered] = oldRep + ((base.key, base.orderPro))
      override def buildData(
          data: NullsOrdering
        , rep: SortByContent
        , oldData: Map[String, (SOrdered, NullsOrdering)]
      ): Map[String, (SOrdered, NullsOrdering)] = {
        oldData + ((rep.key, (rep.orderPro, data)))
      }
    }
  }

  implicit def sortbyColumnRepImplicit2[D](
      implicit orderMap: D => SOrdered
  ): EncoderShape.Aux[SortByContent, NullsOrdering, SortByContent, Map[String, SOrdered], Map[String, (SOrdered, NullsOrdering)]] = {
    new EncoderShape[SortByContent, Map[String, SOrdered], Map[String, (SOrdered, NullsOrdering)]] {
      override type Target = SortByContent
      override type Data   = NullsOrdering
      override def wrapRep(base: SortByContent): SortByContent                                         = base
      override def toLawRep(base: SortByContent, oldRep: Map[String, SOrdered]): Map[String, SOrdered] = oldRep + ((base.key, base.orderPro))
      override def buildData(
          data: NullsOrdering
        , rep: SortByContent
        , oldData: Map[String, (SOrdered, NullsOrdering)]
      ): Map[String, (SOrdered, NullsOrdering)] = {
        oldData + ((rep.key, (rep.orderPro, data)))
      }
    }
  }

}
