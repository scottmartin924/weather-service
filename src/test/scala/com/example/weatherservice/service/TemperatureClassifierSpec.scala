package com.example.weatherservice.service

import com.example.weatherservice.resource.temperature.TemperatureCharacterization
import munit.FunSuite

class TemperatureClassifierSpec extends FunSuite {

  val classifier = TemperatureClassifier.defaultClassifier

  test("should label cold") {
    assertEquals(classifier.classify(-100), TemperatureCharacterization.COLD)
  }
  test("should label hot") {
    assertEquals(classifier.classify(200), TemperatureCharacterization.HOT)
  }

  test("should label moderate") {
    assertEquals(classifier.classify(50), TemperatureCharacterization.MODERATE)
  }
}
