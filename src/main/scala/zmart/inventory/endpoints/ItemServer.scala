package zmart.inventory.endpoints

import io.getquill.context.ZioJdbc.DataSourceLayer
import zhttp.http._
import zhttp.service.Server
import zio.console.Console
import zio.zmx.prometheus.PrometheusClient
import zio.{App, ExitCode, Has, URIO, ZIO}
import zmart.inventory.services.ItemService
import zio.magic._
import zmart.fw.http.auth.AuthenticationApp
import zmart.inventory.repositories.items.ItemRepository

object ItemServer extends App {

  val endpoints: Http[Has[PrometheusClient] with Has[ItemService] with Console, HttpError, Request, Response[Has[ItemService] with Console, HttpError]] =
    MetricsEndpoints.metrics +++ AuthenticationApp.login +++ CORS(
      AuthenticationApp.authenticate(HttpApp.forbidden("None shall pass."), ItemEndpoints.item),
      config = CORSConfig(anyOrigin = true)
    )

  val program: ZIO[Any, Throwable, Nothing] = Server
    .start(8080, endpoints)
    .inject(Console.live, DataSourceLayer.fromPrefix("inventoryDb"), ItemService.layer, ItemRepository.layer, PrometheusClient.live)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.exitCode
}
