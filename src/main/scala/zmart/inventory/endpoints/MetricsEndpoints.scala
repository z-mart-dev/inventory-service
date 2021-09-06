package zmart.inventory.endpoints

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir._
import zhttp.http._
import zio.{Has, RIO, ZIO}
import zio.zmx.MetricSnapshot.Prometheus
import zio.zmx.prometheus.PrometheusClient

object MetricsEndpoints {
  // Tapir endpoints
  val prometheusLogic: Unit => ZIO[Has[PrometheusClient], Nothing, String] = _ => PrometheusClient.snapshot.map(_.value)

  val baseEndpoint: Endpoint[Unit, Unit, String, Any] =
    endpoint
      .tags(List("Support"))
      .name("Metrics")
      .summary("System Metrics")
      .description("Prometheus formatted metrics ready to be processed.")
      .in("metrics")
      .out(stringBody.description("Prometheus formatted metrics"))

  val prometheusEndpoint: ZServerEndpoint[Has[PrometheusClient], Unit, Unit, String] = baseEndpoint.zServerLogic[Has[PrometheusClient]](prometheusLogic)

  type EffectType[A] = RIO[Has[PrometheusClient], A]

  val metricsApp: Http[Has[PrometheusClient], Throwable, Request, Response[Has[PrometheusClient], Throwable]] =
    ZioHttpInterpreter().toHttp(prometheusEndpoint.asInstanceOf[ServerEndpoint[_, _, _, ZioStreams, EffectType]])

  // zHttp 'bare' endpoint
  val metrics: Http[Has[PrometheusClient], Nothing, Request, UResponse] = Http.collectM[Request] { case Method.GET -> Root / "metrics" =>
    PrometheusClient.snapshot.map { case Prometheus(value) =>
      Response.text(value)
    }
  }
}
