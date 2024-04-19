package com.example.weatherservice.service

import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.example.weatherservice.config.LoggingUtil
import com.example.weatherservice.domain.client.WeatherGridPoint
import com.example.weatherservice.domain.geography.GeographicPoint
import com.example.weatherservice.http.client.{
  CacheableWeatherClient,
  WeatherClient
}
import com.example.weatherservice.resource.forecast.WeatherReportResponse

import scala.concurrent.duration.DurationInt

trait ForecastService[F[_]] {
  def retrieveForecast(
      point: GeographicPoint
  ): F[WeatherReportResponse]
}

object ForecastService {

  def apply[F[_]: Sync](
      weatherClient: WeatherClient[F],
      temperatureClassifier: TemperatureClassifier
  ): ForecastService[F] =
    new ForecastService[F] with LoggingUtil[F] {
      override def retrieveForecast(
          point: GeographicPoint
      ): F[WeatherReportResponse] = for {
        _ <- logger.info(s"Retrieve forecast for $point")
        weatherGridPoint <- weatherClient.retrieveGeographicPointInfo(point)
        forecast <- weatherClient.retrieveForecast(weatherGridPoint)
      } yield {
        WeatherReportResponse(
          forecast = forecast.forecast.value,
          temperature = temperatureClassifier.classify(forecast.temperature)
        )
      }
    }
}
