package se.capeit.dev.containercloud.cloud;

public final class ContainerCloudConstants {
    public static final String CloudCode = "Cntnr";

    public static final String ProfileParameterName_ContainerProvider = "ContainerProvider";
    public static final String ProfileParameterValue_ContainerProvider_DockerSocket = "docker-socket";
    public static final String ProfileParameterValue_ContainerProvider_Helios = "helios";

    public static final String ProfileParameterName_DockerSocket_Socket = "DockerSocket-Socket";

    public static final String ProfileParameterName_Helios_MasterUrl = "Helios-MasterUrl";

    public static final String AgentEnvParameterName_ImageId = "CONTAINER_CLOUD_AGENT_IMAGE_ID";
    public static final String AgentEnvParameterName_InstanceId = "HOSTNAME"; // TODO: This is a temporary workaround, make something less dependent on docker implementation detail
    public static final String AgentEnvParameterName_ProfileId = "CONTAINER_CLOUD_PROFILE_ID";

    public String getCloudCode() {
        return CloudCode;
    }

    public String getProfileParameterName_ContainerProvider() {
        return ProfileParameterName_ContainerProvider;
    }

    public String getProfileParameterValue_ContainerProvider_DockerSocket() {
        return ProfileParameterValue_ContainerProvider_DockerSocket;
    }

    public String getProfileParameterValue_ContainerProvider_Helios() {
        return ProfileParameterValue_ContainerProvider_Helios;
    }

    public String getProfileParameterName_DockerSocket_Socket() {
        return ProfileParameterName_DockerSocket_Socket;
    }

    public String getProfileParameterName_Helios_MasterUrl() {
        return ProfileParameterName_Helios_MasterUrl;
    }

    public String getAgentEnvParameterName_ImageId() {
        return AgentEnvParameterName_ImageId;
    }

    public String getAgentEnvParameterName_InstanceId() {
        return AgentEnvParameterName_InstanceId;
    }

    public String getAgentEnvParameterName_ProfileId() {
        return AgentEnvParameterName_ProfileId;
    }
}