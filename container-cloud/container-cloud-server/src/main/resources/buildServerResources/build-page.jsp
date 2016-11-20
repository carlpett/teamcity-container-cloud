<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

ENABLE-CHECKBOX-HERE

<c:forEach items="${cloudProfiles}" var="profile">
  <p>${profile.key} -&lt; ${profile.value}</p>
</c:forEach>

HERE IS END