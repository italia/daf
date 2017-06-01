FROM anapsix/alpine-java:8_jdk_unlimited

ADD nifi nifi

ADD start.sh start.sh

RUN apk update && apk add bash krb5-libs krb5 && \
    ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf && \
    chmod +x /start.sh

EXPOSE 9090

ENTRYPOINT ["/start.sh"]