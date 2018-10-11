package net.scalax.shino.test.umr.reader

import net.scalax.asuna.core.decoder.{DecoderShape, SplitData}
import net.scalax.asuna.mapper.decoder.{DecoderContent, DecoderWrapperHelper}
import slick.ast.Node
import slick.lifted.{FlatShapeLevel, Rep, Shape}

trait CustomRep {

  trait ColumnWrap {
    type Repr
    type DataType
    val rep: Repr
    val shape: Shape[FlatShapeLevel, Repr, DataType, Repr]
    def toNode: Node = shape.toNode(rep)
  }

  case class NodeWrap(node: Node, indexMap: Map[String, Int])

  trait CustomTable[Target <: CustomTable[Target, Model], Model] {

    def customEncodeRef(path: Node, map: Map[String, Int]): Target
    def customPack: Target
    def customNode: NodeWrap

  }

  implicit def customTableShape[Target <: CustomTable[Target, Model], Model]: Shape[FlatShapeLevel, CustomTable[Target, Model], Model, Target] = {
    new Shape[FlatShapeLevel, CustomTable[Target, Model], Model, Target] {
      self =>
      override def pack(value: CustomTable[Target, Model]): Target = value.customPack
      override def packedShape: Shape[FlatShapeLevel, Target, Model, Target] = new Shape[FlatShapeLevel, Target, Model, Target] {
        subSelf =>
        override def pack(value: Target): Target                               = value
        override def packedShape: Shape[FlatShapeLevel, Target, Model, Target] = subSelf
        override def buildParams(extract: Any => Model): Target                = self.buildParams(extract)
        override def encodeRef(value: Target, path: Node): Any                 = value.customEncodeRef(path, value.customNode.indexMap)
        override def toNode(value: Target): Node                               = value.customNode.node
      }
      override def buildParams(extract: Any => Model): Target                    = ???
      override def encodeRef(value: CustomTable[Target, Model], path: Node): Any = value.customPack.customEncodeRef(path, value.customPack.customNode.indexMap)
      override def toNode(value: CustomTable[Target, Model]): Node               = value.customPack.customNode.node
    }
  }

  case class TargetWrapTuple2(head: Any, tail: TargetWrapTuple2)
  case class EncodeRefWrapTuple2(head: Any, tail: EncodeRefWrapTuple2)

  trait RepDecoder[RepOut, DataType] extends DecoderContent[RepOut, DataType] {
    def target: DataType
  }

  object sTarget extends DecoderWrapperHelper[TargetWrapTuple2, TargetWrapTuple2, RepDecoder] {
    override def effect[Rep, D, Out](rep: Rep)(implicit shape: DecoderShape.Aux[Rep, D, Out, TargetWrapTuple2, TargetWrapTuple2]): RepDecoder[Out, D] = {
      val shape1    = shape
      val wrapCol   = shape1.wrapRep(rep)
      val zeroModel = TargetWrapTuple2((), null)
      val reps      = shape1.toLawRep(wrapCol, zeroModel)
      val model     = shape1.takeData(wrapCol, reps).current
      new RepDecoder[Out, D] {
        override def target: D = model
      }
    }
  }

  object sEncodeRef extends DecoderWrapperHelper[EncodeRefWrapTuple2, EncodeRefWrapTuple2, RepDecoder] {
    override def effect[Rep, D, Out](rep: Rep)(implicit shape: DecoderShape.Aux[Rep, D, Out, EncodeRefWrapTuple2, EncodeRefWrapTuple2]): RepDecoder[Out, D] = {
      val shape1    = shape
      val wrapCol   = shape1.wrapRep(rep)
      val zeroModel = EncodeRefWrapTuple2((), null)
      val reps      = shape1.toLawRep(wrapCol, zeroModel)
      val model     = shape1.takeData(wrapCol, reps).current
      new RepDecoder[Out, D] {
        override def target: D = model
      }
    }
  }

  implicit def sExImplicit1[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
  ): DecoderShape.Aux[R, T, T, TargetWrapTuple2, TargetWrapTuple2] = {
    new DecoderShape[R, TargetWrapTuple2, TargetWrapTuple2] {
      override type Target = T
      override type Data   = T
      override def wrapRep(base: R): T                                           = shape.pack(base)
      override def toLawRep(base: T, oldRep: TargetWrapTuple2): TargetWrapTuple2 = TargetWrapTuple2(base, oldRep)
      override def takeData(oldData: T, rep: TargetWrapTuple2): SplitData[T, TargetWrapTuple2] =        SplitData(current = rep.head.asInstanceOf[T], left = rep.tail)
    }
  }

  {
    import slick.jdbc.H2Profile.api.{Rep => SRep, _}

    case class Abc(name: String)
    case class AbcTable(name: SRep[String]) extends CustomTable[AbcTable, Abc] {
      self =>
      override def customEncodeRef(path: Node, map: Map[String, Int]): AbcTable = ???
      override def customPack: AbcTable                                         = sTarget.effect(sTarget.singleModel[AbcTable](self).compile).target
      override def customNode: NodeWrap                                         = ???
    }
  }

}
