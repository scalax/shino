package net.scalax.shino.test.umr.reader

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.asuna.mapper.common.annotations.OverrideProperty
import net.scalax.shino.umr.SlickResultIO
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, Future}

class Test03 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

  case class Friend(id: Long, name: String, nick: String, age: Int)

  class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
    def id   = column[Long]("id", O.AutoInc)
    def name = column[String]("name")
    def nick = column[String]("nick")
    def age  = column[Int]("age")

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
  }

  class FriendTableToOutput(tag: slick.lifted.Tag) extends FriendTable(tag) with SlickResultIO {
    @OverrideProperty(name = "age")
    def ageExt = shinoOutput.shaped(age).dmap(s => s + 1234)
    val getter = shinoOutput.effect(shinoOutput.singleModel[Friend](this).compile).shape
  }

  val friendTq       = TableQuery[FriendTable]
  val friendTqOutput = TableQuery[FriendTableToOutput]

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:reader_test03;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

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

    val result = await(db.run(friendTqOutput.sortBy(_.id).map(_.getter).to[List].result))

    insertIds.size should be(3)
    insertIds.map { s =>
      (s > 0) should be(true)
    }
    result.map(s => s.copy(id = -1)) should be(List(friend1.copy(age = 23 + 1234), friend2.copy(age = 26 + 1234), friend3.copy(age = 20 + 1234)))
  }

}
