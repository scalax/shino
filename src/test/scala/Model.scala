package net.scalax.asuna.slick.simple

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.SlickMapper
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import slick.lifted.ProvenShape

import scala.concurrent.{duration, Await, Future}

case class Friends2(id: Long, name: String, nick: String, age: Int)

class FriendTable2(tag: slick.lifted.Tag) extends Table[Friends2](tag, "firend2") with SlickMapper {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * : ProvenShape[Friends2] = umr.effect(umr.modelOnly[Friends2](this).compile).shape
}

case class Mark(id: Long, name: String, mark: Int, friendId: Long)

class MarkTable(tag: slick.lifted.Tag) extends Table[Mark](tag, "mark") with SlickMapper {
  def id       = column[Long]("id", O.AutoInc)
  def name     = column[String]("name")
  def mark     = column[Int]("mark")
  def friendId = column[Long]("friend_id")

  override def * : ProvenShape[Mark] = umr.effect(umr.modelOnly[Mark](this).compile).shape
}

class DynModel extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val friendTq2 = TableQuery[FriendTable2]
  val markTq    = TableQuery[MarkTable]

  val db = Database.forURL(s"jdbc:h2:mem:hfTest;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

  override def beforeAll = {
    await(db.run((markTq.schema ++ friendTq2.schema).create))
  }

  before {
    val friend1 = Friends2(-1, faker.name.name, faker.weather.description, 23)
    val friend2 = Friends2(-1, faker.name.name, faker.weather.description, 26)
    val friend3 = Friends2(-1, faker.name.name, faker.weather.description, 20)

    val insert = friendTq2.returning(friendTq2.map(_.id)).into((friend, id) => friend.copy(id = id))

    val friend1DBIO = insert += friend1
    val friend2DBIO = insert += friend2
    val friend3DBIO = insert += friend3

    await(db.run(DBIO.sequence(List(friend1DBIO, friend2DBIO, friend3DBIO))))
    val result = await(db.run(friendTq2.result))

    var i = 36
    var j = 2
    val mark = result.map(_.id).map { id =>
      val subList = for (_ <- 0 until j) yield {
        Mark(id = -1, name = faker.address.cityName, mark = { i += 10; i }, friendId = id)
      }
      j += 1
      subList
    }
    await(db.run(markTq ++= mark.flatten.toList))
  }

  after {
    await(db.run(friendTq2.delete))
  }

  "shape" should "auto filer with case class" in {}

}
