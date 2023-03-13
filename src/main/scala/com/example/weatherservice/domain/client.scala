package com.example.weatherservice.domain

import io.circe.optics.JsonPath.root
import io.circe.{Decoder, HCursor, Json}
import zhttp.http.Status

import java.time.ZonedDateTime
import scala.util.control.NoStackTrace

object client {
  sealed trait ClientError extends NoStackTrace
  case class MalformedResponseEntity(message: String) extends ClientError
  case class BadResponseStatus(
      request: String,
      status: Status,
      body: String,
    ) extends ClientError

  // FIXME Should split these up better
  case class GridId(value: String) extends AnyVal
  case class XCoordinate(value: Int) extends AnyVal
  case class YCoordinate(value: Int) extends AnyVal

  case class WeatherGridPoint(
      id: GridId,
      gridX: XCoordinate,
      gridY: YCoordinate,
    )

  object WeatherGridPoint {
    implicit val weatherGridPointDecoder: Decoder[WeatherGridPoint] =
      (c: HCursor) =>
        for {
          gridId <- c.downField("gridId").as[String]
          x <- c.downField("gridX").as[Int]
          y <- c.downField("gridY").as[Int]
        } yield WeatherGridPoint(GridId(gridId), XCoordinate(x), YCoordinate(y))

    def fromPointResponse(json: Json): Either[ClientError, WeatherGridPoint] =
      for {
        properties <- root
          .properties
          .json
          .getOption(json)
          .toRight(MalformedResponseEntity(s"Missing properties field in points response: $json"))
        gridPoint <- properties
          .as[WeatherGridPoint]
          .left
          .map(err => MalformedResponseEntity(s"Failed to parse point response: ${err.message}"))
      } yield gridPoint
  }

  case class ForecastDescription(value: String) extends AnyVal
  case class ForecastStartTime(time: ZonedDateTime)
  case class ForecastEndTime(time: ZonedDateTime)
  case class WeatherReport(
      temperature: Int,
      forecast: ForecastDescription,
      start: ForecastStartTime,
      end: ForecastEndTime,
    )
  object WeatherReport {
    // Decoder for a weather report from a given "period" response entity
    implicit def decoder: Decoder[WeatherReport] = (c: HCursor) =>
      for {
        temp <- c.downField("temperature").as[Int]
        forecast <- c.downField("shortForecast").as[String]
        start <- c.downField("startTime").as[ZonedDateTime]
        end <- c.downField("endTime").as[ZonedDateTime]
      } yield WeatherReport(
        temp,
        ForecastDescription(forecast),
        ForecastStartTime(start),
        ForecastEndTime(end),
      )

    // FIXME So gross
    def fromForecastResponse(json: Json): Either[ClientError, WeatherReport] = {
      // Finds period with number = 1 (so the "current" period)...theoretically...almost always :)
      val _currentPeriodOptic =
        root.properties.periods.each.filter(root.selectDynamic("number").int.exist(_ == 1))
      for {
        currentPeriodWeather <- _currentPeriodOptic
          .json
          .getAll(json)
          .headOption
          .toRight(MalformedResponseEntity("Could not find current period in forecast response"))
        report <- currentPeriodWeather
          .as[WeatherReport]
          .left
          .map(err =>
            MalformedResponseEntity(s"Failed to parse forecast response to report: ${err.message}"),
          )
      } yield report
    }
  }
}
