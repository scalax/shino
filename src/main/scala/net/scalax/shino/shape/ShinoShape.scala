package net.scalax.shino.shape

import slick.SlickException
import slick.ast.Node
import slick.lifted.{FlatShapeLevel, Shape}

trait ShinoShape[Poly, Rep] {
  self =>

  type Data
  type Target

  def packedShape: ShinoShape.Aux[Poly, Target, Data, Target] = new ShinoShape[Poly, Target] {
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
  }

}

object ShinoShape {
  type Aux[Poly, Rep, D, T] = ShinoShape[Poly, Rep] { type Data = D; type Target = T }
}
