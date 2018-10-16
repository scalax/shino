package net.scalax.shino.shape

import slick.SlickException
import slick.ast.Node
import slick.lifted._

sealed trait EncodeTag

object EncodeTag {
  val init: EncodeTag = new EncodeBaseTag {}
}

trait EncodeRefTag extends EncodeTag {
  val path: Node
}

trait EncodeBaseTag extends EncodeTag

trait RepCol[Col <: RepCol[Col, Model], Model] extends Rep[Model] {

  def tag: EncodeTag
  def * : ProvenShape[Model]
  def copyInstance(tag: EncodeTag): Col

  override def encodeRef(path: Node): Col = {
    val path1 = path
    copyInstance(new EncodeRefTag {
      override val path = path1
    })
  }

  override def toNode: Node = {
    def dealWithShape[T <: FlatShapeLevel, S, D, R](rep1: Any, shape1: Shape[T, S, D, R]): Node = {
      val packShape: Shape[T, R, D, R] = shape1.packedShape
      val packValue: R                 = shape1.pack(rep1.asInstanceOf[S])
      println("33" * 100 + packValue.asInstanceOf[MappedProjection[_, _]].toNode.children.toSeq.toList.head.children.toSeq.toList)
      val tag1 = tag
      tag1 match {
        case _: EncodeBaseTag =>
          println("22" * 100 + *.toNode)
          /*packShape.toNode(packValue)*/
          *.toNode
        case tag2: EncodeRefTag =>
          println("11" * 100 + packShape.toNode(packShape.encodeRef(packValue, tag2.path).asInstanceOf[R]))
          packShape.toNode(packShape.encodeRef(packValue, tag2.path).asInstanceOf[R])
      }
    }
    val shape2 = *.shape
    dealWithShape(*.value, shape2)
  }

}

trait RepColHelper {

  implicit def repColInstaceShape[T <: RepCol[T, Model], Model, Level <: ShapeLevel](implicit cv: T <:< RepCol[T, Model]): Shape[Level, T, Model, T] =
    new Shape[Level, T, Model, T] {
      override def pack(value: T): T                      = value
      override def packedShape: Shape[Level, T, Model, T] = this
      override def buildParams(extract: Any => Unpacked): Packed =
        throw new SlickException("Shape does not have the same Mixed and Unpacked type")
      override def encodeRef(value: T, path: Node): Any = value.encodeRef(path)
      override def toNode(value: T): Node               = value.toNode
    }

}
