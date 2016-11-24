package se.capeit.dev.containercloud.cloud.providers;

import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerProviderFactory {
    public static ContainerProvider getProvider(CloudClientParameters parameters) {
        String provider = parameters.getParameter(ContainerCloudConstants.ProfileParameterName_ContainerProvider);
        switch (provider) {
            case ContainerCloudConstants.ProfileParameterValue_ContainerProvider_DockerSocket:
                return new DockerSocketContainerProvider(parameters);
            case ContainerCloudConstants.ProfileParameterValue_ContainerProvider_Helios:
                return new HeliosContainerProvider(parameters);
            default:
                throw new CloudException("Unknown container provider '" + provider + "'");
        }
    }

    public static PropertiesProcessor getPropertiesProcessor() {
        return properties -> {
            if (!properties.containsKey(ContainerCloudConstants.ProfileParameterName_ContainerProvider)) {
                // There's no use trying to validate anything more at this point (shouldn't ever happen anayway...)
                return Stream.of(
                        new InvalidProperty(ContainerCloudConstants.ProfileParameterName_ContainerProvider,
                                "Container provider not selected"))
                        .collect(Collectors.toList());
            }

            PropertiesProcessor specificProcessor;
            String provider = properties.get(ContainerCloudConstants.ProfileParameterName_ContainerProvider);
            switch (provider) {
                case ContainerCloudConstants.ProfileParameterValue_ContainerProvider_DockerSocket:
                    specificProcessor = DockerSocketContainerProvider.getPropertiesProcessor();
                    break;
                case ContainerCloudConstants.ProfileParameterValue_ContainerProvider_Helios:
                    specificProcessor = HeliosContainerProvider.getPropertiesProcessor();
                    break;
                default:
                    throw new CloudException("Unknown container provider '" + provider + "'");
            }

            return specificProcessor.process(properties);
        };
    }
}