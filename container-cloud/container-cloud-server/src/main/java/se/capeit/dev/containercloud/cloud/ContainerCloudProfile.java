package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudImageParameters;
import jetbrains.buildServer.clouds.CloudProfile;
import jetbrains.buildServer.clouds.CloudProfileData;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class ContainerCloudProfile implements CloudProfile {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private final String profileId;
    @NotNull
    private final CloudProfileData profileData;

    public ContainerCloudProfile(String profileId, @NotNull CloudProfileData profileData) {
        this.profileId = profileId;
        this.profileData = profileData;

        LOG.info("Constructed profile!");
    }

    public synchronized void addImage(String containerImageId) {
        Collection<CloudImageParameters> imagesParameters = getImagesParameters();
        if (imagesParameters.stream().anyMatch(img -> img.getId() == containerImageId)) {
            LOG.info("Image " + containerImageId + " is already present in profile " + getProfileId());
            return;
        }

        CloudImageParameters newParameters = new CloudImageParameters();
        imagesParameters.add(newParameters);
        LOG.info("Added " + containerImageId + " to profile " + getProfileId());
    }

    @NotNull
    @Override
    public String getProfileId() {
        return profileId;
    }

    @NotNull
    @Override
    public String profileDescription() {
        return getDescription();
    }

    @NotNull
    @Override
    public String getCloudCode() {
        return profileData.getCloudCode();
    }

    @NotNull
    @Override
    public String getProfileName() {
        return profileData.getProfileName();
    }

    @NotNull
    @Override
    public String getDescription() {
        return profileData.getDescription();
    }

    @NotNull
    @Override
    public CloudClientParameters getParameters() {
        return profileData.getParameters();
    }

    @Nullable
    @Override
    public Long getTerminateIdleTime() {
        return profileData.getTerminateIdleTime();
    }

    @Override
    public boolean getTerminateAgentAfterFirstBuild() {
        return profileData.getTerminateAgentAfterFirstBuild();
    }

    @Override
    public Collection<CloudImageParameters> getImagesParameters() {
        return profileData.getImagesParameters();
    }

    @Override
    public boolean isEnabled() {
        return profileData.isEnabled();
    }
}