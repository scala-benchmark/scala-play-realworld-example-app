name := """real-world-example-project"""

version := "1.0"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.1"

javacOptions ++= Seq("-source", "11", "-target", "11")

libraryDependencies ++= Seq(
  filters,
  evolutions,
  ws,
  ehcache,
  cacheApi,
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.julienrf" %% "play-json-derived-codecs" % "7.0.0",
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  "commons-validator" % "commons-validator" % "1.6",
  "com.github.slugify" % "slugify" % "2.4",
  "com.h2database" % "h2" % "1.4.200",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.apache.commons" % "commons-lang3" % "3.9",
  "com.nrinaudo" %% "kantan.xpath" % "0.6.0",
  "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
  "com.typesafe.akka" %% "akka-http" % "10.1.13",
  "com.github.jwt-scala" %% "jwt-core" % "9.0.5",
  "org.scodec" %% "scodec-bits" % "1.1.37",
  "com.outr" %% "hasher" % "1.2.2",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.typelevel" %% "cats-effect" % "3.5.4",
  "com.github.pathikrit" %% "better-files" % "3.9.2",

  "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided",

  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test",
)

fork in run := true