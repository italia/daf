# Storage Manager


## Deployment

### requirements

1. you should be logged into the internal nexus repository

Since the registry uses https but without a valid certificate, you need to add the registry to the [insecure registries](https://docs.docker.com/registry/insecure/).
Then you can run the following command.

```bash
$ docker login https://10.98.74.120:5000
```

### Publish Image

1. `sbt clean compile`
2. `sbt docker:publish`

### Deploy application

Now we can deploy the application in Kubernetes, with the following steps:

1. ssh in the edge1 or edge2 and check the folder `/glusterfs/volume1/storage-manager/conf` exists
2. place a file `production.conf` with all the production configuration overrides.
3. `cd kubernetes`
4. execute `$ deploy.sh`. it creates a file output.yml that replaces the variable STORAGE_MANAGER_VERSION with 1.0.0-SNAPSTHOT, calls `kubectl delete -f output.yml` and `kubectl create -f output.yml`

## Capabilities

This component exposes APIs to query and download data from HDFS.

In particular it allows to:

1. download a full dataset by a `logical_uri`
2. get the schema of a dataset by a `logical_uri`
3. get the results of a query operated on a dataset, referred to by its `logical_uri`

### Query API

The query API allows filtering of a dataset by application of a subset of functionality as derived from traditional SQL. The exposed functions come in the form of nested JSON objects, as explained below, with an obvious tradeoff between abstraction, verbosity and functionality.

#### Representation

Below you can find an example of body that can be used to make queries.

```bash
http://[host]:[port]/dataset-manager/v1/dataset/[logical-url]/search
```

where `logical-url` would be a URL-encoded URI as defined by the catalog-manager APIs, e.g. `daf%3A%2F%2Fdataset%2Ford%2Falessandro%2Fdefault_org%2FAGRI%2Forganizzazioni%2Fagency_infer_ale`

A query is made up of clauses, of which the following are currently supported:

1. select
2. where
3. groupBy
4. having
5. limit

##### 1. select

In the example below, we are selecting a column named `col1` and a column named `col2` with an alias of `alias_col2`.

```json
{
  "select": [
    { "name": "col1" }, 
    { "name": "col2", "alias": "alias_col2" }
  ]
}
```

This is equivalent to an SQL of

```sql
SELECT col1, col2 AS alias_col2
FROM table
```

Note that the `select` clause is optional, where the wildcard `*` is assumed in case it is omitted.

##### 2. where

In the example below, we are applying a filter where `col1` must be greater than `col2`.

```json
{
  "where": {
    "gt": { "left": "col1", "right": "col2" }
  }
}
```

This is equivalent to an SQL of

```sql
SELECT *
FROM table
WHERE col1 > col2
```

More complex filters are also possible by nesting logical operators such as `and`, `or` and `not`

```json
{
  "where": {
    "not": {
      "and": [{
        "or": [{
          "gt": { "left": "col1", "right": "col2" }
        }, {
          "eq": { "left": "col3", "right": false }
        }]
      }, {
        "neq": { "left": "col4", "right": "'string'" }
      }]
    }
  }
}
``` 

This is equivalent to an SQL of

```sql
SELECT *
FROM table
WHERE NOT(
      col1 > col2 
   OR col3 == false
  AND col4 <> 'string'
)
```

Note how `string` is enclosed in single quotes to represent the constant `string` in the JSON example.

##### 3. groupBy

In this example, we are grouping by `col1` and `col2`, aggregating `col3` using `max` and counting all in-group occurrences, applying no filters.

```json
{
  "select": [
    { "name": "col1" },
    { "name": "col2" },
    {
      "max": { "name": "col3", "alias": "max_col3" }
    }, {
      "count": { "name": "*" }
    }
  ],
  "groupBy": [
    { "name": "col1" }, 
    { "name": "col2" }
  ]
}
```

This is equivalent to an SQL of

```sql
SELECT col1, col2, MAX(col3) AS max_col3, COUNT(*)
FROM table
GROUP BY col1, col2
```

Currently, the validation for `groupBy` queries is pushed down to the query-engine, which validates the query before applying it to the dataset. This relieves he web-layer from complex validations, but in turn gives up control over the error messages that can be returned in case of invalid group-by syntax.

##### 4. having

In this example, we extend the one we've seen above in 3., adding a `having` clause, which is used to filter out group-by result by aggregations.

```json
{
  "select": [
    { "name": "col1" },
    { "name": "col2" },
    {
      "max": { "name": "col3" },
      "alias": "max_col3"
    }, {
      "count": { "name": "*" }
    }
  ],
  "groupBy": [
    { "name": "col1" }, 
    { "name": "col2" }
  ],
  "having": [
    { 
      "gt": { "left": "max_col3", "right": 50 } 
    }
  ]
}
```

This is equivalent to an SQL of

```sql
SELECT col1, col2, MAX(col3) AS max_col3, COUNT(*)
FROM table
GROUP BY col1, col2
HAVING max_col3 > 50 
```

Note that the application will not allow a query to have a `having` clause without a `groupBy`.

##### 5. limit

The `limit` clause will simply return at most the requested number of rows in the result.

```json

{
  "limit": 5
}
```

This is equivalent to an SQL of

```sql
SELECT * 
FROM table
LIMIT 5
```

#### Appendix

This section will outline the different types of representations exposed by the Query API, attempting to make parallels with SQL where possible for simplicity.

##### Column Representation

A Column can take one various shapes, where each is possible to use in different contexts.

###### named-column

Represents a column in a table, referred to by `name` and possibily given an alias.

```json
{ "name": "col1" } 
{ "name": "col2", "alias": "col2_alias" }
```

If used in a `select`, in SQL this would be equivalent to

```sql
SELECT col1, col2 AS col2_alias
```

###### constant-value-column

Represents a column with a constant and possibly an alias.

```json
{ "value": "string" } 
{ "value": 1, "alias": "one_alias" }
```

If used in a `select`, in SQL this would be equivalent to

```sql
SELECT 'string', 1 AS one_alias
```

###### aggregated-column

Represents a column to which an aggregation function is applies, possibly given an alias

```json
{ 
  "max": { "name": "col1", "alias": "max_col1" }
} 
```

In a `select` in SQL, this would be equivalent to

```sql
SELECT MAX(col1) AS max_col1
```

The API currently supports only `min`, `max`, `count`, `sum` and `avg`, which may be extended to support more aggregation in the near future.

##### Filter Operator Representation

Filter operators can be either `comparison` operators, which compare columns to values or other column, or `logical`, which act to affect `comparison` operators.

An example of a `comparison` operator would be `gt`, which can be represented as

```json
{ 
  "gt": { "left": "col3", "right": 50 }
}
{
  "eq": { "left": "col1", "right": "col2" }
}
```

Here we see first a `gt` comparison between a column `col3` and a constant value `50`, whereas in the second we see an `eq` comparison between two columns `col1` and `col2`.
The API current supports only `gt`, `gte`, `lt`, `lte`, `eq` and `neq`, including support for `in` and `like` (with limitations) in the future.

`logical` operators can be used to decorate or group a number of `comparison`s or other filters. This means that `logical` operators can be nested to create complex filters.

```json
{
  "not": { 
    "gt": { "left": "col3", "right": 50 }
  }
}
{
  "and": [{
    "gt": { "left": "col3", "right": 50 }
  }, {
    "eq": { "left": "col1", "right": "col2" }
  }]  
}
{
  "or": [{
    "gt": { "left": "col3", "right": 50 }
  }, {
    "eq": { "left": "col1", "right": "col2" }
  }]  
}
```