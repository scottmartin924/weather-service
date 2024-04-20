package com.example.weatherservice.cache

import cats.syntax.all.*
import cats.Applicative
import cats.effect.{Async, Clock, Ref}

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait TickableClock[F[_]] extends Clock[F] {

  def advanceByDuration(duration: FiniteDuration): F[Unit]
}

object TickableClock {

  /** Creates clock which can be advanced based on the given ref. Sort of a
    * quick hack around using the CE TestKit
    */
  def make[F[_]: Async](ref: Ref[F, Instant]): TickableClock[F] =
    new TickableClock[F] {
      override def applicative: Applicative[F] = summon[Applicative[F]]

      override def monotonic: F[FiniteDuration] =
        ref.get.map(now => FiniteDuration(now.getNano, TimeUnit.NANOSECONDS))

      override def realTime: F[FiniteDuration] = ref.get.map(now =>
        FiniteDuration(now.toEpochMilli, TimeUnit.MILLISECONDS)
      )

      def advanceByDuration(duration: FiniteDuration): F[Unit] =
        ref.getAndUpdate { now =>
          now.plusMillis(duration.toMillis)
        }.void
    }
}
