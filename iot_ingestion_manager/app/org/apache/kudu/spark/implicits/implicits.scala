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
      "org.wartremover.warts.DefaultArguments",
      "org.wartremover.warts.Throw"
    )
  )
  implicit class EnrichedKuduContext(kuduContext: KuduContext)
    extends AnyRef
      with Serializable
      with Logging {

    /**
      * It's a special method that allows to store a dataframe into kudu controlling the key uniqueness.
      * It returns a new dataframe where all the rows containing repetead keys are filtered out.
      * It can be used to check the idempotency of a stream.
      *
      * @param data      the dataframe to be inserted
      * @param tableName the kudu table's name
      * @return a dataframe of rows successfully inserted
      */
    def insertAndReturn(data: DataFrame, tableName: String): DataFrame = {
      val schema = data.schema
      implicit val enc: ExpressionEncoder[Row] = org.apache.spark.sql.catalyst.encoders.RowEncoder(schema)
      data.mapPartitions(iterator => {
        writePartitionRows(iterator, schema, tableName)
      })
    }

    /**
      * It stores rows into Kudu checking the uniqueness of the keys, it returns the rows that have been inserted successfully.
      *
      * @param rows      the rows to be inserted
      * @param schema    the row's schema
      * @param tableName the name of the kudu table
      * @return an iterator on all the rows that have been inserted successfully into kudu
      */
    private def writePartitionRows(
                                    rows: Iterator[Row],
                                    schema: StructType,
                                    tableName: String
                                  ): Iterator[Row] = {
      val table: KuduTable = kuduContext.syncClient.openTable(tableName)
      val indices: Array[(Int, Int)] = schema.fields.zipWithIndex.map({
        case (field, sparkIdx) =>
          sparkIdx -> table.getSchema.getColumnIndex(field.name)
      })
      val session: KuduSession = kuduContext.syncClient.newSession
      session.setFlushMode(FlushMode.AUTO_FLUSH_SYNC) //This is important to be able to check the key uniqueness record by record
      session.setIgnoreAllDuplicateRows(false)
      val insertedRows = try {
        for {
          row <- rows
          insertedRow <- {
            val operation = table.newInsert()
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
