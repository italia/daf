# Ingestion Manager

The ingestion manager given a dataset definition from the catalog-manager
creates the needed NIFI processor to instrument the ingestion of the dataset.

## Setup

### Project Dependencies
 
This project depends on a SNAPSHOT version of the projects common and catalog-manager.
Thus you need to publish them locally. 


```bash
# publish locally the daf-common project
$ cd ../common && sbt publishLocal

# publish locally the catalog manager
$ cd ../catalog-manager && sbt publishLocal

```

### Authorization for local test

By default it uses the **pac4j DirectBasicAuthClient** client with simple test username and password.
That is, you can use an username and password with the same value (e.g. admin/admin, test/test and so on).


```scala

import org.pac4j.http.client.direct.{DirectBasicAuthClient, ParameterClient}
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator

  val directBasicAuthClient = new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

```

This is configured with the property:

```hocon
pac4j.authenticator = "test"
```

in the application.conf file.

### Authorization with ldap/ckan

This is still not clear!!!

## Features

1. `add-new-dataset/{logical-uri`
2. ...

### add-new-dataset

This uri starts the ingestion process. It:
1. creates the group processors for the input and the update attributes
2. creates the connection between the two processors and the main `funnell`
3. starts the nodes and thus the ingestion process

The dataflow used for the analysis is hardcoded in the application.conf, but it would be parametric in the next release.

```hocon

ingmgr.niFi.funnelId = "6f7b46a9-aa15-139f-f792-6dfe7141a96b"
ingmgr.niFi.groupId = "6f7b46a2-aa15-139f-3083-addf34976b6e"
```

Right now there isn't automatic test for the rest web services, but you cna run an example of this feature in the `NifiProcessorSpec`.

On the contrary, to run a end-to-end test of this project you need to:

1. to start an instance of the catalog manager
2. to ingest a new dataset
3. to call the `add-new-dataset` service with the logical uri of the ingested dataset.

**This is in WIP status**.

