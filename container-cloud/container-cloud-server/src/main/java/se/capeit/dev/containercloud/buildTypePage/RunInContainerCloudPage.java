package se.capeit.dev.containercloud.buildTypePage;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.CloudProfile;
import jetbrains.buildServer.clouds.CloudProfileData;
import jetbrains.buildServer.clouds.server.CloudManager;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.web.openapi.*;
import org.jetbrains.annotations.NotNull;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RunInContainerCloudPage extends SimplePageExtension {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClientFactory.class.getName());

    @NotNull
    private final CloudManager cloudManager;

    public RunInContainerCloudPage(@NotNull WebControllerManager controllerManager, @NotNull ProjectManager projectManager,
                                   @NotNull PluginDescriptor descriptor, @NotNull final CloudManager cloudManager) {
        super(controllerManager, PlaceId.ADMIN_EDIT_BUILD_STEPS_ACTIONS_PAGE, "container-cloud", descriptor.getPluginResourcesPath("build-page.jsp"));
        //super("Cntnr", "Run in Container", controllerManager, projectManager);

        LOG.info("Created RunInContainerCloudPage");

        this.cloudManager = cloudManager;

        setPosition(PositionConstraint.after("buildFeatures"));
        register();

        //buildType.addRequirement(new Requirement(ContainerCloudConstants.AGENT_ENV_PARAMETER_IMAGE_ID, imageId, RequirementType.EQUALS));
        //buildType.addRequirement(new Requirement(ContainerCloudConstants.AGENT_ENV_PARAMETER_CLOUD_PROFILE_ID, profileId, RequirementType.EQUALS));
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        LOG.info("Filling RunInContainerCloudPage model");

        model.put("cloudProfiles", getContainerCloudProfiles());
    }

    private Map<String, String> getContainerCloudProfiles() {
        return cloudManager.listProfiles().stream()
                .filter(p -> Objects.equals(p.getCloudCode(), ContainerCloudConstants.CLOUD_CODE))
                .collect(Collectors.toMap(CloudProfile::getProfileId, CloudProfileData::getProfileName));
    }
}
