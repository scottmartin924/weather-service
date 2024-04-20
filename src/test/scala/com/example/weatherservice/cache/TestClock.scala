package com.example.weatherservice.cache

import cats.syntax.all.*
import cats.Applicative
import cats.effect.Clock

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object TestClock {

  def constantTimeClock[F[_]: Applicative](now: Instant): Clock[F] =
    new Clock[F] {
      override def applicative: Applicative[F] = summon[Applicative[F]]

      override def monotonic: F[FiniteDuration] =
        FiniteDuration(now.getNano, TimeUnit.NANOSECONDS).pure

      override def realTime: F[FiniteDuration] =
        FiniteDuration(now.toEpochMilli, TimeUnit.MILLISECONDS).pure
    }
}
