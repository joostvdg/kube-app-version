# Kube App Version

## Deploy Example Applications

### ArgoCD Folders

### ArgoCD Helm Chart

```shell
argocd app create cert-manager \
	--repo https://charts.jetstack.io \
	--helm-chart cert-manager \
	--revision 1.13 \
	--dest-server https://kubernetes.default.svc
```

## TODO

* test with more applications
* add versioning calculation for Git
  * tags?
  * releases?
* add unit tests
* add spring modulith
* add Spotless
* add datastore
* build container
* build native-container
* create Kustomize "package"?
  * or Helm