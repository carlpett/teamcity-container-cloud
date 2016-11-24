package se.capeit.dev.containercloud.cloud.providers;

import jetbrains.buildServer.serverSide.PropertiesProcessor;

public interface ContainerProviderPropertiesProcessor {
    PropertiesProcessor getPropertiesProcessor();
}
