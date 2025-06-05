# Kube App Version

## Run

```shell
mvn spring-boot:run
```

### Test Endpoints

```shell
http :8080/api/versions/outdated
```

## Deploy Example Applications

### ArgoCD

```shell
kubectl apply -f examples/argocd-applications 
```

## TODO

* add versioning calculation for Git
  * tags?
  * releases?
* add unit tests
* add datastore
* build container
* build native-container
* create Kustomize "package"?
  * or Helm

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