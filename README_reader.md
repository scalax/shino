Reader user guide
-------------

- Case 1  

Easily use it by mixing `SlickResultIO` trait.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)
case class FriendReader(name: String, nick: String)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

  def reader = shinoOutput.effect(shinoOutput.singleModel[FriendReader](this).compile).shape
}

val friendTq = TableQuery[FriendTable]
friendTq.sortBy(_.id).map(_.reader).result // DBIO[Seq[FriendReader]]
```

shinoOutput will automatically correspond to the properties of FriendTable and FriendReader. Then generate a read only `MappedProjection[FriendReader, Any]`.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/reader/Test01.scala)
&nbsp;  
&nbsp;  

- Case 2  

You can use `OverrideProperty` annotation like formatter to prevent attribute name conflicts.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)
case class FriendGetter(name: String, nick: String, tableProvider: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def tableProvider = super.tableProvider // Already used.

  @OverrideProperty(name = "tableProvider")
  def nameLength = name.length

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

  def reader = shinoOutput.effect(shinoOutput.singleModel[FriendGetter](this).compile).shape

}

val friendTq = TableQuery[FriendTable]
friendTq.map(_.reader).result // DBIO[Seq[FriendGetter]]
```

Note: In `@OverrideProperty("tableProvider")` you can only use literal string parameter.  

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/reader/Test02.scala)
&nbsp;  
&nbsp;   

- Case 3  

You can use `shinoOutput.shaped` to lift your column. Then you can use method `dmap` and `dzip` to manipulate the columns.

```scala
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

friendTqOutput.sortBy(_.id).map(_.getter).to[List].result // DBIO[List[Friend]]
```
Shino can map column many times. No need to worry about this [issue](https://github.com/slick/slick/issues/1894).

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/reader/Test03.scala)
&nbsp;  
&nbsp;  

- Case 4  

If the value of columnA depends on columnB, but columnB also needs to be evaluated separately. You can use `RootModel` to avoid selecting columnB twice. But you need to define a case class with the same fields as the original case class first.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)
case class NameAndAge(name: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

class FriendTableToInsert(@(RootTable @getter) val ft: FriendTable) extends SlickResultIO {
  @RootModel[NameAndAge]
  def nameAndAge = shinoOutput.shaped(ft.name).dzip(shinoOutput.shaped(ft.age)).dmap { case (name, age) => NameAndAge(s"${name}(law age: ${age})", age + 1) }
  val getter     = shinoOutput.effect(shinoOutput.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery[FriendTable]

friendTq.sortBy(_.id).map(s => new FriendTableToInsert(s).getter).to[List].result // DBIO[List[Friend]] with name and age field changed
```

Note that the annotation has expected you to get the val of type `NameAndAge`. It can be either Rep[NameAndAge] or a value that is manipulated by `shinoOutput.shaped`.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/reader/Test04.scala)
&nbsp;  
&nbsp;  

- Case 5  

If column name, nick, age does not require filters, sortby and so on. They only need to be select, insert and update. You can mixin `ColumnHelper` and override `columnGenerator`. Then you no need to define methods such as name, nick, age.

```scala
class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO with ColumnHelper {

  def id   = column[Long]("id", O.AutoInc)
  def name = Placeholder.value[String]

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

  val getter = shinoOutput.effect(shinoOutput.singleModel[Friend](this).compile).shape

  override def columnGenerator[D](name: String, typedType: TypedType[D]): Rep[D] = {
    val newName = name match {
      case "age" => "age_ext"
      case r     => r
    }
    column(newName)(typedType)
  }

}

val friendTq = TableQuery[FriendTable]
```

Note:
- If you must override existing property(like `name` here). You can use `Placeholder.value[String]` to get the same behavior explicitly.
- Column id still use `def id`. So if you want to map a specific column, just defining a same name property.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/reader/Test05.scala)
