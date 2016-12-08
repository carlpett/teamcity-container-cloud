package se.capeit.dev.containercloud.cloud.providers;

import jetbrains.buildServer.clouds.CloudInstanceUserData;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.ContainerCloudImage;
import se.capeit.dev.containercloud.cloud.ContainerCloudInstance;

public interface ContainerProvider {
    ContainerCloudInstance startInstance(@NotNull String instanceId, @NotNull ContainerCloudImage image, @NotNull CloudInstanceUserData tag);

    void stopInstance(@NotNull ContainerCloudInstance instance);

    TestConnectionResult testConnection();
}
