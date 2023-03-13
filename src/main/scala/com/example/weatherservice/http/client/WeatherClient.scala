package com.example.weatherservice.http.client

import com.example.weatherservice.config.ApplicationConfig
import com.example.weatherservice.domain.client._
import com.example.weatherservice.domain.geography.GeographicPoint
import com.example.weatherservice.resource.health.HealthStatus
import io.circe.{parser, Json}
import zhttp.http.{Headers, Method, Response, Status}
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.ZIO.{logDebug, logError, logInfo}
import zio.{ZIO, ZLayer}

trait WeatherClient {
  // Note: We _do not_ use the forecast that gives the url of the forecast for the point...FIXME Discuss why not
  def retrieveGeographicPointInfo(
      point: GeographicPoint,
    ): ZIO[Any, Throwable, WeatherGridPoint]
  def retrieveForecast(
      point: WeatherGridPoint,
    ): ZIO[Any, Throwable, WeatherReport]
  def health: ZIO[Any, Throwable, HealthStatus]
}

object WeatherClient {
  private val APP_ID_HEADER = "User-Agent"
  type ClientReqs = EventLoopGroup with ChannelFactory

  case class ZioWeatherClient(
      config: ApplicationConfig,
      eventLoopGroup: EventLoopGroup,
      channelFactory: ChannelFactory,
    ) extends WeatherClient {
    private val defaultHeaders = Headers(APP_ID_HEADER -> config.appId)

    // This is odd...there's almost certainly a better way to do this
    private val requestEnv = ZLayer.succeed(eventLoopGroup) ++ ZLayer.succeed(channelFactory)

    private def gridUrlForPoint(point: GeographicPoint) =
      s"${config.pointEndpoint}/${point.lat.value},${point.long.value}"

    private def forecastUrlForPoint(point: WeatherGridPoint) =
      s"${config.forecastEndpoint}/${point.id.value}/${point.gridX.value},${point.gridY.value}/forecast"

    // Convert Client response body to json if request was successful else fail the ZIO
    private def responseAsJson(response: Response, url: String): ZIO[Any, Throwable, Json] =
      if (response.status.isSuccess)
        response.bodyAsString.flatMap { responseStr =>
          parser.parse(responseStr) match {
            case Right(json) => ZIO.succeed(json)
            case Left(err) =>
              logError(err.message) *> ZIO.fail(MalformedResponseEntity(err.message))
          }
        }
      else
        response
          .bodyAsString
          .flatMap(errBody => ZIO.fail(BadResponseStatus(url, response.status, errBody)))

    override def retrieveGeographicPointInfo(
        point: GeographicPoint,
      ): ZIO[Any, Throwable, WeatherGridPoint] =
      for {
        _ <- logInfo(s"Converting $point to grid")
        url <- ZIO.succeed(gridUrlForPoint(point))
        response <- Client
          .request(
            url,
            headers = defaultHeaders,
            method = Method.GET,
          )
          .provide(requestEnv)
        _ <- logDebug(s"Response for call to $url: $response")
        pointJsonResponse <- responseAsJson(response, url)
        point <- ZIO.fromEither(WeatherGridPoint.fromPointResponse(pointJsonResponse))
      } yield point

    override def retrieveForecast(
        gridPoint: WeatherGridPoint,
      ): ZIO[Any, Throwable, WeatherReport] =
      for {
        _ <- logInfo(s"Retrieving forecast for $gridPoint")
        url <- ZIO.succeed(forecastUrlForPoint(gridPoint))
        response <- Client
          .request(
            url,
            headers = defaultHeaders,
            method = Method.GET,
          )
          .provide(requestEnv)
        _ <- logDebug(s"Response for call to $url: $response")
        forecastJsonResponse <- responseAsJson(response, url)
        report <- ZIO.fromEither(WeatherReport.fromForecastResponse(forecastJsonResponse))
      } yield report

    override def health: ZIO[Any, Throwable, HealthStatus] =
      for {
        response <- Client
          .request(
            config.healthEndpoint,
            headers = defaultHeaders,
            method = Method.GET,
          )
          .provide(requestEnv)
        healthStatus <- response.status match {
          case Status.Ok => ZIO.succeed(HealthStatus.OK)
          case _         => ZIO.succeed(HealthStatus.UNAVAILABLE)
        }
      } yield healthStatus
  }

  val live = ZLayer {
    for {
      config <- ZIO.service[ApplicationConfig]
      eventLoopGroup <- ZIO.service[EventLoopGroup]
      channelFact <- ZIO.service[ChannelFactory]
    } yield ZioWeatherClient(config, eventLoopGroup, channelFact)
  }
}
