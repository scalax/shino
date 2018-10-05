package net.scalax.shino.umr

import net.scalax.asuna.core.decoder.{DecoderShape, SplitData}
import net.scalax.asuna.core.encoder.{EncoderShape, EncoderShapeValue}
import net.scalax.asuna.core.formatter.FormatterShape
import net.scalax.asuna.mapper.decoder.{DecoderContent, DecoderWrapperHelper}
import net.scalax.asuna.mapper.encoder.{EncoderContent, EncoderWrapperHelper}
import net.scalax.asuna.mapper.formatter.{FormatterContent, FormatterWrapperHelper}
import slick.lifted.{FlatShapeLevel, MappedProjection, Shape, ShapedValue}

import scala.reflect.ClassTag

trait ShinoFormatterWrapper[RepOut, DataType] extends FormatterContent[RepOut, DataType] {
  def shape(implicit classTag: ClassTag[DataType]): MappedProjection[DataType, Any]
}

trait ShinoDecoderWrapper[RepOut, DataType] extends DecoderContent[RepOut, DataType] {
  def shape(implicit classTag: ClassTag[DataType]): MappedProjection[DataType, Any]
}

trait ShinoEncoderWrapper[RepOut, DataType] extends EncoderContent[RepOut, DataType] {
  def shape(implicit classTag: ClassTag[DataType]): MappedProjection[DataType, Any]
}

trait SlickResultIO {

  private val unitInstance = new SlickShapeValueWrap[(Unit, Unit)] {
    override type Rep   = (Unit, Unit)
    override type Level = FlatShapeLevel
    override val rep   = ((), ())
    override val shape = Shape.tuple2Shape[FlatShapeLevel, Unit, Unit, Unit, Unit, Unit, Unit](Shape.unitShape[FlatShapeLevel], Shape.unitShape[FlatShapeLevel])
  }.asInstanceOf[SlickShapeValueWrap[(Any, Any)]]

  object shino extends FormatterWrapperHelper[SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any), ShinoFormatterWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: FormatterShape.Aux[Rep, D, Out, SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)]): ShinoFormatterWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , unitInstance
      )
      new ShinoFormatterWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          ShapedValue(reps.rep, reps.shape)
            .<>(
                { (t: (Any, Any)) =>
                shape1.takeData(wrapCol, t).current
              }
              , { r: D =>
                Option(shape1.buildData(r, wrapCol, ((), ())))
              }
            )(classTag)
            .asInstanceOf[MappedProjection[D, Any]]
        }
      }
    }
  }

  object shinoOutput extends DecoderWrapperHelper[SlickShapeValueWrap[(Any, Any)], (Any, Any), ShinoDecoderWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: DecoderShape.Aux[Rep, D, Out, SlickShapeValueWrap[(Any, Any)], (Any, Any)]): ShinoDecoderWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , unitInstance
      )
      new ShinoDecoderWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          ShapedValue(reps.rep, reps.shape)
            .<>(
                { (t: (Any, Any)) =>
                shape1.takeData(wrapCol, t).current
              }
              , { r: D =>
                Option.empty
              }
            )
            .asInstanceOf[MappedProjection[D, Any]]
        }
      }
    }
  }

  object shinoInput extends EncoderWrapperHelper[SlickShapeValueWrap[(Any, Any)], (Any, Any), ShinoEncoderWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: EncoderShape.Aux[Rep, D, Out, SlickShapeValueWrap[(Any, Any)], (Any, Any)]): ShinoEncoderWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , unitInstance
      )
      new ShinoEncoderWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          ShapedValue(reps.rep, reps.shape)
            .<>[D](
                { (t: (Any, Any)) =>
                throw new NoSuchElementException("Write only shape can not read data from database")
              }
              , { r: D =>
                Option(shape1.buildData(r, wrapCol, ((), ())))
              }
            )
            .asInstanceOf[MappedProjection[D, Any]]
        }
      }
    }

    trait Setter[Data] {
      def to(data: Data): EncoderShapeValue[Unit, SlickShapeValueWrap[(Any, Any)], (Any, Any)]
    }

    def set[Rep, D, Target](rep: Rep)(implicit encoderShape: EncoderShape.Aux[Rep, D, Target, SlickShapeValueWrap[(Any, Any)], (Any, Any)]): Setter[D] = {
      new Setter[D] {
        def to(data: D): EncoderShapeValue[Unit, SlickShapeValueWrap[(Any, Any)], (Any, Any)] = {
          val rep1 = rep
          new EncoderShapeValue[D, SlickShapeValueWrap[(Any, Any)], (Any, Any)] {
            override type RepType = Target
            override val rep   = encoderShape.wrapRep(rep1)
            override val shape = encoderShape.packed
          }.emap { _: Unit =>
            data
          }
        }
      }
    }

    def sequenceShapeValue(
        v: EncoderShapeValue[Unit, SlickShapeValueWrap[(Any, Any)], (Any, Any)]*
    ): EncoderShapeValue[Unit, SlickShapeValueWrap[(Any, Any)], (Any, Any)] = {
      val list = v.toList
      shaped(list).emap { i: Unit =>
        list.map(_ => i)
      }
    }

    def sequenceShapeValueCol(
        v: List[EncoderShapeValue[Unit, SlickShapeValueWrap[(Any, Any)], (Any, Any)]]
    ): EncoderShapeValue[Unit, SlickShapeValueWrap[(Any, Any)], (Any, Any)] = {
      shaped(v).emap { i: Unit =>
        v.map(_ => i)
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
          override type Rep   = T
          override type Level = L
          override val shape = shape1.packedShape
          override val rep   = shape1.pack(base)
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

object SlickResultIO extends SlickResultIO
