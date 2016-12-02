<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="constants" class="se.capeit.dev.containercloud.feature.RunInContainerCloudConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="cloudProfiles" scope="request" type="java.util.Map<java.lang.String, java.lang.String>" />

<tr>
  <th><label for="${constants.parameterName_CloudProfile}">Container cloud profile</label></th>
  <td>
    <props:selectProperty name="${constants.parameterName_CloudProfile}"
                          enableFilter="false"
                          className="mediumField">
      <c:forEach items="${cloudProfiles}" var="profile">
          <props:option value="${profile.key}">${profile.value}</props:option>
      </c:forEach>
    </props:selectProperty>
  </td>
</tr>

<tr>
  <th><label for="${constants.parameterName_Image}">Image</label></th>
  <td>
    <props:textProperty name="${constants.parameterName_Image}" className="longField" />
  </td>
</tr>