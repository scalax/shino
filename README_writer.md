Writter user guide
-------------

- Case 1  

Easily use it by mixing `SlickResultIO` trait.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)
case class FriendSetter(name: String, nick: String)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape

  def setter = shino.effect(shino.singleModel[FriendSetter](this).compile).shape
}

val friendTq = TableQuery[FriendTable]

friendTq.filter(s => (s.id % 2L) === 1L).map(_.setter).update(FriendSetter(name = "namenamename", nick = "miaomiaomiao")) // Update action
```

Shino will automatically correspond to the properties of FriendTable and FriendSetter. Then generate a Then generate a write only `MappedProjection[FriendSetter, Any]`.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/writer/Test01.scala)
&nbsp;  
&nbsp;  

- Case 2  

You can use to decide if the column needs to be updated at runtime.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)
case class FriendSetter(name: String, nick: String)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

class FriendTableToInsert(ft: FriendTable) extends SlickResultIO {
  def id = ft.id
  def set(name: String, age: Int) = {
    val setter1 = shinoInput.set(ft.name).to(name)
    val s = if (age > 300) {
      val setter2 = shinoInput.set(ft.age).to(age)
      shinoInput.effect(shinoInput.sequenceShapeValue(setter1, setter2))
    } else
        shinoInput.effect(setter1) // not to update age column
    s.shape
  }
}

val friendTq = TableQuery[FriendTable]

val nameWithAge = List(NameWithAge("a1", 234), NameWithAge("a2", 322), NameWithAge("a3", 477))

friendTq.filter(_.id === yourFriendId).map(s => new FriendTableToInsert(s).set(na.name, na.age)).update(())
```

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/writer/Test02.scala)
&nbsp;  
&nbsp;  

- Case 3  

You can use `shinoInput.shaped` to lift your column. Then you can use method `emap` and `ezip` to manipulate the columns.

```scala
case class Friend(id: Long, name: String, nick: String, age: Int)

class FriendTable(tag: slick.lifted.Tag) extends Table[Friend](tag, "firend") with SlickResultIO {
  def id   = column[Long]("id", O.AutoInc)
  def name = column[String]("name")
  def nick = column[String]("nick")
  def age  = column[Int]("age")

  override def * = shino.effect(shino.singleModel[Friend](this).compile).shape
}

class FriendTableToInsert(tag: slick.lifted.Tag) extends FriendTable(tag) with SlickResultIO {
  @OverrideProperty(name = "age")
  def ageExt = shinoInput.shaped(column[Int]("age")).emap[Int](s => s + 1234)
  def setter = shinoInput.effect(shinoInput.singleModel[Friend](this).compile).shape
}

val friendTq         = TableQuery[FriendTable]
val friendTqToInsert = TableQuery[FriendTableToInsert]

val insert = friendTqToInsert.map(_.setter).returning(friendTqToInsert.map(_.id))

val friend1DBIO = insert += friend1 // change age to age + 1234 and then insert.
```
Shino can map column many times. No need to worry about this [issue](https://github.com/slick/slick/issues/1894).

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/writer/Test03.scala)
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

trait FriendTableToInsert extends SlickResultIO {
  @(RootTable @getter)
  val ft: FriendTable
  @RootModel[NameAndAge]
  def nameAndAge = shinoInput.shaped(ft.name).ezip(shinoInput.shaped(ft.age)).emap[NameAndAge](s => (s"${s.name}(law age: ${s.age})", s.age + 1))
  def setter     = shinoInput.effect(shinoInput.singleModel[Friend](this).compile).shape
}

val friendTq = TableQuery[FriendTable]

val insert = friendTq.map(s => new FriendTableToInsert { override val ft = s }.setter).returning(friendTq.map(_.id))

val friend1DBIO = insert += friend1 // change name and age field and then insert.
```

Note that the annotation has expected you to get the val of type `NameAndAge`. It can be either Rep[NameAndAge] or a value that is manipulated by `shinoInput.shaped`.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/writer/Test04.scala)
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

  val setter = shinoInput.effect(shinoInput.singleModel[Friend](this).compile).shape

  override def columnGenerator[D](name: String, typedType: TypedType[D]): Rep[D] = {
    val newName = name match {
      case "age" => "age_ext"
      case r     => r
    }
    column(newName)(typedType)
  }

}

val friendTq = TableQuery[FriendTable]

friendTq.sortBy(_.id).to[List].result // DBIO[List[Friend]]
```

Note:
- If you must override existing property(like `name` here). You can use `Placeholder.value[String]` to get the same behavior explicitly.
- Column id still use `def id`. So if you want to map a specific column, just defining a same name property.

[Test case](https://github.com/scalax/shino/blob/master/src/test/scala/net/scalax/shino/test/umr/writer/Test05.scala)
