#!/bin/bash
kinit -kt /opt/docker/conf/principal.keytab dgreco@DGRECO-MBP.LOCAL
/opt/docker/bin/daf_storage_manager -Dconfig.file=conf/production.conf