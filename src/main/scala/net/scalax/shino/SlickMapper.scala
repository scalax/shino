package net.scalax.shino

import net.scalax.asuna.core.decoder.SplitData
import net.scalax.asuna.core.formatter.FormatterShape
import net.scalax.asuna.mapper.formatter.{FormatterContent, FormatterWrapperHelper}
import slick.lifted.{FlatShapeLevel, MappedProjection, Shape, ShapedValue}

import scala.reflect.ClassTag

trait SlickMapper {

  trait UmrWrapper[RepOut, DataType] extends FormatterContent[RepOut, DataType] {
    def shape(implicit classTag: ClassTag[DataType]): MappedProjection[DataType, Any]
  }

  private val unitInstance = new SlickShapeValueWrap[(Any, Any)] {
    override type Rep   = (Unit, Unit)
    override type Data  = (Unit, Unit)
    override type Level = FlatShapeLevel
    override val rep   = ((), ())
    override val shape = Shape.tuple2Shape[FlatShapeLevel, Unit, Unit, Unit, Unit, Unit, Unit](Shape.unitShape[FlatShapeLevel], Shape.unitShape[FlatShapeLevel])
    override def convert(data: Data): (Any, Any) = {
      (data._1: Any, data._2: Any)
    }
    override def cusBuildData(data: (Any, Any)): Option[(Unit, Unit)] = Option(((), ()))
  }

  object shino extends FormatterWrapperHelper[SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any), UmrWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: FormatterShape.Aux[Rep, D, Out, SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)]): UmrWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , unitInstance
      )
      val s = reps.map { (t: (Any, Any)) =>
        shape1.takeData(wrapCol, t).current
      } { r =>
        Option(shape1.buildData(r, wrapCol, null))
      }
      new UmrWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          ShapedValue(s.rep, s.shape)
            .<>[D](f = { r =>
              s.convert(r)
            }, g = { (r: D) =>
              s.cusBuildData(r)
            })
            .asInstanceOf[MappedProjection[D, Any]]
        }
      }
    }
  }

  implicit def shinoWrapperRepImplicit[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
  ): FormatterShape.Aux[R, D, SlickShapeValueWrap[D], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] = {
    new FormatterShape[R, SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] {
      override type Target = SlickShapeValueWrap[D]
      override type Data   = D
      override def wrapRep(base: R): SlickShapeValueWrap[D] = {
        val shape1 = shape
        new SlickShapeValueWrap[D] {
          override type Data  = D
          override type Rep   = T
          override type Level = L
          override val shape                            = shape1.packedShape
          override val rep                              = shape1.pack(base)
          override def convert(data: D): D              = data
          override def cusBuildData(data: D): Option[D] = Option(data)
        }
      }
      override def toLawRep(base: SlickShapeValueWrap[D], oldRep: SlickShapeValueWrap[(Any, Any)]): SlickShapeValueWrap[(Any, Any)] =
        base.zip(oldRep).asInstanceOf[SlickShapeValueWrap[(Any, Any)]]
      override def takeData(oldData: SlickShapeValueWrap[D], rep: (Any, Any)): SplitData[D, (Any, Any)] =
        SplitData(current = rep._1.asInstanceOf[D], left = rep._2.asInstanceOf[(Any, Any)])
      override def buildData(data: D, rep: SlickShapeValueWrap[D], oldData: (Any, Any)): (Any, Any) = (data, oldData)
    }
  }

  implicit def shinoSlickShapeValueWrapImplicit[D]
    : FormatterShape.Aux[SlickShapeValueWrap[D], D, SlickShapeValueWrap[D], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] = {
    new FormatterShape[SlickShapeValueWrap[D], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] {
      override type Target = SlickShapeValueWrap[D]
      override type Data   = D
      override def wrapRep(base: SlickShapeValueWrap[D]): SlickShapeValueWrap[D] = base
      override def toLawRep(base: SlickShapeValueWrap[D], oldRep: SlickShapeValueWrap[(Any, Any)]): SlickShapeValueWrap[(Any, Any)] =
        base.zip(oldRep).asInstanceOf[SlickShapeValueWrap[(Any, Any)]]
      override def takeData(oldData: SlickShapeValueWrap[D], rep: (Any, Any)): SplitData[D, (Any, Any)] =
        SplitData(current = rep._1.asInstanceOf[D], left = rep._2.asInstanceOf[(Any, Any)])
      override def buildData(data: D, rep: SlickShapeValueWrap[D], oldData: (Any, Any)): (Any, Any) = (data, oldData)
    }
  }

}
