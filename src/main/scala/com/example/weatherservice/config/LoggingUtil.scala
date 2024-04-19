package com.example.weatherservice.config

import cats.effect.kernel.Sync
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

trait LoggingUtil[F[_]: Sync] {
  val logger =
    LoggerFactory.getLoggerFromClass(getClass)(Slf4jFactory.create[F])
}
