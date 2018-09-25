resolvers += Resolver.bintrayRepo(owner = "djx314",repo="releases")
resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "net.scalax" %% "asuna-mapper" % "0.0.1-M3"
)

val slickVersion = "3.2.3"

libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion

libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion exclude ("com.zaxxer", "HikariCP-java6")

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test

libraryDependencies += "com.h2database" % "h2" % "1.4.197" % Test

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25" % Test

libraryDependencies += "com.github.javafaker" % "javafaker" % "0.15" % Test