package zmart.fw.db.migration

import zio._
import zio.console._
import zio.magic._

object RunMigration extends App {

  val program: ZIO[Any, Throwable, Unit] = {
    for {
      _ <- putStrLn("Starting migrations...")
      _ <- MigrationService.clean *> MigrationService.runMigrations
      _ <- putStr("Completed.")
    } yield ()
  }.inject(
    Console.live,
    MigrationService.live
  )

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.exitCode
}
