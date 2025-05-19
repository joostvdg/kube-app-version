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

* should we extract Git and container Image version hashes for the artifacts?
* 