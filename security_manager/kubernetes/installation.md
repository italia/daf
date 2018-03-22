## Installation

### Prerequisites

Superset and Ckan must be properly installed and configured before security manager installation

### Procedure

The installation procedure depends on the environment where the component is deployed and executed.
For this reason, when executing the following steps, replace \<environment\> with `test` or `prod` accordingly.

1. git clone https://github.com/teamdigitale/daf
2. cd `security-manager`
3. execute `./setEnvironment <environment>` to setup the infrastructure environment
3. `sbt docker:publish` to compile and push the docker image on Nexus
4. cd `kubernetes`
5. `./config-map-<environment>.sh` to create config map
6. `./kubectl create -f daf_security_manager.yaml` to deploy the containers in kubernetes
7. Setup user and groups and test installation accordingly to this guide: https://github.com/italia/daf/blob/master/infrastructure/pages/security.md
