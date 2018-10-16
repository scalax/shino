package net.scalax.shino.shape

import slick.ast.{BaseTypedType, Node, ProductNode, TypedType}
import slick.lifted.{AbstractTable, ConstColumn, LiteralColumn, Rep}
import slick.util.ConstArray

trait ShapePoly extends ShapePoly5

object ShapePoly extends ShapePoly

trait ShapePoly5 extends ShapePoly4 {

  implicit final def primitiveShape[T](implicit tm: TypedType[T]): ShinoShape.Aux[ShapePoly, T, T, ConstColumn[T]] = new ShinoShape[ShapePoly, T] {
    override type Data   = T
    override type Target = ConstColumn[T]
    override def wrapRep(value: T): ConstColumn[T]                            = LiteralColumn(value)
    override def encodeRef(value: ConstColumn[T], path: Node): ConstColumn[T] = value.encodeRef(path)
    override def toNode(value: ConstColumn[T]): Node                          = value.toNode
  }

  val unitShapePrototype: ShinoShape.Aux[ShapePoly, Unit, Unit, Unit] = new ShinoShape[ShapePoly, Unit] {
    override type Data   = Unit
    override type Target = Unit
    override def wrapRep(value: Unit): Unit               = ()
    override def encodeRef(value: Unit, path: Node): Unit = ()
    override def toNode(value: Unit): Node                = ProductNode(ConstArray.empty)
  }

}

trait ShapePoly4 extends ShapePoly3 {

  implicit final def tableShape[T, C <: AbstractTable[_]](implicit ev: C <:< AbstractTable[T]): ShinoShape.Aux[ShapePoly, C, T, C] =
    new ShinoShape[ShapePoly, C] {
      override type Data   = T
      override type Target = C
      override def wrapRep(rep: C): C                  = rep
      override def encodeRef(target: C, path: Node): C = target.encodeRef(path).asInstanceOf[C]
      override def toNode(target: C): Node             = target.toNode
    }

}

trait ShapePoly3 extends ShapePoly2 {

  implicit def constColumnShape[T]: ShinoShape.Aux[ShapePoly, ConstColumn[T], T, ConstColumn[T]] = new ShinoShape[ShapePoly, ConstColumn[T]] {
    override type Data   = T
    override type Target = ConstColumn[T]
    override def wrapRep(rep: ConstColumn[T]): ConstColumn[T]                  = rep
    override def encodeRef(target: ConstColumn[T], path: Node): ConstColumn[T] = target.encodeRef(path)
    override def toNode(target: ConstColumn[T]): Node                          = target.toNode
  }

}

trait ShapePoly2 extends ShapePoly1 {

  implicit def optionShape[M](implicit sh: ShinoShape.Aux[ShapePoly, Rep[M], M, Rep[M]]): ShinoShape.Aux[ShapePoly, Rep[Option[M]], Option[M], Rep[Option[M]]] =
    repShape[Option[M]]

  implicit def repColumnShape[T: BaseTypedType] = repShape[T]

}

trait ShapePoly1 {

  def repShape[T]: ShinoShape.Aux[ShapePoly, Rep[T], T, Rep[T]] = new ShinoShape[ShapePoly, Rep[T]] {
    override type Data   = T
    override type Target = Rep[T]
    override def wrapRep(rep: Rep[T]): Rep[T]                  = rep
    override def encodeRef(target: Rep[T], path: Node): Rep[T] = target.encodeRef(path)
    override def toNode(target: Rep[T]): Node                  = target.toNode
  }

}
