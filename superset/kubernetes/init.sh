#!/usr/bin/env bash
for i in `kubectl get pods | grep superset | awk '{print $1}'`
do
  kubectl exec -ti $i superset-init
done
