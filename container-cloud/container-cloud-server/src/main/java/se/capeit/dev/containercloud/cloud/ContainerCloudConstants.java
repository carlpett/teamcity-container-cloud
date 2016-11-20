package se.capeit.dev.containercloud.cloud;

public interface ContainerCloudConstants {
    String CLOUD_CODE = "Cntnr";

    String CONTAINER_PROVIDER_SETTING = "ContainerProvider";

    String AGENT_ENV_PARAMETER_IMAGE_ID = "CONTAINER_CLOUD_AGENT_IMAGE_ID";
    String AGENT_ENV_PARAMETER_INSTANCE_ID = "HOSTNAME"; // TODO: This is a temporary workaround, make something less dependent on docker implementation detail
    String AGENT_ENV_PARAMETER_CLOUD_PROFILE_ID = "CONTAINER_CLOUD_PROFILE_ID";
}