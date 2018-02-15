# Nexus

To install nexus run the following

```
kubectl apply -f ./nexus/daf_nexus.yml
```

use `delete` or `create` instead of apply to delete or create the service and deploy.

Create the secret to deploy dockers (see [here](https://github.com/k8s-community/cluster-deploy) and [here](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/) for more details):
```
kubectl create secret docker-registry regsecret --docker-server=10.98.74.120:5000 --docker-username=daf --docker-password=<put nexus password> --docker-email=daf@DAF.GOV.IT
```
