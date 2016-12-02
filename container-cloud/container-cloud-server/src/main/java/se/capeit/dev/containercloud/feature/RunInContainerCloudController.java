package se.capeit.dev.containercloud.feature;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.CloudProfile;
import jetbrains.buildServer.clouds.CloudProfileData;
import jetbrains.buildServer.clouds.server.CloudManager;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import se.capeit.dev.containercloud.cloud.ContainerCloudConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.stream.Collectors;

public class RunInContainerCloudController extends BaseController {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    @NotNull
    private final CloudManager cloudManager;
    private final PluginDescriptor pluginDescriptor;

    public RunInContainerCloudController(@NotNull SBuildServer server,
                                         @NotNull PluginDescriptor pluginDescriptor,
                                         @NotNull WebControllerManager webControllerManager,
                                         @NotNull CloudManager cloudManager) {
        super(server);

        this.cloudManager = cloudManager;
        this.pluginDescriptor = pluginDescriptor;

        webControllerManager.registerController(pluginDescriptor.getPluginResourcesPath(RunInContainerCloudConstants.FeatureSettingsHtmlFile), this);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse) throws Exception {
        LOG.info("Controller doing Handle");
        ModelAndView mv = new ModelAndView(pluginDescriptor.getPluginResourcesPath(RunInContainerCloudConstants.FeatureSettingsJspFile));

        mv.getModel().put("cloudProfiles", getProfiles());

        return mv;
    }

    private Map<String, String> getProfiles() {
        LOG.info("Controller getting profiles");
        return cloudManager.listProfiles().stream()
                .filter(cloudProfile -> cloudProfile.getCloudCode().equals(ContainerCloudConstants.CloudCode))
                .collect(Collectors.toMap(CloudProfile::getProfileId, CloudProfileData::getProfileName));
    }
}
