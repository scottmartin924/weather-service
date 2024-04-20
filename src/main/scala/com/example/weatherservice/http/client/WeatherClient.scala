package com.example.weatherservice.http.client

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.example.weatherservice.config.{ApplicationConfig, LoggingUtil}
import com.example.weatherservice.domain.client.*
import com.example.weatherservice.domain.geography.GeographicPoint
import com.example.weatherservice.resource.health.HealthStatus
import io.circe.Json
import org.http4s.{Headers, Method, ParseResult, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.circe.*

/** Client for interacting with weather NOAA API
  */
trait WeatherClient[F[_]] {

  /** Find the weather grid point given a lat/long. This is required since most
    * NOAA APIs require a specific coordinate format which is not lat/long
    */
  def fetchGeographicPointInfo(
      point: GeographicPoint
  ): F[WeatherGridPoint]

  /** Retrieve the forecast for a given grid point
    */
  def fetchForecast(
      point: WeatherGridPoint
  ): F[WeatherReport]

  /** Retrieve alerts for the given grid point
    */
  def fetchAlerts(
      point: GeographicPoint
  ): F[List[WeatherAlert]]

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

    private def defaultRequestForUri(uri: Uri): Request[F] =
      Request[F](method = Method.GET, uri = uri, headers = defaultHeaders)

    private def gridLocationRequestForPoint(
        point: GeographicPoint
    ): Either[Throwable, Request[F]] =
      Uri
        .fromString(
          s"${config.pointEndpoint}/${point.lat.value},${point.long.value}"
        )
        .map(defaultRequestForUri)
        .leftMap(e => Throwable(s"Could not create grid url for point: $e"))

    private def forecastRequestForPoint(
        point: WeatherGridPoint
    ): Either[Throwable, Request[F]] =
      Uri
        .fromString(
          s"${config.forecastEndpoint}/${point.id.value}/${point.gridX.value},${point.gridY.value}/forecast"
        )
        .map(defaultRequestForUri)
        .leftMap(e => Throwable(s"Could not create forecast url for point: $e"))

    // https://api.weather.gov/alerts/active?point=30.30797326138821%2C-97.73987820204256
    private def alertRequestForPoint(
        point: GeographicPoint
    ): Either[Throwable, Request[F]] = Uri
      .fromString(
        s"${config.alertsEndpoint}?point=${point.lat.value},${point.long.value}"
      )
      .map(defaultRequestForUri)
      .leftMap(e => Throwable(s"Could not create alerts url for point: $e"))

    private def handleRequestError(
        request: Request[F]
    )(response: Response[F]): F[Throwable] =
      response.as[Json].map { json =>
        BadResponseStatus(
          request = s"${request.method} ${request.uri}",
          status = response.status,
          body = json.noSpaces
        )
      }

    override def fetchGeographicPointInfo(
        point: GeographicPoint
    ): F[WeatherGridPoint] = for {
      request <- Async[F].fromEither(gridLocationRequestForPoint(point))
      _ <- logger.info(
        s"Converting $point to grid. Making request ${request.uri}"
      )
      jsonResponse <- client.expectOr[Json](request)(
        handleRequestError(request)
      )
      point <- Async[F].fromEither(
        WeatherGridPoint.fromPointResponse(jsonResponse)
      )
    } yield point

    override def fetchForecast(point: WeatherGridPoint): F[WeatherReport] =
      for {
        request <- Async[F].fromEither(forecastRequestForPoint(point))
        _ <- logger.info(
          s"Retrieving forecast for $point. Making request ${request.uri}"
        )
        jsonResponse <- client.expectOr[Json](request)(
          handleRequestError(request)
        )
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

    override def fetchAlerts(point: GeographicPoint): F[List[WeatherAlert]] =
      for {
        request <- Async[F].fromEither(alertRequestForPoint(point))
        _ <- logger.info(
          s"Retrieving alerts for point $point. Making request ${request.uri}"
        )
        alertsResponse <- client.expectOr[Json](request)(
          handleRequestError(request)
        )
        alerts <- Async[F].fromEither(
          WeatherAlert.fromAlertsListResponse(alertsResponse)
        )
      } yield alerts
  }
}
