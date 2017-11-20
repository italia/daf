# DAF play helpers

This project contains all the play modules, filters and xxx used to handle security in the DAF.

In particular, the available modules are:

- ClouderaModule
- KerberosModule
- JwtLdapSecurityModule


To include any of this module add the following configurations in your `application.conf` file.

```hocon

play.modules.enabled += "it.gov.daf.play.modules.KerberosModule"
play.modules.enabled += "it.gov.daf.play.modules.ClouderaModule"
play.modules.enabled += "it.gov.daf.play.modules.JwtLdapSecurityModule"
```


The available filters are

- FiltersSecurity
- FilterSecurityCORS

```hocon
play.http.filters += "it.gov.daf.common.play.filters.FiltersSecurity"
play.http.filters += "it.gov.daf.common.play.filters.FilterSecurityCORS"
```

## ClouderaModule

This is used to inject `hadoop_conf_dir` and `hbase_conf_dir` folders in the play configurations.

## KerberosModule

This module is used to create a valid kerberos token. It requires 3 configurations as input:

```hocon

kerberos {
  keytab_refresh_interval = 12 hours
  keytab = "/application/conf/daf.keytab"
  principal = "daf@DAF.GOV.IT"
}
```

default values are provided in `reference.conf` but should check and change it. 
Moreover, the module writes log information in DEBUG mode

## JwtLdapSecurityModule

This module validates/generates JWT tokens based on ldap or simple test username and password authentication.

Add the following configurations to your application.conf. 

### Configurations

- `authenticator`: As authenticator you can use `ldap | test`. 
- `login_attribute`: you can use as values one of the attributes of your ldap users to use for authentication (e.g. mail, uuid)
- 


In the case that ldap is used you have to provide values 
for all the empty string configurations otherwise you'll see a runtime error.

```hocon

pac4j {
  authenticator = "ldap"
  jwt_secret = ""
  ldap {
    connect_timeout = 500 millis
    response_timeout = 1000 millis
    url = ""
    bind_dn = ""
    bind_pwd = ""
    base_user_dn = ""
    login_attribute = ""
    username_attribute = ""
  }
}
```

