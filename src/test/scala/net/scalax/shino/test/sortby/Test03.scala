package net.scalax.shino.test.sortby

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.sortby.{NullsOrdering, OrderingWrap, SortBy}
import net.scalax.shino.umr._
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import slick.ast.TypedType

import scala.concurrent.{duration, Await, Future}

class Test03 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

  case class Friend(id: Long, name: String, nick: String, age: Int)
  case class FriendSort(name: NullsOrdering = SortBy.default, age: OrderingWrap[Int] = SortBy.default.tagged[Int])
  object FriendSort {
    val value: FriendSort = apply()
  }

  class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO with SortByMapper with AutoSortByHelper with ColumnHelper {

    def id   = column[Long]("id", O.AutoInc)
    def name = column[String]("name")

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

    def orderDef = sortby.effect(sortby.singleModel[FriendSort](this).compile).inputData(FriendSort.value)

    override def sortByColumnGenerator[D](name: String, typedType: TypedType[D]): Rep[D] = column(name)(typedType)

    override def columnGenerator[D](name: String, typedType: TypedType[D]): Rep[D] = column(name)(typedType)
  }

  val friendTq = TableQuery[FriendTable]

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:sortby_test03;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

  override def beforeAll = {
    await(db.run(friendTq.schema.create))
  }

  val friend1 = Friend(-1, faker.name.name, faker.weather.description, 23)
  val friend2 = Friend(-1, faker.name.name, faker.weather.description, 26)
  val friend3 = Friend(-1, faker.name.name, faker.weather.description, 20)

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

    val result = await(db.run(friendTq.sortBy { s =>
      val i    = s.orderDef
      val sOpt = SortBy.strictMutiplySort(i.strictSort("age", SortBy.DESC, SortBy.NULLS_LAST))
      sOpt match {
        case Right(s) => s
        case Left(e)  => throw new IllegalArgumentException(e.toString)
      }
    }.result))

    insertIds.size should be(3)
    insertIds.map { s =>
      (s > 0) should be(true)
    }
    result.toList.map(s => s.copy(id = -1)) should be(List(friend1, friend2, friend3).sortBy(_.age).reverse)
  }

}
