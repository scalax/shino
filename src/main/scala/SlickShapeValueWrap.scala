package net.scalax.shino

import net.scalax.asuna.core.decoder.{DecoderShape, SplitData}
import net.scalax.asuna.mapper.decoder.{DecoderContent, DecoderHelper, DecoderWrapperHelper}
import slick.lifted.{FlatShapeLevel, MappedProjection, Shape, ShapedValue}

import scala.reflect.ClassTag

trait SlickShapeValueWrap[E] {
  self =>

  type Rep
  type Data
  type Level <: FlatShapeLevel
  val rep: Rep
  val shape: Shape[Level, Rep, Data, Rep]
  def convert(data: Data): E

  def map[R](cv: E => R): SlickShapeValueWrap[R] = {
    new SlickShapeValueWrap[R] {
      override type Rep   = self.Rep
      override type Data  = self.Data
      override type Level = self.Level
      override val rep   = self.rep
      override val shape = self.shape
      override def convert(data: self.Data): R = {
        cv(self.convert(data))
      }
    }

  }

  def zip[R](other: SlickShapeValueWrap[R]): SlickShapeValueWrap[(E, R)] = {
    new SlickShapeValueWrap[(E, R)] {
      override type Rep   = (self.Rep, other.Rep)
      override type Data  = (self.Data, other.Data)
      override type Level = FlatShapeLevel
      override val rep   = (self.rep, other.rep)
      override val shape = Shape.tuple2Shape[FlatShapeLevel, self.Rep, other.Rep, self.Data, other.Data, self.Rep, other.Rep](self.shape, other.shape)
      override def convert(data: Data): (E, R) = {
        (self.convert(data._1), other.convert(data._2))
      }
    }

  }

}

trait SlickMapper {

  trait UmrWrapper[RepOut, DataType] extends DecoderContent[RepOut, DataType] {
    def shape(implicit classTag: ClassTag[DataType]): MappedProjection[DataType, Any]
  }

  protected val unitInstance = new SlickShapeValueWrap[(Any, Any)] {
    override type Rep   = (Unit, Unit)
    override type Data  = (Unit, Unit)
    override type Level = FlatShapeLevel
    override val rep   = ((), ())
    override val shape = Shape.tuple2Shape[FlatShapeLevel, Unit, Unit, Unit, Unit, Unit, Unit](Shape.unitShape[FlatShapeLevel], Shape.unitShape[FlatShapeLevel])
    override def convert(data: Data): (Any, Any) = {
      (data._1: Any, data._2: Any)
    }
  }

  object umr
      extends DecoderHelper[SlickShapeValueWrap[(Any, Any)], (Any, Any)]
      with DecoderWrapperHelper[SlickShapeValueWrap[(Any, Any)], (Any, Any), UmrWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: DecoderShape.Aux[Rep, D, Out, SlickShapeValueWrap[(Any, Any)], (Any, Any)]): UmrWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , unitInstance
      )
      val s = reps.map { (t: (Any, Any)) =>
        shape1.takeData(wrapCol, t).current
      }
      new UmrWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          ShapedValue(s.rep, s.shape)
            .<>[D](f = { r =>
              s.convert(r)
            }, g = { (r: D) =>
              Option.empty
            })
            .asInstanceOf[MappedProjection[D, Any]]
        }
      }
    }
  }

  implicit def repImplicit[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
  ): DecoderShape.Aux[R, D, SlickShapeValueWrap[D], SlickShapeValueWrap[(Any, Any)], (Any, Any)] = {
    new DecoderShape[R, SlickShapeValueWrap[(Any, Any)], (Any, Any)] {
      override type Target = SlickShapeValueWrap[D]
      override type Data   = D
      override def wrapRep(base: R): SlickShapeValueWrap[D] = {
        val shape1 = shape
        new SlickShapeValueWrap[D] {
          override type Data  = D
          override type Rep   = T
          override type Level = L
          override val shape               = shape1.packedShape
          override val rep                 = shape1.pack(base)
          override def convert(data: D): D = data
        }
      }
      override def toLawRep(base: SlickShapeValueWrap[D], oldRep: SlickShapeValueWrap[(Any, Any)]): SlickShapeValueWrap[(Any, Any)] =
        base.zip(oldRep).map { case (d, t2) => (d: Any, t2: Any) }
      override def takeData(oldData: SlickShapeValueWrap[D], rep: (Any, Any)): SplitData[D, (Any, Any)] =
        SplitData(current = rep._1.asInstanceOf[D], left = rep._2.asInstanceOf[(Any, Any)])
    }
  }

  implicit def slickShapeValueWrapImplicit[D]
    : DecoderShape.Aux[SlickShapeValueWrap[D], D, SlickShapeValueWrap[D], SlickShapeValueWrap[(Any, Any)], (Any, Any)] = {
    new DecoderShape[SlickShapeValueWrap[D], SlickShapeValueWrap[(Any, Any)], (Any, Any)] {
      override type Target = SlickShapeValueWrap[D]
      override type Data   = D
      override def wrapRep(base: SlickShapeValueWrap[D]): SlickShapeValueWrap[D] = base
      override def toLawRep(base: SlickShapeValueWrap[D], oldRep: SlickShapeValueWrap[(Any, Any)]): SlickShapeValueWrap[(Any, Any)] =
        base.zip(oldRep).map { case (d, t2) => (d: Any, t2: Any) }
      override def takeData(oldData: SlickShapeValueWrap[D], rep: (Any, Any)): SplitData[D, (Any, Any)] =
        SplitData(current = rep._1.asInstanceOf[D], left = rep._2.asInstanceOf[(Any, Any)])
    }
  }

}
