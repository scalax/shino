package net.scalax.shino.test

import java.util.Locale

import com.github.javafaker.Faker
import net.scalax.asuna.core.common.Placeholder
import net.scalax.shino.{ColumnHelper, SlickMapper}
import slick.jdbc.H2Profile.api._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import slick.ast.TypedType

import scala.concurrent.{duration, Await, Future}

class Test06 extends FlatSpec with Matchers with EitherValues with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter {

  case class Friend(id: Long, name: String, nick: String, age: Int)

  class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickMapper with ColumnHelper {
    
    def id   = column[Long]("id", O.AutoInc)
    def name = Placeholder.value[String]

    override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

    override def columnGenerator[D](name: String, typedType: TypedType[D]): Rep[D] = {
      val newName = name match {
        case "age" => "age_ext"
        case r     => r
      }
      column(newName)(typedType)
    }
    
  }

  val friendTq2 = TableQuery[FriendTable]

  val local = new Locale("zh", "CN")
  val faker = new Faker(local)

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  val logger = LoggerFactory.getLogger(getClass)

  val db = Database.forURL(s"jdbc:h2:mem:test06;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", keepAliveConnection = true)

  override def beforeAll = {
    await(db.run(friendTq2.schema.create))
  }

  val friend1 = Friend(-1, faker.name.name, faker.weather.description, 23)
  val friend2 = Friend(-1, faker.name.name, faker.weather.description, 26)
  val friend3 = Friend(-1, faker.name.name, faker.weather.description, 20)

  before {}

  after {
    await(db.run(friendTq2.delete))
  }

  "shape" should "auto map with table and case class" in {
    val insert = friendTq2.returning(friendTq2.map(_.id))

    val friend1DBIO = insert += friend1
    val friend2DBIO = insert += friend2
    val friend3DBIO = insert += friend3

    val insertIds = await(db.run(DBIO.sequence(List(friend1DBIO, friend2DBIO, friend3DBIO))))
    val result    = await(db.run(friendTq2.result))

    insertIds.size should be(3)
    insertIds.map { s =>
      (s > 0) should be(true)
    }
    result.toList.map(s => s.copy(id = -1)) should be(List(friend1, friend2, friend3))
  }

}
