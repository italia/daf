# Install Kong

Kong is installed from [kong-dist-kubernetes](https://github.com/Kong/kong-dist-kubernetes).
Please follow their readme to setup the software. In the current installation we are using postgres as db, thus follow all the steps related to kong_postgres.

1. **Deploy a Kong supported database**

    For PostgreSQL, use the `postgres.yaml` file from the kong-dist-kubernetes
    repo to deploy a PostgreSQL `Service` and a `ReplicationController` in the
    cluster:

    ```bash
    $ kubectl create -f postgres.yaml
    ```

2. **Prepare database**

    Using the `kong_migration_<postgres|cassandra>.yaml` file from this repo,
    run the migration job, jump to step 5 if Kong backing databse is up–to–date:

    ```bash
    $ kubectl create -f kong_migration_postgres.yaml
    ```
    Once job completes, you can remove the pod by running following command:

    ```bash
    $ kubectl delete -f kong_migration_postgres.yaml
    ```

3. **Deploy Kong**
    the cluster:

    ```bash
    $ kubectl create -f kong_postgres.yaml
    ```

4. **Verify your deployments**

    You can now see the resources that have been deployed using `kubectl`:

    ```bash
    $ kubectl get all
    ```

    Once the `EXTERNAL_IP` is available for Kong Proxy and Admin services, you
    can test Kong by making the following requests:

    ```bash
    $ curl <kong-admin-ip-address>:8001
    $ curl https://<admin-ssl-ip-address>:8444
    $ curl <kong-proxy-ip-address>:8000
    $ curl https://<kong-proxy-ssl-ip-address>:8443
    ```

5. **Using Kong**

    Quickly learn how to use Kong with the
    [5-minute Quickstart](https://getkong.org/docs/latest/getting-started/quickstart/).


6. run the script to register the apis from kong folder `$ sh add_apis.sh`.

## Charts

If you want to manage Kong installation through Helm Charts, please follow
the [README](/charts/kong/README.md).

## Important Note

When deploying into a Kubernetes cluster with Deployment Manager, it is
important to be aware that deleting `ReplicationController` Kubernetes objects
**does not delete its underlying pods**, and it is your responisibility to
manage the destruction of these resources when deleting or updating a
`ReplicationController` in your configuration.
