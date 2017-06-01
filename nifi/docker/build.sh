#!/usr/bin/env bash

wget -O nifi.tar.gz  http://it.apache.contactlab.it/nifi/1.2.0/nifi-1.2.0-bin.tar.gz && \
    tar -zxvf nifi.tar.gz && \
    rm nifi.tar.gz && \
    mv nifi-1.2.0 nifi

docker build -t 10.103.136.239:5000/daf-nifi:1.0.0 .
docker push 10.103.136.239:5000/daf-nifi:1.0.0
rm -rf nifi