<%@ page import="se.capeit.dev.containercloud.feature.RunInContainerCloudConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="profileParamName" value="<%=RunInContainerCloudConstants.ParameterName_CloudProfile%>"/>
<tr>
  <th><label for="${profileParamName}">Container cloud profile</label></th>
  <td>
    <props:selectProperty name="${profileParamName}"
                          enableFilter="false"
                          className="mediumField">
      <props:option value="some-profile">Profile selector</props:option>
    </props:selectProperty>
  </td>
</tr>
<c:set var="imageParamName" value="<%=RunInContainerCloudConstants.ParameterName_Image%>"/>
<tr>
  <th><label for="${imageParamName}">Image</label></th>
  <td>
    <props:textProperty name="${imageParamName}" className="longField" />
  </td>
</tr>