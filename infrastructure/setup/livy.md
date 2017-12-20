# Livy Installation

1. go to `daf/livy`
2. [optional] build and deploy to the private registry the docker images into the docker folder.
3. execute `kubectl apply -f daf_livy.yml`.
4. Check that there is a file `livy.conf`. in the `glusterfs/livy` folder.


## Example of file livy.conf

```
# What spark master Livy sessions should use.
livy.spark.master = yarn

# What spark deploy mode Livy sessions should use.
livy.spark.deployMode = cluster

# If livy should impersonate the requesting users when creating a new session.
livy.impersonation.enabled = true

# Whether to enable HiveContext in livy interpreter, if it is true hive-site.xml will be detected
# on user request and then livy server classpath automatically.
livy.repl.enableHiveContext = true

livy.server.launch.kerberos.keytab = <path of the x.keytab file>
livy.server.launch.kerberos.principal= <principal for @PLATFORM.DAF.LOCAL>

livy.impersonation.enabled = true
livy.server.auth.type = kerberos

livy.server.auth.kerberos.keytab= <path of the x.keytab file>
livy.server.auth.kerberos.principal=HTTP/<principal for @PLATFORM.DAF.LOCAL>

livy.server.access_control.enabled=true
livy.server.access_control.users=<user that has the access>

livy.superusers=<superusers>

```

## Reference

1. http://henning.kropponline.de/2016/11/06/connecting-livy-to-a-secured-kerberized-hdp-cluster/
