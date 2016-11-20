package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.messages.ContainerState;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

public class ContainerCloudInstance implements CloudInstance {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudInstance.class.getName());
    private final String id;
    private final ContainerCloudImage image;
    private final DockerClient docker;

    public ContainerCloudInstance(String id, ContainerCloudImage image, DockerClient docker) {
        this.id = id;
        this.image = image;
        this.docker = docker;
    }

    // Checks is the agent is running under this instance
    public boolean containsAgent(@NotNull jetbrains.buildServer.serverSide.AgentDescription agent) {
        Map<String, String> configParams = agent.getConfigurationParameters();
        return getInstanceId().equals(configParams.get(ContainerCloudConstants.AGENT_ENV_PARAMETER_INSTANCE_ID)) &&
                getImageId().equals(configParams.get(ContainerCloudConstants.AGENT_ENV_PARAMETER_IMAGE_ID));
    }

    // Returns correct error info if getStatus() returns InstanceStatus.ERROR value.
    public CloudErrorInfo getErrorInfo() {
        try {
            String error = docker.inspectContainer(id).state().error();
            if(error == null || error.trim().isEmpty()) {
                return null;
            }
            return new CloudErrorInfo(error);
        } catch (ContainerNotFoundException e) {
            return null;
        } catch (Exception e) {
            LOG.error("Could not fetch error info", e);
            return new CloudErrorInfo("Failed to get error info", e.getMessage(), e);
        }
    }

    // Returns the reference to the handle of the image this instance started from.
    @NotNull
    public CloudImage getImage() {
        return image;
    }

    // Returns the image identifier
    @NotNull
    public String getImageId() {
        return image.getId();
    }

    // Returns the instance identifier
    @NotNull
    public String getInstanceId() {
        return id;
    }

    // Name of the instance.
    @NotNull
    public String getName() {
        return id;
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
    @NotNull
    public Date getStartedTime() {
        try {
            return docker.inspectContainer(id).created();
        } catch (Exception e) {
            throw new CloudException("Could not determine container start time", e);
        }
    }

    // current status of the instance
    @NotNull
    public InstanceStatus getStatus() {
        ContainerState state;
        try {
            state = docker.inspectContainer(id).state();
        } catch (Exception e) {
            LOG.error("Cannot get state of container " + id, e);
            return InstanceStatus.UNKNOWN;
        }

        if (state.running())
            return InstanceStatus.RUNNING;
        if (state.restarting())
            return InstanceStatus.RESTARTING;
        if (state.oomKilled() || StringUtil.isNotEmpty(state.error()))
            return InstanceStatus.ERROR;
        if (state.finishedAt() != null)
            return InstanceStatus.STOPPED;

        LOG.warn("Could not map state '" + state.toString() + "' to InstanceStatus");
        return InstanceStatus.UNKNOWN;
    }
}