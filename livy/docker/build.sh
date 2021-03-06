#!/usr/bin/env bash
(cd ../incubator-livy; mvn clean package -Pspark-2.2 -DskipTests -Dhadoop.version=2.6.0-cdh5.12.0)
rm -rf spark*
curl -s -L https://github.com/cloudera/spark/archive/spark2-2.2.0-cloudera1.zip -o spark2-2.2.0-cloudera1.zip
unzip spark2-2.2.0-cloudera1.zip
(cd spark-spark2-2.2.0-cloudera1; build/mvn clean install -DskipTests -Pyarn -Phive -Psparkr -Dhive.version=1.1.0-cdh5.12.0 -Dhadoop.version=2.6.0-cdh5.12.0)
rm -rf spark2-2.2.0-cloudera1.zip
rm -rf livy-server*
unzip -o ../incubator-livy/assembly/target/livy-*-bin.zip
mv livy-* livy-server
rm -rf spark2-2.2.0-cloudera1/build
(cd spark-spark2-2.2.0-cloudera1; find . -name "*.class" | xargs rm)
docker build -t 10.98.74.120:5000/daf-livy:1.0.0 .
docker push 10.98.74.120:5000/daf-livy:1.0.0
rm -rf spark-spark2-2.2.0-cloudera1
rm -rf livy-server*
