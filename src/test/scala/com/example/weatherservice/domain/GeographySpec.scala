package com.example.weatherservice.domain

import com.example.weatherservice.domain.geography.GeographicPoint
import zio.test.Assertion.equalTo
import zio.test._

object GeographySpec extends ZIOSpecDefault {
  override def spec =
    suite("GeographyDomainSpec")(
      test("Can make valid geographic point with positive lat/long") {
        val lat = 37.4109
        val long = 94.7050
        val result = for {
          point <- GeographicPoint.make(lat, long)
        } yield assert(point.lat.value)(equalTo(lat)) && assert(point.long.value)(equalTo(long))
        assertTrue(result.isRight)
      },
      test("Can make valid geographic point with negative lat/long") {
        val lat = -37.4109
        val long = -94.7050
        val result =
          for {
            point <- GeographicPoint.make(lat, long)
          } yield assert(point.lat.value)(Assertion.equalTo(lat)) && assert(point.long.value)(
            equalTo(long),
          )
        assertTrue(result.isRight)
      },
      test("Invalid longitude fails to make") {
        val lat = -37.4109
        val long = -194.7050
        val result = for {
          point <- GeographicPoint.make(lat, long)
        } yield point
        assertTrue(result.isLeft)
      },
      test("Invalid latitude fails to make") {
        val lat = -100.4109
        val long = 94.7050
        val result = for {
          point <- GeographicPoint.make(lat, long)
        } yield point
        assertTrue(result.isLeft)
      },
    )
}
