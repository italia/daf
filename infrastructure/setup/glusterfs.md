# GlusterFS

documentazione di esempio
https://github.com/kubernetes/examples/blob/master/staging/volumes/glusterfs/README.md
https://docs.openshift.org/latest/install_config/storage_examples/gluster_example.html

## steps
All needed files are in [daf_provisioning_and_maintenance](https://github.com/teamdigitale/daf_provisioning_and_maintenance/tree/master/kubernetes) project.
Create endopoints and verify the endpoints were created:
```
kubectl create -f glusterfs-endpoints.json

kubectl get endpoints
```
Create a service and check if it is created:

```
kubectl create -f glusterfs-service.json

kubectl get service gluster-cluster
```

Define the persistent volume:
```
kubectl create -f glusterfs-pv.yml
kubectl get pv
```
Define the persistent volume claim:
```
kubectl create -f glusterfs-pvc.yml
kubectl get pvc
```

riferimenti

https://github.com/teamdigitale/daf_provisioning_and_maintenance
