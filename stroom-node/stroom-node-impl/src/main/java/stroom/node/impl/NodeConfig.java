package stroom.node.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import stroom.util.config.annotations.ReadOnly;
import stroom.util.shared.IsConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NodeConfig implements IsConfig {
    private String nodeName = "tba";
    private StatusConfig statusConfig;

    public NodeConfig() {
        this.statusConfig = new StatusConfig();
    }

    @Inject
    public NodeConfig(final StatusConfig statusConfig) {
        this.statusConfig = statusConfig;
    }

    @ReadOnly
    @JsonPropertyDescription("Should only be set per node in application property file")
    @JsonProperty("node")
    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    @JsonProperty("status")
    public StatusConfig getStatusConfig() {
        return statusConfig;
    }

    public void setStatusConfig(final StatusConfig statusConfig) {
        this.statusConfig = statusConfig;
    }

    @Override
    public String toString() {
        return "NodeConfig{" +
                "nodeName='" + nodeName + '\'' +
                '}';
    }
}
