<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%@page import="app.hongs.Cnst"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.CoreLocale"%>
<%@page import="java.io.PrintStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%
    // 如果有内部返回, 则不要显示此页
    if (request.getAttribute(Cnst.RESP_ATTR) != null) {
        return;
    }
%>
<!doctype html>
<html>
    <head>
        <title>HongsCORE::Login</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="icon" type="image/x-icon" href="favicon.ico"/>
        <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/static/assets/css/hongscore.min.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="<%=request.getContextPath()%>/static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="<%=request.getContextPath()%>/static/assets/jquery.min.js"></script>
        <style type="text/css">
            #footbox.navbar, body, .jumbotron, .container
                { background-color: #0071AD; color: #fff; border: 0; }
            #footbox blockquote
                { background-color: #0071AD; color: #fff; }
            h1, h3, pre
                { font-weight: bold; }
            pre
                { background-color: transparent; color: #ddd; border: 0; }
        </style>
    </head>
    <body>
        <div class="jumbotron">
            <div class="container">
                <h1>: (</h1>
                <h3><%=CoreLocale.getInstance().translate("core.error.no.power")%>! <a href="<%=request.getContextPath()%>/" style="color: #ace;"><%=CoreLocale.getInstance().translate("core.error.go.index")%></a>.</h3>
            </div>
        </div>
        <nav id="footbox" class="navbar navbar-fixed-bottom">
            <div class="container">
                <blockquote><p>Copyleft &copy; 2015 黄弘. <small class="pull-right">Powered by <a href="https://github.com/ihongs/HongsCORE/" target="_blank">HongsCORE</a>, and <a href="static/power.html" target="_blank">others</a>.</small></p></blockquote>
            </div>
        </nav>
    </body>
</html>