package se.capeit.dev.containercloud.cloud.providers;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerState;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.log.Loggers;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;
import se.capeit.dev.containercloud.cloud.ContainerCloudImage;
import se.capeit.dev.containercloud.cloud.ContainerCloudInstance;

import java.util.Date;

public class DockerSocketContainerProvider implements ContainerProvider, ContainerInstanceInfoProvider {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudImage.class.getName());
    private static final int CONTAINER_STOP_TIMEOUT_SECONDS = 10;

    private final DockerClient dockerClient;

    public DockerSocketContainerProvider(CloudClientParameters cloudClientParams) {
        try {
            // TODO: Allow parameters from profile
            dockerClient = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new CloudException("Failed to create docker client", e);
        }
    }

    @Override
    public ContainerCloudInstance startInstance(ContainerCloudImage image, CloudInstanceUserData tag) {
        String name = ("container-cloud-agent_" + image.getId() + "-" + generateUniqueId()).replace('/', '_').replace(':', '_').replace('.', '_');

        try {
            LOG.debug("Pulling image " + image.getId());
            dockerClient.pull(image.getId());
            ContainerConfig cfg = ContainerConfig.builder()
                    .image(image.getId())
                    .env("SERVER_URL=" + tag.getServerAddress(),
                            "AGENT_NAME=" + name,
                            ContainerCloudConstants.AGENT_ENV_PARAMETER_IMAGE_ID + "=" + image.getId(),
                            ContainerCloudConstants.AGENT_ENV_PARAMETER_CLOUD_PROFILE_ID + "=" + tag.getProfileId())
                    .build();
            ContainerCreation creation = dockerClient.createContainer(cfg, name);
            LOG.debug("Starting image " + image.getId());
            dockerClient.startContainer(creation.id());

            // TODO: Container hostname is set to first 12 characters of id. Use this as instance id until something better is figured out
            String id = creation.id().substring(0, 12);
            return new ContainerCloudInstance(id, image, this);
        } catch (Exception e) {
            throw new CloudException("Failed to start instance of image " + image.getId(), e);
        }
    }

    @Override
    public void stopInstance(ContainerCloudInstance instance) {
        try {
            dockerClient.stopContainer(instance.getInstanceId(), CONTAINER_STOP_TIMEOUT_SECONDS);
        } catch (Exception e) {
            throw new CloudException("Failed to stop instance " + instance.getInstanceId(), e);
        }
    }

    @Override
    public void dispose() {
    }

    private String generateUniqueId() {
        // TODO: This is just a temporary measure to avoid name conflicts
        return String.valueOf(System.currentTimeMillis() / 1000L);
    }

    @Override
    public String getError(String instanceId) {
        try {
            return dockerClient.inspectContainer(instanceId).state().error();
        } catch (ContainerNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new CloudException("Could not get error for container " + instanceId, e);
        }
    }

    @Override
    public String getNetworkIdentity(String instanceId) {
        try {
            // TODO: Can this be made better? What if the container has multiple interfaces?
            return dockerClient.inspectContainer(instanceId).networkSettings().ipAddress();
        } catch (Exception e) {
            LOG.error("Could not determine container ip", e);
            return null;
        }
    }

    @Override
    public Date getStartedTime(String instanceId) {
        try {
            return dockerClient.inspectContainer(instanceId).created();
        } catch (Exception e) {
            throw new CloudException("Could not determine container start time", e);
        }
    }

    @Override
    public InstanceStatus getStatus(String instanceId) {
        ContainerState state;
        try {
            state = dockerClient.inspectContainer(instanceId).state();
        } catch (Exception e) {
            LOG.error("Cannot get state of container " + instanceId, e);
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
