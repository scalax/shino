package net.scalax.shino.test.umr.reader

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.umr.SlickResultIO
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, Future}

class Test11 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter with CustomRep {

  case class Friend(
      id: Long
    , i1: String = "i1"
    , i2: String = "i2"
    , i3: String = "i3"
    , i4: String = "i4"
    , i5: String = "i5"
    , i6: String = "i6"
    , i7: String = "i7"
    , i8: String = "i8"
    , i9: String = "i9"
    , i10: String = "i10"
    , i11: String = "i11"
    , i12: String = "i12"
    , i13: String = "i13"
    , i14: String = "i14"
    , i15: String = "i15"
    , i16: String = "i16"
    , i17: String = "i17"
    , i18: String = "i18"
    , i19: String = "i19"
    , i20: String = "i20"
    , i21: String = "i21"
    , i22: String = "i22"
    , i23: String = "i23"
    , i24: String = "i24"
    , i25: Int = 111111
    , i26: Int = 2222222
    , i27: Int = 33333333
    , i28: String = "i28"
    , i29: String = "i29"
    , i30: String = "i30"
  )

  case class SubFriend[U24, U25](
      id: Long
    , i1: String = "i1"
    , i24: U24
    , i25: U25
    , i26: Int = 2222222
    , i27: Int = 33333333
    , i28: String = "i28"
  )

  case class LawSubFriendTable[R24, U24, T24, R25, U25, T25](id: Rep[Long], i1: Rep[String], i24: R24, i25: R25, i26: Rep[Int], i27: Rep[Int], i28: Rep[String])(
      implicit val i24Shape: Shape[FlatShapeLevel, R24, U24, T24]
    , val i25Shape: Shape[FlatShapeLevel, R25, U25, T25]
  ) extends CustomTable[SubFriendTable[U24, T24, U25, T25], SubFriend[U24, U25]] {
    self =>
    implicit val i24Shape1 = i24Shape.packedShape
    implicit val i25Shape1 = i25Shape.packedShape

    override def customEncodeRef = sEncodeRef.effect(sEncodeRef.singleModel[SubFriendTable[U24, T24, U25, T25]](self).compile).toRef
    override def customNode      = sNodeGen.effect(sNodeGen.singleModel[SubFriend[U24, U25]](self).compile).toNodeWrap
  }

  case class SubFriendTable[U24, T24, U25, T25](id: Rep[Long], i1: Rep[String], i24: T24, i25: T25, i26: Rep[Int], i27: Rep[Int], i28: Rep[String])(
      implicit i24Shape: Shape[FlatShapeLevel, T24, U24, T24]
    , i25Shape: Shape[FlatShapeLevel, T25, U25, T25]
  ) extends CustomTable[SubFriendTable[U24, T24, U25, T25], SubFriend[U24, U25]] {
    self =>
    override def customEncodeRef = sEncodeRef.effect(sEncodeRef.singleModel[SubFriendTable[U24, T24, U25, T25]](self).compile).toRef
    override def customNode      = sNodeGen.effect(sNodeGen.singleModel[SubFriend[U24, U25]](self).compile).toNodeWrap
  }

  class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
    def id  = column[Long]("id", O.AutoInc)
    def i1  = column[String]("i1")
    def i2  = column[String]("i2")
    def i3  = column[String]("i3")
    def i4  = column[String]("i4")
    def i5  = column[String]("i5")
    def i6  = column[String]("i6")
    def i7  = column[String]("i7")
    def i8  = column[String]("i8")
    def i9  = column[String]("i9")
    def i10 = column[String]("i10")
    def i11 = column[String]("i11")
    def i12 = column[String]("i12")
    def i13 = column[String]("i13")
    def i14 = column[String]("i14")
    def i15 = column[String]("i15")
    def i16 = column[String]("i16")
    def i17 = column[String]("i17")
    def i18 = column[String]("i18")
    def i19 = column[String]("i19")
    def i20 = column[String]("i20")
    def i21 = column[String]("i21")
    def i22 = column[String]("i22")
    def i23 = column[String]("i23")
    def i24 = column[String]("i24")
    def i25 = column[Int]("i25")
    def i26 = column[Int]("i26")
    def i27 = column[Int]("i27")
    def i28 = column[String]("i28")
    def i29 = column[String]("i29")
    def i30 = column[String]("i30")

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
  }

  val friendTq = TableQuery[FriendTable]
  //val shinoFriendTq = ShinoTableQuery.fromTable(cons => new FriendTable(cons))

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:reader_test11;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

  override def beforeAll = {
    await(db.run(friendTq.schema.create))
  }

  val friend1 = Friend(id = -1)
  val friend2 = Friend(id = -1)
  val friend3 = Friend(id = -1)

  before {}

  after {
    await(db.run(friendTq.delete))
  }

  "shape" should "auto map with table and case class" in {

    val insert = friendTq.returning(friendTq.map(_.id))

    val friend1DBIO = insert += friend1
    val friend2DBIO = insert += friend2
    val friend3DBIO = insert += friend3

    val insertIds = await(db.run(DBIO.sequence(List(friend1DBIO, friend2DBIO, friend3DBIO))))

    val query1 = friendTq.filter(s => (s.id % 2L) === 0L).map(s => s.i7)

    val result1 = await(db.run(query1.result))

    result1.toList should be(List("i7"))

  }

}
