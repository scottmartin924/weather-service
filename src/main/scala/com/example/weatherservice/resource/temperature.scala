package com.example.weatherservice.resource

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

object temperature {
  sealed trait TemperatureCharacterization

  object TemperatureCharacterization {
    implicit val tempCodec: Codec[TemperatureCharacterization] =
      deriveEnumerationCodec[TemperatureCharacterization]

    case object HOT extends TemperatureCharacterization
    case object COLD extends TemperatureCharacterization
    case object MODERATE extends TemperatureCharacterization
  }
}
