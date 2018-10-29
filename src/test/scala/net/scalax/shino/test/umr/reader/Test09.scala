package net.scalax.shino.test.umr.reader

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.umr.SlickResultIO
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, Future}

class Test09 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter with CustomRep {

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

  case class SubFriend(
      i1: String = "i1"
    , i2: (String, String) = ("i2", "i3")
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
  )

  case class SubFriendTable(
      i1: Rep[String]
    , i2: (Rep[String], Rep[String])
    , i28: Rep[String]
    , i27: Rep[Int]
    , i26: Rep[Int]
    , i25: Rep[Int]
    , i24: Rep[String]
    , i23: Rep[String]
    , i22: Rep[String]
    , i21: Rep[String]
    , i20: Rep[String]
    , i19: Rep[String]
    , i18: Rep[String]
    , i17: Rep[String]
    , i16: Rep[String]
    , i15: Rep[String]
    , i14: Rep[String]
    , i13: Rep[String]
    , i12: Rep[String]
    , i11: Rep[String]
    , i10: Rep[String]
    , i9: Rep[String]
    , i8: Rep[String]
    , i7: Rep[String]
    , i6: Rep[String]
    , i5: Rep[String]
    , i4: Rep[String]
  ) extends CustomTable[SubFriendTable, SubFriend] {
    self =>

    override def customEncodeRef = sEncodeRef.effect(sEncodeRef.singleModel[SubFriendTable](self).compile).toRef
    override def customNode      = sNodeGen.effect(sNodeGen.singleModel[SubFriend](self).compile).toNodeWrap

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

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:reader_test09;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

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

    val query1: Query[(Rep[Long], SubFriendTable), (Long, SubFriend), Seq] =
      friendTq.map { s =>
        val abc = SubFriendTable(
            i1 = s.i1
          , i2 = (s.i2, s.i3)
          , i28 = s.i28
          , i27 = s.i27
          , i26 = s.i26
          , i25 = s.i25
          , i24 = s.i24
          , i23 = s.i23
          , i22 = s.i22
          , i21 = s.i21
          , i20 = s.i20
          , i19 = s.i19
          , i18 = s.i18
          , i17 = s.i17
          , i16 = s.i16
          , i15 = s.i15
          , i14 = s.i14
          , i13 = s.i13
          , i12 = s.i12
          , i11 = s.i11
          , i10 = s.i10
          , i9 = s.i9
          , i8 = s.i8
          , i7 = s.i7
          , i6 = s.i6
          , i5 = s.i5
          , i4 = s.i4
        )

        (
            s.id
          , abc
        )
      }

    val result1 = await(db.run(query1.result))

    result1.toList should be(
        List(
          (1, SubFriend())
        , (2, SubFriend())
        , (3, SubFriend())
      )
    )

    val query2 = query1.map(s => (s._2.i2._2, s._2.i25))

    val result2 = await(db.run(query2.result))

    result2.toList should be(
        List(
          ("i3", 111111)
        , ("i3", 111111)
        , ("i3", 111111)
      )
    )

    val action1 = await(db.run(query1.map(_._2).update(SubFriend(i2 = ("1234", "5678")))))

    val result3 = await(db.run(query1.map(_._2).result))

    result3.toList should be(
        List(
          SubFriend(i2 = ("1234", "5678"))
        , SubFriend(i2 = ("1234", "5678"))
        , SubFriend(i2 = ("1234", "5678"))
      )
    )

  }

}
