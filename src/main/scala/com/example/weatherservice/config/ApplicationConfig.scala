package com.example.weatherservice.config

import cats.effect.{Resource, Sync}
import pureconfig.ConfigSource
import pureconfig.*
import pureconfig.module.catseffect.syntax.*
import pureconfig.generic.derivation.default.*

case class ApplicationConfig(
    appId: String,
    healthEndpoint: String,
    pointEndpoint: String,
    forecastEndpoint: String,
    alertsEndpoint: String,
    port: Int
) derives ConfigReader

object ApplicationConfig {

  def build[F[_]: Sync](): Resource[F, ApplicationConfig] =
    Resource.eval(ConfigSource.default.loadF[F, ApplicationConfig]())
}
