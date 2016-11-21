package se.capeit.dev.containercloud.cloud.providers;

import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudException;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;

public class ContainerProviderFactory {
    public static ContainerProvider getProvider(CloudClientParameters parameters) {
        String provider = parameters.getParameter(ContainerCloudConstants.CONTAINER_PROVIDER_SETTING);
        switch (provider) {
            case "docker-socket":
                return new DockerSocketContainerProvider(parameters);
            case "helios":
                return new HeliosContainerProvider(parameters);
            default:
                throw new CloudException("Unknown container provider '" + provider + "'");
        }
    }
}
