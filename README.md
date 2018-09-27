# shino

An auto mapper for scala [slick](https://github.com/slick/slick) base on [asuna](https://github.com/scalax/asuna).

How to get it
-------------

Add dependency

```scala
resolvers += Resolver.bintrayRepo("djx314", "maven")
libraryDependencies += "net.scalax" %% "shino" % "0.0.1-M1"
```

User guide
-------------

- Case 1  

Easily use it by mixing `SlickMapper` trait.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickMapper {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery[FriendTable]
```

Shino will automatically correspond to the properties of FriendTable and Friend. Then generate a value to fix `def * =`.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/Test01.scala)
&nbsp;  
&nbsp;  

- Case 2  

If you want to use `id.?` to map id column and donot want to change the original value. You can use the `OverrideProperty` annotation.

```scala
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
```

In `@OverrideProperty("id")` you can only use literal string parameter.  
[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/Test02.scala)
&nbsp;  
&nbsp;  

If you want to override the `id` property but you can't change the table for some reason(like you are using codegen). You can use the `RootTable` annotation.

```scala
case class Friend(id: Option[Long], name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Unit](tag, "firend") with SlickMapper {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = ()
}

class FriendTableExt(@(RootTable @field) val ft: FriendTable) extends Table[Friend](ft.tableTag, ft.tableName) with SlickMapper {
  def id         = ft.id.?
  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery(cons => new FriendTableExt(new FriendTable(cons)))
```

`RootTable` will promote all the properties of FriendTable to the root of FriendTableExt. But the properties defined in FriendTableExt will definitely override the properties defined in FriendTable.
&nbsp;  
Note that: You must use `friendTq.filter(_.ft.name like "myName*")` now.
