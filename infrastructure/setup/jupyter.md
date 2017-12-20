# Jupyter Installation

1. clone the project `daf-recipies-private`
2. go to folder `jupyterhub`
3. [optional] build the docker image if it isn't published in the private registry
4. go to `kubernetes` folder and run `kubectl apply -f daf_jupyterhub.yaml` to deploy jupyter hub in kube
5. execute the script `init.sh`
