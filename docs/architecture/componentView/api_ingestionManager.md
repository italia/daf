# IngestionManager API & endpoints

The IngestionManager provides the following functionalities:

* `ingestion-mgmt/add-with-info/{DatasetCat}` => `IngestionReport`  

  It uses the info contained in the object `DatasetCat` to perform an ingestion of the data referenced in the field `operational -> input_src`. It returns an object (JSON) of type `IngestionReport` containing info on the ingestion process performed and the associated dataset (particularly wrt URI and other calcolated fields that will be needed to finalize the storing of the metadata in the catalog).
`{DatasetCat}` is an object containing catalog info about the dataset to which the incoming data ill be associated.  

* `ingestion-mgmt/add/{dataset query info}/{input file path}` => `IngestionReport`  

  It uses `{dataset query info}` to look up for the dataset in the Catalog. If more than one dataset is found, then an error will be logged and the ingestion will be terminated.  
`{dataset query info}` it is an object (JSON) with the info to be used to look up the dataset from the Catalog.  
`{input file path}` the path of the file containing the data to be ingested. This should normally be placed in the designated entry point folder of HDFS.