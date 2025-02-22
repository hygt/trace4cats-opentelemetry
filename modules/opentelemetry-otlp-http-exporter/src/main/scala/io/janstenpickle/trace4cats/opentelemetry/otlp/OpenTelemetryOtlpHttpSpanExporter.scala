package io.janstenpickle.trace4cats.opentelemetry.otlp

import cats.Foldable
import cats.effect.kernel.{Async, Resource, Temporal}
import io.janstenpickle.trace4cats.`export`.HttpSpanExporter
import io.janstenpickle.trace4cats.kernel.SpanExporter
import io.janstenpickle.trace4cats.model.Batch
import io.janstenpickle.trace4cats.opentelemetry.otlp.json.ResourceSpansBatch
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.blaze.client.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object OpenTelemetryOtlpHttpSpanExporter {
  def blazeClient[F[_]: Async, G[_]: Foldable](
    host: String = "localhost",
    port: Int = 4318,
    ec: Option[ExecutionContext] = None,
    protocol: String = "http"
  ): Resource[F, SpanExporter[F, G]] = for {
    client <- ec.fold(BlazeClientBuilder[F])(BlazeClientBuilder[F].withExecutionContext).resource
    exporter <- Resource.eval(apply[F, G](client, host, port, protocol))
  } yield exporter

  def apply[F[_]: Temporal, G[_]: Foldable](
    client: Client[F],
    host: String = "localhost",
    port: Int = 4318,
    protocol: String = "http"
  ): F[SpanExporter[F, G]] =
    HttpSpanExporter[F, G, ResourceSpansBatch](
      client,
      s"$protocol://$host:$port/v1/traces",
      (batch: Batch[G]) => ResourceSpansBatch.from(batch)
    )
}
