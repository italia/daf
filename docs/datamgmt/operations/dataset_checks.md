# Checks on Dataset coherence
Several checks are performed during the creation of a new dataset as well as anytime new data comes in to feed an existing one.

# InjMod description

###### to be modified for the context.

## Standard Schema
Standard Schema defines a Standard dataset: it declares the existence of a specific Standard dataset and contains a set of information describing its structure and content. It has the following info:
- Specific information on the dataset that are not contained in DCAT_AP
- The list of required fields, with information, among the other, about their format, constraints if any, nature (e.g. measure or a dimension), domain specific info that will help the programmatic use of the data in specific contexts.
- The list of optional fields, with the same info listed for the required ones
- The associated DCAT_AP metadata id
- Information about where the data set is stored and how
- Information about the owner of the data and the group (list of users) that has the right to access the data

In other words, Standard Schema defines all the rules and details that a data sources must oblige to if it wants to belong to a specific Standard dataset. For example, let's imagine we want to build a Standard dataset describing the phenomena "Bike Sharing" and let's suppose that we have multiple data sources each of which collecting bike sharing data for a specific geographic area (e.g. towns). In order to be eligible to insert their data into the "Bike Sharing" Standard dataset, each data source must provide data according to the rules defined in the Standard Schema. The resulting dataset will then be able to describe a unique phenomena in a consistent way across multiple data sources. The latter have two options: either providing data following exactly the same schema defined in the Standard Schema; or provide the minimum set of info required and a set conversion info with which the platform will be able to convert the original schema to the Standard one. In both cases, the tool to do so is the Conversion Schema described below.

You can find the commented metadata of the [Standard Schema here](https://github.com/lilloraffa/daf-datamgmt/blob/master/dataschema/schema-prototype.json), and a [Standard Schema example here](https://github.com/lilloraffa/daf-datamgmt/blob/master/dataschema/mobility/shema-gtfs_fare_attributes.json).

## Conversion Schema
Conversion Schema is the basic tool to ingest both Ordinary and Standard dataset in the platform, every data set in input needs to have an associated Conversion Schema. It has a twofold purpose: 1) providing basic info to describe the dataset stored in the platform, at least at the level of Ordinary dataset; 2) in case of the ingestion of Standard datasets, providing rules to map the incoming dataset schema to the Standard one.

Conversion Schema contains the following info:
- Specific information of the dataset not contained in DCAP_AP
- The reference to the associated Standard Schema in case Conversion Schema is used to map the incoming dataset structure to a Standard one. In this case, a list mapping each input field to the Standard Schema one is provided.
- The list of the so called "Custom Fields" with the same info and structure of the required/optional fields described in the section about Standard Schema. It has a different purpose in case the Conversion Schema is used to map to a Standard one, or it is used to ingest an Ordinary dataset. In the first case, this list contains fields that are not part of the Standard Schema, but are still provided by the data source. In the Ordinary dataset case, it contains the full list of fields that describe the dataset itself.


You can find the metadata of the [Conversion Schema here](https://github.com/lilloraffa/daf-datamgmt/blob/master/dataschema/conv-prototype.json), and a [Conversion Schema example here](https://github.com/lilloraffa/daf-datamgmt/blob/master/dataschema/mobility/examples_conv/it_palermo/conv-gtfs_fare_rules.json).

## Dataset Metadata
This is the lowest level of details the platform need to classify, build a catalogue and expose datasets. Here the level of description is the dataset itself, no info on the internal structure/fields are provided. This is the minimum required info to build the catalogue like the well known Open Data catalogue, and uses CKAN as internal backend.

[TO BE DECIDED IF WE IMPOSE THE DCAT_AP AS MINIMUM REQUIREMENT FOR ACCEPTING DATA]

You can find the metadata of the [Metadata schema here](http://), and an [example here](http://) [coming soon...].
