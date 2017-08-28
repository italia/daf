import java.util.Properties

import it.gov.daf.iotingestion.common.SerializerDeserializer
import it.gov.daf.iotingestion.event.Event
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.rogach.scallop.ScallopConf

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val brokers = opt[String]("brokers", descr = "list of brokers", required = true, short = 'B')
  val numevents = opt[Int]("numevents", descr = "total number of events to generate", required = false, short = 'N', default = Some(10000000))
  verify()
}

object Main extends App {

  val opts = new Conf(args)

  val producerProps = new Properties()
  println(opts.brokers())
  producerProps.setProperty("bootstrap.servers", s"${opts.brokers()}")
  producerProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
  producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
  val producer = new KafkaProducer[Array[Byte], Array[Byte]](producerProps)

  var start = System.currentTimeMillis()
  val NUMEVENTS = opts.numevents()

  def uuid = java.util.UUID.randomUUID

  for (r <- 0 until NUMEVENTS) {
    val event = new Event(
      version = 1L,
      id = uuid.toString,
      ts = start + r,
      event_type_id = 2,
      location = "",
      source = "sensor_id",
      body = None,
      attributes = Map(
        "tag1" -> "value1",
        "tag2" -> "value2"
      )
    )
    val eventBytes = SerializerDeserializer.serialize(event)
    val record = new ProducerRecord[Array[Byte], Array[Byte]]("daf-iot-events", event.id.getBytes(), eventBytes)
    producer.send(record)
    if(r %1000 == 0) {
      println(s"sent $r events")
      producer.flush()
    }
  }
  producer.flush()

  producer.close()
}
