package net.joostvdg.kube_app_version.api.model;

public class Service {
    private String name;
    private String namespace;
    private String clusterIP;
    private String externalIP;

    public Service(String name, String namespace, String clusterIP, String externalIP) {
        this.name = name;
        this.namespace = namespace;
        this.clusterIP = clusterIP;
        this.externalIP = externalIP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service)) {
            return false;
        }

        Service service = (Service) o;

        if (!name.equals(service.name)) return false;

        return namespace.equals(service.namespace);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + namespace.hashCode();
        return result;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getClusterIP() {
        return clusterIP;
    }

    public String getExternalIP() {
        return externalIP;
    }
}
