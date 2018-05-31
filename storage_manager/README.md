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

Given a `Dataframe`, queries can operate to
1. filter on a set of `columns`
2. apply `where` conditions
3. apply `groupBy` on a grouping column, applying multiple grouping conditions of type `column -> aggregation function`.

The supported aggregation functions are: `["count", "max", "mean", "min", "sum"]`

### Example queries

Below you can find an example of body that can be used to make queries.

```bash
http://storage-manager.default.svc.cluster.local:9000/dataset-manager/v1/dataset/daf%253A%252F%252Fdataset%252Ford%252Falessandro%252Fdefault_org%252FAGRI%252Forganizzazioni%252Fagency_infer_ale/search
```

#### Where

```json

{
  "where": [
    "num > 21",
    "agency_name == 'GTT servizio extraurbano'"
  ]
}
```

#### Filter

```json

{
  "select": ["agency_url"]
}

```

#### GroupBy

```json

{
  "groupBy": {
    "groupColumn": "agency_lang",
    "conditions": [
      {
        "column": "num",
        "aggregationFunction": "max"
      }
    ]
  }
}
```

#### Limit

```json

{
  "limit": 5
}
```
