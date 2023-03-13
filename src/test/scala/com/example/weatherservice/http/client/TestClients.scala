package com.example.weatherservice.http.client

import com.example.weatherservice.domain.client.{
  BadResponseStatus,
  GridId,
  MalformedResponseEntity,
  WeatherGridPoint,
  WeatherReport,
  XCoordinate,
  YCoordinate,
}
import com.example.weatherservice.domain.{client, geography}
import com.example.weatherservice.resource.health.HealthStatus
import zhttp.http.Status
import zio.ZIO

object TestClients {
  def successfulWeatherClient(gridPoint: WeatherGridPoint, forecast: WeatherReport): WeatherClient =
    new WeatherClient {
      override def retrieveGeographicPointInfo(
          point: geography.GeographicPoint,
        ): ZIO[Any, Throwable, WeatherGridPoint] =
        ZIO.succeed(gridPoint)

      override def retrieveForecast(
          point: client.WeatherGridPoint,
        ): ZIO[Any, Throwable, WeatherReport] =
        ZIO.succeed(forecast)

      override def health: ZIO[Any, Throwable, HealthStatus] = ???
    }

  def badResponseStatusClient(status: Status): WeatherClient = new WeatherClient {
    override def retrieveGeographicPointInfo(
        point: geography.GeographicPoint,
      ): ZIO[Any, Throwable, WeatherGridPoint] =
      ZIO.succeed(WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0)))

    override def retrieveForecast(point: WeatherGridPoint): ZIO[Any, Throwable, WeatherReport] =
      ZIO.fail(BadResponseStatus("fake.url/forecast", status, "Something went wrong"))

    override def health: ZIO[Any, Throwable, HealthStatus] = ???
  }

  def malformedJsonResponseClient: WeatherClient = new WeatherClient {
    override def retrieveGeographicPointInfo(
        point: geography.GeographicPoint,
      ): ZIO[Any, Throwable, WeatherGridPoint] =
      ZIO.fail(MalformedResponseEntity("That doesn't look rigth..."))

    override def retrieveForecast(point: WeatherGridPoint): ZIO[Any, Throwable, WeatherReport] =
      ZIO.fail(MalformedResponseEntity("That doesn't look rigth..."))

    override def health: ZIO[Any, Throwable, HealthStatus] = ???
  }
}
