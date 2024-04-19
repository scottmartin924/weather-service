package com.example.weatherservice.http

import com.example.weatherservice.cache.WeatherCache
import com.example.weatherservice.handler.ForecastHandler
import com.example.weatherservice.domain.client._
import com.example.weatherservice.http.routes.WeatherRoutes
import com.example.weatherservice.http.client.CacheableWeatherClient
import com.example.weatherservice.http.client.TestClients.{
  badResponseStatusClient,
  malformedJsonResponseClient,
  successfulWeatherClient,
}
import com.example.weatherservice.service.{ForecastService, TemperatureClassifier}
import zhttp.http.{!!, Method, Request, Status, URL}
import zio.test.{assertTrue, Spec, TestClock, TestEnvironment, ZIOSpecDefault}
import zio.{durationInt, Scope, ZLayer}

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object WeatherAppSpec extends ZIOSpecDefault {

  // FIXME Could move some of this stuff out into a common spot
  val (testLocationLat, testLocationLong) = (35.4628, -94.143)
  val fakeGridPoint = WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
  val fakeForecast = WeatherReport(
    75,
    ForecastDescription("Sunny"),
    ForecastStartTime(ZonedDateTime.now().minus(1, ChronoUnit.HOURS)),
    ForecastEndTime(ZonedDateTime.now().plus(1, ChronoUnit.HOURS)),
  )
  // Randomly picked a bad status for NOAA api to return
  val errorResponseStatus = Status.GatewayTimeout

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Weather app spec")(
    test("successfully retrieve forecast") {
      val queryParams =
        Map("lat" -> List(testLocationLat.toString), "long" -> List(testLocationLong.toString))
      val request =
        Request(url = URL(!! / "forecast").setQueryParams(queryParams), method = Method.GET)
      for {
        result <- WeatherRoutes.app(request)
        bodyResponse <- result.bodyAsString
      } yield assertTrue(result.status == Status.Ok) && assertTrue(
        bodyResponse.contains(fakeForecast.forecast.value),
      )
    }.provide(
      WeatherCache.inMemoryWeatherCache,
      ForecastHandler.live,
      ForecastService.live,
      CacheableWeatherClient.live,
      ZLayer.succeed(TemperatureClassifier.defaultClassifier),
      ZLayer.succeed(successfulWeatherClient(fakeGridPoint, fakeForecast)),
    ),
    test("bad request response if missing lat/long") {
      // Missing lat
      val queryParams =
        Map("long" -> List(testLocationLat.toString))
      val request =
        Request(url = URL(!! / "forecast").setQueryParams(queryParams), method = Method.GET)
      for {
        result <- WeatherRoutes.app(request)
      } yield assertTrue(result.status == Status.BadRequest)
    }.provide(
      WeatherCache.inMemoryWeatherCache,
      ForecastHandler.live,
      ForecastService.live,
      CacheableWeatherClient.live,
      ZLayer.succeed(TemperatureClassifier.defaultClassifier),
      ZLayer.succeed(successfulWeatherClient(fakeGridPoint, fakeForecast)),
    ),
    test("bad request response if lat/long is invalid") {
      // Invalid lat
      val queryParams =
        Map("lat" -> List("-1000"), "long" -> List(testLocationLong.toString))
      val request =
        Request(url = URL(!! / "forecast").setQueryParams(queryParams), method = Method.GET)
      for {
        result <- WeatherRoutes.app(request)
      } yield assertTrue(result.status == Status.BadRequest)
    }.provide(
      WeatherCache.inMemoryWeatherCache,
      ForecastHandler.live,
      ForecastService.live,
      CacheableWeatherClient.live,
      ZLayer.succeed(TemperatureClassifier.defaultClassifier),
      ZLayer.succeed(successfulWeatherClient(fakeGridPoint, fakeForecast)),
    ),
    test("error response if NOAA site returns error") {
      val queryParams =
        Map("lat" -> List(testLocationLat.toString), "long" -> List(testLocationLong.toString))
      val request =
        Request(url = URL(!! / "forecast").setQueryParams(queryParams), method = Method.GET)
      for {
        // Have to fork here to adjust test clock
        resultFiber <- WeatherRoutes.app(request).fork
        _ <- TestClock.adjust(1.second)
        result <- resultFiber.join
      } yield assertTrue(result.status == Status.BadGateway)
    }.provide(
      WeatherCache.inMemoryWeatherCache,
      ForecastHandler.live,
      ForecastService.live,
      CacheableWeatherClient.live,
      ZLayer.succeed(TemperatureClassifier.defaultClassifier),
      ZLayer.succeed(badResponseStatusClient(Status.BadGateway)),
    ),
    test("error response if NOAA site returns bad status") {
      val queryParams =
        Map("lat" -> List(testLocationLat.toString), "long" -> List(testLocationLong.toString))
      val request =
        Request(url = URL(!! / "forecast").setQueryParams(queryParams), method = Method.GET)
      for {
        // Have to fork here to adjust test clock
        resultFiber <- WeatherRoutes.app(request).fork
        _ <- TestClock.adjust(1.second)
        result <- resultFiber.join
      } yield assertTrue(result.status == errorResponseStatus)
    }.provide(
      WeatherCache.inMemoryWeatherCache,
      ForecastHandler.live,
      ForecastService.live,
      CacheableWeatherClient.live,
      ZLayer.succeed(TemperatureClassifier.defaultClassifier),
      ZLayer.succeed(badResponseStatusClient(errorResponseStatus)),
    ),
    test("error response if NOAA site returns malformed json") {
      val queryParams =
        Map("lat" -> List(testLocationLat.toString), "long" -> List(testLocationLong.toString))
      val request =
        Request(url = URL(!! / "forecast").setQueryParams(queryParams), method = Method.GET)
      for {
        // Have to fork here to adjust test clock
        resultFiber <- WeatherRoutes.app(request).fork
        _ <- TestClock.adjust(1.second)
        result <- resultFiber.join
      } yield assertTrue(result.status == Status.InternalServerError)
    }.provide(
      WeatherCache.inMemoryWeatherCache,
      ForecastHandler.live,
      ForecastService.live,
      CacheableWeatherClient.live,
      ZLayer.succeed(TemperatureClassifier.defaultClassifier),
      ZLayer.succeed(malformedJsonResponseClient),
    ),
  )
}
