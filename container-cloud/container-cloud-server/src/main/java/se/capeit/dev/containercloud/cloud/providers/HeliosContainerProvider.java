package se.capeit.dev.containercloud.cloud.providers;

import com.intellij.openapi.diagnostic.Logger;
import com.spotify.helios.client.HeliosClient;
import com.spotify.helios.common.descriptors.*;
import com.spotify.helios.common.protocol.CreateJobResponse;
import com.spotify.helios.common.protocol.JobDeleteResponse;
import com.spotify.helios.common.protocol.JobDeployResponse;
import com.spotify.helios.common.protocol.JobUndeployResponse;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;
import se.capeit.dev.containercloud.cloud.ContainerCloudImage;
import se.capeit.dev.containercloud.cloud.ContainerCloudInstance;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class HeliosContainerProvider implements ContainerProvider, ContainerInstanceInfoProvider {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudInstance.class.getName());
    private static final String VERSION = "1";
    private static final int TIMEOUT_JOB_CREATION = 3;

    private final HeliosClient heliosClient;
    private final CloudClientParameters cloudClientParams;
    private final ConcurrentHashMap<String, String> instanceIdJobMap;

    public HeliosContainerProvider(CloudClientParameters cloudClientParams) {
        this.cloudClientParams = cloudClientParams;
        this.heliosClient = HeliosClient.newBuilder()
                .setUser("teamcity-container-cloud")
                .setEndpoints(cloudClientParams.getParameter(ContainerCloudConstants.ProfileParameterName_Helios_MasterUrl))
                .build();
        instanceIdJobMap = new ConcurrentHashMap<>();
    }

    @Override
    public ContainerCloudInstance startInstance(@NotNull String instanceId, @NotNull ContainerCloudImage image, @NotNull CloudInstanceUserData tag) {
        Job.Builder jobBuilder = Job.newBuilder()
                .setImage(image.getId())
                .setName("container-cloud-agent")
                .setVersion(VERSION)
                .setHash(instanceId);
        // Add all tag custom configuration as environment vars
        tag.getCustomAgentConfigurationParameters().forEach(jobBuilder::addEnv);
        Job jobDescriptor = jobBuilder.buildWithoutHash();

        JobDeployResponse jobDeployResponse;
        String id;
        try {
            CreateJobResponse jobResponse = heliosClient.createJob(jobDescriptor).get();
            id = jobResponse.getId();

            // Todo: Host picking strategy
            String host = "solo.local.";
            jobDeployResponse = heliosClient.deploy(Deployment.of(JobId.fromString(id), Goal.START), host).get();
            LOG.info("Started Helios job " + id);
        } catch (Exception e) {
            throw new CloudException("Failed to start Helios job", e);
        }

        if (jobDeployResponse.getStatus() != JobDeployResponse.Status.OK) {
            throw new CloudException("Helios job status is '" + jobDeployResponse.getStatus() + "' not OK");
        }

        ContainerCloudInstance cloudInstance = new ContainerCloudInstance(instanceId, image, this);
        instanceIdJobMap.put(instanceId, id);
        return cloudInstance;
    }

    @Override
    public void stopInstance(@NotNull ContainerCloudInstance instance) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instance.getInstanceId()));
            JobStatus jobStatus = heliosClient.jobStatus(jobId).get();
            Set<String> hosts = jobStatus.getDeployments().keySet();

            for (String host : hosts) {
                JobUndeployResponse jobUndeployResponse = heliosClient.undeploy(jobId, host).get();
                if (jobUndeployResponse.getStatus() != JobUndeployResponse.Status.OK) {
                    throw new CloudException("Failed to undeploy Helios job, status " + jobUndeployResponse.getStatus());
                }
            }
            JobDeleteResponse jobDeleteResponse = heliosClient.deleteJob(jobId).get();
            if (jobDeleteResponse.getStatus() != JobDeleteResponse.Status.OK) {
                throw new CloudException("Failed to remove Helios job, status " + jobDeleteResponse.getStatus());
            }
            instanceIdJobMap.remove(instance.getInstanceId());
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudException("Failed to stop instance", e);
        }
    }

    @Override
    public String getError(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = heliosClient.jobStatus(jobId).get();
            if (jobStatus == null) {
                LOG.warn("Trying to read error state of non-existent job " + jobId.getName());
                return null;
            }
            String host = jobStatus.getTaskStatuses().keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new CloudException("Container " + instanceId + " not deployed on any host"));
            return jobStatus.getTaskStatuses().get(host).getContainerError();
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudException("Failed to read error information from instance " + instanceId, e);
        }
    }

    @Override
    public String getNetworkIdentity(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = heliosClient.jobStatus(jobId).get();
            if (jobStatus == null) {
                LOG.warn("Trying to read network identity of non-existent job " + jobId.getName());
                return null;
            }

            String host = jobStatus.getTaskStatuses().keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new CloudException("Container " + instanceId + " not deployed on any host"));
            // TODO: How to do this?
            return null;
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudException("Failed to read network information from instance " + instanceId, e);
        }
    }

    @Override
    public Date getStartedTime(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = heliosClient.jobStatus(jobId).get();
            if (jobStatus == null) {
                LOG.warn("Trying to read start time of non-existent job " + jobId.getName());
                return null;
            }

            String host = jobStatus.getTaskStatuses().keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new CloudException("Container " + instanceId + " not deployed on any host"));
            return new Date(jobStatus.getTaskStatuses().get(host).getJob().getCreated());
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudException("Failed to read start time information from instance " + instanceId, e);
        }
    }

    @Override
    public InstanceStatus getStatus(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = heliosClient.jobStatus(jobId).get();
            if (jobStatus == null) {
                LOG.warn("Trying to read status of non-existent job " + jobId.getName());
                return InstanceStatus.UNKNOWN;
            }

            String host = jobStatus.getTaskStatuses().keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new CloudException("Container " + instanceId + " not deployed on any host"));
            TaskStatus.State state = jobStatus.getTaskStatuses().get(host).getState();

            if (state.equals(TaskStatus.State.RUNNING))
                return InstanceStatus.RUNNING;
            if (state.equals(TaskStatus.State.STOPPED) || state.equals(TaskStatus.State.EXITED))
                return InstanceStatus.STOPPED;
            if (state.equals(TaskStatus.State.FAILED))
                return InstanceStatus.ERROR;
            if (state.equals(TaskStatus.State.STARTING) || state.equals(TaskStatus.State.CREATING) || state.equals(TaskStatus.State.PULLING_IMAGE))
                return InstanceStatus.STARTING;
            if (state.equals(TaskStatus.State.STOPPING))
                return InstanceStatus.STOPPING;

            LOG.warn("Could not map state '" + state.toString() + "' to InstanceStatus");
            return InstanceStatus.UNKNOWN;
        } catch (InterruptedException | ExecutionException e) {
            return InstanceStatus.UNKNOWN;
        }
    }

    public static PropertiesProcessor getPropertiesProcessor() {
        return properties -> {
            ArrayList<InvalidProperty> toReturn = new ArrayList<>();
            if (!properties.containsKey(ContainerCloudConstants.ProfileParameterName_Helios_MasterUrl) ||
                    properties.get(ContainerCloudConstants.ProfileParameterName_Helios_MasterUrl).isEmpty())
                toReturn.add(new InvalidProperty(ContainerCloudConstants.ProfileParameterName_Helios_MasterUrl,
                        "Helios master Url is required"));

            return toReturn;
        };
    }
}
