package net.scalax.shino.test.umr.reader

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.shino.umr.SlickResultIO
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.{duration, Await, Future}

class Test06 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

  case class Friend(id: Long, name: String, nick: String, age: Int)

  class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
    def id   = column[Long]("id", O.AutoInc)
    def name = column[String]("name")
    def nick = column[String]("nick")
    def age  = column[Int]("age")

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

  }

  case class FriendReader(id: Long, name: String, nick: String, age: Int, subFriends: List[String])

  case class FriendId(id: Long)
  case class SubFriends(subFriends: List[String])

  class FriendTableToSelect(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
    def id   = column[Long]("id", O.AutoInc)
    def name = column[String]("name")
    def nick = column[String]("nick")
    def age  = column[Int]("age")

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

    def reader = shinoOutput.effect(shinoOutput.lazyModel[SubFriends, FriendReader, FriendId](this).compile).shape
  }

  val friendTq         = TableQuery[FriendTable]
  val friendTqToSelect = TableQuery[FriendTableToSelect]

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:reader_test06;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

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

    val friendMap = Map((1L, List("aa", "bb")), (2L, List("cc", "dd", "ee")), (3L, List("ff")))

    def getSubFriends(id: Long): SubFriends = {
      SubFriends(friendMap.get(id).getOrElse(List.empty))
    }

    val result = await(db.run(friendTqToSelect.sortBy(_.id).map(_.reader).result))

    result.size should be(3)
    result.map { s =>
      (s.sub.id > 0) should be(true)
    }

    result.toList.map(a => a(getSubFriends(a.sub.id))) should be(
        List(
          FriendReader(1, friend1.name, friend1.nick, friend1.age, List("aa", "bb"))
        , FriendReader(2, friend2.name, friend2.nick, friend2.age, List("cc", "dd", "ee"))
        , FriendReader(3, friend3.name, friend3.nick, friend3.age, List("ff"))
      )
    )
  }

}
