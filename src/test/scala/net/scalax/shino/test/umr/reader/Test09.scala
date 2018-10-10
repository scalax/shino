package net.scalax.shino.test.umr.reader

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.umr.SlickResultIO
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import slick.ast._
import slick.util.{ConstArray, ProductWrapper}

import scala.concurrent.{duration, Await, Future}
import scala.reflect.ClassTag

class Test09 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

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
    , i25: Int = 111111
    , i26: Int = 2222222
  )

  case class SubFriendTable(i1: Rep[String], i2: (Rep[String], Rep[String]), i25: Rep[Int], i26: Rep[Int])

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

    implicit val shape1: Shape[FlatShapeLevel, SubFriendTable, SubFriend, SubFriendTable] = {
      val stringShape = implicitly[Shape[FlatShapeLevel, Rep[String], String, Rep[String]]]
      val tupleShape  = implicitly[Shape[FlatShapeLevel, (Rep[String], Rep[String]), (String, String), (Rep[String], Rep[String])]]
      val intShape    = implicitly[Shape[FlatShapeLevel, Rep[Int], Int, Rep[Int]]]
      new Shape[FlatShapeLevel, SubFriendTable, SubFriend, SubFriendTable] {
        self =>
        override def pack(value: SubFriendTable): SubFriendTable =
          SubFriendTable(stringShape.pack(value.i1), tupleShape.pack(value.i2), intShape.pack(value.i25), intShape.pack(value.i26))
        override def packedShape: Shape[FlatShapeLevel, SubFriendTable, SubFriend, SubFriendTable] = self
        override def buildParams(extract: Any => SubFriend): SubFriendTable                        = throw new SlickException("Shape does not have the same Mixed and Packed type")
        override def encodeRef(value: SubFriendTable, path: Node): Any =
          SubFriendTable(
              stringShape.encodeRef(value.i1, Select(path, ElementSymbol(1))).asInstanceOf[Rep[String]]
            , tupleShape.encodeRef(value.i2, Select(path, ElementSymbol(2))).asInstanceOf[(Rep[String], Rep[String])]
            , intShape.encodeRef(value.i25, Select(path, ElementSymbol(3))).asInstanceOf[Rep[Int]]
            , intShape.encodeRef(value.i26, Select(path, ElementSymbol(4))).asInstanceOf[Rep[Int]]
          )
        override def toNode(value: SubFriendTable): Node = {
          val productNode = ProductNode(
              ConstArray.from(List(stringShape.toNode(value.i1), tupleShape.toNode(value.i2), intShape.toNode(value.i25), intShape.toNode(value.i26)))
          )
          def toBase(v: Any) = {
            val s = v.asInstanceOf[SubFriend]
            new ProductWrapper(IndexedSeq(s.i1, s.i2, s.i25, s.i26))
          }
          def toMapped(v: Any) = {
            val product = v.asInstanceOf[Product]
            SubFriend(
                product.productElement(0).asInstanceOf[String]
              , product.productElement(1).asInstanceOf[(String, String)]
              , product.productElement(2).asInstanceOf[Int]
              , product.productElement(3).asInstanceOf[Int]
            )
          }
          TypeMapping(productNode, MappedScalaType.Mapper(toBase, toMapped, None), implicitly[ClassTag[SubFriend]])
        }

      }
    }

    val query1: Query[SubFriendTable, SubFriend, Seq] = friendTq.map(s => SubFriendTable(i1 = s.i1, i2 = (s.i2, s.i3), i25 = s.i25, i26 = s.i26))

    val result1 = await(db.run(query1.result))

    result1.toList should be(
        List(
          SubFriend()
        , SubFriend()
        , SubFriend()
      )
    )

    val query2 = query1.map(s => (s.i2, s.i25))

    val result2 = await(db.run(query2.result))

    result2.toList should be(
        List(
          (("i2", "i3"), 111111)
        , (("i2", "i3"), 111111)
        , (("i2", "i3"), 111111)
      )
    )

    val action1 = await(db.run(query1.update(SubFriend(i2 = ("1234", "5678")))))

    val result3 = await(db.run(query1.result))

    result3.toList should be(
        List(
          SubFriend(i2 = ("1234", "5678"))
        , SubFriend(i2 = ("1234", "5678"))
        , SubFriend(i2 = ("1234", "5678"))
      )
    )

  }

}
