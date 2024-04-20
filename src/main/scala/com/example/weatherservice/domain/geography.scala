package com.example.weatherservice.domain

import cats.syntax.all.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.math.BigDecimal.RoundingMode
import scala.util.control.NoStackTrace

object geography {
  sealed trait GeographyError extends NoStackTrace
  case class InvalidPoint(message: String) extends GeographyError

  // Note: all the fancy shenanigans here to attempt to force use of the smart constructors
  sealed abstract case class Latitude private (value: Double)
  sealed abstract case class Longitude private (value: Double)

  // TOD HACK to handle weather.gove API's weird 301 on too high precision values which is odd..not sure how else to handle
  private val MAX_PRECISION_SCALE = 4

  /** Set coordinate values to max precision which weather.gov will return
    * results for. Both stylistically annoying, but also slow to have to make
    * BigDecimals all the time
    */
  private def setCoordinatePrecision(d: Double): Double =
    BigDecimal(d).setScale(MAX_PRECISION_SCALE, RoundingMode.HALF_EVEN).toDouble

  object Latitude {
    private val MAX_LAT = 90
    def make(latitude: Double): Either[GeographyError, Latitude] =
      Either.cond(
        Math.abs(latitude) <= MAX_LAT,
        new Latitude(setCoordinatePrecision(latitude)) {},
        InvalidPoint(
          s"Latitude $latitude is invalid. Value must be between +/- $MAX_LAT"
        )
      )

    given Encoder[Latitude] = Encoder.encodeDouble.contramap(_.value)
    given Decoder[Latitude] =
      Decoder.decodeDouble.emap(d =>
        Latitude.make(d).leftMap(_.getLocalizedMessage)
      )
  }

  object Longitude {
    private val MAX_LONG = 180
    def make(longitude: Double): Either[GeographyError, Longitude] =
      Either.cond(
        Math.abs(longitude) <= MAX_LONG,
        new Longitude(setCoordinatePrecision(longitude)) {},
        InvalidPoint(
          s"Longitude $longitude is invalid. Value must be between +/- $MAX_LONG"
        )
      )

    given Encoder[Longitude] = Encoder.encodeDouble.contramap(_.value)

    given Decoder[Longitude] =
      Decoder.decodeDouble.emap(d =>
        Longitude.make(d).leftMap(_.getLocalizedMessage)
      )
  }

  // Note: There are certainly libraries that do this far better than
  // I do here, but both wanted to move quickly and minimize dependencies
  case class GeographicPoint(lat: Latitude, long: Longitude)
  object GeographicPoint {

    def make(
        latitude: Double,
        longitude: Double
    ): Either[GeographyError, GeographicPoint] = for {
      lat <- Latitude.make(latitude)
      long <- Longitude.make(longitude)
    } yield GeographicPoint(lat, long)

    given Encoder[GeographicPoint] = deriveEncoder[GeographicPoint]
    given Decoder[GeographicPoint] = deriveDecoder[GeographicPoint]
  }
}
