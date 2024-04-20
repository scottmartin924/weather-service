package com.example.weatherservice.cache

import cats.effect.kernel.Resource
import cats.syntax.all.*
import cats.effect.{Clock, Ref, Sync}
import cats.effect.syntax.all.*
import com.example.weatherservice.cache.Cache.CacheValue
import com.example.weatherservice.domain.client.{
  WeatherGridPoint,
  WeatherReport
}

trait WeatherCache[F[_]] extends Cache[F, WeatherGridPoint, WeatherReport]

object WeatherCache {

  // FIXME Make the cache handle timings so can pass in expiry config via implicit
  def inMemoryCache[F[_]: Sync](
      clock: Clock[F]
  ): Resource[F, WeatherCache[F]] =
    Ref
      .of[F, Map[WeatherGridPoint, CacheValue[WeatherReport]]](Map.empty)
      .map { cacheRef =>
        new WeatherCache[F] {
          override def get(key: WeatherGridPoint): F[Option[WeatherReport]] =
            for {
              cache <- cacheRef.get
              now <- clock.realTimeInstant
              // FIXME Extract this
              result = cache
                .get(key)
                .filter { cachedValue =>
                  // Get cache entry if still at a valid time and if not expired
                  val isCacheValid = now.isBefore(cachedValue.expireTime)
                  val isTimeRangeValid =
                    now.isBefore(cachedValue.value.end.time.toInstant)
                  isCacheValid && isTimeRangeValid
                }
                .map(_.value)
            } yield result

          override def set(
              key: WeatherGridPoint,
              value: WeatherReport
          )(using expire: CacheExpiryProtocol): F[Unit] = for {
            now <- clock.realTimeInstant
            expireAt <- now.plusMillis(expire.expireDuration.toMillis).pure
            _ <- cacheRef.update { cache =>
              cache + (key -> CacheValue(value, expireAt))
            }
          } yield ()

          override def clear(): F[Unit] = cacheRef.update(_ => Map.empty)
        }
      }
      .toResource
}
