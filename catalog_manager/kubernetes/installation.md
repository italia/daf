## Installation

### Prerequisites

Security Manager must be properly installed and configured before proceed with Catalog Manager installation

### Procedure

The installation depends on the environment where is is run.
For this reason, when executing the following steps, replace \<environment\> with `test` or `prod` accordingly.

1. git clone https://github.com/teamdigitale/daf
2. cd `catalog-manager`
3. `sbt` or `sbt eval System.setProperty("STAGING", "true")` If you want to deploy on test environment
4. `docker:publish` on sbt console to compile and push the docker image on Nexus
5. cd `kubernetes` 
6. `./config-map-<environment>.sh` to create config map
7. `./kubectl create -f daf_catalog_manager.yaml` to deploy the containers in kubernetes
