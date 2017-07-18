# Data at rest

Once data arrive in the landing area, they are stored in the HDFS adopting the rules described above.

It is worth noting data are always related to a dataset and they are converted in AVRO format by-default. Furthermore, a copy of the unaltered data sent by data-sources is always saved. 

On the basis of the settings provided by the dataset owner during the registration phase, data can be also stored as Parquet file.

## Data organization in HDFS

Data in HDFS are stored adopting the following folder structure.
The forder structure is designed to be as flexible as possible based on the use cases of each dataset type (Standard, Ordinary and Raw).


### Standard Dataset Directory Structure

All Standard datasets are stored in the following HDFS directory:

`/ daf / standard / `

The content of this directory is organized adopting the following rules: 

` domain / subdomain / datasetName.datasetFormat / sourceOrg / `

where:

- `domain` is the parent category to which the dataset belong (e.g. "mobility")
- `subdomain` is the sub-category to which the dataset belong (e.g. "traffic")
- `datasetName` is the name of the dataset
- `datasetFormat` specifies the serialization format. At the moment the Allowed format are: `csv`, `json`, `avro`, `parquet`. 
- `sourceOrg` is the name of the dataset owner. This name is specified as `organizationType_organizationName`.



### Ordinary Dataset Directory Structure 

All Ordinary datasets are stored in the following HDFS directory:

`/ daf / ordinary / `

The content of this directory is organized adopting the following rules: 

` sourceOrg / domain / subdomain / datasetName.datasetFormat / `

where:

- `sourceOrg` is the name of the dataset owner. This name is specified as `organizationType_organizationName`
- `domain` is the parent category to which the dataset belong (e.g. "mobility")
- `subdomain` is the sub-category to which the dataset belong (e.g. "traffic")
- `datasetName` is the name of the dataset
- `datasetFormat` specifies the serialization format. At the moment the Allowed format are: `csv`, `json`, `avro`, `parquet` 


### Raw Dataset Directory Structure

All Raw datasets are stored in the following HDFS directory:

`/ daf / raw / `

The content of this directory is organized adopting the following rules: 

` sourceOrg / domain / subdomain / datasetName.datasetFormat / `