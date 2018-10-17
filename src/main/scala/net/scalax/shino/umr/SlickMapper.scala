package net.scalax.shino.umr

import net.scalax.asuna.core.decoder.{DecoderShape, SplitData}
import net.scalax.asuna.core.encoder.{EncoderShape, EncoderShapeValue}
import net.scalax.asuna.core.formatter.FormatterShape
import net.scalax.asuna.mapper.decoder.{DecoderContent, DecoderWrapperHelper}
import net.scalax.asuna.mapper.encoder.{EncoderContent, EncoderWrapperHelper}
import net.scalax.asuna.mapper.formatter.{FormatterContent, FormatterWrapperHelper}
import slick.SlickException
import slick.ast.{ElementSymbol, Node, ProductNode, Select}
import slick.lifted.{FlatShapeLevel, MappedProjection, Shape, ShapedValue}
import slick.util.{ConstArray, ProductWrapper}

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

  private def transWrapperList(reps: List[SlickShapeValueWrap]): ShapedValue[List[Any], Product] = {
    ShapedValue(
        reps.map(s => s.rep: Any)
      , new Shape[FlatShapeLevel, List[Any], Product, List[Any]] { subSelf =>
        override def pack(value: List[Any]): List[Any] = value

        override def packedShape = subSelf

        override def buildParams(extract: Any => Product): List[Any] = throw new SlickException("Shape does not have the same Mixed and Unpacked type")

        override def encodeRef(value: List[Any], path: Node): List[Any] =
          value.zipWithIndex.zip(reps.map(s => s.shape.asInstanceOf[Shape[FlatShapeLevel, Any, Any, Any]])).map {
            case ((item, index), eachShape) =>
              eachShape.encodeRef(item, Select(path, ElementSymbol(index + 1)))
          }

        override def toNode(value: List[Any]): Node = {
          val nodes =
            value.zip(reps.map(s => s.shape.asInstanceOf[Shape[FlatShapeLevel, Any, Any, Any]])).map { case (item, eachShape) => eachShape.toNode(item) }
          ProductNode(ConstArray.from(nodes))
        }
      }
    )
  }

  object shino extends FormatterWrapperHelper[List[SlickShapeValueWrap], IndexedSeq[Any], List[Any], ShinoFormatterWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: FormatterShape.Aux[Rep, D, Out, List[SlickShapeValueWrap], IndexedSeq[Any], List[Any]]): ShinoFormatterWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , List.empty
      )
      new ShinoFormatterWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          transWrapperList(reps)
            .<>(
                f = { t: Product =>
                shape1.takeData(wrapCol, t.productIterator.toList).current
              }
              , g = { r: D =>
                Option(new ProductWrapper(shape1.buildData(r, wrapCol, IndexedSeq.empty)))
              }
            )(classTag)
            .asInstanceOf[MappedProjection[D, Any]]
        }
      }
    }
  }

  object shinoOutput extends DecoderWrapperHelper[List[SlickShapeValueWrap], List[Any], ShinoDecoderWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: DecoderShape.Aux[Rep, D, Out, List[SlickShapeValueWrap], List[Any]]): ShinoDecoderWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , List.empty
      )
      new ShinoDecoderWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          transWrapperList(reps)
            .<>(
                { t: Product =>
                shape1.takeData(wrapCol, t.productIterator.toList).current
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

  object shinoInput extends EncoderWrapperHelper[List[SlickShapeValueWrap], IndexedSeq[Any], ShinoEncoderWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: EncoderShape.Aux[Rep, D, Out, List[SlickShapeValueWrap], IndexedSeq[Any]]): ShinoEncoderWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1.toLawRep(
          wrapCol
        , List.empty
      )
      new ShinoEncoderWrapper[Out, D] {
        override def shape(implicit classTag: ClassTag[D]): MappedProjection[D, Any] = {
          transWrapperList(reps)
            .<>[D](
                { (t: Product) =>
                throw new NoSuchElementException("Write only shape can not read data from database")
              }
              , { r: D =>
                Option(new ProductWrapper(shape1.buildData(r, wrapCol, IndexedSeq.empty)))

              }
            )
            .asInstanceOf[MappedProjection[D, Any]]
        }
      }
    }

    trait Setter[Data] {
      def to(data: Data): EncoderShapeValue[Unit, List[SlickShapeValueWrap], IndexedSeq[Any]]
    }

    def set[Rep, D, Target](rep: Rep)(implicit encoderShape: EncoderShape.Aux[Rep, D, Target, List[SlickShapeValueWrap], IndexedSeq[Any]]): Setter[D] = {
      new Setter[D] {
        def to(data: D): EncoderShapeValue[Unit, List[SlickShapeValueWrap], IndexedSeq[Any]] = {
          val rep1 = rep
          new EncoderShapeValue[D, List[SlickShapeValueWrap], IndexedSeq[Any]] {
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
        v: EncoderShapeValue[Unit, List[SlickShapeValueWrap], IndexedSeq[Any]]*
    ): EncoderShapeValue[Unit, List[SlickShapeValueWrap], IndexedSeq[Any]] = {
      val list = v.toList
      shaped(list).emap { i: Unit =>
        list.map(_ => i)
      }
    }

    def sequenceShapeValueCol(
        v: List[EncoderShapeValue[Unit, List[SlickShapeValueWrap], IndexedSeq[Any]]]
    ): EncoderShapeValue[Unit, List[SlickShapeValueWrap], IndexedSeq[Any]] = {
      shaped(v).emap { i: Unit =>
        v.map(_ => i)
      }
    }
  }

  implicit def shinoWrapperRepImplicit[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
  ): FormatterShape.Aux[R, D, SlickShapeValueWrapImpl[D], List[SlickShapeValueWrap], IndexedSeq[Any], List[Any]] = {
    new FormatterShape[R, List[SlickShapeValueWrap], IndexedSeq[Any], List[Any]] {
      override type Target = SlickShapeValueWrapImpl[D]
      override type Data   = D
      override def wrapRep(base: R): SlickShapeValueWrapImpl[D] = {
        val shape1 = shape
        new SlickShapeValueWrapImpl[D] {
          override type Rep   = T
          override type Level = L
          override val shape = shape1.packedShape
          override val rep   = shape1.pack(base)
        }
      }
      override def toLawRep(base: SlickShapeValueWrapImpl[D], oldRep: List[SlickShapeValueWrap]): List[SlickShapeValueWrap] =
        base :: oldRep
      override def takeData(oldData: SlickShapeValueWrapImpl[D], rep: List[Any]): SplitData[D, List[Any]] =
        SplitData(current = rep.head.asInstanceOf[D], left = rep.tail)
      override def buildData(data: D, rep: SlickShapeValueWrapImpl[D], oldData: IndexedSeq[Any]): IndexedSeq[Any] = data +: oldData
    }
  }

  implicit def shinoSlickShapeValueWrapImplicit[D]
    : FormatterShape.Aux[SlickShapeValueWrapImpl[D], D, SlickShapeValueWrapImpl[D], List[SlickShapeValueWrap], IndexedSeq[Any], List[Any]] = {
    new FormatterShape[SlickShapeValueWrapImpl[D], List[SlickShapeValueWrap], IndexedSeq[Any], List[Any]] {
      override type Target = SlickShapeValueWrapImpl[D]
      override type Data   = D
      override def wrapRep(base: SlickShapeValueWrapImpl[D]): SlickShapeValueWrapImpl[D] = base
      override def toLawRep(base: SlickShapeValueWrapImpl[D], oldRep: List[SlickShapeValueWrap]): List[SlickShapeValueWrap] =
        base :: oldRep
      override def takeData(oldData: SlickShapeValueWrapImpl[D], rep: List[Any]): SplitData[D, List[Any]] =
        SplitData(current = rep.head.asInstanceOf[D], left = rep.tail)
      override def buildData(data: D, rep: SlickShapeValueWrapImpl[D], oldData: IndexedSeq[Any]): IndexedSeq[Any] = data +: oldData
    }
  }

}

object SlickResultIO extends SlickResultIO
