# Haproxy

Haproxy is used to expose the Impala daemons to the outside.

To install it, the following steps must be followed (from the `infrastructure/haproxy` folder):

1. Configuration

Copy the file `conf/haproxy.cfg` to `glusterfs` in the folder `haproxy/conf`

2. Deploy

Run the following:

```
kubectl create -f haproxy-deployment.yml
kubectl create -f haproxy-service.yml
```
