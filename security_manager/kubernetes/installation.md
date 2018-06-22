## Installation

### Prerequisites

Superset and Ckan must be properly installed and configured before proceed with secutity manager installation

### Procedure

The installation depends on the environment where is is run.
For this reason, when executing the following steps, replace \<environment\> with `test` or `prod` accordingly.

1. git clone https://github.com/teamdigitale/daf
2. cd `security-manager`
3. `sbt` or `sbt eval System.setProperty("STAGING", "true")` If you want to deploy on test environment
4. `docker:publish` to compile and push the docker image on Nexus
5. If not already present, create kerberos config map (refer to daf-operational repo: https://github.com/teamdigitale/daf-operational.git)
6. cd `kubernetes` 
7. `./config-map-<environment>.sh` to create config map
8. `./kubectl create -f daf_security_manager_<environment>.yaml` to deploy the containers in kubernetes
9. Setup user and groups and test installation accordingly to this guide: https://github.com/italia/daf/blob/master/infrastructure/pages/security.md
