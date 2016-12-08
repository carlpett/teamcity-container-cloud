package se.capeit.dev.containercloud.cloud.providers;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class HeliosContainerProvider implements ContainerProvider, ContainerInstanceInfoProvider {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudInstance.class.getName());
    private static final int HELIOS_OPERATION_TIMEOUT_SECONDS = 5;

    private final HeliosClient heliosClient;
    private final CloudClientParameters cloudClientParams;
    private final ConcurrentHashMap<String, String> instanceIdJobMap;
    private final Random random = new Random();

    public HeliosContainerProvider(CloudClientParameters cloudClientParams) {
        this.cloudClientParams = cloudClientParams;
        this.heliosClient = HeliosClient.newBuilder()
                .setUser("teamcity-container-cloud")
                .setEndpoints(cloudClientParams.getParameter(ContainerCloudConstants.ProfileParameterName_Helios_MasterUrl))
                .build();
        instanceIdJobMap = new ConcurrentHashMap<>();
    }

    private String getRandomMatchingHost() {
        try {
            List<String> hosts = getHosts();
            if (hosts.size() == 0) {
                throw new CloudException("No Helios hosts matched conditions set in cloud profile!");
            }

            int idx = random.nextInt(hosts.size());
            return hosts.get(idx);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudException("Could not get host to run agent on", e);
        }
    }

    private <T> T getHeliosResult(ListenableFuture<T> future) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(HELIOS_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private List<String> getHosts() throws InterruptedException, ExecutionException, TimeoutException {
        String namePattern = cloudClientParams.getParameter(ContainerCloudConstants.ProfileParameterName_Helios_HostNamePattern);
        String selectors = cloudClientParams.getParameter(ContainerCloudConstants.ProfileParameterName_Helios_HostSelectors);

        List<String> hosts;
        if (!Strings.isNullOrEmpty(namePattern) && !Strings.isNullOrEmpty(selectors)) {
            Set<String> selectorSet = Arrays.stream(selectors.split(",")).collect(Collectors.toSet());
            hosts = getHeliosResult(heliosClient.listHosts(namePattern, selectorSet));
        } else if (!Strings.isNullOrEmpty(namePattern)) {
            hosts = getHeliosResult(heliosClient.listHosts(namePattern));
        } else if (!Strings.isNullOrEmpty(selectors)) {
            Set<String> selectorSet = Arrays.stream(selectors.split(",")).collect(Collectors.toSet());
            hosts = getHeliosResult(heliosClient.listHosts(selectorSet));
        } else {
            hosts = getHeliosResult(heliosClient.listHosts());
        }
        return hosts;
    }

    @Override
    public ContainerCloudInstance startInstance(@NotNull String instanceId, @NotNull ContainerCloudImage image, @NotNull CloudInstanceUserData tag) {
        Job.Builder jobBuilder = Job.newBuilder()
                .setImage(image.getId())
                .setName("container-cloud-agent")
                .setVersion(instanceId);
        // Add all tag custom configuration as environment vars
        tag.getCustomAgentConfigurationParameters().forEach(jobBuilder::addEnv);
        Job jobDescriptor = jobBuilder.build();

        JobDeployResponse jobDeployResponse;
        String id;
        try {
            CreateJobResponse jobResponse = getHeliosResult(heliosClient.createJob(jobDescriptor));
            id = jobResponse.getId();

            List<String> jobCreationErrors = jobResponse.getErrors();
            if (!jobCreationErrors.isEmpty()) {
                String errorList = jobCreationErrors.stream().collect(Collectors.joining("\n"));
                throw new CloudException("Failed to create Helios job, errors were reported:\n" + errorList);
            }
            if (jobResponse.getStatus() != CreateJobResponse.Status.OK) {
                throw new CloudException("Failed to create Helios job, status is '" + jobResponse.getStatus() + "', not 'OK'");
            }

            String host = getRandomMatchingHost(); // TODO: Implement some more intelligent host picking strategy? (look at host stats like memory etc)
            LOG.debug("Deploying job " + id + " on host " + host);
            jobDeployResponse = getHeliosResult(heliosClient.deploy(Deployment.of(JobId.fromString(id), Goal.START), host));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudException("Failed to start Helios job", e);
        }

        if (jobDeployResponse.getStatus() != JobDeployResponse.Status.OK) {
            throw new CloudException("Helios job status is '" + jobDeployResponse.getStatus() + "' not 'OK'");
        }
        LOG.debug("Started Helios job " + id);

        ContainerCloudInstance cloudInstance = new ContainerCloudInstance(instanceId, image, this);
        instanceIdJobMap.put(instanceId, id);
        return cloudInstance;
    }

    @Override
    public void stopInstance(@NotNull ContainerCloudInstance instance) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instance.getInstanceId()));
            LOG.debug("Stopping Helios job " + jobId);
            JobStatus jobStatus = getHeliosResult(heliosClient.jobStatus(jobId));
            Set<String> hosts = jobStatus.getDeployments().keySet();

            for (String host : hosts) {
                LOG.debug("Undeploying " + jobId + " from " + host);
                JobUndeployResponse jobUndeployResponse = getHeliosResult(heliosClient.undeploy(jobId, host));
                if (jobUndeployResponse.getStatus() != JobUndeployResponse.Status.OK) {
                    throw new CloudException("Failed to undeploy Helios job, status " + jobUndeployResponse.getStatus());
                }
            }

            LOG.debug("Undeploy finished, deleting job " + jobId);
            JobDeleteResponse jobDeleteResponse = getHeliosResult(heliosClient.deleteJob(jobId));
            if (jobDeleteResponse.getStatus() != JobDeleteResponse.Status.OK) {
                throw new CloudException("Failed to remove Helios job, status " + jobDeleteResponse.getStatus());
            }

            LOG.debug("Finished stopping instance " + instance.getInstanceId());
            instanceIdJobMap.remove(instance.getInstanceId());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudException("Failed to stop instance", e);
        }
    }

    @Override
    public String getError(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = getHeliosResult(heliosClient.jobStatus(jobId));
            if (jobStatus == null) {
                LOG.warn("Trying to read error state of non-existent job " + jobId);
                return null;
            }
            Set<String> hostStatuses = jobStatus.getTaskStatuses().keySet();
            if (hostStatuses.isEmpty()) {
                LOG.debug("Trying to read error state before job is deployed for job " + jobId);
                return null;
            }

            // There really should only ever be one host that the job is deployed to, but let's be thorough
            StringBuilder sb = new StringBuilder();
            for (String host : hostStatuses) {
                TaskStatus status = jobStatus.getTaskStatuses().get(host);
                if (status.getContainerError() != null && !status.getContainerError().isEmpty()) {
                    sb.append(host).append(": ").append(status.getContainerError()).append("\n");
                }
            }
            return sb.toString();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudException("Failed to read error information from instance " + instanceId, e);
        }
    }

    @Override
    public String getNetworkIdentity(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = getHeliosResult(heliosClient.jobStatus(jobId));
            if (jobStatus == null) {
                LOG.warn("Trying to read network identity of non-existent job " + jobId.getName());
                return null;
            }

            String host = jobStatus.getTaskStatuses().keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new CloudException("Container " + instanceId + " not deployed on any host"));
            // TODO: How to do this?
            return null;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudException("Failed to read network information from instance " + instanceId, e);
        }
    }

    @Override
    public Date getStartedTime(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = getHeliosResult(heliosClient.jobStatus(jobId));
            if (jobStatus == null) {
                LOG.warn("Trying to read start time of non-existent job " + jobId.getName());
                return null;
            }

            String host = jobStatus.getTaskStatuses().keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new CloudException("Container " + instanceId + " not deployed on any host"));
            return new Date(jobStatus.getTaskStatuses().get(host).getJob().getCreated());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudException("Failed to read start time information from instance " + instanceId, e);
        }
    }

    @Override
    public InstanceStatus getStatus(String instanceId) {
        try {
            JobId jobId = JobId.fromString(instanceIdJobMap.get(instanceId));
            JobStatus jobStatus = getHeliosResult(heliosClient.jobStatus(jobId));
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
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Could not fetch state for " + instanceId, e);
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
