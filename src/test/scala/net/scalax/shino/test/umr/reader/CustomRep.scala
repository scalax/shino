package net.scalax.shino.test.umr.reader

import net.scalax.asuna.core.decoder.{DecoderShape, SplitData}
import net.scalax.asuna.core.formatter.FormatterShape
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.asuna.mapper.decoder.{DecoderContent, DecoderWrapperHelper}
import net.scalax.asuna.mapper.formatter.{FormatterContent, FormatterWrapperHelper}
import slick.ast._
import slick.lifted.{FlatShapeLevel, Shape}
import slick.util.{ConstArray, ProductWrapper}

import scala.reflect.ClassTag

trait CustomRep {

  trait ColumnWrap {
    type Repr
    type DataType
    type Level <: FlatShapeLevel
    val rep: Repr
    val shape: Shape[Level, Repr, DataType, Repr]
    val indexInfo: (String, Int)
    def toNode: Node = shape.toNode(rep)
  }

  case class NodeWrap(node: Node, indexMap: Map[String, Int])

  trait CustomTable[Target <: CustomTable[Target, Model], Model] {

    def customEncodeRef: EncodeRefConvert[Target]
    def customNode: NodeWrap

  }

  implicit def customTableShape[Target <: CustomTable[Target, Model], Model]: Shape[FlatShapeLevel, CustomTable[Target, Model], Model, Target] = {
    new Shape[FlatShapeLevel, CustomTable[Target, Model], Model, Target] {
      self =>
      override def pack(value: CustomTable[Target, Model]): Target = value.customEncodeRef.target
      override def packedShape: Shape[FlatShapeLevel, Target, Model, Target] = new Shape[FlatShapeLevel, Target, Model, Target] {
        subSelf =>
        override def pack(value: Target): Target                               = value
        override def packedShape: Shape[FlatShapeLevel, Target, Model, Target] = subSelf
        override def buildParams(extract: Any => Model): Target                = self.buildParams(extract)
        override def encodeRef(value: Target, path: Node): Target              = value.customEncodeRef.customEncodeRef(path, value.customNode.indexMap)
        override def toNode(value: Target): Node                               = value.customNode.node
      }
      override def buildParams(extract: Any => Model): Target = ???
      override def encodeRef(value: CustomTable[Target, Model], path: Node): Target =
        value.customEncodeRef.target.customEncodeRef.customEncodeRef(path, value.customEncodeRef.target.customNode.indexMap)
      override def toNode(value: CustomTable[Target, Model]): Node = value.customEncodeRef.target.customNode.node
    }
  }

  case class EncodeRefWrapTuple2(head: Any, tail: EncodeRefWrapTuple2)
  trait EncodeRefConvert[Target] {
    self =>
    def target: Target
    def customEncodeRef(path: Node, map: Map[String, Int]): Target
    def map[S](cv: Target => S): EncodeRefConvert[S] = new EncodeRefConvert[S] {
      override def target: S                                             = cv(self.target)
      override def customEncodeRef(path: Node, map: Map[String, Int]): S = cv(self.customEncodeRef(path, map))
    }
  }

  trait RepEncoderDecoder[RepOut, DataType] extends DecoderContent[RepOut, DataType] { self =>
    def customEncodeRef(path: Node, map: Map[String, Int]): DataType
    def target: DataType

    def toRef: EncodeRefConvert[DataType] = new EncodeRefConvert[DataType] {
      override def target: DataType                                             = self.target
      override def customEncodeRef(path: Node, map: Map[String, Int]): DataType = self.customEncodeRef(path, map)

    }
  }

