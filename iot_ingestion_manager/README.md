## Compile the code

In the directory *daf* run the following commands:

> cd common

> sbt clean compile publishLocal

> cd ../iot_ingestion_manager

> sbt clean compile 

Note
If the last command *sbt clean compile* throws a *java.lang.NullPointerException* :

```bash
[warn] Here are some of the libraries that were evicted:
[warn] 	* com.fasterxml.jackson.module:jackson-module-scala_2.11:(2.6.5, 2.4.2, 2.7.2, 2.6.1) -> 2.7.4
[warn] Run 'evicted' to see detailed eviction warnings
java.lang.NullPointerException
```
you have to run two times the command *sbt compile*
