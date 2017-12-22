# Post Installation Services

## setup kubernetes-dashboard

To install the [dashboard](https://github.com/kubernetes/dashboard) run the following:

```
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/master/src/deploy/recommended/kubernetes-dashboard.yaml
```

then you need too add the kubernetes-dashboard to cluster-admin role with the following command.

```
kubectl create clusterrolebinding kubernetes-dashboard-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:kubernetes-dashboard
```

In case of problems, such as remove the dashboard, check the [official doc](https://github.com/kubernetes/dashboard/wiki/Installation)

Run `kubectl proxy` to access the dashboard.


## Setup Helm as Kubernetes Package Manager

Run this command to install the latest version of [Helm](https://helm.sh/). Helm is a package manager for kubernetes.
It has the definition of `charts` that are reusable deploy of applications. You can create your [charts]() or to use other deployed at [kubeapps](https://kubeapps.com/).

We can manage also [secrets](https://github.com/kubeapps/kubeapps/blob/master/docs/getting-started.md).

There is a problem on Helm for the first installation since we need to create the role for [tiller](https://github.com/kubernetes/helm/issues/2224).
A workaround is to run the following commands.

```
kubectl create serviceaccount --namespace kube-system tiller
kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'
```

Then we can see `kubectl get sa --all-namespaces` that tiller is set as service account properly.

```
[root@master ~]# k get sa --all-namespaces
....
NAMESPACE     NAME                         SECRETS   AGE
kube-system   tiller                       1         4m
kube-system   token-cleaner                1         15h
kube-system   ttl-controller               1         15h
kube-system   weave-net                    1         15h

```

then we can run the installation of helm

```bash
curl -fsSL https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
```

and run the command `helm init` to initialize helm. Every client can run this command, since in case tille is already installed it will setup only the client version.




### Install a chart using  helm

1. search the Dashboard `helm search dashboard` or you can navigate to `https://hub.kubeapps.com/`.

```
NAME                        VERSION DESCRIPTION
stable/kubernetes-dashboard 0.4.2   General-purpose web UI for Kubernetes clusters
stable/jasperreports        0.2.1   The JasperReports server can be used as a stand...
stable/kube-ops-view        0.4.1   Kubernetes Operational View - read-only system ...
stable/uchiwa               0.2.2   Dashboard for the Sensu monitoring framework
stable/weave-cloud          0.1.2   Weave Cloud is a add-on to Kubernetes which pro...
stable/weave-scope          0.9.1   A Helm chart for the Weave Scope cluster visual...

```

2. install the dashboard `helm install stable/weave-scope --name=weave-scope`

```
helm install stable/weave-scope
NAME:   nasal-aardvark
LAST DEPLOYED: Sat Dec 16 11:46:42 2017
NAMESPACE: default
STATUS: DEPLOYED

RESOURCES:
==> v1/ConfigMap
NAME                              DATA  AGE
weave-scope-nasal-aardvark-tests  1     1s

==> v1/ServiceAccount
NAME                        SECRETS  AGE
nasal-aardvark-weave-scope  1        1s

==> v1beta1/ClusterRole
NAME                        AGE
nasal-aardvark-weave-scope  1s

==> v1beta1/ClusterRoleBinding
NAME                        AGE
nasal-aardvark-weave-scope  1s

==> v1/Service
NAME                        TYPE       CLUSTER-IP     EXTERNAL-IP  PORT(S)  AGE
nasal-aardvark-weave-scope  ClusterIP  XXXXXXXXXXXXX  <none>       80/TCP   1s

==> v1beta1/DaemonSet
NAME                              DESIRED  CURRENT  READY  UP-TO-DATE  AVAILABLE  NODE SELECTOR  AGE
weave-scope-agent-nasal-aardvark  4        4        0      4           0          <none>         1s

==> v1beta1/Deployment
NAME                                 DESIRED  CURRENT  UP-TO-DATE  AVAILABLE  AGE
weave-scope-frontend-nasal-aardvark  1        1        1           0          0s

==> v1/Pod(related)
NAME                                    READY  STATUS             RESTARTS  AGE
weave-scope-agent-nasal-aardvark-4x2dw  0/1    ContainerCreating  0         0s
weave-scope-agent-nasal-aardvark-kj2wg  0/1    ContainerCreating  0         0s
weave-scope-agent-nasal-aardvark-rlspk  0/1    ContainerCreating  0         0s
weave-scope-agent-nasal-aardvark-rxdbv  0/1    ContainerCreating  0         0s


NOTES:
You should now be able to access the Scope frontend in your web browser, by
using kubectl port-forward:

kubectl -n default port-forward $(kubectl -n default get endpoints \
nasal-aardvark-weave-scope -o jsonpath='{.subsets[0].addresses[0].targetRef.name}') 8080:4040

then browsing to http://localhost:8080/.
For more details on using Weave Scope, see the Weave Scope documentation:

https://www.weave.works/docs/scope/latest/introducing/


```

3. checking the installation `helm ls`

```
[pippo@master ~]# helm ls
NAME        REVISION    UPDATED                     STATUS      CHART               NAMESPACE
weave-scope 1           Sat Dec 16 12:00:46 2017    DEPLOYED    weave-scope-0.9.1   default
```

4. get its status and info `helm status weave-scope`

5. See weave ui

```
kubectl -n default port-forward $(kubectl -n default get endpoints \
weave-scope-weave-scope -o jsonpath='{.subsets[0].addresses[0].targetRef.name}') 8080:4040
```
