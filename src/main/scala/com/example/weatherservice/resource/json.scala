package com.example.weatherservice.resource

import io.circe.generic.extras.Configuration

object json {
  implicit val circeConfig: Configuration =
    Configuration.default.copy(transformConstructorNames = _.toLowerCase)
}
