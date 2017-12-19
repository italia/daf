# Dataset Manager

The dataset manager offers the services to query the `Dataset` ingested into the DAF.
In particular, it exposes rest api for:

1. get the schema of a dataset

```bash
#examples of curl request
```

2. get the records of a dataset

```bash
#examples of curl request
```

3. get the first N records of a dataset

```bash
#examples of curl request
```

4. query a dataset

```bash
#examples of curl request
```

All the services are authenticated with Basic Authentication backed by LDAP.

## Setup

The rest api are implemented using [play-framework](https://www.playframework.com/) and exposes a [swagger-endpoint](https://swagger.io/).

To see the api definition with the swagger ui do the following steps:

1. run the application `sbt run`
2. open the browser at this [link](http://localhost:9000/index.html)

