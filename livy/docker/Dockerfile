FROM ubuntu:16.04

ADD livy-server livy

ADD spark-spark2-2.2.0-cloudera1 spark

RUN apt-get update && apt-get install -yq --no-install-recommends --force-yes \
    default-jdk \
    vim \
    krb5-user && \
    python && \
    ln -sf /etc/krb5.conf /usr/lib/jvm/default-java/jre/lib/security/krb5.conf  && \
    mkdir -p /livy/logs && \
    ln -s /etc/hive/conf/hive-site.xml /spark/conf/hive-site.xml

ENV HADOOP_CONF_DIR /etc/hadoop/conf

ENV SPARK_HOME /spark

EXPOSE 8998

ENTRYPOINT ["/livy/bin/livy-server"]
