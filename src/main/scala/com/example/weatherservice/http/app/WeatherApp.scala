package com.example.weatherservice.http.app

import com.example.weatherservice.controller.ForecastController
import com.example.weatherservice.domain.client.WeatherGridPoint._
import io.circe.generic.auto._
import io.circe.syntax.{EncoderOps, _}
import zhttp.http._

object WeatherApp {
  val app: Http[ForecastController.Controller, Nothing, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! / "forecast" => ForecastController.getForecast(req)
    }
}
