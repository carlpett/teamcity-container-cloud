package se.capeit.dev.containercloud.cloud.providers;

import jetbrains.buildServer.clouds.InstanceStatus;

import java.util.Date;

public interface ContainerInstanceInfoProvider {
    String getError(String instanceId);
    String getNetworkIdentity(String instanceId);
    Date getStartedTime(String instanceId);
    InstanceStatus getStatus(String instanceId);
}
