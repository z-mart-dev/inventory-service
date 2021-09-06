package zmart.inventory.endpoints

import io.getquill.context.ZioJdbc.DataSourceLayer
import zhttp.service.Server
import zio.console.Console
import zio.magic._
import zio.zmx.prometheus.PrometheusClient
import zio.{App, ExitCode, URIO, ZIO}
import zmart.fw.http.auth.AuthenticationEndpoints
import zmart.inventory.repositories.items.ItemRepository
import zmart.inventory.services.ItemService

object ItemServer extends App {

  val endpoints = Documentation.redocApp <> MetricsEndpoints.metricsApp <> AuthenticationEndpoints.loginApp <> ItemEndpoints.itemsHttpApp

  val program: ZIO[Any, Throwable, Nothing] = Server
    .start(8080, endpoints)
    .inject(Console.live, DataSourceLayer.fromPrefix("inventoryDb"), ItemService.layer, ItemRepository.layer, PrometheusClient.live)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.exitCode
}
