<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="constants" class="se.capeit.dev.containercloud.cloud.ContainerCloudConstants"/>

<style type="text/css">
.inline-code {
    display: inline;
    font-family: monospace;
}

.flex-image-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 31em;
}

#test-connection-spinner {
    height: 12px;
    width: 12px;
    align-self: center;
    margin-right: 4px;
}

#test-connection-messages-list {
    list-style: none;
    padding-left: 2em;
}

#test-connection-messages-list > li {
    display: flex;
}
.test-connection-message {
    margin-left: 0.5em;
}

#container-images > option {
    padding-left: 0.2em;
    padding-top: 0.2em;
}
</style>

<tr>
    <th><label for="${constants.profileParameterName_ContainerProvider}">Container provider</label></th>
    <td>
        <props:selectProperty name="${constants.profileParameterName_ContainerProvider}"
                              enableFilter="false"
                              id="container-provider-selector"
                              className="mediumField">
            <props:option value="${constants.profileParameterValue_ContainerProvider_DockerSocket}">Docker socket</props:option>
            <props:option value="${constants.profileParameterValue_ContainerProvider_Helios}">Helios</props:option>
        </props:selectProperty>
    </td>
</tr>

<tr class="container-cloud_provider-settings container-cloud_provider-settings_${constants.profileParameterValue_ContainerProvider_DockerSocket}">
    <th><label for="${constants.profileParameterName_DockerSocket_Endpoint}">Endpoint</label></th>
    <td>
        <props:textProperty name="${constants.profileParameterName_DockerSocket_Endpoint}"
                            className="longField" />
        <span class="error" id="error_${constants.profileParameterName_DockerSocket_Endpoint}"></span>
        <span class="smallNote">
            <p>Url to the Docker API endpoint, reachable from the Teamcity server. Can be either a unix domain socket (<span class="inline-code">unix:///var/run/docker.sock</span>) or a http(s) socket (<span class="inline-code">https://remote-host</span>).</p>
            <p>If not specified, will use the <span class="inline-code">DOCKER_HOST</span> environment variable.</p>
        </span>
    </td>
</tr>

<tr class="container-cloud_provider-settings container-cloud_provider-settings_${constants.profileParameterValue_ContainerProvider_Helios}">
    <th>
        <label for="${constants.profileParameterName_Helios_MasterUrl}">Master url <l:star/></label>
    </th>
    <td>
        <props:textProperty name="${constants.profileParameterName_Helios_MasterUrl}"
                            className="longField" />
        <span class="error" id="error_${constants.profileParameterName_Helios_MasterUrl}"></span>
        <span class="smallNote">
            Url to a Helios master, or a Helios cluster
        </span>
    </td>
</tr>
<tr class="container-cloud_provider-settings container-cloud_provider-settings_${constants.profileParameterValue_ContainerProvider_Helios}">
    <th>
        <label for="${constants.profileParameterName_Helios_HostNamePattern}">Host name pattern</label>
    </th>
    <td>
        <props:textProperty name="${constants.profileParameterName_Helios_HostNamePattern}"
                            className="longField" />
        <span class="error" id="error_${constants.profileParameterName_Helios_HostNamePattern}"></span>
        <span class="smallNote">
            Host name pattern used to filter hosts, using substring matching. If empty, all hosts are used.
        </span>
    </td>
</tr>
<tr class="container-cloud_provider-settings container-cloud_provider-settings_${constants.profileParameterValue_ContainerProvider_Helios}">
    <th>
        <label for="${constants.profileParameterName_Helios_HostSelectors}">Host label selectors</label>
    </th>
    <td>
        <props:textProperty name="${constants.profileParameterName_Helios_HostSelectors}"
                            className="longField" />
        <span class="error" id="error_${constants.profileParameterName_Helios_HostSelectors}"></span>
        <span class="smallNote">
            Label selectors used to filter hosts (eg <span class="inline-code">role=builder</span>). If empty, all hosts are used.
        </span>
    </td>
</tr>

<tr>
    <th><label for="${constants.profileParameterName_Images}">Container images</label></th>
    <td>
        <c:set var="images" value="${propertiesBean.properties[constants.profileParameterName_Images]}"/>
        <input type="hidden" name="prop:${constants.profileParameterName_Images}" id="${constants.profileParameterName_Images}-backing-field"  data-err-id="error_${constants.profileParameterName_Images}" value="<c:out value='${images}'/>"/>
        <div class="flex-image-container">
            <input type="text" class="mediumField textProperty" id="add-image-staging" />
            <!-- This emulates the forms:addButton tag, which for some reason does not play nice with jquery events -->
            <a class="btn" id="add-container-image">
                <span>Add image</span><!-- removed classes: icon_before icon16 addNew -->
            </a>
        </div>
        <span class="error" id="error_${constants.profileParameterName_Images}"></span>

        <div class="flex-image-container">
            <select id="container-images" multiple="multiple" class="mediumField">
            </select>
            <a class="btn" id="remove-container-images">
                <span>Remove image(s)</span>
            </a>
        </div>

        <span class="smallNote">
            Images available from this provider. Note that if images are specified in a "Run in Container Cloud" Build
            feature for a build configuration, these will be added here as well.
        </span>
    </td>
</tr>

<tr>
    <th>Test connection</th>
    <td>
        <div style="display: flex; align-items: center;">
            <div style="padding-right: 1em;">
                <a class="btn" id="test-connection-button">
                    <span>Test connection</span>
                </a>
            </div>
            <div id="test-connection-spinner-container">
                <span id="test-connection-spinner" class="icon-refresh icon-spin progressRing"></span>
                Testing...
            </div>
            <div id="test-connection-results-status">
                <div id="test-connection-status-success">
                    <span id="test-connection-success-icon" class="tc-icon icon16 build-status-icon_successful"></span>
                    Success!
                </div>
                <div id="test-connection-status-error">
                    <span id="test-connection-success-icon" class="tc-icon icon16 build-status-icon_error"></span>
                    Connection test was not successful
                </div>
            </div>
        </div>
        <div id="test-connection-results-messages">
            <ul id="test-connection-messages-list"></ul>
        </div>
    </td>
</tr>

<script type="text/javascript">
// source-id below is the field name for image id in CloudImageParameters
function addExistingImages() {
    var imagesRaw = document.getElementById("${constants.profileParameterName_Images}-backing-field").value;
    var images = JSON.parse(imagesRaw);

    var imageListBox = document.getElementById("container-images");
    images.forEach(function(imageDef) {
        imageListBox.append(new Option(imageDef["source-id"], imageDef["source-id"]));
    });
}

function saveToBackingField() {
    var imagesRaw = $j("#container-images > option").map(function() { return { "source-id": this.value }; }).toArray();
    var images = JSON.stringify(imagesRaw);
    document.getElementById("${constants.profileParameterName_Images}-backing-field").value = images;
}

$j(document).ready(function() {
    $j("#container-provider-selector").change(function() {
        var selected = $j(":selected", this).val();
        $j(".container-cloud_provider-settings").each(function(idx, elem) {
            if($j(elem).hasClass("container-cloud_provider-settings_" + selected)) {
                $j(elem).show();
            } else {
                $j(elem).hide();
            }
        });
    });
    $j("#container-provider-selector").change();

    $j("#add-container-image").click(function() {
        var image = document.getElementById("add-image-staging").value;
        // Validate
        if(!/${constants.containerImageRegex}/.test(image)) {
            document.getElementById("error_${constants.profileParameterName_Images}").innerHTML = "Image must have format owner/image:version or repo-domain/owner/image:version (note that upper-case letters are not allowed)";
            return;
        }
        document.getElementById("error_${constants.profileParameterName_Images}").innerHTML = "";
        // TODO: Validate unique?

        // Add to display
        document.getElementById("container-images").append(new Option(image, image));
        saveToBackingField();

        // Clear text box
        document.getElementById("add-image-staging").value = "";
    });

    $j("#remove-container-images").click(function() {
        var imageListBox = document.getElementById("container-images");
        imageListBox.getElementsBySelector(":selected").forEach(function(elem) {
            elem.remove();
        });

        saveToBackingField();
    });

    // Set initially hidden
    $j("#test-connection-results-status").hide();
    $j("#test-connection-results-messages").hide();
    $j("#test-connection-spinner-container").hide();
    var icons = {
        "INFO": "newVersionLink",
        "WARNING": "yellowTriangle",
        "ERROR": "build-status-icon_error"
    };
    $j("#test-connection-button").click(function() {
        $j("#test-connection-results-status").hide();
        $j("#test-connection-results-messages").hide();
        $j("#test-connection-spinner-container").show();

        // Copied from Vmware cloud plugin, https://github.com/JetBrains/teamcity-vmware-plugin/blob/master/cloud-vmware-server/src/main/resources/buildServerResources/vmware-settings.js
        var url = '<c:url value="${teamcityPluginResourcesPath}${constants.profileSettingsTestConnectionPath}" />';
        BS.ajaxRequest(url, {
            parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
            onFailure: function (response) {
                $j("#test-connection-status-success").hide();
                $j("#test-connection-status-error").show();

                $j("#test-connection-messages-list").empty();
                messages.children().each(function(message) {
                    $j("#test-connection-messages-list").append("<li>" + response.getStatusText() + "</li>");
                });
            },
            onSuccess: function (response) {
                var result = $j(response.responseXML),
                    isOk = result.find("ok"),
                    messages = result.find("messages");

                if (isOk.text() === "true") {
                    $j("#test-connection-status-success").show();
                    $j("#test-connection-status-error").hide();
                } else {
                    $j("#test-connection-status-success").hide();
                    $j("#test-connection-status-error").show();
                }

                $j("#test-connection-messages-list").empty();
                if(messages.length != 0) {
                    messages.children().each(function(idx, message) {
                        var listItem = $j("<li></li>");
                        var icon = icons[$j(message).attr("level")];
                        listItem.append("<div class=\"tc-icon icon_before icon16 " + icon + "\"></div>");
                        listItem.append("<div class=\"test-connection-message\">" + $j(message).text() + "</div>");
                        $j("#test-connection-messages-list").append(listItem);
                    });
                    $j("#test-connection-results-messages").show();
                }

                $j("#test-connection-spinner-container").hide();
                $j("#test-connection-results-status").show();
            }
        });
    });

    addExistingImages();
});
</script>