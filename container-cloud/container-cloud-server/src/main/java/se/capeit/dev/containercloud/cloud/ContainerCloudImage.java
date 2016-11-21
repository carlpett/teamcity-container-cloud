package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerCloudImage implements CloudImage {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudImage.class.getName());
    private final String imageId;
    private final Map<String, ContainerCloudInstance> instances;
    // TODO: Consider overriding hashCode?

    public ContainerCloudImage(String imageId) {
        this.imageId = imageId;
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

    public void registerInstance(ContainerCloudInstance instance) {
        instances.put(instance.getInstanceId(), instance);
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
}