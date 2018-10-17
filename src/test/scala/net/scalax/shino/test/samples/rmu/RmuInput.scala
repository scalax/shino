package net.scalax.shino.test.samples.rmu

import io.circe.{Decoder, JsonObject}
import net.scalax.asuna.core.encoder.{EncoderShape, EncoderShapeValue}
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.asuna.mapper.encoder.{EncoderContent, EncoderWrapperHelper}
import net.scalax.shino.umr.{SlickResultIO, SlickShapeValueWrap}
import slick.lifted.{FlatShapeLevel, Shape}

trait RmuInputWrapper[RepOut, DataType] extends EncoderContent[RepOut, DataType] {
  self =>
  import SlickResultIO._

  def repCol: List[(String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])]
  lazy val shape: EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]] =
    shinoInput.shaped(repCol.map(_._2)).emap[JsonObject](r => (1 to repCol.size).toList.map(_ => r))
  def filter(s: String => Boolean): RmuInputWrapper[RepOut, DataType] = new RmuInputWrapper[RepOut, DataType] {
    override val repCol = self.repCol.filter(r => s(r._1))
  }
}

trait RmuInputHelper {

  import SlickResultIO._

  object rmuInput
      extends EncoderWrapperHelper[List[(String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])], Unit, RmuInputWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(
        implicit shape: EncoderShape.Aux[Rep, D, Out, List[(String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])], Unit]
    ): RmuInputWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps    = shape1.toLawRep(wrapCol, List.empty)
      new RmuInputWrapper[Out, D] {
        override val repCol = reps
      }
    }
  }

  implicit def rmuIntputWrapperRepImplicit1[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
    , decoder: Decoder[D]
  ): EncoderShape.Aux[RepColumnContent[R, D], D, (String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]]), List[
      (String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])
  ], Unit] = {

    new EncoderShape[RepColumnContent[R, D], List[(String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])], Unit] {

      override type Target = (String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])
      override type Data   = D

      override def wrapRep(
          base: RepColumnContent[R, D]
      ): (String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]]) = {
        (base.columnInfo.modelColumnName, shinoInput.shaped(base.rep).emap[JsonObject](d => d(base.columnInfo.modelColumnName).get.as(decoder).right.get))
      }

      override def toLawRep(
          base: (String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])
        , oldRep: List[(String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])]
      ): List[(String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])] = base :: oldRep

      override def buildData(
          data: D
        , rep: (String, EncoderShapeValue[JsonObject, List[SlickShapeValueWrap], IndexedSeq[Any]])
        , oldData: Unit
      ): Unit = ()

    }
  }

}
