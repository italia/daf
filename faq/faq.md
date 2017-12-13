
# F.A.Q

## Kerberos authentication failed: kinit: Included profile directory could not be read while initializing Kerberos 5 library

It happens when one of the edges of kubernetes is restarted. During the restart in the file `/etc/krb5.conf` is injected the line `includedir /var/lib/sss/pubconf/krb5.include.d/`.
You should comment this line `#includedir /var/lib/sss/pubconf/krb5.include.d/`
