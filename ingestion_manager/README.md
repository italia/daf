# Ingestion Manager

The ingestion manager given a dataset definition from the catalog-manager
creates the needed NIFI processor to instrument the ingestion of the dataset.

## Setup

In order to start developing do the following steps


```bash
# publish locally the daf-common project
$ cd ../common && sbt publishLocal

# publish locally the catalog manager
$ cd ../catalog-manager && sbt publishLocal

```


