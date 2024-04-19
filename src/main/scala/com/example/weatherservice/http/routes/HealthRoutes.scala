package com.example.weatherservice.http.routes

import cats.syntax.all.*
import cats.effect.Sync
import com.example.weatherservice.http.client.WeatherClient
import com.example.weatherservice.resource.health.ServiceStatus
import io.circe.syntax.EncoderOps
import com.example.weatherservice.resource.health.ServiceStatus.*
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*

object HealthRoutes {

  def apply[F[_]: Sync](
      weatherClient: WeatherClient[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}

    import dsl.*

    HttpRoutes.of[F] {
      // Note: Any health of web client returns 200 and the status is in the body. Seems to make some sense
      case request @ GET -> Root / "health" =>
        weatherClient.health
          .map { status =>
            Response[F](
              status = Status.Ok
            ).withEntity(status)
          }
    }
  }
}
