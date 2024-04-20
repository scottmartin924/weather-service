package com.example.weatherservice.domain

import com.example.weatherservice.domain.geography.GeographicPoint
import munit.FunSuite

class GeographySpec extends FunSuite {

  test("Can make valid geographic point with positive lat/long") {
    val lat = 37.4109
    val long = 94.7050

    GeographicPoint.make(lat, long) match {
      case Left(err) => fail("Expected to parse geographic point")
      case Right(result) => {
        assertEquals(result.lat.value, lat)
        assertEquals(result.long.value, long)
      }
    }
  }

  test("Can make valid geographic point with negative lat/long") {
    val lat = -37.4109
    val long = -94.7050
    GeographicPoint.make(lat, long) match {
      case Left(err) => fail("Expected to parse geographic point")
      case Right(result) => {
        assertEquals(result.lat.value, lat)
        assertEquals(result.long.value, long)
      }
    }
  }

  test("Invalid longitude fails to make") {
    val lat = -37.4109
    val long = -194.7050

    GeographicPoint.make(lat, long) match {
      case Left(err) => ()
      case Right(result) =>
        fail(
          s"Expected to fail to parse geographic point with invalid long. Parsed to : $result"
        )
    }
  }

  test("Invalid latitude fails to make") {
    val lat = -100.4109
    val long = 94.7050
    GeographicPoint.make(lat, long) match {
      case Left(err) => ()
      case Right(result) =>
        fail(
          s"Expected to fail to parse geographic point with invalid long. Parsed to : $result"
        )
    }
  }
}
