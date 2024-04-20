package com.example.weatherservice

import cats.effect.{Clock, IO, IOApp, Resource}
import com.comcast.ip4s.Port
import com.example.weatherservice.config.{ApplicationConfig, Harness}
import com.example.weatherservice.retry.ClientRetry
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

import scala.concurrent.duration.DurationInt

object Main extends IOApp.Simple {

  private val webClient: Resource[IO, Client[IO]] = EmberClientBuilder
    .default[IO]
    .withRetryPolicy(ClientRetry.exponentialRetry(3.seconds, 2))
    .build

  private def buildWebServer(
      port: Int,
      routes: HttpApp[IO]
  ): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withPort(
        Port.fromInt(port).getOrElse(Port.fromInt(8080).get)
      )
      .withHttpApp(routes)
      .build

  override def run: IO[Unit] = {
    val defaultClock: Clock[IO] = cats.effect.Clock[IO]

    (for {
      client <- webClient
      config <- ApplicationConfig.build[IO]()
      harness <- Harness.default[IO](config, client, defaultClock)
      server <- buildWebServer(config.port, harness.app)
    } yield server).useForever.void
  }
}
