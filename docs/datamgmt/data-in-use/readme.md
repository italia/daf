# Data in use


## Analysis & Data visualization
One of the key features of DAF are analytical functionalities. It offers en environment where data scientists and analysts can perform analysis on data, run statistical & machine learning models and produce data visualizations and reports. The following are the tools we plan to make available for analysis at the time of writing:

- **Notebook**: DAF will create isolated environments to run notebooks that can safely connect to data via HDFS or API to perform analysis. The notebook will have the Catalogue API loaded by default to help dataset search and retrieval and have the scala, python, R kernel integrated with spark.

- **BI Tool**: DAF will have an installed BI tool to offer self service analysis & reporting capabilities to analysts. The BI Tool will typically connect with the dataset that exposes JDBC connections. We are currently scouting for good Open Source alternatives.

- **Data Visualization Templates & Data Stories**: the Data Portal (see below) will give users the possibility to create data visualization on the fly by using the Data Visualization Templates. These templates will leverage standard D3.js and alike code embedded into React/AngularJS modules that will connect to a given dataset via the exposed API and produce a graph. Users can create and share their analysis via Data Stories, a content entity that can contains Data Visualization instances, text, notebook and Gists/Github resources.

## Data Applications
Data applications are an important component of DAF. They are software applications that leverage/embed analysis and models previously performed on data to offer functionalities based on those models. As an example, consider the data scientists team has trained a model that predicts the traffic jam in a specific point of a road based on the current traffic status nearby. This model can be embedded into a microservice that will take as input the geospatial coordinate where we want to predict the traffic and return the prediction.

Data applications are another entity modeled in DAF so to make sure they are easily manageable, easily searchable and possibly connected/related to relevant other data applications and datasets. In this perspective, the data applications will have a catalogue manager similar to the one for datasets, so to implement data applications metadata and search capabilities.