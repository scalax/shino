package net.scalax.shino.shape

import net.scalax.asuna.core.decoder.DecoderShape
import net.scalax.asuna.core.formatter.FormatterShape
import net.scalax.asuna.mapper.decoder.{DecoderContent, DecoderWrapperHelper}
import net.scalax.asuna.mapper.formatter.{FormatterContent, FormatterWrapperHelper}
import slick.SlickException
import slick.ast.{Node, ProductNode}
import slick.lifted.{FlatShapeLevel, Shape}
import slick.util.ConstArray

trait EncodeRefContent[Target] {
  self =>

  def encode(path: Node): Target
  def map[N](cv: Target => N): EncodeRefContent[N] = new EncodeRefContent[N] {
    override def encode(path: Node) = cv(self.encode(path))
  }

}

trait EncodeRefWrapper[Target, Data] extends DecoderContent[Target, Data] {
  self =>

  def encode(path: Node): Data
  def map[N](cv: Data => N): EncodeRefContent[N] = new EncodeRefContent[N] {
    override def encode(path: Node) = cv(self.encode(path))
  }

  def toContent: EncodeRefContent[Data] = new EncodeRefContent[Data] {
    override def encode(path: Node): Data = encode(path)
  }

}

trait ShapeWrap[Target, Data] extends FormatterContent[Target, Data] {
  def toNode(target: Target): Node
}

trait ShinoShape[Poly, Rep] {
  self =>

  type Data
  type Target

  /*def packedShape: ShinoShape.Aux[Poly, Target, Data, Target] = new ShinoShape[Poly, Target] {
    override type Data   = self.Data
    override type Target = self.Target
    override def wrapRep(rep: self.Target): self.Target                  = rep
    override def encodeRef(target: self.Target, path: Node): self.Target = self.encodeRef(target, path)
    override def toNode(target: self.Target): Node                       = self.toNode(target)
  }

  def wrapRep(rep: Rep): Target
  def encodeRef(target: Target, path: Node): Target
  def toNode(target: Target): Node

  def toLaw: Shape[FlatShapeLevel, Rep, Data, Target] = new Shape[FlatShapeLevel, Rep, Data, Target] {
    override def pack(value: Rep): Target                                 = wrapRep(value)
    override def packedShape: Shape[FlatShapeLevel, Target, Data, Target] = self.packedShape.toLaw
    override def buildParams(extract: Any => Unpacked): Target            = throw new SlickException("Shino does not have buildParams implements.")
    override def encodeRef(value: Rep, path: Node): Any                   = self.encodeRef(self.wrapRep(value), path)
    override def toNode(value: Rep): Node                                 = self.toNode(self.wrapRep(value))
  }*/

  def dataFormatterShape: FormatterShape.Aux[Rep, Data, Target, Node, (Any, Any), (Any, Any)]
  def repDecoderShape: DecoderShape.Aux[Target, Target, Target, EncodeRefContent[(Any, Any)], (Any, Any)]

  object repShape extends DecoderWrapperHelper[EncodeRefContent[(Any, Any)], (Any, Any), EncodeRefWrapper] {
    override def effect[Source, D, Out](
        rep: Source
    )(implicit shape: DecoderShape.Aux[Source, D, Out, EncodeRefContent[(Any, Any)], (Any, Any)]): EncodeRefWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val content = shape1.toLawRep(wrapCol, new EncodeRefContent[(Any, Any)] {
        override def encode(path: Node): (Any, Any) = ((), ())
      })
      new EncodeRefWrapper[Out, D] {
        override def encode(path: Node): D = {
          content.map(s => shape1.takeData(wrapCol, s).current).encode(path)
        }
      }
    }
  }

  object dataShape extends FormatterWrapperHelper[Node, (Any, Any), (Any, Any), ShapeWrap] {
    override def effect[Source, D, Out](
        rep: Source
    )(implicit shape: FormatterShape.Aux[Source, D, Out, Node, (Any, Any), (Any, Any)]): ShapeWrap[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val content = shape1.toLawRep(wrapCol, ProductNode(ConstArray.empty))
      new ShapeWrap[Out, D] {
        override def toNode(out: Out): Node = {
          content
        }
      }
    }
  }

  def toLaw: Shape[FlatShapeLevel, Rep, Data, Target] = {
    new Shape[FlatShapeLevel, Rep, Data, Target] {

      override def pack(value: Rep): Target                      = dataFormatterShape.wrapRep(value)
      override def buildParams(extract: Any => Unpacked): Target = throw new SlickException("Shino does not have buildParams implements.")
      override def encodeRef(value: Rep, path: Node): Any        = repShape.effect(dataFormatterShape.wrapRep(value))(repDecoderShape).toContent.encode(path)
      override def toNode(value: Rep): Node                      = dataShape.effect(value)(dataFormatterShape).toNode(dataFormatterShape.wrapRep(value))

      override def packedShape: Shape[FlatShapeLevel, Target, Data, Target] = {
        val pShape: ShinoShape.Aux[Poly, Target, Data, Target] = new ShinoShape[Poly, Target] {
          override type Data   = self.Data
          override type Target = self.Target
          override def dataFormatterShape: FormatterShape.Aux[Target, Data, Target, Node, (Any, Any), (Any, Any)]          = self.dataFormatterShape.packed
          override def repDecoderShape: DecoderShape.Aux[Target, Target, Target, EncodeRefContent[(Any, Any)], (Any, Any)] = self.repDecoderShape.packed
        }
        pShape.toLaw
      }

    }
  }

}

object ShinoShape {
  type Aux[Poly, Rep, D, T] = ShinoShape[Poly, Rep] { type Data = D; type Target = T }
}
