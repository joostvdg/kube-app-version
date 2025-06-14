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

## Run

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

## ValKey Cluster

### Docker Compose

* https://github.com/redislabs-training/dockerized-redis-cluster
* https://github.com/dtaivpp/valkey-samples/blob/main/valkey-cluster.yaml

Open http://localhost:5540 to connect to Redis Insights.
Once connected, you can add the cluster by connecting to one of the nodes.
It is in the same network, so use a local address: `10.0.0.13` for example.
The password in `.env`.

`
