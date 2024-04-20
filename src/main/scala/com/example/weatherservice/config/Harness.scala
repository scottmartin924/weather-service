package com.example.weatherservice.config

import cats.Parallel
import cats.syntax.all.*
import cats.effect.{Clock, Resource, Sync}
import cats.effect.kernel.Async
import com.example.weatherservice.cache.WeatherCache
import com.example.weatherservice.handler.ForecastHandler
import com.example.weatherservice.http.client.{
  CacheableWeatherClient,
  WeatherClient
}
import com.example.weatherservice.http.routes.{HealthRoutes, WeatherRoutes}
import com.example.weatherservice.service.{
  ForecastService,
  TemperatureClassifier
}
import org.http4s.HttpApp
import org.http4s.client.Client

case class Harness[F[_]](
    config: ApplicationConfig,
    weatherCache: WeatherCache[F],
    weatherClient: WeatherClient[F],
    forecastService: ForecastService[F],
    forecastHandler: ForecastHandler[F],
    app: HttpApp[F]
)

object Harness {

  private def buildHttpApp[F[_]: Sync](
      forecastHandler: ForecastHandler[F],
      weatherClient: WeatherClient[F]
  ) =
    (WeatherRoutes(forecastHandler) <+> HealthRoutes(weatherClient)).orNotFound

  def default[F[_]: Async: Parallel](
      config: ApplicationConfig,
      client: Client[F],
      clock: Clock[F]
  ): Resource[F, Harness[F]] = for {
    cache <- WeatherCache.inMemoryCache(clock)
    basicWeatherClient = WeatherClient.apply(client, config)
    cacheableWeatherClient = CacheableWeatherClient.create(
      basicWeatherClient,
      cache
    )
    forecastService = ForecastService.apply(
      cacheableWeatherClient,
      TemperatureClassifier.defaultClassifier
    )
    forecastHandler = ForecastHandler.apply(forecastService)
  } yield Harness(
    config,
    cache,
    cacheableWeatherClient,
    forecastService,
    forecastHandler,
    buildHttpApp(forecastHandler, cacheableWeatherClient)
  )
}
