package se.capeit.dev.containercloud.cloud;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.*;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*; 
import com.spotify.docker.client.exceptions.*;
import jetbrains.buildServer.clouds.*;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;

public class ContainerCloudClient implements CloudClientEx {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private static int created = 0;

    final private DockerClient docker;
    private final CloudState state;
    private CloudErrorInfo lastError;

    public ContainerCloudClient(CloudState state, final CloudClientParameters params) throws DockerCertificateException {
        this.state = state;
        LOG.info("Creating container client");
        docker = DefaultDockerClient.fromEnv().build();
        lastError = null;
    }

    // CloudClient
    public String generateAgentName(jetbrains.buildServer.serverSide.AgentDescription desc) {
        LOG.info("generateAgentName");
        return "TODO-container-agent-name";
    }
    /* Call this method to check if it is possible (in theory) to start new instance of a given image in this profile. */
    public boolean canStartNewInstance(CloudImage image) {
        LOG.info("canStartNewInstance: " + (created < 1));
        return created < 1;
    }
    /* Looks for an image with the specified identifier and returns its handle. */
    public CloudImage findImageById(String imageId) {
        LOG.info("findImageById " + imageId);
        return new ContainerCloudImage(imageId);
    }
    /* Checks if the agent is an instance of one of the running instances of that cloud profile. */
    public CloudInstance findInstanceByAgent(jetbrains.buildServer.serverSide.AgentDescription agent) {
        LOG.info("findInstanceByAgent: " + agent.toString());
        return null;
    }
    /* Returns currect error info if there was any or null. */
    public CloudErrorInfo getErrorInfo() {
        LOG.info("getErrorInfo: " + lastError);
        return lastError;
    }
    /* Lists all user selected images. */
    public Collection<? extends CloudImage> getImages() {
        LOG.info("getImages");
        List<ContainerCloudImage> images = new LinkedList<ContainerCloudImage>();
        // TODO: Determine this magically somehow
        images.add(new ContainerCloudImage("jetbrains/teamcity-agent:latest"));
        return Collections.unmodifiableList(images);
    }
    /* Checks if the client data is fully ready to be queried by the system. */
    public boolean isInitialized() {
        LOG.info("isInitialized");
        return true;
    }

    // CloudClientEx
    /* Notifies client that it is no longer needed, This is a good time to release all resources allocated to implement the client */
    public void dispose() {
        LOG.info("dispose");
    }
    /* Restarts instance if possible */
    public void restartInstance(CloudInstance instance) {
        LOG.info("restartInstance: " + instance.toString());

    }
    /* Starts a new virtual machine instance */
    public CloudInstance startNewInstance(CloudImage image, CloudInstanceUserData tag) {
        LOG.info("startNewInstance");

        ContainerCloudImage containerImage = image instanceof ContainerCloudImage ? (ContainerCloudImage) image : null;
        if (containerImage == null) {
            LOG.error("Cannot start instance with image " + image.getId() + ", not a ContainerCloudImage object");
            return null;
        }

        try {
            ++created;
            docker.pull(containerImage.getId());

            String localIp = InetAddress.getLocalHost().getHostAddress();

            ContainerConfig cfg = ContainerConfig.builder()
                                                 .image(containerImage.getId())
                                                 .env("SERVER_URL=http://" + localIp + ":8111", "AGENT_NAME=mah-agent")
                                                 .build();
            ContainerCreation creation = docker.createContainer(cfg);
            docker.startContainer(creation.id());
            ContainerCloudInstance instance = new ContainerCloudInstance(creation.id(), containerImage, docker);

            state.registerRunningInstance(instance.getImageId(), instance.getInstanceId());

            return instance;
        }
        catch(Exception e) {
            lastError = new CloudErrorInfo(e.getMessage(), e.getMessage(), e);
            LOG.error("Failed to start new ContainerCloudInstance: " + e.getMessage(), e);
            return null;
        }
    }
    /* Terminates instance. */
    public void terminateInstance(CloudInstance instance) {
        LOG.info("terminateInstance " + instance.getImageId());
        ContainerCloudInstance containerInstance = instance instanceof ContainerCloudInstance ? (ContainerCloudInstance) instance : null;

        String id = containerInstance.getInstanceId();
        try {
            docker.stopContainer(id, 10);
        } catch (Exception e) {
            LOG.error("Failed to stop ContainerCloudInstance " + id, e);
        }

        state.registerTerminatedInstance(containerInstance.getImageId(), containerInstance.getInstanceId());
    }
}