#!/usr/bin/env bash
kubectl delete configmap catalog-manager-conf
kubectl create configmap catalog-manager-conf --from-file=../conf/test/prodBaseNew.conf