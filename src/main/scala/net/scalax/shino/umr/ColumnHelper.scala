package net.scalax.shino.umr

import net.scalax.asuna.core.common.Placeholder
import net.scalax.asuna.core.decoder.SplitData
import net.scalax.asuna.core.formatter.FormatterShape
import net.scalax.asuna.mapper.common.RepColumnContent
import slick.ast.TypedType
import slick.lifted.{FlatShapeLevel, Rep, Shape}

trait ColumnHelper {

  def columnGenerator[D](name: String, typedType: TypedType[D]): Rep[D]

  implicit def shinoColumnHelperImplicit[T <: FlatShapeLevel, D](
      implicit typedType: TypedType[D]
    , shape: Shape[T, Rep[D], D, Rep[D]]
  ): FormatterShape.Aux[RepColumnContent[Placeholder[D], D], D, SlickShapeValueWrapImpl[D], List[SlickShapeValueWrap], IndexedSeq[Any], List[Any]] = {
    new FormatterShape[RepColumnContent[Placeholder[D], D], List[SlickShapeValueWrap], IndexedSeq[Any], List[Any]] {
      override type Target = SlickShapeValueWrapImpl[D]
      override type Data   = D
      override def wrapRep(base: => RepColumnContent[Placeholder[D], D]): SlickShapeValueWrapImpl[D] = {
        val shape1 = shape
        type Rep1[R] = slick.lifted.Rep[R]

        new SlickShapeValueWrapImpl[D] {
          override type Rep   = Rep1[D]
          override type Level = T
          override val shape = shape1
          override val rep   = columnGenerator(base.columnInfo.modelColumnName, typedType)
        }
      }
      override def toLawRep(base: SlickShapeValueWrapImpl[D], oldRep: List[SlickShapeValueWrap]): List[SlickShapeValueWrap] =
        base :: oldRep
      override def takeData(oldData: SlickShapeValueWrapImpl[D], rep: List[Any]): SplitData[D, List[Any]] =
        SplitData(current = rep.head.asInstanceOf[D], left = rep.tail)
      override def buildData(data: D, rep: SlickShapeValueWrapImpl[D], oldData: IndexedSeq[Any]): IndexedSeq[Any] = data +: oldData
    }
  }

}
