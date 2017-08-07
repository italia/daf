# DAF - Security & Privacy Issues

## Data at rest security policies

The default storage platform is HDFS, so once the data has been put on HDFS must be protected using the proper permissions.
Moreover, since the data need to be accessible through Impala, a set of proper permissions should be provided to Sentry to open the data to the authorized users.

Regardless the data is accessed either from HDFS or through Impala the security policies should be the same. That means that the security rules defined regardless the particular data acces and should be compiled into prpper permissions rule either on HDFS or Sentry.

### Security rules
1. **Data Ownerhip**

   Any data set should be owned by an identified principal. Even in case of an organisation a specific user should be identified as the owner of that specific dataset.

2. **Singler User Group**

   Any user should have a corresponding group named with the same username, i.e. a user David Greco with a username `david` should have a group called `david` containing only the username `david`.

   This is the default in the POSIX world, in case of an integration with Active Directory this policy needs to be enforced.

   The reason of this rule lies in the fact that Sentry doesn't allow to grant a privilege to a user but only to a group. Since, we want to maintain the single owner principle with to resort to this policy to maintain our security approach coherent.

3. **Access Delegation**

   Only the dataset owner is allowed to provide access to it.

   For example, if a dataset `D` is owned by `david`, only `david` can give read access to D to another user `paul`.

   Implementing this rule means appliying to the dataset HDFS directories additional HDFS ACLs and to grant specific roles at the Sentry level as we will show later.

   The same rule apply in case a user wants to provide access to a group. In general the dataset owner can give access to the dataset to a combination of users and groups.

4. **Access Rights**

   The access rights are only two: READ and WRITE, these rights as said before have to be mapped on proper HDFS ACLs and Sentry roles and grants.

 ### Security Rules and mapping on HDFS and Sentry

 Let's start with ordinary data first. So, we know that the ordinary data HDFS layout is defined in the following way:

 `/ daf / ordinary / `

 The content of this directory is organized adopting the following rules:

 ` sourceOrg / domain / subdomain / datasetName.stage.datasetFormat / `

 where:

 - `sourceOrg` is the name of the organization owning the dataset. This name is specified as `organizationType_organizationName`
 - `domain` is the parent category to which the dataset belong (e.g. "mobility")
 - `subdomain` is the sub-category to which the dataset belong (e.g. "traffic")
 - `datasetName` is the name of the dataset
 - `stage` is the particular stage of the dataset in the transformation pipeline, ex. `landing`, `stage1`
 - `datasetFormat` specifies the serialization format. At the moment the Allowed format are: `csv`, `json`, `avro`, `parquet`.

So, let's suppose that a representative user for `sourceOrg` is `dataowner` then the following rules should be applied to HDFS:

1. `hdfs dfs -chown -R dataowner:dataowner /daf/ordinary/sourceOrg/domain/subdomain/datasetName.stage.datasetFormat`
2. `hdfs dfs -chmod  -R go-rwx /daf/ordinary/sourceOrg/domain/subdomain/datasetName.stage.datasetFormat`
3. `hdfs dfs -setfacl -R -m user:impala:rwx /daf/ordinary/sourceOrg/domain/subdomain/datasetName.stage.datasetFormat`

1) This fix the ownership to the orgamnisation's representative user.
2) Only the dataset owner has full access to that dataset.
3) The impala user need access to all the datasets since Impala doesn't support secure impersonation and all the daemons run under the impala user.

All the directories rooted under `/daf/ordinary/sourceOrg` should follow the same rules.

Once the HDFS rules has been applied the next step is to apply a set of rules at the Sentry side to implement logically the same security rules we described before:

First of all a proper database should be created to hold the dataset, the database should be named accordingly with the following naming convention:

`domain`__`subdomain`

in Impala SQL DDL commands:

`CREATE DATABASE domain__subdomain`

Then an external table should be created pointing to the dataset directory, something like:

```
CREATE EXTERNAL TABLE  domain__subdomain.dataset_stage_format
(
   ...
)
...
LOCATION '/daf/ordinary/sourceOrg/domain/subdomain/dataset.stage.format';
```
Then proper Sentry roles and rights should be granted:

This is for the database:
```
CREATE ROLE db_domain__subdomain_role;
GRANT ALL ON DATABASE domain__subdomain TO ROLE db_domain__subdomain_role;
GRANT ROLE db_domain__subdomain_role TO GROUP dataowner;
INVALIDATE METADATA;
```
where the dataowner group is actually a group containing only the user `dataowner` as dictated by the security rule 2.

Then, this is for the table:
```
CREATE ROLE table_domain__subdomain__dataset_stage_format_role;
GRANT SELECT ON TABLE domain__subdomain.dataset_stage_format TO ROLE table_domain__subdomain__dataset_stage_format_role;
GRANT ROLE table_domain__subdomain__dataset_stage_format_role TO GROUP dataowner;
INVALIDATE METADATA;
```
