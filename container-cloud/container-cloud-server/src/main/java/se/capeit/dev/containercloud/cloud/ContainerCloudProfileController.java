package se.capeit.dev.containercloud.cloud;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.crypt.RSACipher;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import se.capeit.dev.containercloud.cloud.providers.TestConnectionResult;
import se.capeit.dev.containercloud.cloud.providers.ContainerProvider;
import se.capeit.dev.containercloud.cloud.providers.ContainerProviderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class ContainerCloudProfileController extends BaseFormXmlController {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(ContainerCloudClient.class.getName());

    public ContainerCloudProfileController(@NotNull SBuildServer server,
                                           @NotNull PluginDescriptor pluginDescriptor,
                                           @NotNull WebControllerManager webControllerManager) {
        super(server);

        webControllerManager.registerController(pluginDescriptor.getPluginResourcesPath(ContainerCloudConstants.ProfileSettingsTestConnectionPath), this);
    }

    @Override
    protected ModelAndView doGet(@NotNull HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse) {
        return null;
    }

    @Override
    protected void doPost(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Element xmlResponse) {
        final BasePropertiesBean propsBean = new BasePropertiesBean(null);
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true);

        final Map<String, String> properties = propsBean.getProperties();

        xmlResponse.addContent(testConnection(properties));
    }

    private Element testConnection(Map<String, String> properties) {
        CloudClientParameters cloudClientParameters = new CloudClientParameters();
        properties.entrySet().forEach(entry -> cloudClientParameters.setParameter(entry.getKey(), entry.getValue()));

        ContainerProvider provider = ContainerProviderFactory.getProvider(cloudClientParameters);
        TestConnectionResult testConnectionResult = provider.testConnection();

        Element results = new Element("testConnectionResults");

        Element success = new Element("ok");
        success.setText(Boolean.toString(testConnectionResult.isOk()));
        results.addContent(success);

        if (!testConnectionResult.getMessages().isEmpty()) {
            Element messages = new Element("messages");
            for (TestConnectionResult.Message message : testConnectionResult.getMessages()) {
                Element messageElement = new Element("message");
                messageElement.setText(message.getMessage());
                messageElement.setAttribute("level", message.getLevel().name());
                messages.addContent(messageElement);
            }
            results.addContent(messages);
        }

        return results;
    }
}

// Copied from https://github.com/JetBrains/teamcity-azure-plugin/blob/master/plugin-azure-server-base/src/main/java/jetbrains/buildServer/clouds/azure/utils/PluginPropertiesUtil.java
class PluginPropertiesUtil {
    private final static String PROPERTY_PREFIX = "prop:";
    private static final String ENCRYPTED_PROPERTY_PREFIX = "prop:encrypted:";

    private PluginPropertiesUtil() {
    }

    public static void bindPropertiesFromRequest(HttpServletRequest request, BasePropertiesBean bean) {
        bindPropertiesFromRequest(request, bean, false);
    }

    static void bindPropertiesFromRequest(HttpServletRequest request, BasePropertiesBean bean, boolean includeEmptyValues) {
        bean.clearProperties();

        for (final Object o : request.getParameterMap().keySet()) {
            String paramName = (String) o;
            if (paramName.startsWith(PROPERTY_PREFIX)) {
                if (paramName.startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
                    setEncryptedProperty(paramName, request, bean, includeEmptyValues);
                } else {
                    setStringProperty(paramName, request, bean, includeEmptyValues);
                }
            }
        }
    }

    private static void setStringProperty(final String paramName, final HttpServletRequest request,
                                          final BasePropertiesBean bean, final boolean includeEmptyValues) {
        String propName = paramName.substring(PROPERTY_PREFIX.length());
        final String propertyValue = request.getParameter(paramName).trim();
        if (includeEmptyValues || propertyValue.length() > 0) {
            bean.setProperty(propName, toUnixLineFeeds(propertyValue));
        }
    }

    private static void setEncryptedProperty(final String paramName, final HttpServletRequest request,
                                             final BasePropertiesBean bean, final boolean includeEmptyValues) {
        String propName = paramName.substring(ENCRYPTED_PROPERTY_PREFIX.length());
        String propertyValue = RSACipher.decryptWebRequestData(request.getParameter(paramName));
        if (propertyValue != null && (includeEmptyValues || propertyValue.length() > 0)) {
            bean.setProperty(propName, toUnixLineFeeds(propertyValue));
        }
    }

    private static String toUnixLineFeeds(final String str) {
        return str.replace("\r", "");
    }
}
