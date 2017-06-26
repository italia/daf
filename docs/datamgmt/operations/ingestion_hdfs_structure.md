# Ingestion & HDFS Folder Structure
Regardless of the nature of the dataset to be ingested in the platform, the input dataset get stored into a landing area in HDFS. Once there, the platform activate the following pipeline:
- it reads all files contained in a configurable folder. Here, data should be organized into subfolders corresponding to each data owner (the entity that sends the data into the platform).
- Once the dataset has been read, the module looks for the Conversion Schema associated with it. If the it is found, the module try to see if there is an associated "standard schema".
- If the Standard Schema is found, then the module does coherence checks to make sure the input schema is made according to the predefined Standard Schema
- Then the actual dataset is checked wrt the resulting schema. If all the checks passed, then the dataset gets saved into DAF.
- Saving methodology depends on the fact that the dataset belongs to the Standard dataset or not.
- In case there is no Conversion Schema associated with the dataset, the platform consider it as a Raw dataset, so it tries to find a corresponding basic Metadata, and if found, it saves the data accordingly.

## HDFS Folder Structure
The folder structure in HDFS and, eventually, the structure in a database like HBase if required, is designed to be as flexible as possible based on the use cases of each dataset type (Standard, Ordinary and Raw).

### Standard Dataset Structure
The structure in HDFS works as follows: `/ stdData / Category / Group Ownership / Dataset / [partition by owner]`
- `stdData` is the root directory containing all Standard Dataset
- `Category` is the parent category to which the dataset belong (e.g. "Mobility")
- `Group Ownership` is the starting point from which access rights get assigned (e.g. "Open" for open data accessible to anyone)
- `Dataset` contains the actual data
- `[partition by owner]` is a partitioning mechanism internal to the data format used.

A similar structure will be implemented in HBase in case the dataset is required to be stored there.

As for the data structure, it works as follows applied to a .parquet format:
- List of the required fields of the Standard Dataset
- `opt_std`: a JSON structured field containing all the optional fields defined in the Standard Dataset and provided in the dataset being ingested
- `custom`: a JSON structured field containing all the fields that are not part of the Standard Schema but that are provided in the dataset being ingested
- `owner`: the name of the entity to which the data belongs (e.g. "Milano")
- `genre`: the root category to which the dataset belong (e.g. "Mobility")
- `ts`: the timestamp in seconds of the time of the ingestion

### Ordinary Dataset Structure - Alternative 1
The structure in HDFS works as follows: `/ ordData / Owner / Category / Group Ownership / Dataset`
- `ordData` is the root directory containing all Ordinary Dataset
- `Owner` is the name of the entity to which the data belongs (e.g. "Milano")
- `Category` is the parent category to which the dataset belong (i.e. "Mobility")
- `Group Ownership` is the starting point from which access rights get assigned (e.g. "Open" for open data accessible to anyone)
- `Dataset` contains the actual data

A similar structure will be implemented in HBase in case the dataset is required to be stored there.

As for the data structure, it works as follows applied to a .parquet format:
- `raw_data`: Struct of the fields of the dataset
- `owner`: the name of the entity to which the data belongs (e.g. "Milano")
- `genre`: the root category to which the dataset belong (e.g. "Mobility")
- `ts`: the timestamp in seconds of the time of the ingestion

### Ordinary Dataset Structure - Alternative 2
The structure in HDFS works as follows: `/ ordData / Group Ownership / Dataset / [ partition by owner and category]`
- `ordData` is the root directory containing all Ordinary Dataset
- `Group Ownership` is the starting point from which access rights get assigned (e.g. "Open" for open data accessible to anyone)
- `Dataset` contains the actual data
- `[partition by owner and category]` is a partitioning mechanism internal to the data format used.

A similar structure will be implemented in HBase in case the dataset is required to be stored there.

As for the data structure, it works as follows applied to a .parquet format:
`custom`: a JSON structured field containing all the fields of the dataset being ingested
`owner`: the name of the entity to which the data belongs (e.g. "Milano")
`genre`: the root category to which the dataset belong (e.g. "Mobility")
`ts`: the timestamp in seconds of the time of the ingestion


### Raw Dataset Structure
It has the same structure as Ordinary Dataset - Alternative 2
