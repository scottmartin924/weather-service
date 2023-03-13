package com.example.weatherservice.retry

import zio.ZIO.logInfo
import zio.{Duration, Schedule, ZIO}

import scala.concurrent.duration.FiniteDuration

trait Retry {
  // Note: This limits how much the user has to interact with
  // ZIO's schedule, but also limits the number of retry options
  // description field is for logging a string
  def retry[R, E, A](
      delay: FiniteDuration,
      maxAttempts: Int,
      description: String = "",
    )(
      f: ZIO[R, E, A],
    ): ZIO[R, E, A]
}

object Retry {
  // Fixed backoff retry with given delay and max attempts
  val fixedDelay: Retry = new Retry {
    override def retry[R, E, A](
        delay: FiniteDuration,
        maxAttempts: Int,
        description: String = "",
      )(
        f: ZIO[R, E, A],
      ): ZIO[R, E, A] = {
      val fWithLogging = logInfo(s"Running job {$description}") *> f
      fWithLogging.retry(
        Schedule.fixed(Duration.fromScala(delay)) && Schedule.recurs(maxAttempts),
      )
    }
  }
}
