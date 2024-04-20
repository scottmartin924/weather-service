package com.example.weatherservice.cache

import cats.effect.{IO, Ref}
import com.example.weatherservice.domain.client.*
import munit.CatsEffectSuite

import java.time.{Instant, ZonedDateTime}
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.DurationInt

class WeatherCacheSpec extends CatsEffectSuite {

  val expireProtocol = CacheExpiryProtocol.forDuration(1.second)

  test("can insert and retrieve from cache") {
    val now = ZonedDateTime.now()
    val gridPoint =
      WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
    val report = WeatherReport(
      temperature = 20,
      forecast = ForecastDescription("Partly cloudy"),
      start = ForecastStartTime(now.minus(20, ChronoUnit.MINUTES)),
      end = ForecastEndTime(now.plus(20, ChronoUnit.MINUTES))
    )

    val clock = TestClock.constantTimeClock[IO](now.toInstant)
    given CacheExpiryProtocol = expireProtocol

    WeatherCache
      .inMemoryCache(clock)
      .use { cache =>
        for {
          _ <- cache.set(gridPoint, report)
          response <- cache.get(gridPoint)
        } yield {
          assert(response.nonEmpty)
          assertEquals(response.get, report)
        }
      }
  }

  test("cache key misses when GridPoint not present") {
    val now = ZonedDateTime.now()
    val gridPoint =
      WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
    val report = WeatherReport(
      temperature = 20,
      forecast = ForecastDescription("Partly cloudy"),
      start = ForecastStartTime(now.minus(20, ChronoUnit.MINUTES)),
      end = ForecastEndTime(now.plus(20, ChronoUnit.MINUTES))
    )

    val clock = TestClock.constantTimeClock[IO](now.toInstant)
    given CacheExpiryProtocol = expireProtocol

    WeatherCache
      .inMemoryCache(clock)
      .use { cache =>
        for {
          _ <- cache.set(gridPoint, report)
          response <- cache.get(gridPoint.copy(id = GridId("bad")))
        } yield {
          assert(response.isEmpty)
        }
      }
  }

  test("cache entry expires") {
    val now = ZonedDateTime.now()
    val gridPoint =
      WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
    val report = WeatherReport(
      temperature = 20,
      forecast = ForecastDescription("Partly cloudy"),
      start = ForecastStartTime(now.minus(20, ChronoUnit.MINUTES)),
      end = ForecastEndTime(now.plus(20, ChronoUnit.MINUTES))
    )

    given CacheExpiryProtocol = expireProtocol

    for {
      ref <- Ref.of[IO, Instant](now.toInstant)
      clock = TickableClock.make(ref)
      result <- WeatherCache.inMemoryCache(clock).use { cache =>
        cache.set(gridPoint, report) *>
          clock.advanceByDuration(5.minutes) *>
          cache.get(gridPoint)
      }
    } yield assert(result.isEmpty)
  }
}
