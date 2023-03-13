package com.example.weatherservice.cache

import com.example.weatherservice.domain.client._
import zio.test.{Spec, TestClock, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object WeatherCacheSpec extends ZIOSpecDefault {
  private val weatherCache = WeatherCache.inMemoryWeatherCache
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("WeatherCacheSpec")(
    test("can insert and retrieve from cache") {
      val now = ZonedDateTime.now()
      val gridPoint = WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
      val report = WeatherReport(
        temperature = 20,
        forecast = ForecastDescription("Partly cloudy"),
        start = ForecastStartTime(now.minus(20, ChronoUnit.MINUTES)),
        end = ForecastEndTime(now.plus(20, ChronoUnit.MINUTES)),
      )
      for {
        cache <- ZIO.service[WeatherCache]
        _ <- cache.set(gridPoint, report)
        response <- cache.get(gridPoint)
      } yield assertTrue(response.nonEmpty) && assertTrue(response.get == report)
    }.provideLayer(weatherCache),
    test("cache key misses if GridPoint changed") {
      val now = ZonedDateTime.now()
      val gridPoint = WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
      val report = WeatherReport(
        temperature = 20,
        forecast = ForecastDescription("Partly cloudy"),
        start = ForecastStartTime(now.minus(20, ChronoUnit.MINUTES)),
        end = ForecastEndTime(now.plus(20, ChronoUnit.MINUTES)),
      )
      for {
        cache <- ZIO.service[WeatherCache]
        _ <- cache.set(gridPoint, report)
        response <- cache.get(gridPoint.copy(id = GridId("new")))
      } yield assertTrue(response.isEmpty)
    }.provideLayer(weatherCache),
    test("cache entry expires") {
      val now = ZonedDateTime.now()
      val gridPoint = WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
      val report = WeatherReport(
        temperature = 20,
        forecast = ForecastDescription("Partly cloudy"),
        start = ForecastStartTime(now.minus(20, ChronoUnit.MINUTES)),
        end = ForecastEndTime(now.plus(20, ChronoUnit.MINUTES)),
      )
      for {
        cache <- ZIO.service[WeatherCache]
        _ <- TestClock.setTime(now.toInstant)
        _ <- cache.set(gridPoint, report, expireTime = now.toInstant.minus(200, ChronoUnit.MILLIS))
        response <- cache.get(gridPoint)
      } yield assertTrue(response.isEmpty)
    }.provideLayer(weatherCache),
    test("cache entry not found if outside the forecasts time window") {
      val now = ZonedDateTime.now()
      val gridPoint = WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
      val report = WeatherReport(
        temperature = 20,
        forecast = ForecastDescription("Partly cloudy"),
        start = ForecastStartTime(now.minus(20, ChronoUnit.MINUTES)),
        end = ForecastEndTime(now.minus(10, ChronoUnit.MINUTES)),
      )
      for {
        cache <- ZIO.service[WeatherCache]
        now <- TestClock.setTime(now.toInstant)
        _ <- cache.set(gridPoint, report)
        response <- cache.get(gridPoint)
      } yield assertTrue(response.isEmpty)
    }.provideLayer(weatherCache),
  )
}
