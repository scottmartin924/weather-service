package com.example.weatherservice.resource

import com.example.weatherservice.domain.client.WeatherAlert
import com.example.weatherservice.resource.temperature.TemperatureCharacterization
import com.example.weatherservice.resource.temperature.TemperatureCharacterization.*
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

object forecast {
  case class WeatherReportResponse(
      forecast: String,
      temperature: TemperatureCharacterization,
      alerts: List[WeatherAlert]
  )
  object WeatherReportResponse {
    implicit val weatherReportEncoder: Encoder[WeatherReportResponse] =
      deriveEncoder[WeatherReportResponse]
  }
}
