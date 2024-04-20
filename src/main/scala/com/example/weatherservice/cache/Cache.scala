package com.example.weatherservice.cache

import java.time.Instant

trait Cache[F[_], K, V] {

  /** Get value from cache if present, else None
    */
  def get(key: K): F[Option[V]]

  /** Set the value in a cache
    */
  def set(
      key: K,
      value: V
  )(using CacheExpiryProtocol): F[Unit]

  def clear(): F[Unit]
}

object Cache {
  case class CacheValue[V](value: V, expireTime: Instant)
}
