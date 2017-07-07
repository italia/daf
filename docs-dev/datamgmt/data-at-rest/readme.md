# Data at rest

The folder structure in HDFS and, eventually, the structure in a database like HBase if required, is designed to be as flexible as possible based on the use cases of each dataset type (Standard, Ordinary and Raw).



## Standard Dataset Structure

The structure in HDFS works as follows: 

`/ stdData / Category / Group Ownership / Dataset / [partition by owner]`

where:

- `stdData` is the root directory containing all Standard Dataset
- `Category` is the parent category to which the dataset belong (e.g. "Mobility")
- `Group Ownership` is the starting point from which access rights get assigned (e.g. "Open" for open data accessible to anyone)
- `Dataset` contains the actual data
- `[partition by owner]` is a partitioning mechanism internal to the data format used.

A similar structure will be implemented in HBase in case the dataset is required to be stored there.

As for the data structure, it works as follows applied to a `.parquet` format:

- List of the required fields of the standard dataset
- `opt_std`: a JSON structured field containing all the optional fields defined in the Standard Dataset and provided in the dataset being ingested
- `custom`: a JSON structured field containing all the fields that are not part of the Standard Schema but that are provided in the dataset being ingested
- `owner`: the name of the entity to which the data belongs (e.g. "Milano")
- `genre`: the root category to which the dataset belong (e.g. "Mobility")
- `ts`: the timestamp in seconds of the time of the ingestion

## Ordinary Dataset Structure 
###Alternative 1
The structure in HDFS works as follows: 

`/ ordData / Owner / Category / Group Ownership / Dataset`

where:

- `ordData` is the root directory containing all Ordinary Dataset
- `Owner` is the name of the entity to which the data belongs (e.g. "Milano")
- `Category` is the parent category to which the dataset belong (i.e. "Mobility")
- `Group Ownership` is the starting point from which access rights get assigned (e.g. "Open" for open data accessible to anyone)
- `Dataset` contains the actual data

A similar structure will be implemented in HBase in case the dataset is required to be stored there.

As for the data structure, it works as follows applied to a `.parquet` format:

- `raw_data`: Struct of the fields of the dataset
- `owner`: the name of the entity to which the data belongs (e.g. "Milano")
- `genre`: the root category to which the dataset belong (e.g. "Mobility")
- `ts`: the timestamp in seconds of the time of the ingestion

### Alternative 2
The structure in HDFS works as follows: 

`/ ordData / Group Ownership / Dataset / [ partition by owner and category]`

where:

- `ordData` is the root directory containing all ordinary dataset
- `Group Ownership` is the starting point from which access rights get assigned (e.g. "Open" for open data accessible to anyone)
- `Dataset` contains the actual data
- `[partition by owner and category]` is a partitioning mechanism internal to the data format used.

A similar structure will be implemented in HBase in case the dataset is required to be stored there.

As for the data structure, it works as follows applied to a `.parquet` format:

- `custom`: a JSON structured field containing all the fields of the dataset being ingested
- `owner`: the name of the entity to which the data belongs (e.g. "Milano")
- `genre`: the root category to which the dataset belong (e.g. "Mobility")
- `ts`: the timestamp in seconds of the time of the ingestion


## Raw Dataset Structure
It has the same structure as Ordinary Dataset - Alternative 2
