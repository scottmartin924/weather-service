package com.example.weatherservice.http.app

import com.example.weatherservice.controller.ForecastController
import zhttp.http._

object WeatherHttpApp {
  val app = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "forecast" => ForecastController.getForecast(req)
  }
}
