# DAF authorization security
User permission management in DAF is organized assining group and organizations to users. The groups drives permissions to application functions while the organizations set data visibility.


## Application security initial setup

Following steps must be performed to setup DAF application security.

1. Access the freeIPA administrative console with admins credentials.

2. Create two user named "application" and "superset_admin". The last user superset default admin. These names and the password you choose must be reported in configuration of the security manager (application.conf). 

3. Create the groups "daf_admins", "daf_editors" and "daf_viewers". Put the "application" user in the "daf_admins" group.

4. Create the DAF default organization group "default_org" by invoking the Security Manager default organization setup service. The service must be invoked with the previoulsly created "application" credentials. For example with curl:

```bash

$ curl -X POST -i -H "Content-Type: application/json" -H "Authorization: Basic YXBwbGljYXRpb246cGFzc3dvcmQ=" http://localhost:9000/security-manager/v1/daf/defaultOrg

```

5. Come back to the freeIPA console and configure the group "default_org" as the default group of new created users: reach the page on the menu path "identity/Automember/User group rules" and set up the select the value "default_org" on the field "Default user group".

6. Log CKAN with its own admin credentials, select the "default_org" organization and add this user to the organization with "admin" role. 

7. Create an organization named "default_org" on Grafana console.


## User and groups managment
User and organization creation should always be handled through the portal: users and groups not only lives in LDAP and freeIPA but also matches user profiling info in others DAF subsystems like CKAN or Superset.

In order to create a user without step in the registration process is possible to invoke directly the user creation service:

```bash

$ curl -X POST -i -H "Content-Type: application/json" -H "Authorization: Basic YXBwbGljYXRpb246cGFzc3dvcmQ=" -d '{ \
  "uid": "test", \
  "givenname": "test", \
  "sn": "andrea", \
  "mail": "andrea@test.it", \
  "userpassword": "password" \
}' http://localhost:9002/security-manager/v1/daf/user

```
