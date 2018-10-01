package net.scalax.shino.test

import io.circe.{Decoder, Encoder, Json, JsonObject}
import net.scalax.asuna.core.encoder.EncoderShape
import net.scalax.asuna.core.formatter.FormatterShapeValue
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.asuna.mapper.encoder.{EncoderContent, EncoderWrapperHelper}
import net.scalax.shino.umr.{SlickMapper, SlickShapeValueWrap}
import slick.lifted.{FlatShapeLevel, MappedProjection, Shape}

trait RmuWrapper[RepOut, DataType] extends EncoderContent[RepOut, DataType] {
  def shape: FormatterShapeValue[JsonObject, SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)]
}

trait RmuHelper {

  private object slickMapper extends SlickMapper

  import slickMapper._

  object rmu extends EncoderWrapperHelper[FormatterShapeValue[Map[String, Json], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)], Unit, RmuWrapper] {
    override def effect[Rep, D, Out](
        rep: Rep
    )(
        implicit shape: EncoderShape.Aux[Rep, D, Out, FormatterShapeValue[Map[String, Json], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)], Unit]
    ): RmuWrapper[Out, D] = {
      val shape1  = shape
      val wrapCol = shape1.wrapRep(rep)
      val reps = shape1
        .toLawRep(
            wrapCol
          , shino
            .shaped(new SlickShapeValueWrap[(Unit, Unit)] {
              override type Rep   = (Unit, Unit)
              override type Level = FlatShapeLevel
              override val rep = ((), ())
              override val shape =
                Shape.tuple2Shape[FlatShapeLevel, Unit, Unit, Unit, Unit, Unit, Unit](Shape.unitShape[FlatShapeLevel], Shape.unitShape[FlatShapeLevel])
            })
            .fmap { _: (Unit, Unit) =>
              Map.empty[String, Json]
            } { _ =>
              ((), ())
            }
        )
        .fmap { m: Map[String, Json] =>
          JsonObject.fromMap(m)
        } { obj: JsonObject =>
          obj.toMap
        }
      new RmuWrapper[Out, D] {
        override def shape: FormatterShapeValue[JsonObject, SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] = {
          reps
        }
      }
    }
  }

  implicit def rmuWrapperRepImplicit1[R, D, T, L <: FlatShapeLevel](
      implicit shape: Shape[L, R, D, T]
    , encoder: Encoder[D]
    , decoder: Decoder[D]
  ): EncoderShape.Aux[RepColumnContent[R, D], D, (String, FormatterShapeValue[(String, Json), SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)]), FormatterShapeValue[
      Map[String, Json]
    , SlickShapeValueWrap[(Any, Any)]
    , (Any, Any)
    , (Any, Any)
  ], Unit] = {

    new EncoderShape[RepColumnContent[R, D], FormatterShapeValue[
        Map[String, Json]
      , SlickShapeValueWrap[(Any, Any)]
      , (Any, Any)
      , (Any, Any)
    ], Unit] {

      override type Target = (String, FormatterShapeValue[(String, Json), SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)])
      override type Data   = D

      override def wrapRep(
          base: RepColumnContent[R, D]
      ): (String, FormatterShapeValue[(String, Json), SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)]) = {
        (base.columnInfo.modelColumnName, shino.shaped(base.rep).fmap(d => (base.columnInfo.modelColumnName, encoder(d))) { r =>
          r._2.as(decoder).right.get
        })
      }

      override def toLawRep(
          base: (String, FormatterShapeValue[(String, Json), SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)])
        , oldRep: FormatterShapeValue[Map[String, Json], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)]
      ): FormatterShapeValue[Map[String, Json], SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)] = {
        base._2
          .fzip(oldRep)
          .fmap {
            case (baseData, oldData) =>
              oldData + baseData
          } { map =>
            ((base._1, map.get(base._1).get), map)
          }
      }

      override def buildData(
          data: D
        , rep: (String, FormatterShapeValue[(String, Json), SlickShapeValueWrap[(Any, Any)], (Any, Any), (Any, Any)])
        , oldData: Unit
      ): Unit = ()

    }
  }

}
