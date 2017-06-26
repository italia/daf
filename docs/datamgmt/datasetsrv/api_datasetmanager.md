# Dataset Manager
The Dataset Manager API manages operations related to the Dataset entity, such as: returning the data  of the dataset (or a sample of it) in a specified format, create a specific view on top of it, get the dataset schema in a given format (e.g. AVRO), create a new dataset based on an existing one but saved into a different storage mechanism or based on a transformation of the existing dataset [not sure on this one yet, maybe this should be managed by the catalog manager?], etc.

## API Structure
`dataset-mgmt/get-data/{dataset URI o ID}/[{query type}]/[{output data format}]/[{sql query on data}]` => `DatasetData`  
It retrieves the data of the dataset and allows for options specifying the data format (i.e. JSON, CSV, etc.) of the output, the type of query to be performed (i.e. a bulk operation, a sample, etc.), and a SQL query (other querying languages may be considered for the future) to perform specific queries on the dataset.  There may be simpler version of this endpoint with predefined options.  
`{dataset URI o ID}` can be either the dataset URI or its associated ID.  
`[{query type}]` is optional and specifies the type of data result wanted. For example, `bulk` will retrieve the entire dataset (access rights to bulk operation should be carefully set), `sample` will retrieve a 5% sample of the entire dataset, `sample10` will retrieve the 10%, `sample100R` will retrieve the first 100 rows, etc.  
`[{output data format}]` is an optional field specifying the data format the output will have, e.g. `json`, `csv`, `avro`, etc.  
`[{sql query on data}]` is optional and allows to specify a SQL query on the data.

`dataset-mgmt/feat-jdbc/{enble/disable}/{dataset URI o ID}` => `DsFeatReport`  
API that enable or disable the feature "Expose a JBDC connector for the dataset". It returns an object of type `DsFeatReport` which contains info on the operation performed.  
`{enble/disable}` it tells to enable or disable the feature.

`dataset-mgmt/feat-jdbc/get/{dataset URI o ID}` => `DsFeatJdbcConn`  
API that returns a `DsFeatJdbcConn` object (JSON) that contains info on the JDBC connection exposed by the dataset, if enabled.
`{enble/disable}` it tells to enable or disable the feature.
