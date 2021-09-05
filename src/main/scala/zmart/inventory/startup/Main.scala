package zmart.inventory.startup

import zio.console.putStrLn
import zio.{App, ExitCode, URIO, ZIO}
import zmart.fw.db.migration.RunMigration
import zmart.inventory.endpoints.ItemServer

object Main extends App {

  val program: ZIO[Any, Throwable, Nothing] = (RunMigration.program *> ItemServer.program)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (putStrLn("Starting up Inventory Service...") *> program).exitCode

}
