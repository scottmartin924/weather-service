package com.example.weatherservice.http.client

import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.example.weatherservice.cache.{CacheExpiryProtocol, WeatherCache}
import com.example.weatherservice.config.LoggingUtil
import com.example.weatherservice.domain.client
import com.example.weatherservice.domain.client.{
  WeatherGridPoint,
  WeatherReport
}
import com.example.weatherservice.domain.geography.*
import com.example.weatherservice.resource.health.HealthStatus

import scala.concurrent.duration.DurationInt

object CacheableWeatherClient {

  /** A weather client which caches (some of) the returned results
    */
  def create[F[_]: Sync](
      weatherClient: WeatherClient[F],
      cache: WeatherCache[F],
      cacheExpiryProtocol: CacheExpiryProtocol =
        CacheExpiryProtocol.forDuration(2.minutes)
  ): WeatherClient[F] = new WeatherClient[F] with LoggingUtil[F] {

    given CacheExpiryProtocol = cacheExpiryProtocol

    // Note: We don't cache long/lat to grid points but we could (might be tricky though since would want to give jitter probably)
    override def fetchGeographicPointInfo(
        point: GeographicPoint
    ): F[WeatherGridPoint] = weatherClient.fetchGeographicPointInfo(point)

    override def fetchForecast(point: WeatherGridPoint): F[WeatherReport] =
      for {
        cachedReport <- cache.get(point)
        report <- cachedReport match {
          case Some(report) =>
            logger.info(s"Cache hit for $point") *> report.pure
          case None =>
            for {
              _ <- logger.info(s"Cache miss for $point")
              forecast <- weatherClient.fetchForecast(point)
              _ <- cache.set(point, forecast)
            } yield forecast
        }
      } yield report

    override def health: F[HealthStatus] = weatherClient.health

    // TODO Should be caching this too, but just didn't
    override def fetchAlerts(
        point: GeographicPoint
    ): F[List[client.WeatherAlert]] = weatherClient.fetchAlerts(point)
  }
}
