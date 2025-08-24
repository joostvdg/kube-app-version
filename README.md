# Kube App Version

## TODO

* add configurable kubernetes endpoint
* test with argocd app deployment in Kind cluster
* add unit tests
  * use test containers for Redis
* GitHub Actions release
  * build container
  * build native-container
* run collection as recurring service
  * use a library?
  * quartz? or spring ecosystem alternative?
* add security -> JWT?
* add configuration options
  * with or w/o security token
  * run collection on startup y/n
  * run collection recurring y/n
  * recurring interval
* add versioning calculation for Git
  * tags?
  * releases?
* GUI application
  * with Vaadin?
  * with NextJS
  * any new hip alternatives?
* CLI to interact with servers
  * written in Rust
  * use Ratatui for console UI?


## Test Kustomize Deployment

```shell
brew install kustomize
```

```shell
kustomize build kubernetes/kustomize/overlays/kind | yq
```


```shell
kubectl create ns kav
```

```shell
kustomize build kubernetes/kustomize/overlays/kind | kubectl apply -n kav -f -
```

```shell
kubectl get sa,deploy,svc,po -n kav
```

### Create Secret for GitHub

```shell
export GITHUB_TOKEN=
```

```shell
kubectl create secret generic github-token \
    -n kav \
    --from-literal=token="${GITHUB_TOKEN}"
```

### Install Redis

```shell
helm upgrade --install \
  redis  oci://registry-1.docker.io/bitnamicharts/redis \
  --namespace kav
```

## Run

```shell
export GITHUB_TOKEN=
```

```shell
mvn spring-boot:run
```

```shell
REDIS_MODE=DISABLED LOGGING_LEVEL_NET_JOOSTVDG=DEBUG mvn spring-boot:run
```

```shell
REDIS_MODE=REQUIRED REDIS_RECONNECT_INTERVAL=30000 mvn spring-boot:run
```

```shell
REDIS_MODE=REQUIRED mvn spring-boot:run
```

```shell
REDIS_MODE=OPTIONAL mvn spring-boot:run
```

```shell
KUBERNETES_CLUSTER_ENDPOINT=127.0.0.1:44177 mvn spring-boot:run
```

### Test Endpoints

```shell
http :8080/api/versions/outdated
```

```shell
http :8080/api/versions/artifacts
```

### Run Docker Image

* https://www.chainguard.dev/unchained/building-minimal-and-low-cve-images-for-java
* https://snyk.io/blog/best-practices-to-build-java-containers-with-docker/
* https://github.com/joostvdg/ingress-dns-export-controller/blob/main/Dockerfile

```shell
docker run --rm -it --name kav ghcr.io/joostvdg/kube-app-version:0.1.0 -e APP_REDIS_MODE=OPTIONAL
```

### Docker Native

* https://github.com/joostvdg/where-was-i/blob/main/Dockerfile.native

## Deploy Example Applications

### ArgoCD

```shell
kubectl apply -f examples/argocd-applications 
```

## Helm Indexs

### Classica

* Chart repo: https://kriegalex.github.io/k8s-charts
* Index Location:  https://kriegalex.github.io/k8s-charts/index.yaml

```shell
http https://kriegalex.github.io/k8s-charts/index.yaml
```

### OCI

* Chart repo: oci://registry-1.docker.io/bitnamicharts


#### Docker Hub

```shell
http "https://hub.docker.com/v2/namespaces/bitnamicharts/repositories/keycloak/tags?page_size=10"
```

#### GitHub

* https://docs.github.com/en/rest/packages/packages?apiVersion=2022-11-28#list-package-versions-for-a-package-owned-by-a-user

Owned by a User.

```shell
curl -L \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer <YOUR-TOKEN>" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/users/USERNAME/packages/PACKAGE_TYPE/PACKAGE_NAME/versions
```

```shell
skopeo list-tags docker://ghcr.io/joostvdg/cmg
```

```shell
http -A bearer -a ${GITHUB_TOKEN} https://api.github.com/users/joostvdg/packages/container/cmg/versions
```

```shell
skopeo list-tags docker://ghcr.io/gabe565/charts/gotify
```

```shell
http -A bearer -a ${GITHUB_TOKEN} https://api.github.com/users/gabe565/packages/container/charts%2Fgotify/versions
```

### Dockerhub

* oci://registry-1.docker.io/bitnamicharts/keycloak
* https://www.arthurkoziel.com/dockerhub-registry-api/

```shell
skopeo list-tags docker://registry-1.docker.io/bitnamicharts
```

```shell
export AUTH_SERVICE='registry.docker.io'
export AUTH_SCOPE="repository:bitnamicharts/keycloak:pull"
export DHUB_TOKEN=$(curl -fsSL "https://auth.docker.io/token?service=$AUTH_SERVICE&scope=$AUTH_SCOPE" | jq --raw-output '.token')
```

```shell
curl -fsSL \
    -H "Authorization: Bearer $TOKEN" \
    "registry-1.docker.io/v2/bitnamicharts/keycloak/tags/list" | jq
```

```shell
http -A bearer -a ${DHUB_TOKEN} "https://registry-1.docker.io/v2/bitnamicharts/keycloak/tags/list"
```

## Kube Prometheus Stack

