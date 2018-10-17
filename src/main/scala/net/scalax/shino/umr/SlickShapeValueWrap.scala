package net.scalax.shino.umr

import slick.ast.Node
import slick.lifted.{FlatShapeLevel, Shape}

trait SlickShapeValueWrap {
  self =>

  type Data
  type Rep
  type Level <: FlatShapeLevel
  val rep: Rep
  val shape: Shape[Level, Rep, Data, Rep]

  def toNode: Node = shape.toNode(rep)

}

trait SlickShapeValueWrapImpl[D] extends SlickShapeValueWrap {
  self =>

  override type Data = D

  override type Rep
  override type Level <: FlatShapeLevel
  override val rep: Rep
  override val shape: Shape[Level, Rep, D, Rep]

}
