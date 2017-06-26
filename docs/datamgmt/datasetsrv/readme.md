# Dataset Services
Dataset is one of the fundamental entities of DAF. Here we discuss the microservices designed to perform operations on Datasets.

## Dataset Catalog Manager
The Catalog Manager is an API manage the creation, update and deletion of datasets in DAF. In particular, it takes care of the metadata information associated to a Dataset ([see here for more info on metadata](https://github.com/lilloraffa/daf-datamgmt/blob/master/datamgmt/metadata/dataset_metadata.md)).
A list of endpoints and functionalities about the [Catalog Manager is here](https://github.com/lilloraffa/daf-datamgmt/blob/master/datamgmt/datasetsrv/api_catalogmanager.md))

## Data Ingestion Manager
The Data Ingestion Manager API takes care of the actual ingestion of data associated to a Dataset (which, as a reminder, is an entity made of a combination of metadata and data). In particular, the API takes as input data and info needed to identify the Dataset to which the data needs to be associated with. Before actually storing the data in DAF, the API performs a set of coherence checks between the metadata contained in the catalogue and the data schema implied in the input data.
A list of endpoints and functionalities about the [Ingestion Manager is here](https://github.com/lilloraffa/daf-datamgmt/blob/master/datamgmt/datasetsrv/api_ingestionmanager.md))


## Dataset Manager
The Dataset Manager API manages operations related to the Dataset entity, such as: returning the data  of the dataset (or a sample of it) in a specified format, create a specific view on top of it, get the dataset schema in a given format (e.g. AVRO), create a new dataset based on an existing one but saved into a different storage mechanism or based on a transformation of the existing dataset [not sure on this one yet, maybe this should be managed by the catalog manager?], etc.
A list of endpoints and functionalities about the [Dataset Manager is here](https://github.com/lilloraffa/daf-datamgmt/blob/master/datamgmt/datasetsrv/api_datasetmanager.md))
