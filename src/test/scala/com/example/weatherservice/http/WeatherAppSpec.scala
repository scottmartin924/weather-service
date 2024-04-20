package com.example.weatherservice.http

import cats.Parallel
import cats.effect.{IO, Sync}
import com.example.weatherservice.handler.ForecastHandler
import com.example.weatherservice.domain.client.*
import com.example.weatherservice.http.routes.WeatherRoutes
import com.example.weatherservice.http.client.{TestClients, WeatherClient}
import com.example.weatherservice.http.client.TestClients.{
  badResponseStatusClient,
  successfulWeatherClient
}
import com.example.weatherservice.service.{
  ForecastService,
  TemperatureClassifier
}
import munit.CatsEffectSuite
import org.http4s.{HttpRoutes, Method, Request, Status, Uri}

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class WeatherAppSpec extends CatsEffectSuite {

  // FIXME Could move some of this stuff out into a common spot
  val (testLocationLat, testLocationLong) = (35.4628, -94.143)
  val fakeGridPoint: WeatherGridPoint =
    WeatherGridPoint(GridId("test"), XCoordinate(0), YCoordinate(0))
  val fakeForecast: WeatherReport = WeatherReport(
    75,
    ForecastDescription("Sunny"),
    ForecastStartTime(ZonedDateTime.now().minus(1, ChronoUnit.HOURS)),
    ForecastEndTime(ZonedDateTime.now().plus(1, ChronoUnit.HOURS))
  )
  val fakeAlerts: List[WeatherAlert] = List(
    WeatherAlert("Flooding", "We're gonna need a bigger boat"),
    WeatherAlert("Sharknado", "It's a tornado...with sharks")
  )

  def buildHttpRoutes[F[_]: Sync: Parallel](
      client: WeatherClient[F]
  ): HttpRoutes[F] = {
    val forecastService =
      ForecastService[F](client, TemperatureClassifier.defaultClassifier)
    val handler = ForecastHandler(forecastService)
    WeatherRoutes(handler)
  }

  // Randomly picked a bad status for NOAA api to return
  val errorResponseStatus = Status.GatewayTimeout

  test("successfully retrieve forecast") {

    val successClient = TestClients.successfulWeatherClient[IO](
      fakeGridPoint,
      fakeForecast,
      fakeAlerts
    )
    val request =
      Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString(
          s"/forecast?lat=$testLocationLat&long=$testLocationLong"
        )
      )
    for {
      maybeResult <- buildHttpRoutes(successClient).run(request).value
      _ = assert(maybeResult.isDefined)
      result = maybeResult.get
      bodyResponse <- result.bodyText.compile.string
    } yield {
      assertEquals(result.status, Status.Ok)
      assert(bodyResponse.contains(fakeForecast.forecast.value))
    }
  }

  test("not found response if missing lat/long") {
    val successClient = TestClients.successfulWeatherClient[IO](
      fakeGridPoint,
      fakeForecast,
      fakeAlerts
    )

    // NOTE: lat query parameter is missing so won't match to a route and since these routes aren't yet wrapped
    // in a .orNotfound (that doesn't happen until the harness is used in Main) should get an empty response from the kleisli
    val request =
      Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString(
          s"/forecast?long=$testLocationLong"
        )
      )

    buildHttpRoutes(successClient)
      .run(request)
      .value
      .map { result =>
        assert(result.isEmpty)
      }
  }

  test("bad request response if lat/long is invalid") {
    val successClient = TestClients.successfulWeatherClient[IO](
      fakeGridPoint,
      fakeForecast,
      fakeAlerts
    )

    // NOTE: lat query parameter is invalid
    val request =
      Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString(
          s"/forecast?long=$testLocationLong&lat=-1500"
        )
      )
    for {
      result <- buildHttpRoutes(successClient).run(request).value
      _ = assert(result.isDefined)
    } yield {
      assertEquals(result.get.status, Status.BadRequest)
    }
  }

  test("error response if NOAA site returns error") {

    // InternalServerError is an error chosen at random, any could've worked
    val badGatewayClient =
      badResponseStatusClient[IO](Status.InternalServerError)
    val request =
      Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString(
          s"/forecast?lat=$testLocationLat&long=$testLocationLong"
        )
      )

    for {
      maybeResult <- buildHttpRoutes(badGatewayClient).run(request).value
      _ = assert(maybeResult.isDefined)
      result = maybeResult.get
    } yield {
      assertEquals(result.status, Status.InternalServerError)
    }
  }
}
