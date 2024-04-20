ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.3.3"

val CatsEffectVersion = "3.3.12"
val Http4sVersion = "0.23.26"
val CirceVersion = "0.14.6"
val PureConfigVersion = "0.17.6"

lazy val root = (project in file("."))
  .settings(
    name := "weather-service",
    scalacOptions ++= Seq("-Wunused:all"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "org.typelevel" %% "cats-effect-kernel" % CatsEffectVersion,
      "org.typelevel" %% "cats-effect-std" % CatsEffectVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-optics" % "0.15.0",
      "com.github.pureconfig" %% "pureconfig-core" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    )
  )
