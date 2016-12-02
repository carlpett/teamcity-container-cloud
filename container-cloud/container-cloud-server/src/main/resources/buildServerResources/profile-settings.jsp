<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="constants" class="se.capeit.dev.containercloud.cloud.ContainerCloudConstants"/>

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
            Url to the Docker API endpoint, reachable from the Teamcity server. Can be either a unix domain socket
            (<pre>unix:///var/run/docker.sock</pre>) or a http(s) socket (<pre>https://remote-host</pre>)
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
            Label selectors used to filter hosts (eg <pre>role=builder</pre>). If empty, all hosts are used.
        </span>
    </td>
</tr>

<script type="text/javascript">
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
});
</script>