* https://artifacthub.io/packages/helm/prometheus-community/kube-prometheus-stack

### Install

```shell
helm upgrade --install \
  --values prom-values.yaml \
  prom-stack prometheus-community/kube-prometheus-stack
```

### Retrieve Grafana Password

```shell
kubectl --namespace default get secrets prom-stack-grafana -o jsonpath="{.data.admin-password}" | base64 -d ; echo
```

### Port-Forward to open locally

```shell
export POD_NAME=$(kubectl --namespace default get pod -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=prom-stack" -oname)
kubectl --namespace default port-forward $POD_NAME 3000
```

### Open Telemetry Operator

* https://artifacthub.io/packages/helm/opentelemetry-helm/opentelemetry-kube-stack

### Add Helm Repo

```shell
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update
```

```shell
helm upgrade --install \
    --set opentelemetry-operator.admissionWebhooks.certManager.enabled=false \
    --set admissionWebhooks.autoGenerateCert.enabled=true \
    otel-stack open-telemetry/opentelemetry-kube-stack
```

## OpenTelemetry Collector Standalone

* https://artifacthub.io/packages/helm/opentelemetry-helm/opentelemetry-collector

```shell
helm upgrade --install otel-collector \
  --values otel-values.yaml \
  open-telemetry/opentelemetry-collector 
```

## Tempo

```shell
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

```shell
helm upgrade --install tempo grafana/tempo
```

## ValKey Cluster

### Docker Compose

* https://github.com/redislabs-training/dockerized-redis-cluster
* https://github.com/dtaivpp/valkey-samples/blob/main/valkey-cluster.yaml

Open http://localhost:5540 to connect to Redis Insights.
Once connected, you can add the cluster by connecting to one of the nodes.
It is in the same network, so use a local address: `10.0.0.13` for example.
The password in `.env`.


## Error To Solve

```shell
.ClassNotFoundException: io.opentelemetry.sdk.internal.StandardComponentId$ExporterType
```

```shell
kube-app-version-74b98cbf94-vgz6m kube-app-version      ... 206 common frames omitted
kube-app-version-74b98cbf94-vgz6m kube-app-version Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter]: Factory method 'otlpHttpSpanExporter' threw exception with message: io/opentelemetry/sdk/internal/StandardComponentId$ExporterType
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.beans.factory.support.SimpleInstantiationStrategy.lambda$instantiate$0(SimpleInstantiationStrategy.java:199) ~[spring-beans-6.2.8.jar!/:6.2.8]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiateWithFactoryMethod(SimpleInstantiationStrategy.java:88) ~[spring-beans-6.2.8.jar!/:6.2.8]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:168) ~[spring-beans-6.2.8.jar!/:6.2.8]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:653) ~[spring-beans-6.2.8.jar!/:6.2.8]
kube-app-version-74b98cbf94-vgz6m kube-app-version      ... 226 common frames omitted
kube-app-version-74b98cbf94-vgz6m kube-app-version Caused by: java.lang.NoClassDefFoundError: io/opentelemetry/sdk/internal/StandardComponentId$ExporterType
kube-app-version-74b98cbf94-vgz6m kube-app-version      at io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder.<init>(OtlpHttpSpanExporterBuilder.java:53) ~[opentelemetry-exporter-otlp-1.51.0.jar!/:1.51.0]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter.builder(OtlpHttpSpanExporter.java:59) ~[opentelemetry-exporter-otlp-1.51.0.jar!/:1.51.0]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingConfigurations$Exporters.otlpHttpSpanExporter(OtlpTracingConfigurations.java:88) ~[spring-boot-actuator-autoconfigure-3.5.3.jar!/:3.5.3]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source) ~[na:na]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at java.base/java.lang.reflect.Method.invoke(Unknown Source) ~[na:na]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.beans.factory.support.SimpleInstantiationStrategy.lambda$instantiate$0(SimpleInstantiationStrategy.java:171) ~[spring-beans-6.2.8.jar!/:6.2.8]
kube-app-version-74b98cbf94-vgz6m kube-app-version      ... 229 common frames omitted
kube-app-version-74b98cbf94-vgz6m kube-app-version Caused by: java.lang.ClassNotFoundException: io.opentelemetry.sdk.internal.StandardComponentId$ExporterType
kube-app-version-74b98cbf94-vgz6m kube-app-version      at java.base/java.net.URLClassLoader.findClass(Unknown Source) ~[na:na]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at java.base/java.lang.ClassLoader.loadClass(Unknown Source) ~[na:na]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.boot.loader.net.protocol.jar.JarUrlClassLoader.loadClass(JarUrlClassLoader.java:107) ~[app.jar:0.0.1-SNAPSHOT]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at org.springframework.boot.loader.launch.LaunchedClassLoader.loadClass(LaunchedClassLoader.java:91) ~[app.jar:0.0.1-SNAPSHOT]
kube-app-version-74b98cbf94-vgz6m kube-app-version      at java.base/java.lang.ClassLoader.loadClass(Unknown Source) ~[na:na]
kube-app-version-74b98cbf94-vgz6m kube-app-version      ... 235 common frames omitted
```

## Not Working

* CloudBees
* external secrets operator
* gotify
* harness-delegate
