package net.scalax.shino

import slick.lifted.{FlatShapeLevel, Shape}

trait SlickShapeValueWrap[E] {
  self =>

  type Rep
  type Data
  type Level <: FlatShapeLevel
  val rep: Rep
  val shape: Shape[Level, Rep, Data, Rep]
  def convert(data: Data): E
  def cusBuildData(data: E): Option[Data]

  def map[R](cv: E => R)(ecv: R => Option[E]): SlickShapeValueWrap[R] = {
    new SlickShapeValueWrap[R] {
      override type Rep   = self.Rep
      override type Data  = self.Data
      override type Level = self.Level
      override val rep   = self.rep
      override val shape = self.shape
      override def convert(data: self.Data): R = {
        cv(self.convert(data))
      }
      override def cusBuildData(data: R): Option[self.Data] = {
        ecv(data).flatMap(d => self.cusBuildData(d))
      }
    }

  }

  def zip[R](other: SlickShapeValueWrap[R]): SlickShapeValueWrap[(E, R)] = {
    new SlickShapeValueWrap[(E, R)] {
      override type Rep   = (self.Rep, other.Rep)
      override type Data  = (self.Data, other.Data)
      override type Level = FlatShapeLevel
      override val rep                         = (self.rep, other.rep)
      override val shape                       = Shape.tuple2Shape[FlatShapeLevel, self.Rep, other.Rep, self.Data, other.Data, self.Rep, other.Rep](self.shape, other.shape)
      override def convert(data: Data): (E, R) = (self.convert(data._1), other.convert(data._2))
      override def cusBuildData(data: (E, R)): Option[(self.Data, other.Data)] = {
        self.cusBuildData(data._1).flatMap(a1 => other.cusBuildData(data._2).map(a2 => (a1, a2)))
      }
    }

  }

}
