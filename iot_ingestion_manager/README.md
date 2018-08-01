# Iot Ingestion Manager

## Synopsis

This module exposes APIs that allow external services to register their sources with DAF, allowing for real-time ingestion into the DAF platform. Currently, clients are allowed to push events into DAF using the JSON API described below.

The received event will be posted onto a Kafka queue that is dedicated to the specific external source that pushed the event onto DAF. A background Nifi process then acts as a consumer, pulling the data from Kafka and pushing into Kudu as a time-series dataset. The data can then be queried from Kudu using Impala.

As a first implementation, the system will be assumed to be adopting a defensive at-least-once message guarantee, both on the producer and consumer ends. Note that the server assumes no responsibility for failing to produce messages unless it responds with `200`, meaning that the message has been successfully produced to the designated queue. The client should reattempt pushing the event unless the response from the server indicates a fatal and repeatable error, such as `400`.

Also, note that the ordering of the messages **should** be assumed to be mixed, both on the producer and consumer ends. This means that chronological (or otherwise) ordering of events will not be guaranteed, neither on Kafka nor Kudu. 

## API

##### 1. Registration

In a typical workflow, a client will be expected to register its intention to start pushing messages to DAF by first registering an entry in the `catalog-manager`, describing the catalog entry as a `stream`.

The registration process is then done by `POST`ing an empty body to the following API
```bash
http://[host]:[port]/iot-manager/v1/stream/[catalog-id]
```

The server will then retrieve the catalog entry with that `catalog-id` from the `catalog-manager` and attempt create a stream in Nifi using information stored in the catalog. When this is successful, the server will respond with `201 Created`.

##### 2. Event Pushing

After the registration has been done, the client can start pushing events to the server by calling the following API
```bash
http://[host]:[port]/iot-manager/v1/stream/[catalog-id]
```
The client should `PUT` an `Event`, following the representation below:
```json
{
  "id": "some-id",
  "source": "test-source",
  "version": 100,
  "timestamp": 1532423327223,
  "location": {
    "latitude": 66.550331132,
    "longitude": 25.886996452
  },
  "certainty": 0.75,
  "eventType": "metric",
  "customType": "sensor",
  "comment": "Test reading with moderate certainty",
  "payload": {
    "int": 1,
    "string": "two",
    "double": 0.975,
    "boolean": false
  },
  "attributes": {
    "attr1": 1,
    "attr2": 0.975,
    "attr3": false
  }
}
```

The information surrounding the core of the message is contained in the `payload` attribute, which is a JSON object containing any number of properties, the structure of which must match the schema contained in the stream's respective catalog entry. The items here are not processed by the server in that they are simply written as a JSON `String` in the resulting `Event` Avro representation.

Note that the `attributes` item is another JSON object that is treated in much the same way as the `payload`, only it is not validated by the server, but is rather simply serialized to a `String`.

The rest of the information acts as an envelope, which helps the server add some context and metadata to the specific event.

All events coming from Kafka have a common `Event` structure, which is represented in Avro, following the table below:

| Name            | Type     | Description                                                     | Required | Default |
|:----------------|:---------|:----------------------------------------------------------------|:--------:|:-------:|
| version         | long     | Version of this schema                                          | N        | 1       |
| id              | string   | A globally unique identifier for an event                       | Y        |         |
| timestamp       | long     | Timestamp in milliseconds since the epoch                       | Y        |         |
| certainty       | double   | Estimation of the certainty of this particular event \[0,1\]    | N        | 1.0     |
| source          | string   | Name of the entity that originated this event                   | Y        |         |
| type            | enum     | Type of event: `METRIC`, `STATE_CHANGE` `OTHER`                 | Y        |         |
| subtype         | string   | Extra field to qualify the event in its custom domain           | N        |         |
| eventAnnotation | string   | Free-text explanation of what happened in this particular event | N        |         |
| location        | location | Location from which the event was generated                     | N        |         |
| body            | string   | Event content represented in JSON                               | Y        |         |
| attributes      | string   | Event-specific key/value pairs, represented in JSON             | N        |         |

Note that the Avro schemas can be found under the `avro/` directory of this project.

## Data Flow Description

The IoT domain in DAF is centered around the concept of `Event`s, which can take one of a few forms:
- a point of a given time series (e.g. average speed measured by a sensor)
- a changing state event (e.g. event on/off of a sensor) 
- a generic event (e.g. unstructured information send by a sensor). 

The `Event` will also contain some decorating data around the payload itself, which serves to give some context and customization to the meaning of the event.

When starting out, a client is expected to first register the catalog entry to the IoT service by means of the API above. This will create the backend components that are necessary for storage and retrieval of the data, such as the topic in Kafka, the table in Kudu and the Nifi process that will start collecting messages from the designated Kafka topic.

Note that every service will have its own dedicated topic in Kafka and table in Kudu. This level of granularity allows for better management and integration with the current security implementation within DAF. This implies that, much like every other API and service in DAF, all operations and communications that are handled by the IoT manager will be secured following the same reasoning and design in the general DAF security architecture. This implies that only designated users will be allowed to push events on DAF, whose identity may also be stored as part of the envelope information for later auditing.

##### Storage and Analysis

The Kudu tables will also be loaded in Impala and secured with Sentry, allowing for additional methods of exploration besides simply using Spark. The schema is centered around the same concept of `Event`, indexing the `id` and `timestamp` fields to improve query performance.

##### Client Streaming

Authorized clients may be allowed in the future to stream data through a socket connection opened against the server. In those cases the server will first check that the user is authenticated and authorized, before initiating the consumption from the dedicated Kafka topic.
