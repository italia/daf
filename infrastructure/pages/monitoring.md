# Monitoring

As tool for monitoring we use Prometheus + Grafana

## Prometheus

Prometheus is installed using the official [kubernetes chart](https://github.com/kubernetes/charts/tree/master/stable/prometheus).

`helm install --name prometheus stable/prometheus -f ./prometheus/values.yaml`

to verify the installation:


```bash

$ helm ls

$ helm status prometheus

```

### Update configurations

All the configurations for the AlertManager and Prometheus are saved via [ConfigMap](https://kubernetes.io/docs/tasks/configure-pod-container/configmap/).
If you run the following command you can see the configuration files.

```bash

$ k get configmap
NAME                                 DATA      AGE
prometheus-prometheus-alertmanager   1         3d
prometheus-prometheus-server         3         3d

```

In particular:

1. prometheus-prometheus-alertmanager contains the configuration for the alertmanager. You can check how they work [here](https://prometheus.io/docs/alerting/configuration/).
2. prometheus-prometheus-server contains the configurations for the server. You can check how they work [here](https://prometheus.io/docs/operating/configuration/).

#### Setup Prometheus Scraping Config

In the folder `prometheus` you can find the file `prometheus.yaml` with the current configurations.
you can download the latest version from the kubernetes config map with following command:

```bash
$ k get configmap prometheus-prometheus-server -o yaml > prometheus/prometheus.yaml

```

Then you can edit the scraping config and reload the helm chart with the following command:

```bash
$ helm upgrade -f prometheus.yaml prometheus stable/prometheus
```

in case of errors you can delete and reload the helm chart.

#### Instrument your micro-service to expose the metrics

In order to setup the metrics in your micro-service please take a look at the [corresponding tutorial](../doc/metrics_setup.md).

#### Metrics Persistence [TODO]

Add glusterfs volume claim for the chart

## Grafana

To install grafana we use the official [kubernetes chart of grafana](https://github.com/kubernetes/charts/tree/master/stable/grafana).
In the folder `grafana` check at the file `values.yaml`. It contains the default configuration used to run the chart.
The `daf prometheus` datasource is configured ad runtime. As regards the password we setup for now `admin:admin`, but we can integrate the instance to run with ldap, by modifying the corresponding section in the file [values.yaml](./grafana/values.yaml).

To run the chart:

```bash
helm install --name metrics stable/grafana -f ./grafana/values.yaml
```

to get the default admin password run the following command:

`kubectl get secret --namespace default metrics-grafana -o jsonpath="{.data.grafana-admin-password}" | base64 --decode ; echo`

to upgrade the config map please check the documentation made for prometheus. The commands to run are the same.

Right now there is a bug with the job that setup the datasource a thus we need to provide it manually.

### [TODO] setup persistence volume for dashboards and configurations.

## Kube-Daemon

https://github.com/appscode/kubed
