package com.example.weatherservice.resource

import com.example.weatherservice.resource.temperature.TemperatureCharacterization
import com.example.weatherservice.resource.temperature.TemperatureCharacterization._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import scala.util.control.NoStackTrace

object forecast {
  case class WeatherReportResponse(forecast: String, temperature: TemperatureCharacterization)
  object WeatherReportResponse {
    implicit val weatherReportEncoder: Encoder[WeatherReportResponse] =
      deriveEncoder[WeatherReportResponse]
  }

  sealed trait InvalidRequest extends NoStackTrace
  case class MissingPointInfo(message: String) extends InvalidRequest
}
