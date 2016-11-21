<%@ page import="se.capeit.dev.containercloud.cloud.ContainerCloudConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="paramName" value="<%=ContainerCloudConstants.CONTAINER_PROVIDER_SETTING%>"/>

<tr>
  <th><label for="${paramName}">Container provider</label></th>
  <td>
    <props:selectProperty name="${paramName}"
                          enableFilter="false"
                          id="container-provider-selector"
                          className="mediumField">
      <props:option value="docker-socket">Docker socket</props:option>
      <props:option value="helios">Helios</props:option>
    </props:selectProperty>
  </td>
</tr>