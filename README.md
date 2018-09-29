# shino

An auto mapper for scala [slick](https://github.com/slick/slick) base on [asuna](https://github.com/scalax/asuna). Type safe, type driven, no runtime reflection.

How to get it
-------------

Add dependency

```scala
resolvers += Resolver.bintrayRepo("djx314", "maven")
libraryDependencies += "net.scalax" %% "shino" % "0.0.1-M1"
```

Can I use it in production?
-------------
Nope. Since the mapping rules in asuna is not stable.

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

- Case 3  

If you want to override the `id` property but you can't change the table for some reason(like you are using codegen). You can use the `RootTable` annotation.

```scala
case class Friend(id: Option[Long], name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickMapper {
  self =>

  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * =
    shino.effect(shino.singleModel[Friend](new FriendTableExt { override val ft = self }: FriendTableExt).compile).shape

}

trait FriendTableExt {

  @RootTable val ft: FriendTable

  def id = ft.id.?

}

val friendTq2 = TableQuery[FriendTable]
```

`RootTable` will promote all the properties of FriendTable to the root of FriendTableExt. But the properties defined in FriendTableExt will definitely override the properties defined in FriendTable.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/Test03.scala)
&nbsp;  
&nbsp;  

- Case 4  

You can use `shino.wrap` to lift your column. Then you can use method `map` and `zip` to manipulate the columns.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickMapper {
  def id   = column[Long]("id", O.AutoInc)
  def name = shino.shaped(column[String]("name")).fmap(s => "user name:" + s)(t => t)
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery[FriendTable]
```
Shino can map column many times. No need to worry about this [issue](https://github.com/slick/slick/issues/1894).

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/Test04.scala)
&nbsp;  
&nbsp;  

- Case 5  

If the value of columnA depends on columnB, but columnB also needs to be evaluated separately. You can use `RootModel` to avoid selecting columnB twice. But you need to define a case class with the same fields as the original case class first.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)
case class NameAndAge(name: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickMapper {
  def id       = column[Long]("id", O.AutoInc)
  def name_ext = column[String]("name")
  def nick     = column[String]("nick")
  def age_ext  = column[Int]("age")

  @RootModel[NameAndAge]
  def name_age =
    shino
      .wrap(name_ext)
      .zip(shino.wrap(age_ext))
      .map { case (name, age) => NameAndAge("user name:" + name + ", age:" + age, age) }(t => Option((t.name, t.age)))

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery[FriendTable]
```

Note that the annotation has expected you to get the val of type `NameAndAge`. It can be either Rep[NameAndAge] or a value that is manipulated by `shino.wrap`.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/Test05.scala)
&nbsp;  
&nbsp;  

- Case 6  

If column name, nick, age does not require filters, sortby and so on. They only need to be select, insert and update. You can mixin `ColumnHelper` and override `columnGenerator`. Then you no need to define methods such as name, nick, age.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickMapper with ColumnHelper {

  def id   = column[Long]("id", O.AutoInc)
  def name = Placeholder.value[String]

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

  override def columnGenerator[D](name: String, typedType: TypedType[D]): Rep[D] = {
    val newName = toSnakeName(name)
    column(newName)(typedType)
  }

}

val friendTq = TableQuery[FriendTable]
```

Note:
- If you must override existing property(like `name` here). You can use `Placeholder.value[String]` to get the same behavior explicitly.
- Column id still use `def id`. So if you want to map a specific column, just defining a same name property.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/Test06.scala)
