package se.capeit.dev.containercloud.cloud;

import com.google.common.base.Strings;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.AgentDescription;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.providers.ContainerProvider;
import se.capeit.dev.containercloud.cloud.providers.ContainerProviderFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerCloudClient implements CloudClientEx {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private final CloudState state;
    private final Map<String, ContainerCloudImage> images;
    private final CloudClientParameters cloudClientParams;
    private final ContainerProvider containerProvider;
    private boolean canCreateContainers;

    public ContainerCloudClient(CloudState state, CloudClientParameters params) {
        LOG.info("Creating container client for profile " + state.getProfileId());

        this.cloudClientParams = params;
        this.state = state;
        this.canCreateContainers = true;

        this.images = loadImagesFromProfileParameters();
        this.containerProvider = ContainerProviderFactory.getProvider(cloudClientParams);
    }

    private Map<String, ContainerCloudImage> loadImagesFromProfileParameters() {
        String imagesJson = cloudClientParams.getParameter(ContainerCloudConstants.ProfileParameterName_Images);
        if (Strings.isNullOrEmpty(imagesJson)) {
            return new HashMap<>();
        }
        return CloudImageParameters.collectionFromJson(imagesJson).stream()
                .map(CloudImageParameters::getId)
                .collect(Collectors.toMap(id -> id, ContainerCloudImage::new));
    }

    private void saveImagesToProfileParameters() {
        List<CloudImageParameters> cloudImageParameters = images.values().stream()
                .map(image -> {
                    CloudImageParameters cip = new CloudImageParameters();
                    cip.setParameter(CloudImageParameters.SOURCE_ID_FIELD, image.getId());
                    return cip;
                })
                .collect(Collectors.toList());
        cloudClientParams.setParameter(ContainerCloudConstants.ProfileParameterName_Images, CloudImageParameters.collectionToJson(cloudImageParameters));
    }

    public synchronized void addImage(String containerImageId) {
        if (images.containsKey(containerImageId)) {
            LOG.debug("Image " + containerImageId + " is already present in profile " + state.getProfileId());
            return;
        }

        // Add to active list of images
        images.put(containerImageId, new ContainerCloudImage(containerImageId));
        // Add to profile itself so image is still available if server is restarted
        saveImagesToProfileParameters();

        LOG.debug("Added " + containerImageId + " to profile " + state.getProfileId());
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
        String imageId = agent.getAvailableParameters().get("env." + ContainerCloudConstants.AgentEnvParameterName_ImageId);
        if (Strings.isNullOrEmpty(imageId)) {
            return null;
        }

        CloudImage image = findImageById(imageId);
        if (image == null) {
            return null;
        }

        String instanceId = agent.getAvailableParameters().get("env." + ContainerCloudConstants.AgentEnvParameterName_InstanceId);
        if (Strings.isNullOrEmpty(instanceId)) {
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
    }

    /* Restarts instance if possible */
    public void restartInstance(@NotNull CloudInstance instance) {
        throw new UnsupportedOperationException("Restart not supported");
    }

    /* Starts a new agent */
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
            String instanceId = generateInstanceId(image);
            tag.addAgentConfigurationParameter("SERVER_URL", tag.getServerAddress());
            tag.addAgentConfigurationParameter("AGENT_NAME", "container-cloud_" + instanceId);
            tag.addAgentConfigurationParameter(ContainerCloudConstants.AgentEnvParameterName_ImageId, image.getId());
            tag.addAgentConfigurationParameter(ContainerCloudConstants.AgentEnvParameterName_InstanceId, instanceId);

            ContainerCloudInstance instance = containerProvider.startInstance(instanceId, containerImage, tag);
            // TODO: Should there be some instance/image mapping registry instead?
            containerImage.registerInstance(instance);
            state.registerRunningInstance(instance.getImageId(), instance.getInstanceId());
            return instance;
        } catch (Exception e) {
            LOG.error("Failed to start new ContainerCloudInstance: " + e.getMessage(), e);
            throw new CloudException(e.getMessage(), e);
        }
    }

    @NotNull
    private String generateInstanceId(@NotNull CloudImage image) {
        return image.getId().replace('/', '_').replace(':', '_').replace('.', '_') + "_" + System.currentTimeMillis();
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
            cloudImage.unregisterInstance(cloudInstance.getInstanceId());
        } catch (Exception e) {
            LOG.error("Failed to stop ContainerCloudInstance " + instance.getInstanceId(), e);
        }
        state.registerTerminatedInstance(image.getId(), instance.getInstanceId());
    }
}