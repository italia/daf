#!/usr/bin/env bash

set -e

#disabled since we are deploying a snapshot version
#cd ../../bin
#. ./setVersions.sh
#cd -

export INGESTION_MANAGER_VERSION=1.0.0-SNAPSHOT

# test for template
envsubst < daf_ingestion_manager.yml > output.yml

kubectl delete -f output.yml

kubectl create -f output.yml

rm output.yml
