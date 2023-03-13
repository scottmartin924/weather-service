package com.example.weatherservice.resource

import com.example.weatherservice.resource.temperature.TemperatureCharacterization
import com.example.weatherservice.resource.temperature.TemperatureCharacterization._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

object forecast {
  case class WeatherReportResponse(forecast: String, temperature: TemperatureCharacterization)
  object WeatherReportResponse {
    implicit val weatherReportEncoder: Encoder[WeatherReportResponse] =
      deriveEncoder[WeatherReportResponse]
  }
}
