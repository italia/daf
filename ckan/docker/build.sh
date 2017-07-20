#!/usr/bin/env bash
docker build -t 10.98.74.120:5000/daf-ckan-solr:1.0.0 ./solr
docker build -t 10.98.74.120:5000/daf-ckan:1.0.0 ./ckan
docker push 10.98.74.120:5000/daf-ckan-solr:1.0.0
docker push 10.98.74.120:5000/daf-ckan:1.0.0
