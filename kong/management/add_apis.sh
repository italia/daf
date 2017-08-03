#!/usr/bin/env bash

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-storage-manager' \
  --data 'uris=/storage-manager' \
  --data 'upstream_url=http://storage-manager.default.svc.cluster.local:9000/storage-manager'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-security-manager' \
  --data 'uris=/security-manager' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/security-manager'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-opentsdb' \
  --data 'uris=/opentsdb/v1' \
  --data 'upstream_url=http://opentsdb.default.svc.cluster.local:4242'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-livy' \
  --data 'uris=/livy' \
  --data 'upstream_url=http://livy.default.svc.cluster.local:8998'
