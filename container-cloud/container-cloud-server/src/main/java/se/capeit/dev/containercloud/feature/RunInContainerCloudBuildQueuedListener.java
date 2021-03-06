package se.capeit.dev.containercloud.feature;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.CloudClientEx;
import jetbrains.buildServer.clouds.server.CloudManager;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.ContainerCloudClient;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class RunInContainerCloudBuildQueuedListener extends BuildServerAdapter {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private final CloudManager cloudManager;

    public RunInContainerCloudBuildQueuedListener(@NotNull SBuildServer server, @NotNull CloudManager cloudManager) {
        this.cloudManager = cloudManager;

        server.addListener(this);
    }

    @Override
    public void buildTypeAddedToQueue(@NotNull SQueuedBuild queuedBuild) {
        // This is added as a sort of "last resort" to make sure versioned settings etc don't bypass the
        // buildTypePersisted event and leave builds hanging in the queue.
        addImageForBuildType(queuedBuild.getBuildType());
    }

    @Override
    public void buildTypePersisted(@NotNull SBuildType buildType) {
        addImageForBuildType(buildType);
    }

    private void addImageForBuildType(@NotNull SBuildType buildType) {
        Collection<SBuildFeatureDescriptor> featureDescriptors = buildType.getBuildFeaturesOfType(RunInContainerCloudConstants.TYPE);
        // There is either zero or one feature
        Optional<SBuildFeatureDescriptor> feature = featureDescriptors.stream().findAny();

        // If there is no RunInContainerCloudFeature, do nothing
        if (!feature.isPresent()) {
            return;
        }

        // There is a feature, so get the parameters
        Map<String, String> parameters = feature.get().getParameters();
        String profileId = parameters.get(RunInContainerCloudConstants.ParameterName_CloudProfile);
        CloudClientEx clientEx = cloudManager.getClientIfExists(profileId);
        if (clientEx == null || !(clientEx instanceof ContainerCloudClient)) {
            LOG.warn("BuildType " + buildType.getConfigId() + " has a RunInContainerCloud feature indicating profile " + profileId + ", but there is no such cloud client registered");
            return;
        }

        LOG.info("Adding image " + parameters.get(RunInContainerCloudConstants.ParameterName_Image) + " to profile " + profileId);
        ContainerCloudClient containerClient = (ContainerCloudClient) clientEx;
        containerClient.addImage(parameters.get(RunInContainerCloudConstants.ParameterName_Image));
    }
}
