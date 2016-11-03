package se.capeit.dev.containercloud.cloud;

import java.util.Date;

import com.intellij.openapi.util.text.StringUtil;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerState;
import jetbrains.buildServer.clouds.*;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;

public class ContainerCloudInstance implements CloudInstance {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudInstance.class.getName());
    private final String id;
    private final ContainerCloudImage image;
    private final DockerClient docker;

    // Checks is the agent is running under this instance
    public boolean containsAgent(jetbrains.buildServer.serverSide.AgentDescription agent) {
        return true;
    }

    public ContainerCloudInstance(String id, ContainerCloudImage image, DockerClient docker) {
        this.id = id;
        this.image = image;
        this.docker = docker;
    }

    public String getAgentName() {
        LOG.info("ContainerCloudInstance.getAgentName");
        return "pelle-instance-agent-name";
    }
           
    // Returns correct error info if getStatus() returns InstanceStatus.ERROR value.
    public CloudErrorInfo getErrorInfo() {
        try {
            return new CloudErrorInfo(docker.inspectContainer(id).state().error());
        } catch (Exception e) {
            LOG.error("Could not fetch error info", e);
            return new CloudErrorInfo("Failed to get error info", e.getMessage(), e);
        }
    }
    
    // Returns the reference to the handle of the image this instance started from.
    public CloudImage getImage() {
        return image;
    }
    
    // Returns the image identifier
    public String getImageId() {
        return image.getId();
    }
    
    // Returns the instance identifier
    public String getInstanceId() {
        return id;
    }
    
    // Name of the instance.
    public String getName() {
        return "pelle-instance-name";
    }
    
    // Returns the instance's DNS name (if one exists) or IPv4 address (if no DNS names).
    public String getNetworkIdentity() {
        try {
            return docker.inspectContainer(id).networkSettings().bridge();
        } catch (Exception e) {
            LOG.error("Could not determine container ip", e);
            return null;
        }
    }
    
    // Returns the instance started time.
    public Date getStartedTime() {
        try {
            return docker.inspectContainer(id).created();
        } catch (Exception e) {
            LOG.error("Could not determine container start time", e);
            return null;
        }
    }
    
    // current status of the instance
    public InstanceStatus getStatus() {
        ContainerState state = null;
        try {
            state = docker.inspectContainer(id).state();
        } catch (Exception e) {
            LOG.error("Cannot get state of container " + id, e);
        }

        if(state.running())
            return InstanceStatus.RUNNING;
        if(state.restarting())
            return InstanceStatus.RESTARTING;
        if(state.oomKilled() || StringUtil.isNotEmpty(state.error()))
            return InstanceStatus.ERROR;
        if(state.finishedAt() != null)
            return InstanceStatus.STOPPED;

        LOG.warn("Could not map state '" + state.toString() + "' to InstanceStatus");
        return InstanceStatus.UNKNOWN;
    }
}