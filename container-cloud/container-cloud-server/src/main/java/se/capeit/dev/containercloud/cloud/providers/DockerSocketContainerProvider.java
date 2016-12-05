package se.capeit.dev.containercloud.cloud.providers;

import com.google.common.base.Strings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerState;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;
import se.capeit.dev.containercloud.cloud.ContainerCloudImage;
import se.capeit.dev.containercloud.cloud.ContainerCloudInstance;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DockerSocketContainerProvider implements ContainerProvider, ContainerInstanceInfoProvider {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudImage.class.getName());
    private static final int CONTAINER_STOP_TIMEOUT_SECONDS = 10;

    private final DockerClient dockerClient;

    public DockerSocketContainerProvider(CloudClientParameters cloudClientParams) {
        try {
            DefaultDockerClient.Builder builder = DefaultDockerClient.fromEnv();

            String apiEndpoint = cloudClientParams.getParameter(ContainerCloudConstants.ProfileParameterName_DockerSocket_Endpoint);
            if (!Strings.isNullOrEmpty(apiEndpoint)) {
                builder.uri(apiEndpoint);
            }

            dockerClient = builder.build();
        } catch (DockerCertificateException e) {
            throw new CloudException("Failed to create docker client", e);
        }
    }

    @Override
    public ContainerCloudInstance startInstance(@NotNull String instanceId, @NotNull ContainerCloudImage image, @NotNull CloudInstanceUserData tag) {
        try {
            LOG.debug("Pulling image " + image.getId());
            dockerClient.pull(image.getId());

            List<String> environment = new ArrayList<>();
            tag.getCustomAgentConfigurationParameters().forEach((key, value) -> environment.add(key + "=" + value));

            ContainerConfig cfg = ContainerConfig.builder()
                    .image(image.getId())
                    .env(environment)
                    .build();
            ContainerCreation creation = dockerClient.createContainer(cfg, instanceId);
            LOG.debug("Starting image " + image.getId());
            dockerClient.startContainer(creation.id());

            return new ContainerCloudInstance(instanceId, image, this);
        } catch (Exception e) {
            throw new CloudException("Failed to start instance of image " + image.getId(), e);
        }
    }

    @Override
    public void stopInstance(@NotNull ContainerCloudInstance instance) {
        try {
            LOG.debug("Stopping container " + instance.getInstanceId());
            dockerClient.stopContainer(instance.getInstanceId(), CONTAINER_STOP_TIMEOUT_SECONDS);
        } catch (InterruptedException | DockerException e) {
            throw new CloudException("Failed to stop instance " + instance.getInstanceId(), e);
        }
    }

    @Override
    public String getError(String instanceId) {
        try {
            return dockerClient.inspectContainer(instanceId).state().error();
        } catch (ContainerNotFoundException e) {
            return null;
        } catch (InterruptedException | DockerException e) {
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

    @NotNull
    public static PropertiesProcessor getPropertiesProcessor() {
        return properties -> {
            ArrayList<InvalidProperty> toReturn = new ArrayList<>();

            return toReturn;
        };
    }
}
