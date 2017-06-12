FROM ubuntu:16.04

ADD opentsdb-2.3.0_all.deb opentsdb-2.3.0_all.deb

ADD start.sh start.sh

RUN apt-get update && apt-get install -yq --no-install-recommends --force-yes \
    default-jdk \
    vim \
    krb5-user && \
    ln -sf /etc/krb5.conf /usr/lib/jvm/default-java/jre/lib/security/krb5.conf && \
    dpkg -i opentsdb-2.3.0_all.deb && \
    rm opentsdb-2.3.0_all.deb && \
    chmod +x start.sh
    
EXPOSE 4242

ENTRYPOINT ["/start.sh"]