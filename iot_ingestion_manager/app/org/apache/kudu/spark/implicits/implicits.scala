package org.apache.kudu.spark

import org.apache.kudu.client.SessionConfiguration.FlushMode
import org.apache.kudu.client.{KuduSession, KuduTable}
import org.apache.kudu.spark.kudu.{KuduContext, KuduRelation}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.types.{DataTypes, StructType}
import org.apache.spark.sql.{DataFrame, Row}

import scala.language.postfixOps

package object implicits {

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Throw"
    )
  )
  implicit class EnrichedKuduContext(kuduContext: KuduContext)
    extends AnyRef
      with Serializable
      with Logging {

    def insertAndReturn(data: DataFrame, tableName: String): DataFrame = {
      val df = writeRows(data, tableName, Insert)
      val count = df.count()
      logInfo(s"Inserted $count rows into Kudu")
      df
    }

    def writeRows(data: DataFrame,
                  tableName: String,
                  operation: OperationType): DataFrame = {
      val schema = data.schema
      implicit val enc: ExpressionEncoder[Row] = org.apache.spark.sql.catalyst.encoders.RowEncoder(schema)
      data.mapPartitions(iterator => {
        writePartitionRows(iterator, schema, tableName, operation)
      })
    }

    def writePartitionRows(
                            rows: Iterator[Row],
                            schema: StructType,
                            tableName: String,
                            operationType: OperationType
                          ): Iterator[Row] = {
      val table: KuduTable = kuduContext.syncClient.openTable(tableName)
      val indices: Array[(Int, Int)] = schema.fields.zipWithIndex.map({
        case (field, sparkIdx) =>
          sparkIdx -> table.getSchema.getColumnIndex(field.name)
      })
      val session: KuduSession = kuduContext.syncClient.newSession
      session.setFlushMode(FlushMode.AUTO_FLUSH_SYNC)
      session.setIgnoreAllDuplicateRows(operationType.ignoreDuplicateRowErrors)
      val insertedRows = try {
        for {
          row <- rows
          insertedRow <- {
            val operation = operationType.operation(table)
            for ((sparkIdx, kuduIdx) <- indices) {
              if (row.isNullAt(sparkIdx)) {
                operation.getRow.setNull(kuduIdx)
              } else
                schema.fields(sparkIdx).dataType match {
                  case DataTypes.StringType =>
                    operation.getRow.addString(kuduIdx, row.getString(sparkIdx))
                  case DataTypes.BinaryType =>
                    operation.getRow
                      .addBinary(kuduIdx, row.getAs[Array[Byte]](sparkIdx))
                  case DataTypes.BooleanType =>
                    operation.getRow
                      .addBoolean(kuduIdx, row.getBoolean(sparkIdx))
                  case DataTypes.ByteType =>
                    operation.getRow.addByte(kuduIdx, row.getByte(sparkIdx))
                  case DataTypes.ShortType =>
                    operation.getRow.addShort(kuduIdx, row.getShort(sparkIdx))
                  case DataTypes.IntegerType =>
                    operation.getRow.addInt(kuduIdx, row.getInt(sparkIdx))
                  case DataTypes.LongType =>
                    operation.getRow.addLong(kuduIdx, row.getLong(sparkIdx))
                  case DataTypes.FloatType =>
                    operation.getRow.addFloat(kuduIdx, row.getFloat(sparkIdx))
                  case DataTypes.DoubleType =>
                    operation.getRow.addDouble(kuduIdx, row.getDouble(sparkIdx))
                  case DataTypes.TimestampType =>
                    operation.getRow.addLong(kuduIdx,
                      KuduRelation.timestampToMicros(
                        row.getTimestamp(sparkIdx)))
                  case t =>
                    throw new IllegalArgumentException(
                      s"No support for Spark SQL type $t")
                }
            }
            val operationResponse = session.apply(operation)
            if (operationResponse.hasRowError)
              None
            else
              Some(row)
          }
        } yield insertedRow
      } finally {
        val _ = session.close()
      }
      insertedRows
    }
  }

}
