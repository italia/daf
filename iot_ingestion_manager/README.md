## How to publish iot_ingestion_manager project 

In the directory *daf* run the following commands:

> cd common

> sbt compile 

> sbt compile publishLocal

Note
The first execution of the first *compile* command throws a *java.lang.NullPointerException* :

```bash
[warn] Here are some of the libraries that were evicted:
[warn] 	* com.fasterxml.jackson.module:jackson-module-scala_2.11:(2.6.5, 2.4.2, 2.7.2, 2.6.1) -> 2.7.4
[warn] Run 'evicted' to see detailed eviction warnings
java.lang.NullPointerException
```
Therefore, you have to run the command *sbt compile* another time.

> cd ../iot_ingestion_manager/client

> sbt compile publishLocal

> cd /path/to/iot_ingestion_manager/common

> sbt compile publishLocal

> cd ..

> sbt compile publishLocal

