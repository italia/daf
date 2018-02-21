# Ingress

The external access to the services is managed by [Ingress](https://github.com/kubernetes/ingress-nginx/blob/master/README.md)

The general info on the Ingress setup can be found [here](https://github.com/kubernetes/ingress-nginx/blob/master/deploy/README.md#generic-deployment)

The Ingress setup for Daf can be done by executing the following steps

1. Prerequisites

This is mandatory for production, but not need for test

At the bottom of the `nginx-ingress-controller.yml` file there is a reference to the tls secret with the certificates needed for TLS.
Before executing the next steps, be sure to have the correct certificates in place, as explained in `infrastructure/certbot/README.md`

2. Setup Ingress controller

From the `infrastructure/ingress` folder, run:

```
kubectl create -f namespace.yaml
kubectl create -f default-backend.yaml
kubectl create -f configmap.yaml
kubectl create -f tcp-services-configmap.yaml
kubectl create -f udp-services-configmap.yaml
kubectl create -f rbac.yaml
kubectl create -f nginx-ingress-controller.yml
```

3. Ingress resources

To expose the kubernetes services to the outside world, the Ingress resources need to be deployed, with the following commands:

```
kubectl create -f dataportal-ingress.yml
kubectl create -f dataportal-private-ingress.yml
kubectl create -f datascience-ingress.yml
kubectl create -f graph-ingress.yml
kubectl create -f kong-ingress.yml
kubectl create -f superset-ingress.yml
kubectl create -f supersetd-ingress.yml
```
