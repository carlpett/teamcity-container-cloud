package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;


public class ContainerCloudClientFactory implements CloudClientFactory {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClientFactory.class.getName());
    private final String jspPath;

    public ContainerCloudClientFactory(final CloudRegistrar cloudRegistrar,
                                       final PluginDescriptor pluginDescriptor) {
        LOG.info("Creating ContainerCloudClientFactory");
        jspPath = pluginDescriptor.getPluginResourcesPath("profile-settings.jsp");
        cloudRegistrar.registerCloudFactory(this);
    }

    @NotNull
    public ContainerCloudClient createNewClient(@NotNull final CloudState state, @NotNull final CloudClientParameters params) {
        try {
            return new ContainerCloudClient(state, params);
        } catch (Exception e) {
            throw new CloudException("Failed to create new Container Cloud client", e);
        }
    }

    // Checks if the agent could be an instance of one of the running profiles.
    public boolean canBeAgentOfType(@NotNull jetbrains.buildServer.serverSide.AgentDescription description) {
        LOG.info("ClientCloudClientFactory.canBeAgentOfType " + description.toString() + ", hardcoded true!");
        return true;
    }

    // The formal name of the cloud type.
    @NotNull
    public String getCloudCode() {
        return ContainerCloudConstants.CLOUD_CODE;
    }

    // Description to be shown on the web pages
    @NotNull
    public String getDisplayName() {
        return "Container Cloud";
    }

    // Properties editor jsp
    public String getEditProfileUrl() {
        return jspPath;
    }

    // Return initial values for form parameters.
    @NotNull
    public Map<String, String> getInitialParameterValues() {
        LOG.info("String> getInitialParameterValues");
        return Collections.emptyMap();
    }

    // Returns the properties processor instance (validator).
    @NotNull
    public PropertiesProcessor getPropertiesProcessor() {
        LOG.info("getPropertiesProcessor");
        return properties -> Collections.emptyList();
    }
}