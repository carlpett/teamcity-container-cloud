package se.capeit.dev.containercloud.cloud;

import java.util.Collection;
import java.util.Collections;
import jetbrains.buildServer.clouds.*;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;

public class ContainerCloudImage implements CloudImage {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudImage.class.getName());
    private final String containerImage;

    public ContainerCloudImage(String containerImage) {
        LOG.info("ContainerCloudImage.ctor");
        this.containerImage = containerImage;
    }

    // Finds instance by instanceId
    public CloudInstance findInstanceById(String id) {
        LOG.info("findInstanceById");
        return null;
    }

    // Returns the description of the image.
    public String getDescription() {
        LOG.info("getDescription");
        return "pelle-image-desc";
    }

    // Returns the identifier of this image, in the vendor-specific form.
    public String getId() {
        LOG.info("getId");
        return containerImage;
    }

    // Returns all instances of running image
    public Collection<? extends CloudInstance> getInstances() {
        LOG.info("getInstances");
        return Collections.emptyList();
    }

    public String getName() {
        LOG.info("getName");
        return "pelle-image-name";
    }

    // Returns error information of there was an error.
    public CloudErrorInfo getErrorInfo() {
        LOG.info("getErrorInfo");
        return null;
    }

    public Integer getAgentPoolId() {
        LOG.info("getAgentPoolId");
        return 0;
    }
}