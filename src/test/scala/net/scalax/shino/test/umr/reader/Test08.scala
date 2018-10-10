package net.scalax.shino.test.umr.reader

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.umr.SlickResultIO
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, Future}

class Test08 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

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
  )

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

  class SubFriendTable(tag: slick.lifted.Tag, ft: FriendTable) extends Table[Unit](ft.tableTag, ft.tableName) with SlickResultIO {
    def i1  = ft.i1
    def i2  = ft.i2
    def i3  = ft.i3
    def i4  = ft.i4
    def i5  = ft.i5
    def i6  = ft.i6
    def i7  = ft.i7
    def i8  = ft.i8
    def i9  = ft.i9
    def i10 = ft.i10
    def i11 = ft.i11
    def i12 = ft.i12
    def i13 = ft.i13
    def i14 = ft.i14
    def i15 = ft.i15
    def i16 = ft.i16
    def i17 = ft.i17
    def i18 = ft.i18
    def i19 = ft.i19
    def i20 = ft.i20
    def i21 = ft.i21
    def i22 = ft.i22
    def i23 = ft.i23
    def i24 = ft.i24
    def i25 = ft.i25
    def i26 = ft.i26

    override def * = ()
  }

  object SubFriendTable {
    def apply(ft: FriendTable): SubFriendTable = TableQuery(cons => new SubFriendTable(cons, ft)).baseTableRow
  }

  val friendTq = TableQuery[FriendTable]

  object Wrap extends SlickResultIO {
    def toShape1(sf: SubFriendTable) = {
      shino.effect(shino.singleModel[SubFriend](sf).compile).shape
    }
  }

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:reader_test08;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

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

    val query1: Query[SubFriendTable, Unit, Seq]   = friendTq.map(s => SubFriendTable(s))
    val subFriendQuery: Query[Any, SubFriend, Seq] = query1.map(s => Wrap.toShape1(s))
    val query2                                     = query1.map(s => (s.i2, s.i3))

    val result  = await(db.run(subFriendQuery.result))
    val result2 = await(db.run(query2.result))

    result.toList should be(
        List(
          SubFriend()
        , SubFriend()
        , SubFriend()
      )
    )

    result2.toList should be(
        List(
          ("i2", "i3")
        , ("i2", "i3")
        , SubFriend()
      )
    )

  }

}
