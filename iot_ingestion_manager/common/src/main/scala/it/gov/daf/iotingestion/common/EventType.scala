package it.gov.daf.iotingestion.common

/**
  * Created by fabiana on 18/07/17.
  */
object EventType extends Enumeration {
  type EventType = Value
  val Metric = Value(0)
  val ChangeState = Value(1)
  val GenericEvent = Value(2)

}
