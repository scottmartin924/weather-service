package com.example.weatherservice.cache

import java.time.Instant

trait Cache[F[_], K, V] {
  
  // FIXME Add docs

  // Get cache value from key
  def get(key: K): F[Option[V]]

  // Note: defaults to never expire
  def set(
      key: K,
      value: V
  )(using CacheExpiryProtocol): F[Unit]

  def clear(): F[Unit]
}

object Cache {
  case class CacheValue[V](value: V, expireTime: Instant)
}
