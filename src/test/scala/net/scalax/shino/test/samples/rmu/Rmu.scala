package net.scalax.shino.test

import io.circe.{Decoder, Encoder, Json, JsonObject}
import net.scalax.asuna.core.decoder.DecoderShapeValue
import net.scalax.asuna.core.encoder.EncoderShape
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.asuna.mapper.encoder.{EncoderContent, EncoderWrapperHelper}
import net.scalax.shino.umr.{SlickResultIO, SlickShapeValueWrap}
import slick.lifted.{FlatShapeLevel, Shape}

trait RmuWrapper[RepOut, DataType] extends EncoderContent[RepOut, DataType] {
  self =>
  import SlickResultIO._

  def repCol: List[(String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])]
  lazy val shape: DecoderShapeValue[JsonObject, SlickShapeValueWrap[(Any, Any)], (Any, Any)] =
    shinoOutput.shaped(repCol.map(s => s._2.dmap(r => (s._1, r)))).dmap(JsonObject.fromIterable)
  def filter(s: String => Boolean): RmuWrapper[RepOut, DataType] = new RmuWrapper[RepOut, DataType] {
    override val repCol = self.repCol.filter(r => s(r._1))
  }
}

trait RmuHelper {

  import SlickResultIO._

  object rmu extends EncoderWrapperHelper[List[(String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])], Unit, RmuWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(
        implicit shape: EncoderShape.Aux[Rep, D, Out, List[(String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])], Unit]
    ): RmuWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps    = shape1.toLawRep(wrapCol, List.empty)
      new RmuWrapper[Out, D] {
        override val repCol = reps
      }
    }
  }

  implicit def rmuWrapperRepImplicit1[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
    , encoder: Encoder[D]
    , decoder: Decoder[D]
  ): EncoderShape.Aux[RepColumnContent[R, D], D, (String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)]), List[
      (String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])
  ], Unit] = {

    new EncoderShape[RepColumnContent[R, D], List[(String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])], Unit] {

      override type Target = (String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])
      override type Data   = D

      override def wrapRep(
          base: RepColumnContent[R, D]
      ): (String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)]) = {
        (base.columnInfo.modelColumnName, shinoOutput.shaped(base.rep).dmap(d => encoder(d)))
      }

      override def toLawRep(
          base: (String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])
        , oldRep: List[(String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])]
      ): List[(String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])] = {
        base :: oldRep
      }

      override def buildData(
          data: D
        , rep: (String, DecoderShapeValue[Json, SlickShapeValueWrap[(Any, Any)], (Any, Any)])
        , oldData: Unit
      ): Unit = ()

    }
  }

}
