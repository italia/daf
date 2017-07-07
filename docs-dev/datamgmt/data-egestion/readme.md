# Data egestion

The standardized data organization and the rich metadata collected makes it possible to expose data in automatic ways via standard API as soon as a new dataset gets created in DAF. In particular, at the time of writing, we are implementing the following methods:

 - **Storage Manager**: it is made by a set of microservices that exposed the following basic functionalities for data retrieval:

  - `{API}/getDataset/{retrieve option}/{output format}/{dataset id, or dataset URI}`: this endpoint is created by default and returns the dataset identified by either its id or URI.    
  `{retrieve option}`: this specifies filters on the data to be retrieved (e.g. can do a bulk extraction, returns only a sample, ecc.)
  `{output format}`: this option specifies the format in which data will be retrieved. A default option is JSON.
  - `{API}/getDS/{keyword}/{dataset id, or dataset URI}`: this is a simplified version of the previous endpoint, that uses specific configuration of it to simplify access.
  - `{API}/searchDS/{search options}/{output options}`: this allows to search the dataset catalogue to find dataset based on the search criteria defined in `{search options}`.
 - **JDBC Connection**: Some dataset can be configured (via the Catalogue Manager) to expose a JDBC connection, managed either via Impala or SparkSQL
 - **HDFS Access via Notebook**: data can also be accessed using Notebook for analysis. Technically, this is done via access to HDFS, user profiling rules defined.
