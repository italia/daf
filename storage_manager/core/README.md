# Storage Manager

This library exposes the logic to query: HDFS, KUDU and OpenTSDB.
In particular it allows to:

1. get a dataset by a `logical_uri`
2. get the schema of a dataset by a `logical_uri`

Given a Dataframe, it can
1. filtering on a set of `columns`
4. apply `where` conditions
5. apply `groupBy` on a grouping column applying multiple grouping conditions of type column -> aggregation function.

The supported aggregation functions are: ["count", "max", "mean", "min", "sum"]
All the implemented operations takes as input a Try[DataFrame] and a list of parameters

```scala
import org.apache.spark.sql.DataFrame
import scala.util.{Failure, Success, Try}

   def implementedFunction(df: Try[DataFrame], params: Any*): Try[DataFrame] = ???
```

To check how to use this library please take a look at the tests.

## How to use it

... WIP

## Test

All the test can be executed by running:

```sbtshell

sbt test
```
    