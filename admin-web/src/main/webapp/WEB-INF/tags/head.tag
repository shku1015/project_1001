<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="title" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- ②③ 공통 <head>: Bootstrap 5 + Tabler (ADR-0013) --%>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><c:out value="${title}"/> - 관리자 서비스</title>
<link rel="stylesheet" href="<c:url value='/webjars/tabler__core/${tablerVersion}/dist/css/tabler.min.css'/>">
<link rel="stylesheet" href="<c:url value='/common/css/admin.css'/>">
