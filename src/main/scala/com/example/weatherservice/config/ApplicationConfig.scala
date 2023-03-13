package com.example.weatherservice.config

import zio.config._
import zio.config.derivation.name
import zio.config.magnolia.descriptor

case class ApplicationConfig(
    appId: String,
    @name("weatherHealthEndpoint") healthEndpoint: String,
    pointEndpoint: String,
    forecastEndpoint: String,
    port: Int,
  )

object ApplicationConfig {
  private val appConfig: ConfigDescriptor[ApplicationConfig] = descriptor[ApplicationConfig]
  val live = ZConfig
    .fromPropertiesFile("src/main/resources/application.properties", appConfig)
}
