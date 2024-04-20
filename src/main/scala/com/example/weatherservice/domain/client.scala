package com.example.weatherservice.domain

import cats.syntax.all.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.optics.JsonPath.root
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.Status

import java.time.ZonedDateTime
import scala.util.control.NoStackTrace

object client {
  sealed trait ClientError extends NoStackTrace
  case class MalformedResponseEntity(message: String) extends ClientError {
    override def getMessage: String = message
  }
  case class BadResponseStatus(
      request: String,
      status: Status,
      body: String
  ) extends ClientError

  object BadResponseStatus {

    given Encoder[Status] = Encoder.encodeInt.contramap(_.code)

    given Encoder[BadResponseStatus] = deriveEncoder[BadResponseStatus]
  }

  case class GridId(value: String) extends AnyVal
  case class XCoordinate(value: Int) extends AnyVal
  case class YCoordinate(value: Int) extends AnyVal

  case class WeatherGridPoint(
      id: GridId,
      gridX: XCoordinate,
      gridY: YCoordinate
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
        properties <- json.hcursor
          .downField("properties")
          .as[Json]
          .leftMap(err =>
            MalformedResponseEntity(
              s"Missing properties field in points response. Error: ${err} on json: $json"
            )
          )
        gridPoint <- properties
          .as[WeatherGridPoint]
          .leftMap(err =>
            MalformedResponseEntity(
              s"Failed to parse point response: ${err.message}"
            )
          )
      } yield gridPoint
  }

  case class ForecastDescription(value: String) extends AnyVal
  case class ForecastStartTime(time: ZonedDateTime)
  case class ForecastEndTime(time: ZonedDateTime)
  case class WeatherReport(
      temperature: Int,
      forecast: ForecastDescription,
      start: ForecastStartTime,
      end: ForecastEndTime
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
        ForecastEndTime(end)
      )

    def fromForecastResponse(json: Json): Either[ClientError, WeatherReport] = {
      // Finds period with number = 1 (so the "current" period)...theoretically...almost always :)
      val _currentPeriodOptic =
        root.properties.periods.each
          .filter(root.selectDynamic("number").int.exist(_ == 1))
      for {
        currentPeriodWeather <- _currentPeriodOptic.json
          .getAll(json)
          .headOption
          .toRight(
            MalformedResponseEntity(
              "Could not find current period in forecast response"
            )
          )
        report <- currentPeriodWeather
          .as[WeatherReport]
          .left
          .map(err =>
            MalformedResponseEntity(
              s"Failed to parse forecast response to report: ${err.message}"
            ),
          )
      } yield report
    }
  }

  case class WeatherAlert(
      event: String,
      headline: String
  )

  object WeatherAlert {
    given Encoder[WeatherAlert] = deriveEncoder[WeatherAlert]
    given Decoder[WeatherAlert] = deriveDecoder[WeatherAlert]

    def fromAlertJson(json: Json): Either[ClientError, WeatherAlert] = {
      val _alertEventOptic = root.properties.event.string
      val _alertHeadlineOption = root.properties.headline.string

      for {
        event <- _alertEventOptic
          .getOption(json)
          .toRight(
            MalformedResponseEntity(
              s"Did not find event string in alert json"
            )
          )
        headline <- _alertHeadlineOption
          .getOption(json)
          .toRight(
            MalformedResponseEntity(
              s"Did not find headline string in alert json"
            )
          )
      } yield WeatherAlert(event, headline)
    }

    def fromAlertsListResponse(
        json: Json
    ): Either[ClientError, List[WeatherAlert]] = {
      val _alertsArrayOptic = root.features.arr
      for {
        alertsArray <- _alertsArrayOptic
          .getOption(json)
          .toRight(
            MalformedResponseEntity(
              s"Did not find alerts array"
            )
          )
        weatherAlerts <- alertsArray.traverse(fromAlertJson)
      } yield weatherAlerts.toList
    }
  }
}
