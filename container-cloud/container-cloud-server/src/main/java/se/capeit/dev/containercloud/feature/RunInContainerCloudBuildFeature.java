package se.capeit.dev.containercloud.feature;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.server.CloudManager;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RunInContainerCloudBuildFeature extends BuildFeature {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private final String editParametersPath;
    private final CloudManager cloudManager;

    public RunInContainerCloudBuildFeature(PluginDescriptor pluginDescriptor, CloudManager cloudManager) {
        this.editParametersPath = pluginDescriptor.getPluginResourcesPath(RunInContainerCloudConstants.FeatureSettingsHtmlFile);
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

        String profileId = params.get(RunInContainerCloudConstants.ParameterName_CloudProfile);
        String profileName = cloudManager.findProfileById(profileId).getProfileName();
        sb.append("Cloud profile: ");
        sb.append(profileName);

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
            else if (!properties.get(RunInContainerCloudConstants.ParameterName_Image).matches(RunInContainerCloudConstants.ContainerImageRegex))
                toReturn.add(new InvalidProperty(RunInContainerCloudConstants.ParameterName_Image,
                        "Image must have format owner/image:version or repo-domain/owner/image:version (note that upper-case letters are not allowed)"));

            return toReturn;
        };
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultParameters() {
        HashMap<String, String> defaults = new HashMap<>();
        defaults.put(RunInContainerCloudConstants.ParameterName_CloudProfile, "");
        defaults.put(RunInContainerCloudConstants.ParameterName_Image, "");

        return defaults;
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return editParametersPath;
    }

    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return false;
    }
}
