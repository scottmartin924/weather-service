package com.example.weatherservice.resource

import io.circe.syntax.*
import cats.syntax.all.*
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, JsonObject}

object health {
  sealed trait HealthStatus
  object HealthStatus {
    given Encoder[HealthStatus] = Encoder.encodeJsonObject.contramap { status =>
      JsonObject("healthStatus" -> status.toString.asJson)
    }

    case object OK extends HealthStatus
    case object UNAVAILABLE extends HealthStatus

    def fromString(s: String): Either[String, HealthStatus] =
      s.toLowerCase match {
        case "ok"          => HealthStatus.OK.asRight
        case "unavailable" => HealthStatus.UNAVAILABLE.asRight
        case e =>
          s"Unable to parse: '$e' to a HealthStatus".asLeft[HealthStatus]
      }
  }

  case class ServiceStatus(weatherClientStatus: HealthStatus)
  object ServiceStatus {
    given Encoder[ServiceStatus] = deriveEncoder[ServiceStatus]
  }
}
