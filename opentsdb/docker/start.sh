#!/usr/bin/env bash

rm -rf /tmp/opentsdb/*

kinit -kt /etc/opentsdb/daf.keytab daf@PLATFORM.DAF.LOCAL

export JVMARGS="-Djava.security.auth.login.config=/etc/opentsdb/jaas.conf"

/usr/share/opentsdb/bin/tsdb tsd --staticroot=/usr/share/opentsdb/static --cachedir=/tmp/opentsdb --port=4242

