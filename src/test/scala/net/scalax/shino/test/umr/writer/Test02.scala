package net.scalax.shino.test.umr.writer

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.umr.SlickResultIO
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, Future}

class Test02 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

  case class Friend(id: Long, name: String, nick: String, age: Int)
  case class FriendSetter(name: String, nick: String)

  class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
    def id   = column[Long]("id", O.AutoInc)
    def name = column[String]("name")
    def nick = column[String]("nick")
    def age  = column[Int]("age")

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
  }

  class FriendTableToInsert(ft: FriendTable) extends SlickResultIO {
    def id = ft.id
    def set(name: String, age: Int) = {
      val setter1 = shinoInput.set(ft.name).to(name)
      val s = if (age > 300) {
        val setter2 = shinoInput.set(ft.age).to(age)
        shinoInput.effect(shinoInput.sequenceShapeValue(setter1, setter2))
      } else shinoInput.effect(setter1)
      s.shape
    }
  }

  val friendTq = TableQuery[FriendTable]

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:writer_test02;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

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
    case class NameWithAge(name: String, age: Int)
    val nameWithAge = List(NameWithAge("a1", 234), NameWithAge("a2", 322), NameWithAge("a3", 477))

    val actions =
      insertIds.zip(nameWithAge).map { case (id, na) => friendTq.filter(_.id === id).map(s => new FriendTableToInsert(s).set(na.name, na.age)).update(()) }
    val updateAction = await(db.run(DBIO.sequence(actions)))

    val result = await(db.run(friendTq.sortBy(_.id).result))

    insertIds.size should be(3)
    insertIds.map { s =>
      (s > 0) should be(true)
    }
    result.toList.map(s => s.copy(id = -1)) should be(
        List(friend1.copy(name = "a1"), friend2.copy(name = "a2", age = 322), friend3.copy(name = "a3", age = 477))
    )
  }

}
