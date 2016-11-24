package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.AgentDescription;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.providers.ContainerProvider;
import se.capeit.dev.containercloud.cloud.providers.ContainerProviderFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContainerCloudClient implements CloudClientEx {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private final CloudState state;
    private final Map<String, ContainerCloudImage> images;
    private final CloudClientParameters cloudClientParams;
    private final ContainerProvider containerProvider;
    private boolean canCreateContainers;

    public ContainerCloudClient(CloudState state, final CloudClientParameters params) {
        LOG.info("Creating container client for profile " + state.getProfileId());

        this.cloudClientParams = params;
        this.state = state;
        this.canCreateContainers = true;
        this.images = new HashMap<>();
        images.put("jetbrains/teamcity-agent:10.0.2", new ContainerCloudImage("jetbrains/teamcity-agent:10.0.2"));

        this.containerProvider = ContainerProviderFactory.getProvider(cloudClientParams);
    }

    public synchronized void addImage(String containerImageId) {
        if (images.containsKey(containerImageId)) {
            LOG.info("Image " + containerImageId + " is already present in profile " + state.getProfileId());
            return;
        }

        images.put(containerImageId, new ContainerCloudImage(containerImageId));
        LOG.info("Added " + containerImageId + " to profile " + state.getProfileId());
    }

    // CloudClient
    public String generateAgentName(@NotNull AgentDescription desc) {
        LOG.info("generateAgentName");
        return "TODO-container-agent-name";
    }

    /* Call this method to check if it is possible (in theory) to start new instance of a given image in this profile. */
    public boolean canStartNewInstance(@NotNull CloudImage image) {
        // TODO: Validate that the image is in the list of images
        return canCreateContainers;
    }

    /* Looks for an image with the specified identifier and returns its handle. */
    public CloudImage findImageById(@NotNull String imageId) {
        return images.getOrDefault(imageId, null);
    }

    /* Checks if the agent is an instance of one of the running instances of that cloud profile. */
    public CloudInstance findInstanceByAgent(@NotNull AgentDescription agent) {
        String imageId = agent.getAvailableParameters().get("env." + ContainerCloudConstants.AGENT_ENV_PARAMETER_IMAGE_ID);
        if (imageId == null) {
            return null;
        }

        CloudImage image = findImageById(imageId);
        if (image == null) {
            return null;
        }

        String instanceId = agent.getAvailableParameters().get("env." + ContainerCloudConstants.AGENT_ENV_PARAMETER_INSTANCE_ID); // Container hostname is same as container id[0:12]
        if (instanceId == null) {
            return null;
        }

        return image.findInstanceById(instanceId);
    }

    /* Returns correct error info if there was any or null. */
    public CloudErrorInfo getErrorInfo() {
        return null;
    }

    /* Lists all user selected images. */
    @NotNull
    public Collection<? extends CloudImage> getImages() {
        //LOG.info("Get images: " + images.keySet().stream().collect(Collectors.joining(",")));
        return images.values();
    }

    /* Checks if the client data is fully ready to be queried by the system. */
    public boolean isInitialized() {
        return true;
    }

    // CloudClientEx
    /* Notifies client that it is no longer needed, This is a good time to release all resources allocated to implement the client */
    public void dispose() {
        LOG.info("Disposing ContainerCloudClient");
        canCreateContainers = false;
        //images.values().forEach(ContainerCloudImage::dispose);
        containerProvider.dispose();
    }

    /* Restarts instance if possible */
    public void restartInstance(@NotNull CloudInstance instance) {
        throw new UnsupportedOperationException("Restart not supported");
    }

    /* Starts a new virtual machine instance */
    @NotNull
    public CloudInstance startNewInstance(@NotNull CloudImage image, @NotNull CloudInstanceUserData tag) {
        if (!canCreateContainers) {
            LOG.error("Cannot create new container of image " + image.getId() + ", disposing");
            return null;
        }

        ContainerCloudImage containerImage = image instanceof ContainerCloudImage ? (ContainerCloudImage) image : null;
        if (containerImage == null) {
            throw new CloudException("Cannot start instance with image " + image.getId() + ", not a ContainerCloudImage object");
        }

        try {
            tag.setAgentRemovePolicy(CloudConstants.AgentRemovePolicyValue.RemoveAgent);
            ContainerCloudInstance instance = containerProvider.startInstance(containerImage, tag);
            // TODO: Should there be some instance/image mapping registry instead?
            containerImage.registerInstance(instance);
            state.registerRunningInstance(instance.getImageId(), instance.getInstanceId());
            return instance;
        } catch (Exception e) {
            LOG.error("Failed to start new ContainerCloudInstance: " + e.getMessage(), e);
            throw new CloudException(e.getMessage(), e);
        }
    }

    /* Terminates instance. */
    public void terminateInstance(@NotNull CloudInstance instance) {
        LOG.info("terminateInstance " + instance.getImageId());
        CloudImage image = instance.getImage();

        ContainerCloudImage cloudImage = image instanceof ContainerCloudImage ? (ContainerCloudImage) image : null;
        if (cloudImage == null) {
            LOG.error("Cannot stop instance with id " + instance.getInstanceId() + ", not does not have a ContainerCloudImage");
            return;
        }
        ContainerCloudInstance cloudInstance = instance instanceof ContainerCloudInstance ? (ContainerCloudInstance) instance : null;

        try {
            containerProvider.stopInstance(cloudInstance);
        } catch (Exception e) {
            LOG.error("Failed to stop ContainerCloudInstance " + instance.getInstanceId(), e);
        }
        state.registerTerminatedInstance(image.getId(), instance.getInstanceId());
    }
}