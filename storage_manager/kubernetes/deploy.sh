#!/usr/bin/env bash

set -e

# cd ../../bin
# . ./setVersions.sh
# cd -

export STORAGE_MANAGER_VERSION=1.0.0-SNAPSHOT

# test for template
envsubst < daf_storage_manager.yml > output.yml

kubectl delete -f output.yml

kubectl create -f output.yml

rm output.yml
