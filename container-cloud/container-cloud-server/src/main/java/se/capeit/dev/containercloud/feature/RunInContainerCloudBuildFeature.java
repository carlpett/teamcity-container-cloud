package se.capeit.dev.containercloud.feature;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RunInContainerCloudBuildFeature extends BuildFeature {
    private final String jspPath;

    public RunInContainerCloudBuildFeature(final PluginDescriptor pluginDescriptor) {
        jspPath = pluginDescriptor.getPluginResourcesPath("feature-settings.jsp");
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
        return new PropertiesProcessor() {
            public Collection<InvalidProperty> process(Map<String, String> properties) {
                ArrayList<InvalidProperty> toReturn = new ArrayList<InvalidProperty>();
                if (!properties.containsKey(RunInContainerCloudConstants.ParameterName_CloudProfile))
                    toReturn.add(new InvalidProperty(RunInContainerCloudConstants.ParameterName_CloudProfile,
                            "Please choose a cloud profile"));
                if (!properties.containsKey(RunInContainerCloudConstants.ParameterName_Image) ||
                        properties.get(RunInContainerCloudConstants.ParameterName_Image).isEmpty())
                    toReturn.add(new InvalidProperty(RunInContainerCloudConstants.ParameterName_Image,
                            "Please choose an image"));

                return toReturn;
            }
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
