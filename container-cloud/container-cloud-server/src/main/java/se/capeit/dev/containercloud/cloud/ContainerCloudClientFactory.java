package se.capeit.dev.containercloud.cloud;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jetbrains.buildServer.clouds.CloudClientFactory;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudRegistrar;
import jetbrains.buildServer.clouds.CloudState;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;


public class ContainerCloudClientFactory implements CloudClientFactory {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClientFactory.class.getName());
    private final String jspPath;

    public ContainerCloudClientFactory(final CloudRegistrar cloudRegistrar, 
                                       final PluginDescriptor pluginDescriptor) {
        LOG.info("ContainerCloudClientFactory.ctor");
        jspPath = pluginDescriptor.getPluginResourcesPath("profile-settings.jsp");
        cloudRegistrar.registerCloudFactory(this);

    }

    public ContainerCloudClient createNewClient(final CloudState state, final CloudClientParameters params) {
        LOG.info("createNewClient");
        try {
            return new ContainerCloudClient(state, params);
        } 
        catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    // Checks if the agent could be an instance of one of the running profiles.
    public boolean canBeAgentOfType(jetbrains.buildServer.serverSide.AgentDescription description) {
        LOG.info("canBeAgentOfType " + description.toString() + ": false");
        return false;
    }

    // The formal name of the cloud type.
    public String getCloudCode() {
        return ContainerCloudConstants.TYPE;
    }

    // Description to be shown on the web pages
    public String getDisplayName() {
        return "Container Cloud";
    }

    // Properties editor jsp
    public String getEditProfileUrl() {
        return jspPath;
    }

    // Return initial values for form parameters.
    public Map<String, String> getInitialParameterValues() {
        LOG.info("String> getInitialParameterValues");
        return Collections.emptyMap();
    }

    // Returns the properties processor instance (validator).
    public PropertiesProcessor getPropertiesProcessor() {
        LOG.info("getPropertiesProcessor");
        return new PropertiesProcessor() {
            public Collection<InvalidProperty> process(final Map<String, String> properties) {
                return Collections.emptyList();
            }
        };
    }
}