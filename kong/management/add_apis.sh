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
 --data 'name=daf-sso-manager' \
 --data 'uris=/sso-manager' \
 --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/sso-manager'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-datipubblici' \
  --data 'uris=/dati-gov' \
  --data 'upstream_url=http://datipubblici.default.svc.cluster.local:9000/dati-gov'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-iot-ingestion-manager' \
  --data 'uris=/iot-ingestion-manager' \
  --data 'upstream_url=http://iot-ingestion-manager.default.svc.cluster.local:9900/iot-ingestion-manager'

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
