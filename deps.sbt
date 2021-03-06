resolvers += Resolver.bintrayRepo(owner = "djx314", repo = "maven")
resolvers += Resolver.sonatypeRepo("releases")

//libraryDependencies += "net.scalax" %% "asuna-mapper" % "0.0.2-SNAP20181022.1"

val slickVersion = "3.2.3"

libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion

libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion exclude ("com.zaxxer", "HikariCP-java6")

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test

libraryDependencies += "com.h2database" % "h2" % "1.4.197" % Test

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25" % Test

libraryDependencies += "com.github.javafaker" % "javafaker" % "0.15" % Test

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
    "io.circe" %% "circe-core"
  , "io.circe" %% "circe-generic"
  , "io.circe" %% "circe-parser"
).map(_ % circeVersion % Test)

dependsOn(RootProject(file("..") / "asuna"))
