package com.example.weatherservice.cache

import zio.ZIO

import java.time.Instant

trait Cache[K, V] {
  // Get cache value from key
  def get(key: K): ZIO[Any, Nothing, Option[V]]

  // Note: defaults to never expire
  def set(
      key: K,
      value: V,
      expireTime: Instant = Instant.MAX,
    ): ZIO[Any, Nothing, Unit]

  def clear(): ZIO[Any, Nothing, Unit]
}

object Cache {
  case class CacheValue[V](value: V, expireTime: Instant)
}
