package se.capeit.dev.containercloud.cloud.providers;

import jetbrains.buildServer.clouds.CloudInstanceUserData;
import se.capeit.dev.containercloud.cloud.ContainerCloudImage;
import se.capeit.dev.containercloud.cloud.ContainerCloudInstance;

public interface ContainerProvider {
    ContainerCloudInstance startInstance(ContainerCloudImage image, CloudInstanceUserData tag);
    void stopInstance(ContainerCloudInstance instance);
    void dispose();
}
