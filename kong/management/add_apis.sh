#!/usr/bin/env bash

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-storage-manager' \
  --data 'uris=/storage-manager/v1' \
  --data 'upstream_url=http://storage-manager.default.svc.cluster.local:9000/storage-manager/v1'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-security-manager' \
  --data 'uris=/security-manager/v1' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/security-manager/v1'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=swagger-ui-docs' \
  --data 'uris=/docs/swagger-ui' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/docs/swagger-ui'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-security-manager-swagger' \
  --data 'uris=/security-manager/swagger-ui/index.html' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/swagger-ui/index.html'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-storage-manager-swagger' \
  --data 'uris=/storage-manager/swagger-ui/index.html' \
  --data 'upstream_url=http://storage-manager.default.svc.cluster.local:9000/swagger-ui/index.html'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-security-manager-swagger-json' \
  --data 'uris=/security-manager/swagger.json' \
  --data 'upstream_url=http://security-manager.default.svc.cluster.local:9000/security-manager/swagger.json'

curl -i -X POST \
  --url http://kong-admin.default.svc.cluster.local:8001/apis/ \
  --data 'name=daf-storage-manager-swagger-json' \
  --data 'uris=/storage-manager/swagger.json' \
  --data 'upstream_url=http://storage-manager.default.svc.cluster.local:9000/storage-manager/swagger.json'
