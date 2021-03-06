package se.capeit.dev.containercloud.cloud;

public final class ContainerCloudConstants {
    public static final String CloudCode = "Cntnr";
    public static final String ProfileSettingsJspFile = "profile-settings.jsp";
    public static final String ProfileSettingsTestConnectionPath = "profile-settings/test-connection.jsp";

    public static final String ProfileParameterName_ContainerProvider = "ContainerProvider";
    public static final String ProfileParameterName_Images = "Images";

    public static final String ProfileParameterValue_ContainerProvider_DockerSocket = "docker-socket";
    public static final String ProfileParameterValue_ContainerProvider_Helios = "helios";

    public static final String ProfileParameterName_DockerSocket_Endpoint = "DockerSocket-Endpoint";

    public static final String ProfileParameterName_Helios_MasterUrl = "Helios-MasterUrl";
    public static final String ProfileParameterName_Helios_HostNamePattern = "Helios-HostNamePattern";
    public static final String ProfileParameterName_Helios_HostSelectors = "Helios-HostSelectors";

    public static final String AgentEnvParameterName_ImageId = "CONTAINER_CLOUD_AGENT_IMAGE_ID";
    public static final String AgentEnvParameterName_InstanceId = "CONTAINER_CLOUD_INSTANCE_ID";

    // This regular expression validates a container image, source: https://github.com/docker/distribution/blob/master/reference/regexp.go
    // The expression corresponds to what would be generated by the code anchored(NameRegexp, literal(":"), TagRegexp)
    public static final String ContainerImageRegex = "^(?:(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])(?:(?:\\.(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]))+)?(?::[0-9]+)?/)?[a-z0-9]+(?:(?:(?:[._]|__|[-]*)[a-z0-9]+)+)?(?:(?:/[a-z0-9]+(?:(?:(?:[._]|__|[-]*)[a-z0-9]+)+)?)+)?:[\\w][\\w.-]{0,127}$";

    // JSP getters
    public String getCloudCode() {
        return CloudCode;
    }

    public String getProfileSettingsTestConnectionPath() {
        return ProfileSettingsTestConnectionPath;
    }

    public String getProfileParameterName_ContainerProvider() {
        return ProfileParameterName_ContainerProvider;
    }

    public String getProfileParameterName_Images() {
        return ProfileParameterName_Images;
    }

    public String getProfileParameterValue_ContainerProvider_DockerSocket() {
        return ProfileParameterValue_ContainerProvider_DockerSocket;
    }

    public String getProfileParameterValue_ContainerProvider_Helios() {
        return ProfileParameterValue_ContainerProvider_Helios;
    }

    public String getProfileParameterName_DockerSocket_Endpoint() {
        return ProfileParameterName_DockerSocket_Endpoint;
    }

    public String getProfileParameterName_Helios_MasterUrl() {
        return ProfileParameterName_Helios_MasterUrl;
    }

    public String getProfileParameterName_Helios_HostNamePattern() {
        return ProfileParameterName_Helios_HostNamePattern;
    }

    public String getProfileParameterName_Helios_HostSelectors() {
        return ProfileParameterName_Helios_HostSelectors;
    }

    public String getAgentEnvParameterName_ImageId() {
        return AgentEnvParameterName_ImageId;
    }

    public String getAgentEnvParameterName_InstanceId() {
        return AgentEnvParameterName_InstanceId;
    }

    public String getContainerImageRegex() {
        // Formatted for Javascript, so need to escape inner slashes
        return ContainerImageRegex.replace("/", "\\/");
    }
}