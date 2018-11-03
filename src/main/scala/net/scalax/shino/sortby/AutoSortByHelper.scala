package net.scalax.shino.umr

import net.scalax.asuna.core.common.Placeholder
import net.scalax.asuna.core.encoder.EncoderShape
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.shino.sortby.{NullsOrdering, OrderingWrap}
import slick.ast.TypedType
import slick.lifted.{Rep, Ordered => SOrdered}

trait AutoSortByHelper {
  self =>

  def sortByColumnGenerator[D](name: String, typedType: TypedType[D]): Rep[D]

  implicit def sortByColumnImplicit1[D](
      implicit typedType: TypedType[D]
    , cv1: Rep[D] => SOrdered
  ): EncoderShape.Aux[RepColumnContent[Placeholder[OrderingWrap[D]], OrderingWrap[D]], OrderingWrap[D], SortByContent, Map[String, SOrdered], Map[
      String
    , (SOrdered, NullsOrdering)
  ]] = {
    new EncoderShape[RepColumnContent[Placeholder[OrderingWrap[D]], OrderingWrap[D]], Map[String, SOrdered], Map[String, (SOrdered, NullsOrdering)]] {
      override type Target = SortByContent
      override type Data   = OrderingWrap[D]

      override def wrapRep(base: => RepColumnContent[Placeholder[OrderingWrap[D]], OrderingWrap[D]]): SortByContent =
        SortByContent(base.columnInfo.tableColumnSymbol.name, cv1(sortByColumnGenerator(base.columnInfo.tableColumnSymbol.name, typedType)))

      override def buildRep(base: SortByContent, oldRep: Map[String, SOrdered]): Map[String, SOrdered] = oldRep + ((base.key, base.rep))

      override def buildData(
          data: OrderingWrap[D]
        , rep: SortByContent
        , oldData: Map[String, (SOrdered, NullsOrdering)]
      ): Map[String, (SOrdered, NullsOrdering)] = oldData + ((rep.key, (rep.rep, data.content)))

    }
  }

}
