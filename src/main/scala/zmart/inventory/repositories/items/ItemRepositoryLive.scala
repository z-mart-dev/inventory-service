package zmart.inventory.repositories.items

import io.getquill.context.ZioJdbc.QuillZioExt
import io.getquill.context.qzio.ImplicitSyntax._
import zio._
import zio.zmx.metrics._
import zmart.fw.db._
import zmart.zmx.ServiceMetrics

import java.io.Closeable
import javax.sql.DataSource

case class ItemRepositoryLive(dataSource: DataSource with Closeable) extends ItemRepository {
  import ZQuillContext._
  implicit val env = Implicit(Has(dataSource))

  override def create(item: ItemRecord): Task[ItemRecord] = transaction {
    for {
      _       <- run(ItemQueries.insertItem(item))
      items   <- run(ItemQueries.itemsQuery)
      created <- ZIO.fromOption(items.headOption).orElseFail(new Exception("Cannot find after create?")) @@ ServiceMetrics.createCountAll
    } yield created
  }.implicitDS

  override def all: Task[Seq[ItemRecord]] = run(ItemQueries.itemsQuery).implicitDS @@ ServiceMetrics.listCountAll

  override def findById(id: Long): Task[ItemRecord] = {
    for {
      results <- run(ItemQueries.byId(id)).implicitDS
      item    <- ZIO.fromOption(results.headOption).orElseFail(NotFoundException(s"Could not find item with id $id", id)) @@ ServiceMetrics.listCountAll
    } yield item
  }

}

object ItemQueries {

  import ZQuillContext._

  // NOTE - if you put the type here you get a 'dynamic query' - which will never wind up working...
  implicit val itemSchemaMeta = schemaMeta[ItemRecord]("item")
  implicit val itemInsertMeta = insertMeta[ItemRecord](_.id)

  val itemsQuery                   = quote(query[ItemRecord])
  def byId(id: Long)               = quote(itemsQuery.filter(_.id == lift(id)))
  def insertItem(item: ItemRecord) = quote(itemsQuery.insert(lift(item)))
}
