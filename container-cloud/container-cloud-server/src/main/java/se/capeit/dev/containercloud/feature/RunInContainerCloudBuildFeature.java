package se.capeit.dev.containercloud.feature;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.server.CloudManager;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementType;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.capeit.dev.containercloud.cloud.ContainerCloudClient;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RunInContainerCloudBuildFeature extends BuildFeature {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private final String jspPath;
    private final PluginDescriptor pluginDescriptor;
    private final CloudManager cloudManager;

    public RunInContainerCloudBuildFeature(final PluginDescriptor pluginDescriptor, CloudManager cloudManager) {
        this.jspPath = pluginDescriptor.getPluginResourcesPath(RunInContainerCloudConstants.FeatureSettingsHtmlFile);
        this.pluginDescriptor = pluginDescriptor;
        this.cloudManager = cloudManager;
    }

    @NotNull
    @Override
    public String getType() {
        return RunInContainerCloudConstants.TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Run in Container Cloud";
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cloud profile: ");
        sb.append(params.get(RunInContainerCloudConstants.ParameterName_CloudProfile));
        sb.append("\nImage: ");
        sb.append(params.get(RunInContainerCloudConstants.ParameterName_Image));

        return sb.toString();
    }

    @Nullable
    @Override
    public PropertiesProcessor getParametersProcessor() {
        return properties -> {
            ArrayList<InvalidProperty> toReturn = new ArrayList<>();
            if (!properties.containsKey(RunInContainerCloudConstants.ParameterName_CloudProfile))
                toReturn.add(new InvalidProperty(RunInContainerCloudConstants.ParameterName_CloudProfile,
                        "Please choose a cloud profile"));
            if (!properties.containsKey(RunInContainerCloudConstants.ParameterName_Image) ||
                    properties.get(RunInContainerCloudConstants.ParameterName_Image).isEmpty())
                toReturn.add(new InvalidProperty(RunInContainerCloudConstants.ParameterName_Image,
                        "Please choose an image"));

            String profileId = properties.get(RunInContainerCloudConstants.ParameterName_CloudProfile);
            ContainerCloudClient client = (ContainerCloudClient) cloudManager.getClientIfExists(profileId);
            String imageId = properties.get(RunInContainerCloudConstants.ParameterName_Image);
            client.addImage(imageId);

            return toReturn;
        };
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultParameters() {
        HashMap<String, String> defaults = new HashMap<String, String>();
        defaults.put(RunInContainerCloudConstants.ParameterName_CloudProfile, "");
        defaults.put(RunInContainerCloudConstants.ParameterName_Image, "");

        return defaults;
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return jspPath;
    }

    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return false;
    }
}
