package com.example.weatherservice.domain

import scala.util.control.NoStackTrace

object geography {
  // FIXME Is this overkills
  sealed trait GeographyError extends NoStackTrace
  case class InvalidPoint(message: String) extends GeographyError

  // Note: all the fancy shenanigans here to attempt to force use of the smart constructors
  sealed abstract case class Latitude private (value: Double)
  sealed abstract case class Longitude private (value: Double)

  object Latitude {
    // TODO Consider if want to do something about this...the magic 90 is strange
    private val MAX_LAT = 90
    def make(latitude: Double): Either[GeographyError, Latitude] =
      Either.cond(
        Math.abs(latitude) <= MAX_LAT,
        new Latitude(latitude) {},
        InvalidPoint(s"Latitude $latitude is invalid. Value must be between +/- $MAX_LAT"),
      )
  }

  object Longitude {
    private val MAX_LONG = 180
    def make(longitude: Double): Either[GeographyError, Longitude] = Either.cond(
      Math.abs(longitude) <= MAX_LONG,
      new Longitude(longitude) {},
      InvalidPoint(s"Longitude $longitude is invalid. Value must be between +/- $MAX_LONG"),
    )
  }

  // Note: There are certainly libraries that do this far better than
  // I do here, but both wanted to move quickly and minimize dependencies
  case class GeographicPoint(lat: Latitude, long: Longitude)
  object GeographicPoint {
    def make(latitude: Double, longitude: Double): Either[GeographyError, GeographicPoint] = for {
      lat <- Latitude.make(latitude)
      long <- Longitude.make(longitude)
    } yield GeographicPoint(lat, long)
  }
}
