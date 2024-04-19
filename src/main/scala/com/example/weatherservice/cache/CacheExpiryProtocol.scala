package com.example.weatherservice.cache

import scala.concurrent.duration.Duration

trait CacheExpiryProtocol {
  def expireDuration: Duration
}

object CacheExpiryProtocol {
  def forDuration(d: Duration): CacheExpiryProtocol = new CacheExpiryProtocol:
    override def expireDuration: Duration = d
}
