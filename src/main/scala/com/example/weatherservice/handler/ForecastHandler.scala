package com.example.weatherservice.handler

import cats.syntax.all.*
import cats.effect.kernel.Sync
import com.example.weatherservice.domain.client.BadResponseStatus
import com.example.weatherservice.domain.geography.{
  GeographicPoint,
  GeographyError,
  InvalidPoint
}
import com.example.weatherservice.service.ForecastService
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import org.http4s.Status
import org.http4s.Status.Ok
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.circe.CirceEntityCodec.*

trait ForecastHandler[F[_]] {

  def fetchForecast(
      request: Request[F],
      latValue: Double,
      longValue: Double
  ): F[Response[F]]
}

object ForecastHandler {

  object LatCoordinateQueryParamDecoder
      extends QueryParamDecoderMatcher[Double]("lat")
  object LongCoordinateQueryParamDecoder
      extends QueryParamDecoderMatcher[Double]("long")

  // FIXME Decide how we want this to work...probably
  def apply[F[_]: Sync](
      forecastService: ForecastService[F]
  ): ForecastHandler[F] = new ForecastHandler[F]:

    override def fetchForecast(
        request: Request[F],
        latVal: Double,
        longVal: Double
    ): F[Response[F]] = {
      val weatherResponse: F[Response[F]] = for {
        geoPoint <- Sync[F].fromEither(GeographicPoint.make(latVal, longVal))
        result <- forecastService.retrieveForecast(geoPoint)
      } yield Response[F](status = Status.Ok).withEntity(result)

      weatherResponse.recover {
        case geoErr: GeographyError =>
          Response(status = Status.BadRequest).withEntity(geoErr)
        case clientErr: BadResponseStatus =>
          // Note: we make our error status the same as the clients...that's perhaps not ideal
          Response(status = clientErr.status).withEntity(clientErr)
        case err =>
          Response(status = Status.InternalServerError)
            .withEntity(err.getMessage.asJson.noSpaces)
      }
    }
}
