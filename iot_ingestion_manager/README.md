# Iot Ingestion Manager

## Synopsis

This project  ingests IoT data from a Kafka queue and store them into HDFS and KUDU. All data coming from Kafka are represented as  `Event` and are stored in HDFS, KUSU as  [`HdfsEvent`](), [`KuduEvent`]() (respectively).

The ingestion is processed real time using Spark Streaming .

## Development

### Directory structure
-   `project`: project definition files
-   `src`: source code and property files
- `note`: ipython notebooks for querying KUDU and HDFS 

### Built with

-  Scala 2.11.8
-  Apache Kafka 0.10.X
-  Kudu 1.4.0
- Spark 2.2.X 
- Hadoop 2.6.0

More detailed information about dependencies and their version could be found within  `build.sbt`  file.    

### How to Run

First compile the code:
```bash
> sbt clean compile test
> sbt universal:packageBin
```
The last command will generate a compress folder *iotingestionmanager-2.0.0.zip* (in ./target/universal/) containing the jar of our application plus all shared libraries.
```bash
unzip iotingestionmanager-2.0.0.zip
mv iotingestionmanager-2.0.0/lib/it.teamdigitale.iotingestionmanager-2.0.0.jar ./iotingestionmanager-2.0.0/ 
```
Then execute it using a spark shell:
```bash
export JARS=$(JARS=(./target/universal/lib/*.jar); IFS=,; echo "${JARS[*]}")

export SPARK_KAFKA_VERSION=0.10

 spark2-submit \
 --class it.teamdigitale.Main \
 --principal name@DAF.GOV.IT \
 --master yarn \
 --deploy-mode cluster \
 --keytab /path/to/key.keytab \
 --files "/path/to/application.conf" \
 --conf "spark.driver.extraJavaOptions= -Dconfig.file=application.conf" \
 --conf "spark.driver.extraClassPath=application.conf" \
 --properties-file "path/to/spark.conf" \
 --jars $JARS it.teamdigitale.iotingestionmanager-2.0.0.jar
```
Note that:
* *--properties-file* option set extra Spark properties. [Here]() an example of spark property file; 
* *--files* option distributes input files on driver and executor machines
* *--jars* is a comma-separated list of local jars to include on the driver’s and executors' classpaths

## Technologies involved

**Apache Kudu** does incremental inserts of the events. Its aim is to provide a hybrid storage layer between HDFS (leveraging its very fast scans of large datasets) and HBase (leveraging its fast primary key inserts/lookups). Kudu can provide much faster scans of data for analytics, compliments of its columnar storage architecture.
**Apache HDFS** all IoT data are stored into HDFS.
**Apache Kafka** allows us to abstract the data ingestion in a scalable way, versus tightly. coupling it to the Spark streaming framework (which would have allowed only a single purpose). Kafka is attractive for its ability to scale to millions of events per second, and its ability to integrate well with many technologies like Spark Streaming.
**Spark Streaming** is able to represent complex event processing workflows with very few lines of code (in this case using Scala).
**Impala** enables us to easily analyze data that is being used in an ad-hoc manner. I used it as a query engine to directly query the data that I had loaded into Kudu to help understand the patterns I could use to build a model. 


## Data Flow Description
The IoT Ingestion Manager handles all IoT events ingested within DAF. In particular, each IoT event could be:
- a point of a given time series (e.g. average speed measured by a sensor)
- a changing state event (e.g. event on/off of a sensor) 
- a generic event (e.g. unstructured information send by a sensor). 

All events coming from Kafka have a common structure: `Event`. Then, they are processed and transformed to the appropriate format for HDFS and KUDU.

Finally, users can analyze stored data via Impala and Spark.

### HDFS Schema Design
All IoT events are stored in HDFS, In particular, they are partitioned by `source` and `day`.  This improves query performance based on these fields.

### Kudu Schema Design
Kudu tables have a structured data model similar to tables in a traditional RDBMS. Our Event Table is designed considering the following best practices:

	1. Data would be distributed in such a way that reads and writes are spread evenly across tablet servers. This is impacted by partitioning.
	2. Tablets would grow at an even, predictable rate and load across tablets would remain steady over time. This is most impacted by partitioning.
	3. Scans would read the minimum amount of data necessary to fulfill a query. This is impacted mostly by primary key design, but partitioning also plays a role via partition pruning.  

Kudu is characterized by two main concepts:
* **Partitioning**: how row are mapped to tablets. With either type of partitioning, it is possible to partition based on only a subset of the primary key column. For example, a primary key of “(host, timestamp)” could be range-partitioned on only the timestamp column.  We should avoid hotspot issues;
* **Indexing**: how data, within each partition, gets sorted. Within any tablet, rows are written in the sort order of the primary key. In the case of a compound key, sorting is determined by the order that the columns in the key are declared. For hash-based distribution, a hash of the entire key is used to determine the “bucket” that values will be placed in. Indexing is used for uniqueness as well as providing quick access to individual rows. 

The above concepts are critical for Kudu performance and stability.

In the following we describe common query that user can do on the Event table:
* Get all data points of a given time series;
* Get all data points of a time series ranging into an interval time;
* Get all events in a given temporal range;

To optimize the above query we identify the following primary keys:
*	source: id of the entity that originated this event (e.g. url of web service). 
*	event_subtype_id: It's an additional field that can be used to additionally qualify the event. It is used with source attribute for identifying uniquely a timeseries.
*	ts: epoch timestamp in milliseconds. 

Finally we use source and ts as indices.