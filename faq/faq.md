
# F.A.Q

## Kerberos authentication failed: kinit: Included profile directory could not be read while initializing Kerberos 5 library

It happens when one of the edges of kubernetes is restarted. During the restart in the file `/etc/krb5.conf` is injected the line `includedir /var/lib/sss/pubconf/krb5.include.d/`.
You should comment this line `#includedir /var/lib/sss/pubconf/krb5.include.d/`

## Netty Error Caused by: java.lang.NoClassDefFoundError: Could not initialize class org.asynchttpclient.netty.NettyResponseFuture

You you exclude `netty` from the cloudera dependencies. For examples in the storage manager you can do:

```sbt
hbaseLibrariesTEST
      .map(_.exclude("io.netty", "netty"))
```
