#!/usr/bin/env bash

tsdtmp=${TMPDIR-'/tmp'}/tsd    # For best performance, make sure
mkdir -p "$tsdtmp"             # your temporary directory uses tmpfs
/opentsdb/tsdb tsd -port=4242 --staticroot=build/staticroot --cachedir="$tsdtmp"