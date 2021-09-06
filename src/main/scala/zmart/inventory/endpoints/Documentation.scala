package zmart.inventory.endpoints

import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.redoc.Redoc
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir._
import zhttp.http.{Http, Request, Response}
import zio.zmx.prometheus.PrometheusClient
import zio.{Has, RIO, Task}
import zmart.fw.http.auth.AuthenticationEndpoints
import zmart.inventory.services.ItemService

object Documentation {
  type Env           = Has[PrometheusClient] with Has[ItemService]
  type EffectType[A] = RIO[Env, A]

  val allEndpoints =
    List(MetricsEndpoints.prometheusEndpoint.widen[Env], AuthenticationEndpoints.loginServerEndpoint.widen[Env]) ++ ItemEndpoints.allEndpoints.map(
      _.widen[Env]
    )

  val openApi: OpenAPI                                                  = OpenAPIDocsInterpreter().serverEndpointsToOpenAPI(allEndpoints, "Items API", "1.0")
  val redocApp: Http[Any, Throwable, Request, Response[Any, Throwable]] = ZioHttpInterpreter().toHttp(Redoc[Task](title = "Items API", yaml = openApi.toYaml))
}
