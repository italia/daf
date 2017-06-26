# Dataset Catalog Manager
The Catalog Manager is an API manage the creation, update and deletion of datasets in DAF. In particular, it takes care of the metadata information associated to a Dataset ([see here for more info on metadata](https://github.com/lilloraffa/daf-datamgmt/blob/master/datamgmt/metadata/dataset_metadata.md)).

## API Structure
`catalog-ds/add/{info}` => `CatOpsReport`
It creates a new Dataset in the Dataset Catalog. It returns an object (JSON) of type `CatOpsReport` that provides info on the insert operation. In case the input source of the data has been provided. It perform coherence checks that are necessary to save the new dataset (e.g. check vs an associated StandardSchema).   
`{info}` is an object (JSON) containing metadata info for the Dataset to be created.

`catalog-ds/update/{dataset URI o ID}/{info}` => `CatOpsReport`
It updates the info of an existing Dataset. It returns an object (JSON) of type `CatOpsReport` that provides info on the update operation.
`{dataset URI o ID}` can be either the dataset URI or its associated ID.  
`{info}` is an object (JSON) containing metadata info for the Dataset to be created.

`catalog-ds/get/{query info}/[{return info}]` => `List[DatasetCat]`  
It looks for one or more datasets matching the `{query info}` parameters. It is also possible (optional) to specify which info will be retrieved, otherwise the whole dataset info will be passed. It returns a list of `DatasetCat` object, containing metadata info of the Dataset returned.
`{query info}` it is an object (JSON) with the info to be used to look up the dataset from the Catalog.  
`[{return info}]` it is an optional parameter/object (JSON) to restrict the fields to be returned.

`catalog-ds/check-metadata/{info}` => `MetadataCheckReport`  
It checks if the metadata in input (`{info}`) are internally coherent. Moreover, if the metadata contains info on data to be ingested, it also check that the data structure of the input data are coherent with the schema defined in the metadata info. It returns an object of type `MetadataCheckReport` that contains the results of the checks performed.

`catalog-ds/check-metadata-ds/{dataset URI o ID}/{info}` => `MetadataCheckReport`  
It looks up the metadata of the dataset specified via its URI or ID and checks if the retrieved metadata in input (`{info}`) are internally coherent. Moreover, if the metadata contains info on data to be ingested, it also check that the data structure of the input data are coherent with the schema defined in the metadata info. It returns an object of type `MetadataCheckReport` that contains the results of the checks performed.
