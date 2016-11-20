/*package se.capeit.dev.containercloud.buildTypePage;

import com.intellij.openapi.diagnostic.Logger;
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
import se.capeit.dev.containercloud.feature.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RunInContainerCloudController extends BaseController {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    private final PluginDescriptor pluginDescriptor;
    private final CloudManager cloudManager;

    public RunInContainerCloudController(SBuildServer server,
                                         WebControllerManager controllerManager,
                                         final PluginDescriptor pluginDescriptor,
                                         final CloudManager cloudManager) {
        super(server);
        //controllerManager.registerController(pluginDescriptor.getPluginResourcesPath(se.capeit.dev.containercloud.feature.RunInContainerCloudConstants.FeatureSettingsHtmlFile), this);

        this.pluginDescriptor = pluginDescriptor;
        this.cloudManager = cloudManager;
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest httpServletRequest,
                                    @NotNull HttpServletResponse httpServletResponse) throws Exception {
        ModelAndView mv = new ModelAndView(pluginDescriptor.getPluginResourcesPath(se.capeit.dev.containercloud.feature.RunInContainerCloudConstants.FeatureSettingsJspFile));
        mv.getModel().put("cloudProfiles", getContainerCloudProfiles());
        return mv;
    }

    private Map<String, String> getContainerCloudProfiles() {
        return cloudManager.listProfiles().stream()
                .filter(p -> Objects.equals(p.getCloudCode(), ContainerCloudConstants.CLOUD_CODE))
                .collect(Collectors.toMap(p -> p.getProfileId(), p -> p.getProfileName()));
    }
}
*/