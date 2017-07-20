#!/usr/bin/env bash
kubectl exec  -ti `kubectl get pods | grep ckan | awk '{print $1}'` -c ckan -- ./ckan-vocabulary.sh