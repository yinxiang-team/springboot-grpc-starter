package com.yinxiang.microservice.grpc.doc;

import com.yinxiang.microservice.grpc.generator.BaseContentBuilder;
import com.yinxiang.microservice.grpc.generator.infos.*;

/**
 * The doc html file builder.
 * @author Huiyuan Fu
 */
final class HtmlBuilder extends BaseContentBuilder {
  @Override
  protected String buildBegin(ServiceInfo serviceInfo) {
    serviceInfo.setFileName(serviceInfo.getName());
    return "<html>\n<head>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n\t<title>"
            + serviceInfo.getName() + " gRPC-API</title>\n</head>\n<body>\n";
  }

  @Override
  protected String buildMethod(MethodInfo methodInfo) {
    StringBuilder builder = new StringBuilder();
    builder.append("\t<table align=\"center\">\n\t\t<tr bgcolor=\"#FF7D0A\">\n\t\t\t<th colspan=\"2\">")
            .append(methodInfo.getComment())
            .append("</th>\n\t\t</tr>\n");
    builder.append("\t\t<tr>\n\t\t\t<td bgcolor=\"#69CCF0\">PATH</td>\n\t\t\t<td bgcolor=\"#FFF569\">")
            .append(methodInfo.getServiceInfo().getUrl())
            .append(methodInfo.getPath())
            .append("</td>\n\t\t<tr>\n");
    builder.append("\t\t<tr>\n\t\t\t<td bgcolor=\"#69CCF0\">METHOD</td>\n\t\t\t<td bgcolor=\"#FFF569\">")
            .append(methodInfo.getMethod())
            .append("</td>\n\t\t<tr>\n");
    builder.append("\t\t<tr>\n\t\t\t<td bgcolor=\"#69CCF0\">HEADER</td>\n\t\t\t<td bgcolor=\"#FFF569\">\n")
            .append("\t\t\t\t<table border=\"1\">\n\t\t\t\t\t<tr>\n\t\t\t\t\t\t");
    methodInfo.getHeaders().forEach(header -> builder.append("<td>").append(header).append("</td>"));
    builder.append("\n\t\t\t\t\t</tr>\n\t\t\t\t</table>\n\t\t\t</td>\n\t\t<tr>\n");
    builder.append("\t\t<tr>\n\t\t\t<td bgcolor=\"#69CCF0\">BODY</td>\n\t\t\t<td bgcolor=\"#00FF96\">\n\t\t\t\t<pre>\n");
    appendMessage(builder, methodInfo.getInput(), 1);
    builder.append("</pre>\n\t\t</td>\n\t\t<tr>\n\t\t\t<td bgcolor=\"#69CCF0\">RESPONSE</td>\n\t\t\t<td bgcolor=\"#00FF96\">\n\t\t\t\t<pre>\n");
    appendMessage(builder, methodInfo.getOutput(), 1);
    builder.append("</pre>\n\t\t</td>\n\t</table>\n\t<HR align=center width=300color=#987cb9 SIZE=1>\n");
    return builder.toString();
  }

  @Override
  protected String buildEnd(ServiceInfo serviceInfo) {
    return "\n</body>\n</html>";
  }
}
