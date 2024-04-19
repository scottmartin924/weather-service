package com.example.weatherservice.resource

import cats.syntax.all.*
import io.circe.{Codec, Decoder, Encoder}

object temperature {
  sealed trait TemperatureCharacterization

  object TemperatureCharacterization {
    given Codec[TemperatureCharacterization] = Codec.from(
      Decoder.decodeString.emap(TemperatureCharacterization.fromString),
      Encoder.encodeString.contramap(_.toString)
    )

    case object HOT extends TemperatureCharacterization
    case object COLD extends TemperatureCharacterization
    case object MODERATE extends TemperatureCharacterization

    def fromString(s: String): Either[String, TemperatureCharacterization] =
      s.toLowerCase match {
        case "hot"      => TemperatureCharacterization.HOT.asRight
        case "cold"     => TemperatureCharacterization.COLD.asRight
        case "moderate" => TemperatureCharacterization.MODERATE.asRight
        case e =>
          s"Unable to parse: '$e' to a TemperatureCharacterization"
            .asLeft[TemperatureCharacterization]
      }
  }
}
