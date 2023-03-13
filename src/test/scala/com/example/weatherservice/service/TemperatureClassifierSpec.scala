package com.example.weatherservice.service

import com.example.weatherservice.resource.temperature.TemperatureCharacterization
import zio.Scope
import zio.test.Assertion.equalTo
import zio.test._

object TemperatureClassifierSpec extends ZIOSpecDefault {

  val classifier = TemperatureClassifier.defaultClassifier
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("TemperatureClassifier")(
    test("should label cold") {
      assert(classifier.classify(-100))(equalTo(TemperatureCharacterization.COLD))
    },
    test("should label hot") {
      assert(classifier.classify(200))(equalTo(TemperatureCharacterization.HOT))
    },
    test("should label moderate") {
      assert(classifier.classify(50))(equalTo(TemperatureCharacterization.MODERATE))
    }
  )
}
