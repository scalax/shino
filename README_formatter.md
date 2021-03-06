Formatter user guide
-------------

- Case 1  

Easily use it by mixing `SlickResultIO` trait.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery[FriendTable]
```

Shino will automatically correspond to the properties of FriendTable and Friend. Then generate a value to fix `def * =`.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/formatter/Test01.scala)
&nbsp;  
&nbsp;  

- Case 2  

If you want to use `id.?` to map id column and donot want to change the original value. You can use the `OverrideProperty` annotation.

```scala
case class Friend(id: Option[Long], name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
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

Note: In `@OverrideProperty("id")` you can only use literal string parameter.  

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/formatter/Test02.scala)
&nbsp;  
&nbsp;  

- Case 3  

If you want to override the `id` property but you can't change the table for some reason(like you are using codegen). You can use the `RootTable` annotation.

```scala
case class Friend(id: Option[Long], name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
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

val friendTq = TableQuery[FriendTable]
```

`RootTable` will promote all the properties of FriendTable to the root of FriendTableExt. But the properties defined in FriendTableExt will definitely override the properties defined in FriendTable.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/formatter/Test03.scala)
&nbsp;  
&nbsp;  

- Case 4  

You can use `shino.shaped` to lift your column. Then you can use method `fmap` and `fzip` to manipulate the columns.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = shino.shaped(column[String]("name")).fmap(s => "user name:" + s)(t => t)
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery[FriendTable]
```
Shino can map column many times. No need to worry about this [issue](https://github.com/slick/slick/issues/1894).

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/formatter/Test04.scala)
&nbsp;  
&nbsp;  

- Case 5  

If column name, nick, age does not require filters, sortby and so on. They only need to be select, insert and update. You can mixin `ColumnHelper` and override `columnGenerator`. Then you no need to define methods such as name, nick, age.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO with ColumnHelper {

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

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/formatter/Test05.scala)
