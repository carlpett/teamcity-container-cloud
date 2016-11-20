package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContainerCloudImage implements CloudImage {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudImage.class.getName());
    private static final int CONTAINER_STOP_TIMEOUT_SECONDS = 10;
    private final String imageId;
    private DockerClient docker;
    private final Map<String, ContainerCloudInstance> instances;
    private AtomicBoolean canStartNewContainers = new AtomicBoolean(true);

    public ContainerCloudImage(String imageId) {
        this.imageId = imageId;
        try {
            this.docker = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            this.docker = null;
        }
        this.instances = new ConcurrentHashMap<>();
    }

    // TODO: Periodically check status of all instances

    // Finds instance by instanceId
    public CloudInstance findInstanceById(@NotNull String id) {
        return instances.get(id);
    }

    // Returns the identifier of this image, in the vendor-specific form.
    @NotNull
    public String getId() {
        return imageId;
    }

    // Returns all instances of running image
    @NotNull
    public Collection<? extends CloudInstance> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    @NotNull
    public String getName() {
        return imageId;
    }

    // Returns error information of there was an error.
    public CloudErrorInfo getErrorInfo() {
        return null;
    }

    public Integer getAgentPoolId() {
        LOG.info("getAgentPoolId");
        return 0;
    }

    private String generateUniqueId() {
        // TODO: This is just a temporary measure to avoid name conflicts
        return String.valueOf(System.currentTimeMillis() / 1000L);
    }

    ContainerCloudInstance startContainer(CloudInstanceUserData tag) throws DockerException, InterruptedException {
        if (!canStartNewContainers.get()) {
            LOG.error("Cannot create new container of image " + getId() + ", disposing");
            return null;
        }

        String name = ("container-cloud-agent_" + imageId + "-" + generateUniqueId()).replace('/', '_').replace(':', '_').replace('.', '_');

        docker.pull(imageId);
        ContainerConfig cfg = ContainerConfig.builder()
                .image(imageId)
                .env("SERVER_URL=" + tag.getServerAddress(),
                        "AGENT_NAME=" + name,
                        ContainerCloudConstants.AGENT_ENV_PARAMETER_IMAGE_ID + "=" + imageId,
                        ContainerCloudConstants.AGENT_ENV_PARAMETER_CLOUD_PROFILE_ID + "=" + tag.getProfileId())
                .build();
        ContainerCreation creation = docker.createContainer(cfg, name);
        docker.startContainer(creation.id());

        // FIXME: Container hostname is set to first 12 characters of id. Use this as instance id until something better is figured out
        String id = creation.id().substring(0, 12);
        ContainerCloudInstance instance = new ContainerCloudInstance(id, this, docker);
        instances.put(instance.getInstanceId(), instance);

        return instance;
    }

    void stopContainer(ContainerCloudInstance instance) throws DockerException, InterruptedException {
        docker.stopContainer(instance.getInstanceId(), CONTAINER_STOP_TIMEOUT_SECONDS);
        boolean success = instances.remove(instance.getInstanceId(), instance);
        if (!success) {
            LOG.error("Could not remove instance " + instance.getInstanceId() + " from image " + imageId + " instance list");
        }
    }

    void dispose() {
        canStartNewContainers.set(false);
        for (ContainerCloudInstance instance : instances.values()) {
            try {
                LOG.info("Image " + getId() + " is being disposed. Stopping associated instance " + instance.getInstanceId());
                stopContainer(instance);
            } catch (Exception e) {
                LOG.error("Failed to stop container " + instance.getInstanceId() + " during disposal of image " + getId(), e);
            }
        }
    }
}