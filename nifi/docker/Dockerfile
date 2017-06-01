FROM anapsix/alpine-java:8_jdk_unlimited

ADD nifi nifi

RUN apk update && apk add bash krb5-libs krb5 && \
    ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf && \
    rm -rf /nifi/conf

CMD ln -sf /nificonf/$(hostname -s) /nifi/conf

EXPOSE 9090

ENTRYPOINT ["/nifi/bin/nifi.sh", "run"]