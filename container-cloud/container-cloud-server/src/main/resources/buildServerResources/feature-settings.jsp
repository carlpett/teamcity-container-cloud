<%@ page import="se.capeit.dev.containercloud.feature.RunInContainerCloudConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="cloudProfiles" type="java.util.Map<java.lang.String, java.lang.String>" scope="request"/>

<c:set var="profileParamName" value="<%=RunInContainerCloudConstants.ParameterName_CloudProfile%>"/>
<tr>
  <th><label for="${profileParamName}">Container cloud profile</label></th>
  <td>
    <props:selectProperty name="${profileParamName}"
                          enableFilter="false"
                          className="mediumField">
      <c:forEach items="${cloudProfiles}" var="profile">
          <props:option value="${profile.key}">${profile.value}</props:option>
      </c:forEach>
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