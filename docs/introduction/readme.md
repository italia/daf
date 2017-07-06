# Data & Analytics Framework (DAF)

The Data & Analytics Framework (DAF) is a combination of:

- A **Big Data Platform** to centralize and store (data lake), manipulate and standardize (data engine) and re-distribute (API & Data Application) data and insights.
- A **Data Team** (data scientists + data engineers) which uses and evolve the Big Data Platform to analyze data, create ML models and build data applications and data visualization products.

DAF has a the following twofold goal: 

- eliminate silos into which data are typically "trapped", and make entities be logically interconnected so to shed light on phenomena by looking at them via multiple perspectives; 
- easy analysts and data scientists make analysis and build applications that uses the insights from data to create "intelligent" services.

This documentation focuses on the Big Data Platform.

A key concept underlying the DAF platform is the [**dataset**](../dataset/). 

In a nutshell, you can think the DAF platform as a big data environment offering capabilities for:

- *storing and managing datasets*: users can register datasets and to load them on the platform specifying the ingestion models (e.g. batch, streaming), the serialization formats (e.g. Avro, Parquet), the desired serving layers (e.g. HBase, Impala), metadata, etc;  
- *processing and analysing datasets*: the platform provides an environment composed by a set of tools for data scientists and analysts. By using these tools these ones can perform analysis on data, run statistical and machine learning models, and produce data visualizazions and reports;   
- *redistributing datasets, developing data application, publishing insights*: the platform provides tools for enabling the publication of opendata, data stories, data application, etc.   
 

The following image provides an architectural overview of the DAF:

[Insert Image](https://docs.google.com/presentation/d/1LDDrG7VsYoXXIbfbg6tQ9z7DfHw7ukkNnwVygt6jOOQ/edit)

Continue your tour looking a more detailed description of the [DAF architecture](architecture/).