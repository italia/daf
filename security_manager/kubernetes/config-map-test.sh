#!/usr/bin/env bash
kubectl delete configmap security-manager-conf
kubectl create configmap security-manager-conf --from-file=../conf/test/prodBaseNew.conf