package net.joostvdg.kube_app_version.api.model;
import java.util.Objects;

public class DNSEntry {
    private String fqdn;
    private String ip;
    private String port;
    private String namespace;
    private String kind;
    private String controller;
    private String clusterName;
    private String clusterIP;

    public DNSEntry(
            String fqdn,
            String ip,
            String port,
            String namespace,
            String kind,
            String controller,
            String clusterName,
            String clusterIP) {
        this.fqdn = fqdn;
        this.ip = ip;
        this.port = port;
        this.namespace = namespace;
        this.kind = kind;
        this.controller = controller;
        this.clusterName = clusterName;
        this.clusterIP = clusterIP;
    }

    @Override
    public String toString() {
        return "DNSEntry{"
                + "fqdn='"
                + fqdn
                + '\''
                + ", ip='"
                + ip
                + '\''
                + ", port='"
                + port
                + '\''
                + ", namespace='"
                + namespace
                + '\''
                + ", kind='"
                + kind
                + '\''
                + ", controller='"
                + controller
                + '\''
                + ", clusterName='"
                + clusterName
                + '\''
                + ", clusterIP='"
                + clusterIP
                + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DNSEntry dnsEntry)) return false;

        if (!fqdn.equals(dnsEntry.fqdn)) return false;
        return clusterIP.equals(dnsEntry.clusterIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqdn, ip);
    }

    public String getFqdn() {
        return fqdn;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKind() {
        return kind;
    }

    public String getController() {
        return controller;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getClusterIP() {
        return clusterIP;
    }
}
