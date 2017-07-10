# Adding a new Dataset

From a user perpective, [to add a new dataset](../userManual/adding-a-new-dataset) is quite simple: it is enough to successfully complete the registration procedure.

From the DAF platform perspective, adding a new dataset means creating a new instance of the DatasetCatalog (**??? verificare**) that will hold all metadata information about the dataset as well as operational info needed for backend operations. 

Data can be added synchronously (i.e. data is saved in DAF at the same time the dataset is created in the [CatalogManager](../../architecture/componentView)) or at a later time (with an append operation on an existing dataset).  

Here you find the logical pipeline followed by the ingestion procedure of a new dataset, together with the details of which microservices will be used to perform the required tasks.

## Create a new Dataset in the CatalogManager
This is the first step to be performed to create a new dataset. It is a compound activities that goes from receiving metadata info about the dataset, perform a series of checks on the coherence of the info, and storing them into the CatalogManager.

### Step 1. Send metadata info
Metadata info are sent either via a webform or by directly calling the `catalogds/add/{info}` API (see the [related doc](../../architecture/componentView/api_CatalogManager.md) for more info on the API). The service will then send the metadata info to the following step of the pipeline that perform checks on them.

### Step 2. Coherence checks
The metadata info are passed to another service that perform the coherence checks on them. The checks aim at verifying the following conditions:

- in case the dataset is linked to a standard schema, it checks that the dataschema and the conversion rules are coherent with respect to the standard schema;
- in case the insert procedure has also data to be ingested, it checks that the dataschema defined is coherent with what is inferred from the data.
The coherence check step returns an report object with info about the test performed.

### Step 3. Calculation of automatically generated fields
The info collected so far is then sent to a module that calculate the automatically generated information to be stored into the CatalogManager, such as the dataset URI, the fact that it is a Standard, Ordinary, or Raw dataset, etc.

### Step 4. Save the metadata info in the Catalog Manager


## Add Data to the new created Dataset
If the input information contains indication of data to be associated with the new Dataset, then the pipeline will continue with the following steps.

### Step 5. Store the data
The dataset URI is then enough to store the dataset in the appropriate hdfs folder and format.

### Step 6. Activate services based on the data ingested
Every time new data comes in (either in case of the creation of a new dataset, or in case we are ingesting data into an existing one), the system activates a list of services that, based on what is defined in the `operational` part of the metadata, perform operations to enable services on the dataset or generate/updates analytics info on it. Some examples of such kind of service are:

- add the dataset in the Hive metacatalog and expose a JDBC connection to it
- calculate automatic statistics on the dataset fields
- add an index in ElasticSearch to allow for search in the content of the dataset
- etc.
