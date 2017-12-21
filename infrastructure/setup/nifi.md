# Nifi

## Steps

1. Delete Nifi services:
 
```
kubectl delete -f nifi/kubernetes/daf_nifi.yml
```

2. Create a service and check if it is created:

```
kubectl create -f daf_nifi.yml
```

**NB**:check if /etc/krb5.conf file does not contains the following row:
``` 
   includedir /path/to/file/krb5.include.d 
```