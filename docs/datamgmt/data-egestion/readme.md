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

 
## Data Portal
Data Portal is a web based user interface for the DAF, giving users the capabilities to access to DAF functionalities and data products. The Data Portal will expand the typical features of the popular data catalogues (e.i. dati.gov.it) with the following:

- **Data Visualization Templates**: they are modules that generate a specific data visualization based on a data source specified. They will give the user the possibility to use an already implemented standard visualizations (i.e. line graph, bar chart, pie chart, etc.) or to create his own graph in d3.js (or any other javascript graph libraries supported), and they will natively integrate with the DAF api exposing data. Data Visualization Template are also managed by their CMS, to help for search and reusability.
- **Data Stories**: they are entities containing stories and analysis about a specific phenomena described with data, and can be made of text, Data Visualization objects, notebooks, gists/github resources. They are basically the way users create data related content in the Data Portal.
- **Social & Collaboration**: Users can create their own Data Stories and share them on the platform so other users can read and/or fork them to build a new Data Story on top of an existing one.
- **User Profile**: a restricted area where the user can see and manage the content she created, as well as have access to personal data that are available in DAF (i.e. the citizen dashboard). The latter functionality will be accessed with a 2nd/3rd level SPID, meanwhile for the first part a lighter registration may be enough.
