# DAF Architecture

The DAF (Data Analytics Framework) is a platform originally designed to gather and store data coming from different Italian public administrations. As a consequence, it provides efficient and easy to use ingestion mechanisms for allowing external organisations to simply ingest their data into the platform with minimal human intervention.
The DAF platform shouldn’t only provide support for data at rest and fast data (streaming), but also for storing and managing collections of unstructured data, textual documents.
Besides providing those storing capabilities the next main goal is to provide a powerful mechanism for data integration, i.e. a way for integrating data that traditionally reside on separate silos. Enabling the correlation of data sets normally residing on different systems/organizations can become a very powerful enabling factor for discovering new insights on the data.
The platform should allow the data scientists to access its computational power for implementing advanced analytics algorithms.

Take a tour of the DAF architecture looking at:

- the [logical view](./logicalView/)
- the [component/𝜇-service view](./componentView/)
- the [deployment view](./deploymentView/)
