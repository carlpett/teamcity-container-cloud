package se.capeit.dev.containercloud.agent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfigurationEx;
import jetbrains.buildServer.clouds.CloudConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class ContainerCloudAgentPropertiesSetter {
    private static final Logger LOG = Loggers.AGENT; // Logger.getInstance(ContainerCloudInstance.class.getName());

    private final BuildAgentConfigurationEx agentConfiguration;
    @NotNull
    private final EventDispatcher<AgentLifeCycleListener> events;

    public ContainerCloudAgentPropertiesSetter(final BuildAgentConfigurationEx agentConfiguration,
                                               @NotNull EventDispatcher<AgentLifeCycleListener> events) {
        LOG.info("Created ContainerCloudAgentPropertiesSetter");
        this.agentConfiguration = agentConfiguration;
        this.events = events;

        events.addListener(new AgentLifeCycleAdapter() {
            @Override
            public void afterAgentConfigurationLoaded(@NotNull BuildAgent buildAgent) {
                LOG.info("ContainerCloudAgentPropertiesSetter: Setting terminate after build flag");
                agentConfiguration.addConfigurationParameter(CloudConstants.AGENT_TERMINATE_AFTER_BUILD, "true");
            }
        });
    }
}
