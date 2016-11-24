package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.providers.ContainerInstanceInfoProvider;

import java.util.Date;
import java.util.Map;

public class ContainerCloudInstance implements CloudInstance {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudInstance.class.getName());
    private final String id;
    private final ContainerCloudImage image;
    private final ContainerInstanceInfoProvider infoProvider;

    public ContainerCloudInstance(String id, ContainerCloudImage image, ContainerInstanceInfoProvider infoProvider) {
        this.id = id;
        this.image = image;
        this.infoProvider = infoProvider;
    }

    // Checks is the agent is running under this instance
    public boolean containsAgent(@NotNull jetbrains.buildServer.serverSide.AgentDescription agent) {
        Map<String, String> configParams = agent.getConfigurationParameters();
        return getInstanceId().equals(configParams.get(ContainerCloudConstants.AgentEnvParameterName_InstanceId)) &&
                getImageId().equals(configParams.get(ContainerCloudConstants.AgentEnvParameterName_ImageId));
    }

    // Returns correct error info if getStatus() returns InstanceStatus.ERROR value.
    public CloudErrorInfo getErrorInfo() {
        try {

            String error = infoProvider.getError(id);
            if (error == null || error.trim().isEmpty()) {
                return null;
            }
            return new CloudErrorInfo(error);
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
        return infoProvider.getNetworkIdentity(id);
    }

    // Returns the instance started time.
    @NotNull
    public Date getStartedTime() {
        return infoProvider.getStartedTime(id);
    }

    // current status of the instance
    @NotNull
    public InstanceStatus getStatus() {
        return infoProvider.getStatus(id);
    }
}