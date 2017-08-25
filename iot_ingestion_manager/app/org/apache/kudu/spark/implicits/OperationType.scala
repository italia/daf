package org.apache.kudu.spark.implicits

import org.apache.kudu.client.{KuduTable, Operation}

sealed trait OperationType {
  def operation(table: KuduTable): Operation

  def ignoreDuplicateRowErrors: Boolean = false
}

case object Insert extends OperationType {
  override def operation(table: KuduTable): Operation = table.newInsert()
}

case object InsertIgnore extends OperationType {
  override def operation(table: KuduTable): Operation = table.newInsert()

  override def ignoreDuplicateRowErrors: Boolean = true
}

case object Update extends OperationType {
  override def operation(table: KuduTable): Operation = table.newUpdate()
}

case object Upsert extends OperationType {
  override def operation(table: KuduTable): Operation = table.newUpsert()
}

case object Delete extends OperationType {
  override def operation(table: KuduTable): Operation = table.newDelete()
}
