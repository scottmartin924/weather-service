package com.example.weatherservice.resource

import com.example.weatherservice.resource.json._
import io.circe.generic.extras.semiauto.{deriveConfiguredEncoder, deriveEnumerationCodec}
import io.circe.{Codec, Encoder}

object health {
  sealed trait HealthStatus
  object HealthStatus {
    implicit val tempCodec: Codec[HealthStatus] =
      deriveEnumerationCodec[HealthStatus]

    case object OK extends HealthStatus
    case object UNAVAILABLE extends HealthStatus
  }

  case class ServiceStatus(weatherClientStatus: HealthStatus)
  object ServiceStatus {
    implicit val serviceStatusEncoder: Encoder.AsObject[ServiceStatus] =
      deriveConfiguredEncoder[ServiceStatus]
  }
}
