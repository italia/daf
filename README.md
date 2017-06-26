# Data & Analytics Framework (DAF)
The Data & Analytics Framework (DAF) is a combination of:
- A **Big Data Platform** to centralize and store (data lake), manipulate and standardize (data engine) and re-distribute (API & Data Application) data and insights.
- A **Data Team** (data scientists + data engineers) which uses and evolve the Big Data Platform to analyze data, create ML models and build data applications and data visualization products.

DAF has a twofold goal: 1. eliminate silos into which data are typically "trapped", and make entities be logically interconnected so to shed light on phenomena by looking at them via multiple perspectives; 2. easy analysts and data scientists make analysis and build applications that uses the insights from data to create "intelligent" services.

## Logical Components
DAF is made of the following logical components, that are technically detailed in the [Architecture document here](https://github.com/teamdigitale/daf/blob/master/docs/architecture/architecture.md).

### Data Ingestion & Catalogue Manager
In DAF, data is organized into logical entities called *dataset*. A dataset is a combination of metadata, data, and other operational info needed to manage ingestion, update, lookup, creation of views and alike. It is general purpose, in the sense that it can be used to model both batch, streaming, semi-structured data.

The Data Ingestion & Catalogue Manager module has the following goals/functionalities:
- It is the entry point for data in DAF
- It associates relevant information to a dataset to help usability, search and use of semantics
- It helps to keep the datasets well organized
- It facilitate the development of automatic services on top of datasets (i.e. api that retrieves data and does basic operations on data set)
- It sets ownership and access rules to data

The ingestion module is the entry point for data in DAF and it is responsible for collecting the info & metadata about the dataset, check the coherence of the information provided with the actual schema of the input data, manage the upload and storage of the data in HDFS, and organizes the data into "meaningful" HDFS folder structure. You can find a prototype of the [ingestion module here](https://github.com/lilloraffa/daf-injection), it is just to prove the concept, the actual deployment will be done via micro services.

We are trying to use and make sense of Ontologies, Controlled Vocabularies and and Linked Data available. In particular, the ingestion module allows to mark the dataset itself and its fields with annotations that tries to associate a sort of semantics to data. I.e. the dataset of the agencies managing the public transportation in Turin, will be associated with the URI of EU Vocabolary for [Transportation] (http://publications.europa.eu/resource/authority/data-theme/TRAN) as theme (`dcat:theme`), the URI of [Public Transportation](http://eurovoc.europa.eu/4512) as category (`dct:subject`) , with the URI for [spatial reference] (http://www.geonames.org/3165524) (dcat:spatial). Then, its fields having info about the carrier will be marked with an appropriate URI of [Transportation agency](http://vocab.gtfs.org/terms#Agency) a context.

The Ingestion and Catalogue Manager also defines the ways the dataset will get input data:
- **Batch ingestion** --> it defines an input src from which the system expects a file with data having the data schema compatible with the one defined in the catalogue. If the coherence checks are passed, the data is appended to the existing dataset (or inserted if it is the first time data gets into the platform)
- **Stream ingestion** --> it defines an API endpoint that accepts data (ideally in JSON format or other to be defined) to be appended to the existing dataset
- **Transformation pipeline** --> a dataset can be created/updated based on a pipeline that gets input from another ingested dataset. The catalogue manager will store info on the specific pipeline procedure to be called when the input dataset gets updated.

All ingestion activities are tracked via a logging mechanism, and a messaging system that leverage Kafka.

### Data Management
Thanks to the standardization and rules imposed by the Ingestion and Catalogue module, data in DAF are well organized into a proper folder structures, and exposed to the right group of ownership that dictates who can do what with a given dataset. Every dataset is characterized by a logical URI and physical locations URL with the following structure:
- **URI**: `{domain}://dataset/{dataset type}/{group of ownership}/{owner}/{theme}/{dataset name}`
- **URL**: `{hdfs base path}/{dataset type}/{owner}/{theme}/{group of ownership}/{dataset name}` (there is one exception for Standard Dataset, see below.)
Data in DAF are of 3 types: Standard Dataset (or VID, very important dataset), Ordinary Dataset and Raw Dataset.
- **Standard Dataset**: A dataset with a national relevance, that describes a phenomena in a standardize way nationwide. It implicitly defines a standard data structure and is typically supported by an ontology, and has the aim to ingest data from multiple data sources. An example can be again the public transportation services: they describes a phenomena that is the same everywhere, but it is managed locally by multiple entities (in this case typically by each Town). The data generated locally gets ingested, transformed into the common Standard Schema and added to the relevant Standard Dataset.  
They have a slightly different folder structure than the other, as they are unique and have a unique owner: `{hdfs base path}/{dataset type}/{theme}/{group of ownership}/{dataset name}`
- **Ordinary Dataset**: A dataset that does not belong to a standard schema, but has the minimum required level of metadata and information. Those are typically dataset that are relevant at the level of influence of the entity that generates and send the dataset to DAF. The required metadata are useful to make this dataset easily searchable and understandable by other users of DAF.
- **Raw Dataset**: This is a dataset that does not have the minimum required info to become an Ordinary Dataset.

#### Exposing Data
The standardized data organization and the rich metadata collected makes it possible to expose data in automatic ways via standard API as soon as a new dataset gets created in DAF. In particular, at the time of writing, we are implementing the following methods:
- **Storage Manager**: it is made by a set of microservices that exposed the following basic functionalities for data retrieval:
  - `{API}/getDataset/{retrieve option}/{output format}/{dataset id, or dataset URI}`: this endpoint is created by default and returns the dataset identified by either its id or URI.    
  `{retrieve option}`: this specifies filters on the data to be retrieved (e.g. can do a bulk extraction, returns only a sample, ecc.)
  `{output format}`: this option specifies the format in which data will be retrieved. A default option is JSON.
  - `{API}/getDS/{keyword}/{dataset id, or dataset URI}`: this is a simplified version of the previous endpoint, that uses specific configuration of it to simplify access.
  - `{API}/searchDS/{search options}/{output options}`: this allows to search the dataset catalogue to find dataset based on the search criteria defined in `{search options}`.
- **JDBC Connection**: Some dataset can be configured (via the Catalogue Manager) to expose a JDBC connection, managed either via Impala or SparkSQL
- **HDFS Access via Notebook**: data can also be accessed using Notebook for analysis. Technically, this is done via access to HDFS, user profiling rules defined.

### Analysis & Data visualization
One of the key features of DAF are analytical functionalities. It offers en environment where data scientists and analysts can perform analysis on data, run statistical & machine learning models and produce data visualizations and reports. The following are the tools we plan to make available for analysis at the time of writing:
- **Notebook**: DAF will create isolated environments to run notebooks that can safely connect to data via HDFS or API to perform analysis. The notebook will have the Catalogue API loaded by default to help dataset search and retrieval and have the scala, python, R kernel integrated with spark.
- **BI Tool**: DAF will have an installed BI tool to offer self service analysis & reporting capabilities to analysts. The BI Tool will typically connect with the dataset that exposes JDBC connections. We are currently scouting for good Open Source alternatives.
- **Data Visualization Templates & Data Stories**: the Data Portal (see below) will give users the possibility to create data visualization on the fly by using the Data Visualization Templates. These templates will leverage standard D3.js and alike code embedded into React/AngularJS modules that will connect to a given dataset via the exposed API and produce a graph. Users can create and share their analysis via Data Stories, a content entity that can contains Data Visualization instances, text, notebook and Gists/Github resources.

### Data Applications
Data applications are an important component of DAF. They are software applications that leverage/embed analysis and models previously performed on data to offer functionalities based on those models. As an example, consider the data scientists team has trained a model that predicts the traffic jam in a specific point of a road based on the current traffic status nearby. This model can be embedded into a microservice that will take as input the geospatial coordinate where we want to predict the traffic and return the prediction.

Data applications are another entity modeled in DAF so to make sure they are easily manageable, easily searchable and possibly connected/related to relevant other data applications and datasets. In this perspective, the data applications will have a catalogue manager similar to the one for datasets, so to implement data applications metadata and search capabilities.

### Data Portal
Data Portal is a web based user interface for the DAF, giving users the capabilities to access to DAF functionalities and data products. The Data Portal will expand the typical features of the popular data catalogues (e.i. dati.gov.it) with the following:
- **Data Visualization Templates**: they are modules that generate a specific data visualization based on a data source specified. They will give the user the possibility to use an already implemented standard visualizations (i.e. line graph, bar chart, pie chart, etc.) or to create his own graph in d3.js (or any other javascript graph libraries supported), and they will natively integrate with the DAF api exposing data. Data Visualization Template are also managed by their CMS, to help for search and reusability.
- **Data Stories**: they are entities containing stories and analysis about a specific phenomena described with data, and can be made of text, Data Visualization objects, notebooks, gists/github resources. They are basically the way users create data related content in the Data Portal.
- **Social & Collaboration**: Users can create their own Data Stories and share them on the platform so other users can read and/or fork them to build a new Data Story on top of an existing one.
- **User Profile**: a restricted area where the user can see and manage the content she created, as well as have access to personal data that are available in DAF (i.e. the citizen dashboard). The latter functionality will be accessed with a 2nd/3rd level SPID, meanwhile for the first part a lighter registration may be enough.

## Where to go from here
- DAF Architecture [here](https://github.com/teamdigitale/daf/blob/master/docs/architecture/architecture.md)
- Data Management in DAF [here](https://github.com/teamdigitale/daf/blob/master/docs/datamgmt/readme.md)
- DataMgmt Microservices [here](https://github.com/teamdigitale/daf/blob/master/docs/datamgmt/datasetsrv/readme.md)
- Ingestion and Data Structure [here](https://github.com/teamdigitale/daf/blob/master/docs/datamgmt/operations/ingestion_dataset_add.md)
