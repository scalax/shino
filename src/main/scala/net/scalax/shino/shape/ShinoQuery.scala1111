package net.scalax.shino.shape

import slick.ast.Node
import slick.dbio.{Effect, NoStream}
import slick.jdbc.JdbcProfile
import slick.lifted._
import slick.sql.FixedSqlAction

import scala.language.higherKinds

trait ShinoQuery[E, U, C[_]] extends Rep[U] {
  self =>

  def law: Query[E, U, C]

  override def encodeRef(path: Node): ShinoQuery[E, U, C] = {
    ShinoQuery.fromQuery(law.encodeRef(path))
  }

  override def toNode: Node = law.toNode

  def flatMap[F, T, D[_]](f: E => ShinoQuery[F, T, D]): ShinoQuery[F, T, C] = ShinoQuery.fromQuery(self.law.flatMap(s => f(s).law))

  def map[F, G, T](f: E => F)(implicit shape: ShinoShape.Aux[ShapePoly, F, T, G]): ShinoQuery[G, T, C] =
    ShinoQuery.fromQuery(self.law.map(s => f(s))(shape.toLaw))

  def filter[T <: Rep[_]](f: E => T)(implicit wt: CanBeQueryCondition[T]): ShinoQuery[E, U, C]    = ShinoQuery.fromQuery(law.filter(f)(wt))
  def filterNot[T <: Rep[_]](f: E => T)(implicit wt: CanBeQueryCondition[T]): ShinoQuery[E, U, C] = ShinoQuery.fromQuery(law.filterNot(f)(wt))
  def withFilter[T: CanBeQueryCondition](f: E => T): ShinoQuery[E, U, C]                          = ShinoQuery.fromQuery(law.withFilter(f)(implicitly[CanBeQueryCondition[T]]))

  def result(implicit profile: JdbcProfile): profile.StreamingProfileAction[C[U], U, Effect.Read] =
    profile.api.streamableQueryActionExtensionMethods(law).result

  def insert(data: U)(implicit profile: JdbcProfile): FixedSqlAction[Int, NoStream, Effect.Write] =
    profile.api.queryInsertActionExtensionMethods(law).+=(data)

  def insertAll(data: Iterable[U])(implicit profile: JdbcProfile): FixedSqlAction[Option[Int], NoStream, Effect.Write] =
    profile.api.queryInsertActionExtensionMethods(law).++=(data)

  def update(data: U)(implicit profile: JdbcProfile): FixedSqlAction[Int, NoStream, Effect.Write] =
    profile.api.queryUpdateActionExtensionMethods(law).update(data)

}

object ShinoQuery {

  def fromQuery[F, T, C[_]](query: Query[F, T, C]): ShinoQuery[F, T, C] = new ShinoQuery[F, T, C] { override val law = query }

}

trait ShinoTableQuery[E <: AbstractTable[_]] extends ShinoQuery[E, E#TableElementType, Seq] {

  override def law: TableQuery[E]
  lazy val shaped: ShapedValue[E, E#TableElementType] = law.shaped
  override lazy val toNode: Node                      = law.toNode
  def baseTableRow: E                                 = law.baseTableRow

}

object ShinoTableQuery {

  def fromTable[E <: AbstractTable[_]](cons: Tag => E): ShinoTableQuery[E] = new ShinoTableQuery[E] { override val law = TableQuery(tag => cons(tag)) }

}
