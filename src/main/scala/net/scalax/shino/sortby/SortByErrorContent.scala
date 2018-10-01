package net.scalax.shino.umr

import net.scalax.shino.sortby.NullsOrdering

trait AllowMap {
  def allowMap: Map[String, NullsOrdering]

  override def toString: String = {
    allowMap
      .map {
        case (key, value) =>
          "\n" + s"""${key}:
         |${value.toString}""".stripMargin
      }
      .mkString("\n")
  }
}

trait SortByErrorContent {
  def key: String
  def direction: String
  def nullsParameter: String
  def allowMap: AllowMap
  override def toString: String = {
    s"""Error sort by parameter:
       |======================================================
       |key: ${key}
       |direction parameter: ${direction}
       |nulls parameter: ${nullsParameter}
       |allow parameters:
       |${allowMap.toString}
       |======================================================
     """.stripMargin
  }
}

case class SortByErrorContentImpl(
    override val key: String
  , override val direction: String
  , override val nullsParameter: String
  , override val allowMap: AllowMap
) extends SortByErrorContent {
  override def toString: String = super.toString
}
