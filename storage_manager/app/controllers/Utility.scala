package controllers

import java.io.StringWriter

import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Null",
    "org.wartremover.warts.AsInstanceOf"
  )
)
object Utility {

  def rowToJson(rowType: DataType)(row: Row): String = {
    val sw = new StringWriter()
    val jf = new JsonFactory()
    val g: JsonGenerator = jf.createGenerator(sw)

    def go: (DataType, Any) => Unit = {
      case (_, null) | (NullType, _) => g.writeNull()
      case (StringType, value: String) => g.writeString(value)
      case (BinaryType, value: Array[Byte]) => g.writeBinary(value)
      case (ByteType, value: Byte) => g.writeNumber(value.toInt)
      case (ShortType, value: Short) => g.writeNumber(value)
      case (IntegerType, value: Int) => g.writeNumber(value)
      case (LongType, value: Long) => g.writeNumber(value)
      case (DecimalType(), value: java.math.BigDecimal) => g.writeNumber(value)
      case (TimestampType, value: java.sql.Timestamp) => g.writeString(value.toString)
      case (FloatType, value: Float) => g.writeNumber(value)
      case (DoubleType, value: Double) => g.writeNumber(value)
      case (BooleanType, value: Boolean) => g.writeBoolean(value)
      case (ArrayType(ty, _), values: Seq[_]) =>
        g.writeStartArray()
        values.foreach(go(ty, _))
        g.writeEndArray()

      case (StructType(ty), values: Any) =>
        g.writeStartObject()
        ty.zip(values.asInstanceOf[Row].toSeq).foreach {
          case (_, null) =>
          case (field, value) =>
            g.writeFieldName(field.name)
            go(field.dataType, value)
        }
        g.writeEndObject()
    }

    go(rowType, row)
    g.close()
    sw.toString
  }

}