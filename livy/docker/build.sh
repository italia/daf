#!/usr/bin/env bash
(cd ../incubator-livy; mvn clean package -Pspark-2.1 -DskipTests -Dhadoop.version=2.6.0-cdh5.11.0)
rm -rf spark*
curl -s -L https://github.com/cloudera/spark/archive/spark2-2.1.0-cloudera1.zip -o spark2-2.1.0-cloudera1.zip
unzip spark2-2.1.0-cloudera1.zip
(cd spark-spark2-2.1.0-cloudera1; build/mvn clean install -DskipTests -Pyarn -Phive -Dhadoop.version=2.6.0-cdh5.11.0)
rm -rf spark2-2.1.0-cloudera1.zip
rm -rf livy-server*
unzip -o ../incubator-livy/assembly/target/livy-server*.zip
mv livy-server-* livy-server
rm -rf spark2-2.1.0-cloudera1/build
(cd spark-spark2-2.1.0-cloudera1; find . -name "*.class" | xargs rm)
docker build -t 10.98.74.120:5000/daf-livy:1.0.0 .
docker push 10.98.74.120:5000/daf-livy:1.0.0
rm -rf spark-spark2-2.1.0-cloudera1
rm -rf livy-server*
