package com.example.weatherservice.cache

import com.example.weatherservice.cache.Cache.CacheValue
import com.example.weatherservice.domain.client.{WeatherGridPoint, WeatherReport}
import zio.{Clock, Ref, ZIO, ZLayer}

import java.time.Instant

class WeatherCache private (ref: Ref[Map[WeatherGridPoint, CacheValue[WeatherReport]]])
    extends Cache[WeatherGridPoint, WeatherReport] {
  override def get(key: WeatherGridPoint): ZIO[Any, Nothing, Option[WeatherReport]] = for {
    cache <- ref.get
    now <- Clock.instant
  } yield
  // Get cache entry if still at a valid time and if not expired
  cache
    .get(key)
    .filter { cachedValue =>
      val isCacheValid = now.isBefore(cachedValue.expireTime)
      val isTimeRangeValid = now.isBefore(cachedValue.value.end.time.toInstant)
      isCacheValid && isTimeRangeValid
    }
    .map(_.value)

  override def set(
      key: WeatherGridPoint,
      value: WeatherReport,
      expireTime: Instant = Instant.MAX,
    ): ZIO[Any, Nothing, Unit] =
    ref.update { cache =>
      cache + (key -> CacheValue(value, expireTime))
    }

  override def clear(): ZIO[Any, Nothing, Unit] = ref.update(_.empty)
}

object WeatherCache {
  val inMemoryWeatherCache: ZLayer[Any, Nothing, WeatherCache] = ZLayer {
    Ref
      .make(Map.empty[WeatherGridPoint, CacheValue[WeatherReport]])
      .map(new WeatherCache(_))
  }
}
