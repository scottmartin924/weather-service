import sbt._

object Dependencies {
  val ZioVersion = "2.0.0"
  val ZHTTPVersion = "2.0.0-RC9"
  val ZioConfigVersion = "3.0.0-RC9"
  val CirceVersion = "0.14.1"

  def circeDep(dependency: String) = "io.circe" %% dependency % CirceVersion

  val `zio-http` = "io.d11" %% "zhttp" % ZHTTPVersion
  val `zio-config` = "dev.zio" %% "zio-config" % ZioConfigVersion
  val `zio-config-magnolia` = "dev.zio" %% "zio-config-magnolia" % ZioConfigVersion
  val `zio-http-test` = "io.d11" %% "zhttp" % ZHTTPVersion % Test
  val circe =
    Seq("circe-core", "circe-generic", "circe-parser", "circe-optics", "circe-generic-extras").map(
      circeDep,
    )

  val `zio-test` = "dev.zio" %% "zio-test" % ZioVersion % Test
  val `zio-test-sbt` = "dev.zio" %% "zio-test-sbt" % ZioVersion % Test
}
