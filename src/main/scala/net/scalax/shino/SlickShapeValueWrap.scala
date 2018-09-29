package net.scalax.shino

import slick.lifted.{FlatShapeLevel, Shape}

trait SlickShapeValueWrap[Data] {
  self =>

  type Rep
  type Level <: FlatShapeLevel
  val rep: Rep
  val shape: Shape[Level, Rep, Data, Rep]

  def zip[R](other: SlickShapeValueWrap[R]): SlickShapeValueWrap[(Data, R)] = {
    new SlickShapeValueWrap[(Data, R)] {
      override type Rep   = (self.Rep, other.Rep)
      override type Level = FlatShapeLevel
      override val rep   = (self.rep, other.rep)
      override val shape = Shape.tuple2Shape[FlatShapeLevel, self.Rep, other.Rep, Data, R, self.Rep, other.Rep](self.shape, other.shape)
    }

  }

}
