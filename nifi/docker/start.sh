#!/usr/bin/env bash

ln -sf /nificonf/$(hostname -s) /nifi/conf

/nifi/bin/nifi.sh run