package zmart.inventory.repositories.items

import zio._

import java.io.Closeable
import javax.sql.DataSource

trait ItemRepository {
  def create(item: ItemRecord): Task[ItemRecord]
  def all: Task[Seq[ItemRecord]]
  def findById(id: Long): Task[ItemRecord]
}

object ItemRepository {
  def create(item: ItemRecord): RIO[Has[ItemRepository], ItemRecord] = ZIO.serviceWith[ItemRepository](_.create(item))
  def all: RIO[Has[ItemRepository], Seq[ItemRecord]]                 = ZIO.serviceWith[ItemRepository](_.all)
  def findById(id: Long): RIO[Has[ItemRepository], ItemRecord]       = ZIO.serviceWith[ItemRepository](_.findById(id))

  val layer: URLayer[Has[DataSource with Closeable], Has[ItemRepository]] = (ItemRepositoryLive(_)).toLayer
}
