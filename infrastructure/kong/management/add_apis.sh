#!/usr/bin/env bash

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-dataset-manager' \
  --data 'uris=/dataset-manager' \
  --data 'upstream_url=http://storage-manager.default.svc.cluster.local:9000/dataset-manager'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-storage-manager' \
  --data 'uris=/storage-manager' \
  --data 'upstream_url=http://storage-manager.default.svc.cluster.local:9000/storage-manager'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-security-manager-ipa' \
  --data 'uris=/security-manager/v1/ipa' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/security-manager/v1/ipa'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-security-manager-daf' \
  --data 'uris=/security-manager/v1/daf' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/security-manager/v1/daf' \

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-security-manager-token' \
  --data 'uris=/security-manager/v1/token' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/security-manager/v1/token'

 curl -i -X POST \
 --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
 --data 'name=daf-sso-manager' \
 --data 'uris=/sso-manager/secured' \
 --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/sso-manager/secured'

 curl -i -X POST \
 --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
 --data 'name=daf-catalog-manager' \
 --data 'uris=/catalog-manager' \
 --data 'upstream_url=http://catalog-manager.default.svc.cluster.local:9000/catalog-manager' \
 --data 'upstream_send_timeout=240000' \
 --data 'upstream_read_timeout=240000' \
 --data 'upstream_connect_timeout=240000'

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
  
curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=ontonehub' \
  --data 'uris=/stanbol' \
  --data 'upstream_url=http://ontonehub.default.svc.cluster.local:8000/stanbol'
