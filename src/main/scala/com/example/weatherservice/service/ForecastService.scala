package com.example.weatherservice.service

import com.example.weatherservice.domain.geography.GeographicPoint
import com.example.weatherservice.http.client.CacheableWeatherClient
import com.example.weatherservice.resource.forecast.WeatherReportResponse
import com.example.weatherservice.retry.Retry
import zio.ZIO.logInfo
import zio.{ZIO, ZLayer}

import scala.concurrent.duration.DurationInt

trait ForecastService {
  def retrieveForecast(
      point: GeographicPoint,
    ): ZIO[Any, Throwable, WeatherReportResponse]
}

object ForecastService {
  class Service(weatherClient: CacheableWeatherClient, temperatureClassifier: TemperatureClassifier)
      extends ForecastService {
    // Note: Would want to tune these (or get rid of them) based on the behavior of the NOAA API
    // Also, probably would only want to retry on certain statuses...this currently retries on everything
    // (including for example a Too Many Requests response :( )
    private val retryDelay = 10000.millis
    private val retryCount = 2
    private def withRetry[R, E, A] =
      Retry.exponentialBackWithDefaultClock.retry[R, E, A](retryDelay, retryCount, _: String) _

    // FIXME If end up caching then make a CacheableWeatherClient or something
    override def retrieveForecast(
        point: GeographicPoint,
      ): ZIO[Any, Throwable, WeatherReportResponse] =
      for {
        _ <- logInfo(s"Retrieve forecast for $point")
        weatherGridPoint <- withRetry("Get geographic point")(
          weatherClient.retrieveGeographicPointInfo(point),
        )
        forecast <- withRetry("Get forecast for point")(
          weatherClient.retrieveForecast(weatherGridPoint),
        )
      } yield {
        val tempDesc = temperatureClassifier.classify(forecast.temperature)
        WeatherReportResponse(forecast = forecast.forecast.value, temperature = tempDesc)
      }
  }

  val live
      : ZLayer[CacheableWeatherClient with TemperatureClassifier, Any, ForecastService.Service] =
    ZLayer {
      for {
        client <- ZIO.service[CacheableWeatherClient]
        classifier <- ZIO.service[TemperatureClassifier]
      } yield new Service(client, classifier)
    }
}
