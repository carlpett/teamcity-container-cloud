package se.capeit.dev.containercloud.cloud.providers;

import java.util.Map;

public interface ContainerProviderSettingsValidator {
    Map<String, String> validateSettings();
}
