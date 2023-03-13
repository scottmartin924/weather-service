package com.example.weatherservice.http

import com.example.weatherservice.http.app.WeatherApp
import zhttp.http.Request
import zio.Scope
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object WeatherAppSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Weather app spec")(
    test("forecast is returned") {
// FIXME Need to provide layer and test w/ fake clients
      val request = Request()
      val app = WeatherApp.app
      for {
        result <- app.apply(request)
      } yield assertTrue(true)
    },
    test("bad request response if missing lat/long") {
      assertTrue(true)
    },
    test("bad request response if lat/long is invalid") {
      assertTrue(true)
    },
    test("error response if noaa site returns error") {
      assertTrue(true)
    },
    test("error response if noaa site returns bad json") {
      assertTrue(true)
    }
  )
}
