package com.example.weatherservice.http.app

import com.example.weatherservice.http.client.WeatherClient
import com.example.weatherservice.resource.health.ServiceStatus
import io.circe.syntax.EncoderOps
import zhttp.http._
import zio.ZIO
import com.example.weatherservice.resource.health.ServiceStatus._

object HealthStatusApp {
  val app = Http.collectZIO[Request] {
    case Method.GET -> !! / "health" =>
      ZIO
        .serviceWithZIO[WeatherClient](_.health)
        .map(status =>
          Response
            .json(ServiceStatus(status).asJson(serviceStatusEncoder).noSpaces)
            .setStatus(Status.Ok),
        )
  }
}
