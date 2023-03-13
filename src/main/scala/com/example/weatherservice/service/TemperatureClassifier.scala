package com.example.weatherservice.service

import com.example.weatherservice.resource.temperature.TemperatureCharacterization

trait TemperatureClassifier {
  def classify(temp: Double): TemperatureCharacterization
}

object TemperatureClassifier {
  // Note: Could make these ranges configurable
  val defaultClassifier: TemperatureClassifier = {
    case t if t <= 30 => TemperatureCharacterization.COLD
    case t if t >= 90 => TemperatureCharacterization.HOT
    case _            => TemperatureCharacterization.MODERATE
  }
}
