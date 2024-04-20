package com.example.weatherservice.http.routes

import cats.effect.Sync
import com.example.weatherservice.handler.ForecastHandler
import com.example.weatherservice.handler.ForecastHandler.{
  LatCoordinateQueryParamDecoder,
  LongCoordinateQueryParamDecoder
}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.*

object WeatherRoutes {
  def apply[F[_]: Sync](
      forecastHandler: ForecastHandler[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}

    import dsl.*

    HttpRoutes.of[F] {
      case request @ GET -> Root / "forecast" :?
          LatCoordinateQueryParamDecoder(
            lat
          ) +& LongCoordinateQueryParamDecoder(long) =>
        forecastHandler.fetchForecast(request, lat, long)
    }
  }
}
