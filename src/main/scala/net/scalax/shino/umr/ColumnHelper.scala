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
  ): FormatterShape.Aux[RepColumnContent[Placeholder[D], D], D, SlickShapeValueWrap[D], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] = {
    new FormatterShape[RepColumnContent[Placeholder[D], D], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] {
      override type Target = SlickShapeValueWrap[D]
      override type Data   = D
      override def wrapRep(base: RepColumnContent[Placeholder[D], D]): SlickShapeValueWrap[D] = {
        val shape1 = shape
        type Rep1[R] = slick.lifted.Rep[R]

        new SlickShapeValueWrap[D] {
          override type Rep   = Rep1[D]
          override type Level = T
          override val shape = shape1
          override val rep   = columnGenerator(base.columnInfo.modelColumnName, typedType)
        }
      }
      override def toLawRep(base: SlickShapeValueWrap[D], oldRep: SlickShapeValueWrap[(Any, Any)]): SlickShapeValueWrap[(Any, Any)] =
        base.zip(oldRep).asInstanceOf[SlickShapeValueWrap[(Any, Any)]]
      override def takeData(oldData: SlickShapeValueWrap[D], rep: (Any, Any)): SplitData[D, (Any, Any)] =
        SplitData(current = rep._1.asInstanceOf[D], left = rep._2.asInstanceOf[(Any, Any)])
      override def buildData(data: D, rep: SlickShapeValueWrap[D], oldData: (Any, Any)): (Any, Any) = (data, oldData)
    }
  }

}
