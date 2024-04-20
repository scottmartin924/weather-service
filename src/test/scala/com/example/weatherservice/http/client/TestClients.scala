package com.example.weatherservice.http.client

import cats.effect.Sync
import cats.syntax.all.*
import com.example.weatherservice.domain.client.{
  BadResponseStatus,
  GridId,
  MalformedResponseEntity,
  WeatherAlert,
  WeatherGridPoint,
  WeatherReport,
  XCoordinate,
  YCoordinate
}
import com.example.weatherservice.domain.{client, geography}
import com.example.weatherservice.resource.health.HealthStatus
import org.http4s.Status

object TestClients {
  def successfulWeatherClient[F[_]: Sync](
      gridPoint: WeatherGridPoint,
      forecast: WeatherReport,
      alerts: List[WeatherAlert]
  ): WeatherClient[F] =
    new WeatherClient[F] {
      override def fetchAlerts(
          point: geography.GeographicPoint
      ): F[List[client.WeatherAlert]] = alerts.pure

      override def fetchGeographicPointInfo(
          point: geography.GeographicPoint
      ): F[WeatherGridPoint] = gridPoint.pure

      override def fetchForecast(
          point: client.WeatherGridPoint
      ): F[WeatherReport] = forecast.pure

      override def health: F[HealthStatus] = ???
    }

  def badResponseStatusClient[F[_]: Sync](status: Status): WeatherClient[F] =
    new WeatherClient[F] {
      override def fetchGeographicPointInfo(
          point: geography.GeographicPoint
      ): F[WeatherGridPoint] =
        WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0)).pure

      override def fetchForecast(
          point: WeatherGridPoint
      ): F[WeatherReport] =
        Sync[F].raiseError(
          BadResponseStatus("fake.url/forecast", status, "Something went wrong")
        )

      override def fetchAlerts(
          point: geography.GeographicPoint
      ): F[List[client.WeatherAlert]] = ???

      override def health: F[HealthStatus] = ???
    }

  def malformedJsonResponseClient[F[_]: Sync]: WeatherClient[F] =
    new WeatherClient[F] {
      override def fetchGeographicPointInfo(
          point: geography.GeographicPoint
      ): F[WeatherGridPoint] =
        Sync[F].raiseError(
          MalformedResponseEntity("That doesn't look right...")
        )

      override def fetchForecast(
          point: WeatherGridPoint
      ): F[WeatherReport] =
        Sync[F].raiseError(
          MalformedResponseEntity("That doesn't look right...")
        )

      override def fetchAlerts(
          point: geography.GeographicPoint
      ): F[List[client.WeatherAlert]] = Sync[F].raiseError(
        MalformedResponseEntity("That doesn't look right...")
      )

      override def health: F[HealthStatus] = ???
    }
}
