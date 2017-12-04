# Storage Manager




## Deployment

### requirements

1. you should be logged into the internal nexus repository

Since the registry uses https but without a valid certificate, you need to add the registry to the [insecure registries](https://docs.docker.com/registry/insecure/).
Then you can run the following command.

```bash

$ docker login https://10.98.74.120:5000
```

### Publish Image

1. `sbt clean compile`
2. `sbt docker:publish`

### Deploy application

Now we can deploy the application in Kubernetes, with the following steps:

1. ssh in the edge1 or edge2 and check the folder `/glusterfs/volume1/storage-manager/conf` exists
2. place a file `production.conf` with all the production configuration overrides.
3. `cd kubernetes`
4. execute `$ deploy.sh`. it creates a file output.yml that replaces the variable STORAGE_MANAGER_VERSION with 1.0.0-SNAPSTHOT, calls `kubectl delete -f output.yml` and `kubectl create -f output.yml`