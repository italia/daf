#!/usr/bin/env bash

ln -sf /nificonf/$(hostname -s) /nifi/conf

ln -sf /nifiscripts/$(hostname -s) /nifi/scripts

/nifi/bin/nifi.sh run