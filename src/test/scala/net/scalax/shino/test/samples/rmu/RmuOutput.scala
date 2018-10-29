package net.scalax.shino.test.samples.rmu

import io.circe.{Encoder, Json, JsonObject}
import net.scalax.asuna.core.decoder.DecoderShapeValue
import net.scalax.asuna.core.encoder.EncoderShape
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.asuna.mapper.encoder.{EncoderContent, EncoderWrapperHelper}
import net.scalax.shino.umr.{SlickResultIO, SlickShapeValueWrap}
import slick.lifted.{FlatShapeLevel, Shape}

trait RmuOutputWrapper[RepOut, DataType] extends EncoderContent[RepOut, DataType] {
  self =>
  import SlickResultIO._

  def repCol: List[(String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])]
  lazy val shape: DecoderShapeValue[JsonObject, List[SlickShapeValueWrap], List[Any]] =
    shinoOutput.shaped(repCol.map(s => s._2.dmap(r => (s._1, r)))).dmap(JsonObject.fromIterable)
  def filter(s: String => Boolean): RmuOutputWrapper[RepOut, DataType] = new RmuOutputWrapper[RepOut, DataType] {
    override val repCol = self.repCol.filter(r => s(r._1))
  }
}

trait RmuOutputHelper {

  import SlickResultIO._

  object rmuOutput extends EncoderWrapperHelper[List[(String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])], Unit, RmuOutputWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(
        implicit shape: EncoderShape.Aux[Rep, D, Out, List[(String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])], Unit]
    ): RmuOutputWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps    = shape1.toLawRep(wrapCol, List.empty)
      new RmuOutputWrapper[Out, D] {
        override val repCol = reps
      }
    }
  }

  implicit def rmuOutputWrapperRepImplicit1[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
    , encoder: Encoder[D]
  ): EncoderShape.Aux[RepColumnContent[R, D], D, (String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]]), List[
      (String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])
  ], Unit] = {

    new EncoderShape[RepColumnContent[R, D], List[(String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])], Unit] {

      override type Target = (String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])
      override type Data   = D

      override def wrapRep(
          base: => RepColumnContent[R, D]
      ): (String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]]) = {
        (base.columnInfo.tableColumnSymbol.name, shinoOutput.shaped(base.rep).dmap(d => encoder(d)))
      }

      override def toLawRep(
          base: (String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])
        , oldRep: List[(String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])]
      ): List[(String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])] = {
        base :: oldRep
      }

      override def buildData(
          data: D
        , rep: (String, DecoderShapeValue[Json, List[SlickShapeValueWrap], List[Any]])
        , oldData: Unit
      ): Unit = ()

    }
  }

}