  object sEncodeRef extends DecoderWrapperHelper[EncodeRefConvert[EncodeRefWrapTuple2], EncodeRefWrapTuple2, RepEncoderDecoder] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: DecoderShape.Aux[Rep, D, Out, EncodeRefConvert[EncodeRefWrapTuple2], EncodeRefWrapTuple2]): RepEncoderDecoder[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)

      val zeroModel = new EncodeRefConvert[EncodeRefWrapTuple2] {
        override def customEncodeRef(path: Node, map: Map[String, Int]): EncodeRefWrapTuple2 = EncodeRefWrapTuple2((), null)
        override def target: EncodeRefWrapTuple2                                             = EncodeRefWrapTuple2((), null)
      }

      val reps  = shape1.toLawRep(wrapCol, zeroModel)
      val model = reps.map(r => shape1.takeData(wrapCol, r).current)

      new RepEncoderDecoder[Out, D] {
        override def customEncodeRef(path: Node, map: Map[String, Int]): D = model.customEncodeRef(path, map)
        override def target: D                                             = model.target
      }
    }
  }

  implicit def sExImplicit2[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
  ): DecoderShape.Aux[RepColumnContent[R, T], T, EncodeRefConvert[T], EncodeRefConvert[EncodeRefWrapTuple2], EncodeRefWrapTuple2] = {
    new DecoderShape[RepColumnContent[R, T], EncodeRefConvert[EncodeRefWrapTuple2], EncodeRefWrapTuple2] {

      override type Target = EncodeRefConvert[T]
      override type Data   = T

      override def wrapRep(base: RepColumnContent[R, T]): EncodeRefConvert[T] = new EncodeRefConvert[T] {
        override def customEncodeRef(path: Node, map: Map[String, Int]): T =
          shape.encodeRef(base.rep, Select(path, ElementSymbol(map(base.columnInfo.modelColumnName)))).asInstanceOf[T]
        override def target: T = shape.pack(base.rep)
      }

      override def toLawRep(base: EncodeRefConvert[T], oldRep: EncodeRefConvert[EncodeRefWrapTuple2]): EncodeRefConvert[EncodeRefWrapTuple2] =
        new EncodeRefConvert[EncodeRefWrapTuple2] {
          override def customEncodeRef(path: Node, map: Map[String, Int]): EncodeRefWrapTuple2 =
            EncodeRefWrapTuple2(base.customEncodeRef(path, map), oldRep.customEncodeRef(path, map))
          override def target: EncodeRefWrapTuple2 = EncodeRefWrapTuple2(base.target, oldRep.target)
        }

      override def takeData(oldData: EncodeRefConvert[T], rep: EncodeRefWrapTuple2): SplitData[T, EncodeRefWrapTuple2] =
        SplitData(current = rep.head.asInstanceOf[T], left = rep.tail)

    }
  }

  trait ModelConvert[Target, DataType] extends FormatterContent[Target, DataType] {
    self =>
    def from(data: List[Any]): DataType
    def to(data: DataType): IndexedSeq[Any]
    def tranTable: List[Node]
    def mapIndex: Map[String, Int]
    def toNodeWrap(implicit c: ClassTag[DataType]): NodeWrap = {
      val tableList   = tranTable
      val productNode = ProductNode(ConstArray.from(tableList))
      def toBase(v: Any) = {
        val s = v.asInstanceOf[DataType]
        new ProductWrapper(to(s))
      }
      def toMapped(v: Any) = {
        val product = v.asInstanceOf[Product]
        from(product.productIterator.toList)
      }
      val typeNode = TypeMapping(productNode, MappedScalaType.Mapper(toBase, toMapped, None), c)
      val map      = mapIndex.map { case (key, value) => (key, tableList.size - value) }
      NodeWrap(typeNode, map)
    }
  }

  object sNodeGen extends FormatterWrapperHelper[(List[ColumnWrap], Int), IndexedSeq[Any], List[Any], ModelConvert] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(implicit shape: FormatterShape.Aux[Rep, D, Out, (List[ColumnWrap], Int), IndexedSeq[Any], List[Any]]): ModelConvert[Out, D] = {
      val shape1    = shape
      val wrapCol   = shape1.wrapRep(rep)
      val (reps, _) = shape1.toLawRep(wrapCol, (List.empty, 0))
      new ModelConvert[Out, D] {
        override def from(data: List[Any]): D     = shape1.takeData(wrapCol, data).current
        override def to(data: D): IndexedSeq[Any] = shape1.buildData(data, wrapCol, IndexedSeq.empty)
        override def tranTable: List[Node]        = reps.map(r => r.shape.toNode(r.rep))
        override def mapIndex                     = reps.map(s => s.indexInfo).toMap
      }
    }
  }

  implicit def sExImplicit3[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
  ): FormatterShape.Aux[RepColumnContent[R, D], D, Int => ColumnWrap, (List[ColumnWrap], Int), IndexedSeq[Any], List[Any]] = {
    new FormatterShape[RepColumnContent[R, D], (List[ColumnWrap], Int), IndexedSeq[Any], List[Any]] {
      override type Target = Int => ColumnWrap
      override type Data   = D
      override def wrapRep(base: RepColumnContent[R, D]): Int => ColumnWrap = {
        val shape1 = shape

        { index: Int =>
          new ColumnWrap {
            override type Repr     = T
            override type DataType = D
            override type Level    = L
            override val rep       = shape1.pack(base.rep)
            override val shape     = shape1.packedShape
            override val indexInfo = (base.columnInfo.modelColumnName, index)
          }
        }
      }
      override def toLawRep(base: Int => ColumnWrap, oldRep: (List[ColumnWrap], Int)): (List[ColumnWrap], Int) = (base(oldRep._2) :: oldRep._1, oldRep._2 + 1)
      override def takeData(rep: Int => ColumnWrap, oldData: List[Any]): SplitData[D, List[Any]]               = SplitData(oldData.head.asInstanceOf[D], oldData.tail)
      override def buildData(data: D, rep: Int => ColumnWrap, oldData: IndexedSeq[Any]): IndexedSeq[Any]       = data +: oldData
    }
  }

}
