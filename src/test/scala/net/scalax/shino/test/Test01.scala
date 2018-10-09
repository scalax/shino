package net.scalax.shino.test.umr

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.asuna.core.decoder.{DecoderShape, SplitData}
import net.scalax.asuna.mapper.common.RepColumnContent
import net.scalax.asuna.mapper.common.annotations.RootModel
import net.scalax.asuna.mapper.decoder.{DecoderContent, DecoderWrapperHelper}
import slick.jdbc.H2Profile.api._
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, ExecutionContext, Future}

object Test01 extends App {

  trait Helper {

    case class WrapHelper(ec: ExecutionContext, data: Future[Either[Exception, (Any, Any)]])

    trait FEWrapper[RepOut, DataType] extends DecoderContent[RepOut, DataType] {
      def data(implicit ec: ExecutionContext): Future[Either[Exception, DataType]]
    }

    object fe extends DecoderWrapperHelper[WrapHelper, (Any, Any), FEWrapper] {
      override def effect[Rep, D, Out](
          rep: Rep
      )(implicit shape: DecoderShape.Aux[Rep, D, Out, WrapHelper, (Any, Any)]): FEWrapper[Out, D] = {
        val shape1  = shape
        val wrapCol = shape1.wrapRep(rep)
        new FEWrapper[Out, D] {
          override def data(implicit ec: ExecutionContext): Future[Either[Exception, D]] = {
            val wrap = shape1.toLawRep(wrapCol, WrapHelper(ec, Future.successful(Right(((), ())))))
            wrap.data.map(r => r.right.map(d1 => shape1.takeData(wrapCol, d1).current))(ec)
          }
        }
      }
    }

    implicit def feImplicit1[T, R](
        implicit cv: R <:< Either[Exception, T]
    ): DecoderShape.Aux[RepColumnContent[Future[R], T], T, Future[R], WrapHelper, (Any, Any)] = {
      new DecoderShape[RepColumnContent[Future[R], T], WrapHelper, (Any, Any)] {
        override type Data   = T
        override type Target = Future[R]
        override def wrapRep(base: RepColumnContent[Future[R], T]): Future[R] = base.rep
        override def toLawRep(base: Future[R], oldRep: WrapHelper): WrapHelper = {
          val either = oldRep.data.flatMap(s => base.map(d => cv(d).right.flatMap(d1 => s.right.map(s1 => (d1, s1): (Any, Any))))(oldRep.ec))(oldRep.ec)
          WrapHelper(oldRep.ec, either)
        }
        override def takeData(rep: Future[R], oldData: (Any, Any)): SplitData[T, (Any, Any)] =
          SplitData(oldData._1.asInstanceOf[T], oldData._2.asInstanceOf[(Any, Any)])
      }
    }

    implicit def feImplicit2[T, R](
        implicit cv: R <:< Either[Exception, T]
    ): DecoderShape.Aux[RepColumnContent[R, T], T, Either[Exception, T], WrapHelper, (Any, Any)] = {
      new DecoderShape[RepColumnContent[R, T], WrapHelper, (Any, Any)] {
        override type Data   = T
        override type Target = Either[Exception, T]
        override def wrapRep(base: RepColumnContent[R, T]): Either[Exception, T] = cv(base.rep)
        override def toLawRep(base: Either[Exception, T], oldRep: WrapHelper): WrapHelper = {
          val either = oldRep.data.map(s => base.right.flatMap(d => s.right.map(s1 => (d, s1): (Any, Any))))(oldRep.ec)
          WrapHelper(oldRep.ec, either)
        }
        override def takeData(rep: Either[Exception, T], oldData: (Any, Any)): SplitData[T, (Any, Any)] =
          SplitData(oldData._1.asInstanceOf[T], oldData._2.asInstanceOf[(Any, Any)])
      }
    }

    implicit def feImplicit3[T]: DecoderShape.Aux[RepColumnContent[Future[T], T], T, Future[T], WrapHelper, (Any, Any)] = {
      new DecoderShape[RepColumnContent[Future[T], T], WrapHelper, (Any, Any)] {
        override type Data   = T
        override type Target = Future[T]
        override def wrapRep(base: RepColumnContent[Future[T], T]): Future[T] = base.rep
        override def toLawRep(base: Future[T], oldRep: WrapHelper): WrapHelper = {
          val either = oldRep.data.flatMap(s => base.map(d => s.right.map(s1 => (d, s1): (Any, Any)))(oldRep.ec))(oldRep.ec)
          WrapHelper(oldRep.ec, either)
        }
        override def takeData(rep: Future[T], oldData: (Any, Any)): SplitData[T, (Any, Any)] =
          SplitData(oldData._1.asInstanceOf[T], oldData._2.asInstanceOf[(Any, Any)])
      }
    }

    implicit def feImplicit4[T]: DecoderShape.Aux[RepColumnContent[T, T], T, T, WrapHelper, (Any, Any)] = {
      new DecoderShape[RepColumnContent[T, T], WrapHelper, (Any, Any)] {
        override type Data   = T
        override type Target = T
        override def wrapRep(base: RepColumnContent[T, T]): T = base.rep
        override def toLawRep(base: T, oldRep: WrapHelper): WrapHelper = {
          val either = oldRep.data.map(s => s.right.map(s1 => (base, s1): (Any, Any)))(oldRep.ec)
          WrapHelper(oldRep.ec, either)
        }
        override def takeData(rep: T, oldData: (Any, Any)): SplitData[T, (Any, Any)] =
          SplitData(oldData._1.asInstanceOf[T], oldData._2.asInstanceOf[(Any, Any)])
      }
    }
  }

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:run_test01;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

  case class Friend(i1: Long, i2: String, i3: String, i4: Int, i5: String, i6: Int)
  case class FriendSetter(i3: String, i4: Int)

  object ModelHelper {

    val i1 = 11L
    val i2 = Right("i2")

    @RootModel[FriendSetter]
    val i3_i4 = Future.successful(Right(FriendSetter(i3 = "i3", i4 = 44)))

    val i5 = Future.successful("iiiiiiii")
    val i6 = Right(66)

  }

  object Abc extends Helper {
    val ec   = scala.concurrent.ExecutionContext.global
    val data = fe.effect(fe.singleModel[Friend](ModelHelper).compile).data(ec)
  }

  println(await(Abc.data))

}
