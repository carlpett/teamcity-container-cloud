package se.capeit.dev.containercloud.feature;

public final class RunInContainerCloudConstants {
    public static final String TYPE = "RunInContainerCloud";
    public static final String FeatureSettingsJspFile = "feature-settings.jsp";
    public static final String FeatureSettingsHtmlFile = "feature-settings.html";

    public static final String ParameterName_CloudProfile = "CloudProfile";
    public static final String ParameterName_Image = "Image";

    public String getParameterName_CloudProfile() { return ParameterName_CloudProfile; }
    public String getParameterName_Image() { return ParameterName_Image; }
}
