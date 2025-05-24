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

* fix delta's properties


```json
{
  "currentArtifactVersion": "1.13",
  "deployedAppVersion": "1.13",
  "latestGARelease": "1.17.2",
  "latestOverallVersion": "1.18.0-alpha.0",
  "latestPreRelease": "1.18.0-alpha.0",
  "majorVersionDelta": 0,
  "minorVersionDelta": 4,
  "nextMajorVersion": "1.17.2",
  "nextMinorVersion": "1.13.6"
}
```