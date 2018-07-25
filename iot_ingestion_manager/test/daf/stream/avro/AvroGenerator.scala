package daf.stream.avro

import java.io.{ File, FileOutputStream }

import it.gov.daf.iot.event.Event
import org.apache.avro.file.DataFileWriter
import org.apache.avro.io.{ DatumWriter, EncoderFactory }
import org.apache.avro.specific.SpecificDatumWriter
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.scalatest.{ MustMatchers, WordSpec }

import scala.util.{ Random, Success, Try }

class AvroGenerator extends WordSpec with MustMatchers {

  "An Avro generator" must {

    "generate a bunch of events" in {
      write { sample(50) } must be { 'Success }
    }

  }

  private def sample(numEvents: Int) = Gen.listOfN(numEvents, AvroGen.event).pureApply(Gen.Parameters.default, Seed.random())

  private def createSuffix = f"${Random.nextInt(1000)}%04d-${Random.nextInt(1000)}%04d"

  private def write(events: Seq[Event]) = for {
    writer   <- Success { new SpecificDatumWriter[Event](Event.SCHEMA$) }
    fileName <- Success { s"test-dir/events-$createSuffix" }
    _        <- writeJson(events, writer, fileName)
    _        <- writeBinary(events, writer, fileName)
  } yield ()

  private def writeJson(events: Seq[Event],
                        writer: DatumWriter[Event] = new SpecificDatumWriter[Event](Event.SCHEMA$),
                        fileName: String = s"test-dir/events-$createSuffix") = Try {
    val jsonFileStream = new FileOutputStream(s"$fileName.json")
    val jsonEncoder    = EncoderFactory.get().jsonEncoder(Event.SCHEMA$, jsonFileStream)

    events.foreach { writer.write(_, jsonEncoder) }
    jsonEncoder.flush()
    jsonFileStream.close()
  }

  private def writeBinary(events: Seq[Event],
                          writer: DatumWriter[Event] = new SpecificDatumWriter[Event](Event.SCHEMA$),
                          fileName: String = s"test-dir/events-$createSuffix") = Try {
    val binaryFileStream = new FileOutputStream(s"$fileName.avro")
    val binaryEncoder    = EncoderFactory.get().binaryEncoder(binaryFileStream, null)

    events.foreach { writer.write(_, binaryEncoder) }
    binaryEncoder.flush()
    binaryFileStream.close()
  }

  private def writeAvro(events: Seq[Event]) = {
    val eventWriter = new SpecificDatumWriter[Event](classOf[Event])
    val fileWriter  = new DataFileWriter[Event](eventWriter)
    val file = new File(s"test-dir/events-$createSuffix.avro")
    fileWriter.create(Event.SCHEMA$, file)
    events.foreach { fileWriter.append }
    fileWriter.close()
  }

}
