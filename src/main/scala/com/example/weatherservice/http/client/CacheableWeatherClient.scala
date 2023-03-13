package com.example.weatherservice.http.client

import com.example.weatherservice.cache.WeatherCache
import com.example.weatherservice.domain.client
import com.example.weatherservice.domain.client.{WeatherGridPoint, WeatherReport}
import com.example.weatherservice.domain.geography._
import com.example.weatherservice.resource.health.HealthStatus
import zio.ZIO.logInfo
import zio.{Clock, ZIO, ZLayer}

import java.time.Duration

class CacheableWeatherClient(
    weatherClient: WeatherClient,
    cache: WeatherCache,
  ) extends WeatherClient {
  // Note: Uses Java b/c using Instant from java.time
  private val defaultCacheExpiry = Duration.ofMinutes(3)

  // Note: We don't cache long/lat to grid points but we could (might be tricky though)
  override def retrieveGeographicPointInfo(
      point: GeographicPoint,
    ): ZIO[Any, Throwable, client.WeatherGridPoint] =
    weatherClient.retrieveGeographicPointInfo(point)

  override def retrieveForecast(
      point: WeatherGridPoint,
    ): ZIO[Any, Throwable, WeatherReport] =
    for {
      cachedReport <- cache.get(point)
      report <- cachedReport match {
        case Some(report) => logInfo(s"Cache hit for $point") *> ZIO.succeed(report)
        case None =>
          for {
            _ <- logInfo(s"Cache miss for $point")
            forecast <- weatherClient.retrieveForecast(point)
            now <- Clock.instant
            _ <- cache.set(point, forecast, now.plus(defaultCacheExpiry))
          } yield forecast
      }
    } yield report

  override def health: ZIO[Any, Throwable, HealthStatus] =
    weatherClient.health
}

object CacheableWeatherClient {
  def live: ZLayer[WeatherClient with WeatherCache, Nothing, CacheableWeatherClient] = ZLayer {
    for {
      cache <- ZIO.service[WeatherCache]
      client <- ZIO.service[WeatherClient]
    } yield new CacheableWeatherClient(client, cache)
  }
}
