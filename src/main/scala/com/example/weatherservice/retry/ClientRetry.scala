// FIXME Either convert this to CE/http4s retry or delete it
package com.example.weatherservice.retry

import org.http4s.client.middleware.RetryPolicy

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

object ClientRetry {
  // Fixed backoff retry with given delay and max attempts
  def exponentialRetry[F[_]](
      maxWait: FiniteDuration,
      maxRetries: Int
  ): RetryPolicy[F] =
    RetryPolicy(RetryPolicy.exponentialBackoff(3.seconds, 2))
}
