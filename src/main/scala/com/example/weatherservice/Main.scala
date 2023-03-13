package com.example.weatherservice

import com.example.weatherservice.cache.WeatherCache
import com.example.weatherservice.config.ApplicationConfig
import com.example.weatherservice.controller.ForecastController
import com.example.weatherservice.http.app.{HealthStatusApp, WeatherHttpApp}
import com.example.weatherservice.http.client.{CacheableWeatherClient, WeatherClient}
import com.example.weatherservice.service.{ForecastService, TemperatureClassifier}
import zhttp.http._
import zhttp.service.{ChannelFactory, EventLoopGroup, Server}
import zio._

object Main extends ZIOAppDefault {
  val app = WeatherHttpApp.app ++ HealthStatusApp.app

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO
      .service[ApplicationConfig]
      .flatMap { config =>
        Server
          .start(config.port, app)
          .exitCode
      }
      .provide(
        EventLoopGroup.auto(),
        ChannelFactory.auto,
        ForecastService.live,
        WeatherClient.live,
        CacheableWeatherClient.live,
        WeatherCache.inMemoryWeatherCache,
        ZLayer.succeed(TemperatureClassifier.defaultClassifier),
        ApplicationConfig.live,
        ForecastController.live,
      )
}
