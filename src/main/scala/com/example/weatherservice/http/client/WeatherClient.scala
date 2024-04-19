package com.example.weatherservice.http.client

import cats.data.EitherT
import cats.effect.kernel.{Async, Sync}
import cats.syntax.all.*
import com.example.weatherservice.config.{ApplicationConfig, LoggingUtil}
import com.example.weatherservice.domain.client.*
import com.example.weatherservice.domain.geography.GeographicPoint
import com.example.weatherservice.domain.geography.GeographicPoint.*
import com.example.weatherservice.resource.health.HealthStatus
import io.circe.{Json, parser}
import org.http4s.{Headers, Method, ParseResult, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*

// TODO Do we want Either or EitherT
trait WeatherClient[F[_]] {
  // Note: We _do not_ use the forecast that gives the url of the forecast for the point (can discuss why)
  def retrieveGeographicPointInfo(
      point: GeographicPoint
  ): F[WeatherGridPoint]
  def retrieveForecast(
      point: WeatherGridPoint
  ): F[WeatherReport]
  def health: F[HealthStatus]
}

object WeatherClient {
  private val APP_ID_HEADER = "User-Agent"

  def apply[F[_]: Async](
      client: Client[F],
      config: ApplicationConfig
  ): WeatherClient[F] = new WeatherClient[F] with LoggingUtil[F] {

    private val defaultHeaders: Headers = Headers(APP_ID_HEADER -> config.appId)
    private val healthUri: ParseResult[Uri] =
      Uri.fromString(config.healthEndpoint)

    private def gridUrlForPoint(
        point: GeographicPoint
    ): Either[Throwable, Uri] =
      Uri
        .fromString(
          s"${config.pointEndpoint}/${point.lat.value},${point.long.value}"
        )
        .leftMap(e => Throwable(s"Could not create grid url for point: $e"))

    private def forecastUrlForPoint(
        point: WeatherGridPoint
    ): Either[Throwable, Uri] =
      Uri
        .fromString(
          s"${config.forecastEndpoint}/${point.id.value}/${point.gridX.value},${point.gridY.value}/forecast"
        )
        .leftMap(e => Throwable(s"Could not create forecast url for point: $e"))

    override def retrieveGeographicPointInfo(
        point: GeographicPoint
    ): F[WeatherGridPoint] = for {
      _ <- logger.info(s"Converting $point to grid")
      uri <- Async[F].fromEither(gridUrlForPoint(point))
      // FIXME Make expectOr I think and do error handling
      // FIXME get straight to weatherGridPoint
      jsonResponse <- client.expect[Json](uri)
      point <- Async[F].fromEither(
        WeatherGridPoint.fromPointResponse(jsonResponse)
      )
    } yield point

    override def retrieveForecast(point: WeatherGridPoint): F[WeatherReport] =
      for {
        _ <- logger.info(s"Retrieving forecast for $point")
        uri <- Async[F].fromEither(forecastUrlForPoint(point))
        // FIXME expectOr
        jsonResponse <- client.expect[Json](uri)
        report <- Async[F].fromEither(
          WeatherReport.fromForecastResponse(jsonResponse)
        )
      } yield report

    override def health: F[HealthStatus] = for {
      uri <- Async[F].fromEither(healthUri)
      request = Request[F](
        uri = uri,
        headers = defaultHeaders,
        method = Method.GET
      )
      status <- client.run(request).use { (response: Response[F]) =>
        response.status match {
          case Status.Ok => HealthStatus.OK.pure
          case _         => HealthStatus.UNAVAILABLE.pure
        }
      }
    } yield status
  }
}
