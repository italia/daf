#!/usr/bin/env bash

wget -O opentsdb-2.3.0_all.deb  https://github.com/OpenTSDB/opentsdb/releases/download/v2.3.0/opentsdb-2.3.0_all.deb && \

docker build -t 10.98.74.120:5000/daf-opentsdb:1.0.0 .
docker push 10.98.74.120:5000/daf-opentsdb:1.0.0
rm opentsdb-2.3.0_all.deb