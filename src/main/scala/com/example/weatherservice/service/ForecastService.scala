package com.example.weatherservice.service

import cats.Parallel
import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.example.weatherservice.config.LoggingUtil
import com.example.weatherservice.domain.geography.GeographicPoint
import com.example.weatherservice.http.client.{WeatherClient}
import com.example.weatherservice.resource.forecast.WeatherReportResponse

trait ForecastService[F[_]] {
  def retrieveForecast(
      point: GeographicPoint
  ): F[WeatherReportResponse]
}

object ForecastService {

  def apply[F[_]: Sync: Parallel](
      weatherClient: WeatherClient[F],
      temperatureClassifier: TemperatureClassifier
  ): ForecastService[F] =
    new ForecastService[F] with LoggingUtil[F] {
      override def retrieveForecast(
          point: GeographicPoint
      ): F[WeatherReportResponse] = for {
        _ <- logger.info(s"Retrieve forecast for $point")
        weatherGridPoint <- weatherClient.fetchGeographicPointInfo(point)
        apiResponses <- weatherClient
          .fetchForecast(weatherGridPoint)
          .parProduct(weatherClient.fetchAlerts(point))
      } yield {
        val (forecast, alerts) = apiResponses
        WeatherReportResponse(
          forecast = forecast.forecast.value,
          temperature = temperatureClassifier.classify(forecast.temperature),
          alerts
        )
      }
    }
}
