package com.example.weatherservice.controller

import com.example.weatherservice.domain.client.BadResponseStatus
import com.example.weatherservice.domain.client.WeatherGridPoint._
import com.example.weatherservice.domain.geography.{GeographicPoint, GeographyError}
import com.example.weatherservice.resource.forecast.MissingPointInfo
import com.example.weatherservice.service.ForecastService
import io.circe.generic.auto._
import io.circe.syntax.{EncoderOps, _}
import zhttp.http._
import zio.ZIO.{logError, logInfo}
import zio.{ZIO, ZLayer}

object ForecastController {
  class Controller(service: ForecastService) {
    def getForecast(request: Request): ZIO[Any, Nothing, Response] = {
      val latitudeField = "lat"
      val longitudeField = "long"
      val queryParameters = request.url.queryParams
      val point = for {
        lat <- queryParameters(latitudeField).headOption.flatMap(_.toDoubleOption)
        long <- queryParameters(longitudeField).headOption.flatMap(_.toDoubleOption)
      } yield (lat, long)

      val job = for {
        point <- ZIO
          .fromOption(point)
          .mapError(_ => MissingPointInfo("Both lat and long query parameters must be provided"))
        point <- ZIO.fromEither(GeographicPoint.make(point._1, point._2))
        _ <- logInfo(s"Retrieving forecast for lat,long: $point")
        forecast <- service.retrieveForecast(point)
      } yield Response.json(forecast.asJson.noSpaces).setStatus(Status.Ok)

      // FIXME Better error handling
      job.catchAll {
        case geoErr: GeographyError =>
          ZIO.succeed(Response.json(geoErr.asJson.noSpaces).setStatus(Status.BadRequest))
        case clientErr: BadResponseStatus =>
          // Note: we make our error status the same as the clients...that's perhaps not ideal
          ZIO.succeed(
            Response.json(clientErr.asJson.noSpaces).setStatus(clientErr.status),
          )
        case err =>
          logError(s"Failed to retrieve forecast: ${err}") *> ZIO.succeed(
            Response.json(err.getMessage.asJson.noSpaces).setStatus(Status.InternalServerError),
          )
      }
    }
  }

  // Accessor method to be used from app
  def getForecast(
      request: Request,
    ): ZIO[Controller, Nothing, Response] =
    ZIO.serviceWithZIO[Controller](_.getForecast(request))

  val live = ZLayer {
    for {
      service <- ZIO.service[ForecastService]
    } yield new Controller(service)
  }
}
