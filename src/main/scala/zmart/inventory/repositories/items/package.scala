package zmart.inventory.repositories

import java.time.Instant

package object items {
  case class ItemRecord(id: Long = -1, name: String, description: String, unitPrice: Double, createdAt: Instant = Instant.now)
  case class InvoiceRecord(id: Long = -1, userId: Long, total: Double, paid: Boolean, createdAt: Instant)

}
