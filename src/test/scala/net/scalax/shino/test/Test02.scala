package net.scalax.shino.test

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.asuna.mapper.common.annotations.OverrideProperty
import net.scalax.shino.SlickMapper
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, Future}

class Test02 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

  case class Friend(id: Option[Long], name: String, nick: String, age: Int)

  class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickMapper {
    def id   = column[Long]("id", O.AutoInc)
    def name = column[String]("name")
    def nick = column[String]("nick")
    def age  = column[Int]("age")

    @OverrideProperty("id")
    def id_? = id.?

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
  }

  val friendTq = TableQuery[FriendTable]

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:test02;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

  override def beforeAll = {
    await(db.run(friendTq.schema.create))
  }

  val friend1 = Friend(Option.empty, faker.name.name, faker.weather.description, 23)
  val friend2 = Friend(Option.empty, faker.name.name, faker.weather.description, 26)
  val friend3 = Friend(Option.empty, faker.name.name, faker.weather.description, 20)

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
    val result    = await(db.run(friendTq.result))

    insertIds.size should be(3)
    insertIds.map { s =>
      (s > 0) should be(true)
    }
    result.toList.map(s => s.copy(id = Option.empty)) should be(List(friend1, friend2, friend3))
  }

}
